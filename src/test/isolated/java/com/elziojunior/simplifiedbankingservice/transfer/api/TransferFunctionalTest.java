package com.elziojunior.simplifiedbankingservice.transfer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.elziojunior.simplifiedbankingservice.api.ApiExceptionHandler;
import com.elziojunior.simplifiedbankingservice.api.TransferController;
import com.elziojunior.simplifiedbankingservice.api.TransferTokenController;
import com.elziojunior.simplifiedbankingservice.configuration.SecurityConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateTransferDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferMapperImpl;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferTokenMapperImpl;
import com.elziojunior.simplifiedbankingservice.service.CreateTransferService;
import com.elziojunior.simplifiedbankingservice.service.IssueTransferTokenService;
import com.elziojunior.simplifiedbankingservice.exception.TransferConflictException;
import com.elziojunior.simplifiedbankingservice.exception.TransferNotFoundException;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;

import java.util.function.Supplier;

@WebMvcTest({TransferTokenController.class, TransferController.class})
@Import({
        ApiExceptionHandler.class,
        SecurityConfiguration.class,
        TransferMapperImpl.class,
        TransferTokenMapperImpl.class
})
class TransferFunctionalTest {

    private static final UUID TOKEN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRANSFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueTransferTokenService issueTransferTokenService;

    @MockitoBean
    private CreateTransferService createTransferService;

    @MockitoBean
    private ApiMetrics apiMetrics;

    @BeforeEach
    void executeObservedOperation() {
        when(apiMetrics.observe(any(ApiOperation.class), any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(1).get());
    }

    /** Proves token issuance is public during the temporary authentication exception. */
    @Test
    void shouldIssueTokenWithoutAuthenticationOrCsrf() throws Exception {
        when(issueTransferTokenService.issue()).thenReturn(new IssuedTransferTokenDto(
                TOKEN, OffsetDateTime.parse("2026-08-31T14:10:00Z")));

        mockMvc.perform(post("/api/v1/transfer-tokens"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(TOKEN.toString()))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-31T14:10:00Z"));
    }

    /** Proves transfer execution maps the public header, body, and completed response. */
    @Test
    void shouldCreateTransferWithoutAuthenticationOrCsrf() throws Exception {
        CreateTransferDto input = new CreateTransferDto(TOKEN, 1L, 2L, new BigDecimal("12.345"));
        when(createTransferService.create(input)).thenReturn(
                new CompletedTransferDto(TRANSFER_ID, 1L, 2L, new BigDecimal("12.34")));

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":1,"destinationAccountId":2,"amount":12.345}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(TRANSFER_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sourceAccountId").value(1))
                .andExpect(jsonPath("$.destinationAccountId").value(2))
                .andExpect(jsonPath("$.amount").value(12.34));

        verify(createTransferService).create(input);
    }

    /** Proves missing and malformed idempotency headers receive safe failures. */
    @Test
    void shouldRejectInvalidIdempotencyHeaders() throws Exception {
        assertTransferProblem(null, "Invalid transfer request", "The Idempotency-Key header is required.", 400);
        assertTransferProblem("not-a-uuid", "Invalid transfer request", "The Idempotency-Key header is invalid.", 400);

        verify(createTransferService, never()).create(any());
    }

    /** Proves bean validation rejects invalid transfer payloads before service invocation. */
    @Test
    void shouldRejectInvalidTransferPayload() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\":1,\"destinationAccountId\":2,\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("The request is invalid."));

        verify(createTransferService, never()).create(any());
    }

    /** Proves known transfer failures map to stable public statuses without internals. */
    @Test
    void shouldTranslateKnownTransferFailures() throws Exception {
        when(createTransferService.create(any()))
                .thenThrow(new TransferNotFoundException("A transfer account does not exist."))
                .thenThrow(new TransferConflictException("The transfer conflicts with current state."))
                .thenThrow(new TransientDataAccessResourceException("database detail"));

        assertTransferProblem(TOKEN.toString(), "Transfer account not found",
                "A transfer account does not exist.", 404);
        assertTransferProblem(TOKEN.toString(), "Transfer conflict",
                "The transfer conflicts with current state.", 409);
        assertTransferProblem(TOKEN.toString(), "Transfer temporarily unavailable",
                "The transfer could not be completed because persistence is unavailable.", 503);
    }

    private void assertTransferProblem(String token, String title, String detail, int statusCode) throws Exception {
        var request = post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceAccountId\":1,\"destinationAccountId\":2,\"amount\":10}");
        if (token != null) {
            request.header("Idempotency-Key", token);
        }

        mockMvc.perform(request)
                .andExpect(status().is(statusCode))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(statusCode))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.detail").value(detail));
    }
}

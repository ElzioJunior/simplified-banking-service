package com.elziojunior.simplifiedbankingservice.transfer.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.elziojunior.simplifiedbankingservice.api.ApiExceptionHandler;
import com.elziojunior.simplifiedbankingservice.api.TransferController;
import com.elziojunior.simplifiedbankingservice.api.TransferTokenController;
import com.elziojunior.simplifiedbankingservice.configuration.ApiMetricsConfiguration;
import com.elziojunior.simplifiedbankingservice.configuration.SecurityConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateTransferDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferMapperImpl;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferTokenMapperImpl;
import com.elziojunior.simplifiedbankingservice.service.TransferService;
import com.elziojunior.simplifiedbankingservice.service.TransferTokenService;
import com.elziojunior.simplifiedbankingservice.exception.TransferConflictException;
import com.elziojunior.simplifiedbankingservice.exception.TransferNotFoundException;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetricsInterceptor;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@WebMvcTest({TransferTokenController.class, TransferController.class})
@Import({
        ApiExceptionHandler.class,
        ApiMetrics.class,
        ApiMetricsConfiguration.class,
        ApiMetricsInterceptor.class,
        SecurityConfiguration.class,
        TransferFunctionalTest.MetricsTestConfiguration.class,
        TransferMapperImpl.class,
        TransferTokenMapperImpl.class
})
class TransferFunctionalTest {

    private static final UUID TOKEN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRANSFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferTokenService transferTokenService;

    @MockitoBean
    private TransferService transferService;

    @Autowired
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void clearMetrics() {
        meterRegistry.clear();
    }

    /** Proves token issuance is public during the temporary authentication exception. */
    @Test
    void shouldIssueTokenWithoutAuthenticationOrCsrf() throws Exception {
        when(transferTokenService.issueTransferToken()).thenReturn(new IssuedTransferTokenDto(
                TOKEN, OffsetDateTime.parse("2026-08-31T14:10:00Z")));

        mockMvc.perform(post("/api/v1/transfer-tokens"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(TOKEN.toString()))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-31T14:10:00Z"));

        assertRequestMetrics(ApiOperation.TRANSFER_TOKEN_ISSUE, "successful", 1);
    }

    /** Proves transfer execution maps the public header, body, and completed response. */
    @Test
    void shouldCreateTransferWithoutAuthenticationOrCsrf() throws Exception {
        CreateTransferDto input = new CreateTransferDto(TOKEN, 1L, 2L, new BigDecimal("12.345"));
        when(transferService.createTransfer(input)).thenReturn(
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

        verify(transferService).createTransfer(input);
        assertRequestMetrics(ApiOperation.TRANSFER_CREATE, "successful", 1);
    }

    /** Proves missing and malformed idempotency headers receive safe failures. */
    @Test
    void shouldRejectInvalidIdempotencyHeaders() throws Exception {
        assertTransferProblem(null, "Invalid transfer request", "The Idempotency-Key header is required.", 400);
        assertTransferProblem("not-a-uuid", "Invalid transfer request", "The Idempotency-Key header is invalid.", 400);

        verify(transferService, never()).createTransfer(any());
        assertRequestMetrics(ApiOperation.TRANSFER_CREATE, "rejected", 2);
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

        verify(transferService, never()).createTransfer(any());
        assertRequestMetrics(ApiOperation.TRANSFER_CREATE, "rejected", 1);
    }

    /** Proves known transfer failures map to stable public statuses without internals. */
    @Test
    void shouldTranslateKnownTransferFailures() throws Exception {
        when(transferService.createTransfer(any()))
                .thenThrow(new TransferNotFoundException("A transfer account does not exist."))
                .thenThrow(new TransferConflictException("The transfer conflicts with current state."))
                .thenThrow(new TransientDataAccessResourceException("database detail"));

        assertTransferProblem(
                TOKEN.toString(), "Transfer account not found", "A transfer account does not exist.", 404);
        assertTransferProblem(TOKEN.toString(), "Transfer conflict", "The transfer conflicts with current state.", 409);
        assertTransferProblem(TOKEN.toString(), "Transfer temporarily unavailable",
                "The transfer could not be completed because persistence is unavailable.", 503);

        assertThat(meterRegistry.counter(
                "banking.api.requests.total", "operation", ApiOperation.TRANSFER_CREATE.metricTag()).count())
                .isEqualTo(3);
        assertThat(meterRegistry.counter(
                "banking.api.requests.rejected", "operation", ApiOperation.TRANSFER_CREATE.metricTag()).count())
                .isEqualTo(2);
        assertThat(meterRegistry.counter(
                "banking.api.requests.failed", "operation", ApiOperation.TRANSFER_CREATE.metricTag()).count())
                .isOne();
        assertThat(meterRegistry.counter(
                "banking.api.database.errors", "operation", ApiOperation.TRANSFER_CREATE.metricTag()).count())
                .isOne();
        assertThat(meterRegistry.timer(
                "banking.api.request.latency", "operation", ApiOperation.TRANSFER_CREATE.metricTag()).count())
                .isEqualTo(3);
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

    private void assertRequestMetrics(ApiOperation operation, String outcome, long expected) {
        assertThat(meterRegistry.counter(
                "banking.api.requests.total", "operation", operation.metricTag()).count()).isEqualTo(expected);
        assertThat(meterRegistry.counter(
                "banking.api.requests." + outcome, "operation", operation.metricTag()).count()).isEqualTo(expected);
        assertThat(meterRegistry.timer(
                "banking.api.request.latency", "operation", operation.metricTag()).count()).isEqualTo(expected);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MetricsTestConfiguration {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}

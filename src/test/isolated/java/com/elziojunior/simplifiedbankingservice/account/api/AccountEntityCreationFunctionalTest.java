package com.elziojunior.simplifiedbankingservice.account.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.elziojunior.simplifiedbankingservice.api.AccountController;
import com.elziojunior.simplifiedbankingservice.api.ApiExceptionHandler;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetricsInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.elziojunior.simplifiedbankingservice.exception.AccountCreationValidationException;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateAccountDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMapperImpl;
import com.elziojunior.simplifiedbankingservice.service.CreateAccountService;
import com.elziojunior.simplifiedbankingservice.model.dto.CreatedAccountDto;
import com.elziojunior.simplifiedbankingservice.configuration.SecurityConfiguration;
import com.elziojunior.simplifiedbankingservice.configuration.ApiMetricsConfiguration;

@WebMvcTest(AccountController.class)
@Import({
        AccountMapperImpl.class,
        ApiExceptionHandler.class,
        ApiMetricsConfiguration.class,
        ApiMetricsInterceptor.class,
        SecurityConfiguration.class
})
class AccountEntityCreationFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateAccountService createAccountService;

    @MockitoBean
    private ApiMetrics apiMetrics;

    /** Proves the public endpoint returns the complete 201 creation contract. */
    @Test
    void shouldCreateAccountWithoutAuthenticationOrCsrfToken() throws Exception {
        when(createAccountService.create(any(CreateAccountDto.class))).thenReturn(new CreatedAccountDto(
                41L,
                "Ada Lovelace",
                new BigDecimal("100.00"),
                OffsetDateTime.parse("2026-08-31T13:45:00Z")));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","initialBalance":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(41))
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.createdAt").value("2026-08-31T13:45:00Z"));

        verify(createAccountService).create(new CreateAccountDto("Ada Lovelace", new BigDecimal("100")));
    }

    /** Proves missing, blank, and oversized names fail before application invocation. */
    @Test
    void shouldRejectInvalidNamesWithSafeProblemDetails() throws Exception {
        assertBadRequest("{\"initialBalance\":0}", "Invalid request", "The request is invalid.");
        assertBadRequest("{\"name\":\"   \",\"initialBalance\":0}", "Invalid request", "The request is invalid.");
        assertBadRequest(
                "{\"name\":\"" + "a".repeat(256) + "\",\"initialBalance\":0}",
                "Invalid request", "The request is invalid.");

        verify(createAccountService, never()).create(any());
    }

    /** Proves null and negative balances fail at the transport validation boundary. */
    @Test
    void shouldRejectInvalidBalancesWithSafeProblemDetails() throws Exception {
        assertBadRequest("{\"name\":\"Ada\"}", "Invalid request", "The request is invalid.");
        assertBadRequest("{\"name\":\"Ada\",\"initialBalance\":-0.001}", "Invalid request", "The request is invalid.");

        verify(createAccountService, never()).create(any());
    }

    /** Proves malformed JSON is rejected without exposing parser internals. */
    @Test
    void shouldRejectMalformedJsonWithSafeProblemDetails() throws Exception {
        assertBadRequest("{not-json", "Invalid request", "The request body is invalid or unreadable.");

        verify(createAccountService, never()).create(any());
    }

    /** Proves monetary overflow found by the application is translated safely. */
    @Test
    void shouldTranslateApplicationValidationToProblemDetails() throws Exception {
        when(createAccountService.create(any())).thenThrow(
                new AccountCreationValidationException("Initial balance exceeds the supported monetary range."));

        assertBadRequest(
                "{\"name\":\"Ada\",\"initialBalance\":99999999999999999.995}",
                "Invalid account creation request", "Initial balance exceeds the supported monetary range.");
    }

    /** Proves unapproved account read, list, update, and deletion operations remain absent. */
    @Test
    void shouldReturnNotFoundForUnsupportedAccountRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/v1/accounts/41"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/accounts/41"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/accounts/41"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/accounts/41"))
                .andExpect(status().isNotFound());
    }

    /** Proves the temporary API exception does not expose operational endpoints. */
    @Test
    void shouldKeepActuatorProtected() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    private void assertBadRequest(String content, String title, String detail) throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.detail").value(detail));
    }
}

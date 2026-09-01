package com.elziojunior.simplifiedbankingservice.account.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.elziojunior.simplifiedbankingservice.api.AccountMovementController;
import com.elziojunior.simplifiedbankingservice.api.ApiExceptionHandler;
import com.elziojunior.simplifiedbankingservice.configuration.ApiMetricsConfiguration;
import com.elziojunior.simplifiedbankingservice.configuration.SecurityConfiguration;
import com.elziojunior.simplifiedbankingservice.exception.AccountMovementNotFoundException;
import com.elziojunior.simplifiedbankingservice.exception.AccountMovementValidationException;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetricsInterceptor;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.model.dto.ListAccountMovementsDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementItemDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementLookbackPeriod;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMovementMapperImpl;
import com.elziojunior.simplifiedbankingservice.service.ListAccountMovementsService;

import io.micrometer.core.instrument.Timer;

@WebMvcTest(AccountMovementController.class)
@Import({
        AccountMovementMapperImpl.class,
        ApiExceptionHandler.class,
        ApiMetricsConfiguration.class,
        ApiMetricsInterceptor.class,
        SecurityConfiguration.class
})
class AccountMovementListingFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAccountMovementsService listAccountMovementsService;

    @MockitoBean
    private ApiMetrics apiMetrics;

    /**
     * Proves the unauthenticated default request returns the exact fixed-page
     * response and bounded operation metric.
     */
    @Test
    void shouldReturnDefaultMovementPageAndRecordMetrics() throws Exception {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-31T18:45:00Z");
        UUID operationId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        when(listAccountMovementsService.list(
                new ListAccountMovementsDto(41L, 0, MovementLookbackPeriod.ONE_DAY, null)))
                .thenReturn(new MovementPageDto(
                        List.of(new MovementItemDto(
                                42L, operationId, MovementType.CREDIT, new BigDecimal("100.00"), createdAt)),
                        0,
                        10,
                        1,
                        1));
        Timer.Sample sample = org.mockito.Mockito.mock(Timer.Sample.class);
        when(apiMetrics.start()).thenReturn(sample);

        mockMvc.perform(get("/api/v1/accounts/41/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].operationId").value(operationId.toString()))
                .andExpect(jsonPath("$.content[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-08-31T18:45:00Z"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(listAccountMovementsService)
                .list(new ListAccountMovementsDto(41L, 0, MovementLookbackPeriod.ONE_DAY, null));
        verify(apiMetrics).recordOutcome(ApiOperation.MOVEMENT_LIST, 200, sample);
    }

    /** Proves page, each fixed period, and movement type are parsed and combined at the HTTP boundary. */
    @Test
    void shouldBindCombinedMovementFilters() throws Exception {
        for (String value : List.of("1d", "1w", "1M")) {
            MovementLookbackPeriod period = switch (value) {
                case "1d" -> MovementLookbackPeriod.ONE_DAY;
                case "1w" -> MovementLookbackPeriod.ONE_WEEK;
                case "1M" -> MovementLookbackPeriod.ONE_MONTH;
                default -> throw new IllegalStateException();
            };
            ListAccountMovementsDto query = new ListAccountMovementsDto(41L, 2, period, MovementType.DEBIT);
            when(listAccountMovementsService.list(query)).thenReturn(new MovementPageDto(List.of(), 2, 10, 21, 3));

            mockMvc.perform(get("/api/v1/accounts/41/movements")
                            .param("page", "2")
                            .param("period", value)
                            .param("type", "DEBIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2));

            verify(listAccountMovementsService).list(query);
        }
    }

    /** Proves an existing account with no matches has a normal empty response envelope. */
    @Test
    void shouldReturnEmptyMovementPage() throws Exception {
        when(listAccountMovementsService.list(any())).thenReturn(new MovementPageDto(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/api/v1/accounts/41/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    /** Proves negative pages, unsupported periods, and unsupported types fail before application invocation. */
    @Test
    void shouldRejectInvalidTransportFiltersAndRecordRejectedMetric() throws Exception {
        Timer.Sample sample = org.mockito.Mockito.mock(Timer.Sample.class);
        when(apiMetrics.start()).thenReturn(sample);

        assertBadRequest("?page=-1", "Invalid request", "The request is invalid.");
        assertBadRequest("?period=30d", "Invalid request", "The request is invalid.");
        assertBadRequest("?period=1m", "Invalid request", "The request is invalid.");
        assertBadRequest("?type=UNKNOWN", "Invalid request", "The request is invalid.");

        verify(listAccountMovementsService, never()).list(any());
        verify(apiMetrics, org.mockito.Mockito.times(4)).recordOutcome(ApiOperation.MOVEMENT_LIST, 400, sample);
    }

    /** Proves application validation and unknown accounts retain safe application-specific Problem Details. */
    @Test
    void shouldTranslateApplicationQueryFailures() throws Exception {
        when(listAccountMovementsService.list(any()))
                .thenThrow(new AccountMovementValidationException("Movement period is required."))
                .thenThrow(new AccountMovementNotFoundException("The requested account does not exist."));

        assertBadRequest(
                "?period=1d",
                "Invalid movement query",
                "Movement period is required.");
        mockMvc.perform(get("/api/v1/accounts/999/movements"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Account not found"))
                .andExpect(jsonPath("$.detail").value("The requested account does not exist."));
    }

    /** Proves persistence failures use movement-specific safe wording and retain bounded database metrics. */
    @Test
    void shouldTranslateMovementDatabaseFailure() throws Exception {
        TransientDataAccessResourceException failure =
                new TransientDataAccessResourceException("secret database detail");
        when(listAccountMovementsService.list(any())).thenThrow(failure);

        mockMvc.perform(get("/api/v1/accounts/41/movements"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Movement query temporarily unavailable"))
                .andExpect(jsonPath("$.detail")
                        .value("The movements could not be retrieved because persistence is unavailable."));

        verify(apiMetrics).recordDatabaseFailure(ApiOperation.MOVEMENT_LIST, failure);
    }

    /** Proves the movement resource is read-only and does not gain an unsupported write method. */
    @Test
    void shouldRejectUnsupportedWriteMethod() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/41/movements"))
                .andExpect(status().isMethodNotAllowed());
    }

    private void assertBadRequest(String query, String title, String detail) throws Exception {
        mockMvc.perform(get("/api/v1/accounts/41/movements" + query))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.detail").value(detail));
    }
}

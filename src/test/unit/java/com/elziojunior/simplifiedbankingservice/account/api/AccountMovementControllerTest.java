package com.elziojunior.simplifiedbankingservice.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.api.AccountMovementController;
import com.elziojunior.simplifiedbankingservice.exception.AccountMovementValidationException;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementFilterRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.ListAccountMovementsDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementLookbackPeriod;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMovementMapper;
import com.elziojunior.simplifiedbankingservice.service.ListAccountMovementsService;

import jakarta.servlet.http.HttpServletRequest;

class AccountMovementControllerTest {

    /** Proves the controller only maps, invokes the read use case, and maps its result. */
    @Test
    void shouldMapMovementQueryAndResult() {
        ListAccountMovementsService service = mock(ListAccountMovementsService.class);
        AccountMovementMapper mapper = mock(AccountMovementMapper.class);
        AccountMovementFilterRequest request = new AccountMovementFilterRequest(null, null, null);
        ListAccountMovementsDto query =
                new ListAccountMovementsDto(41L, 0, MovementLookbackPeriod.ONE_DAY, null);
        MovementPageDto page = new MovementPageDto(List.of(), 0, 10, 0, 0);
        AccountMovementPageResponse expected = new AccountMovementPageResponse(List.of(), 0, 10, 0, 0);
        when(mapper.toDto(41L, request)).thenReturn(query);
        when(service.list(query)).thenReturn(page);
        when(mapper.toResponse(page)).thenReturn(expected);
        AccountMovementController controller = new AccountMovementController(service, mapper);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getParameterMap()).thenReturn(Map.of("period", new String[] {"1d"}));

        AccountMovementPageResponse response = controller.list(41L, request, httpRequest);

        assertThat(response).isEqualTo(expected);
        verify(mapper).toDto(41L, request);
        verify(service).list(query);
        verify(mapper).toResponse(page);
    }

    /** Proves unknown query fields are rejected before mapping or application execution to prevent silent fallback. */
    @Test
    void shouldRejectUnsupportedQueryParametersBeforeMapping() {
        ListAccountMovementsService service = mock(ListAccountMovementsService.class);
        AccountMovementMapper mapper = mock(AccountMovementMapper.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getParameterMap()).thenReturn(Map.of("start", new String[] {"2026-08-01T00:00:00Z"}));
        AccountMovementController controller = new AccountMovementController(service, mapper);

        assertThatThrownBy(() -> controller.list(
                        41L, new AccountMovementFilterRequest(null, null, null), httpRequest))
                .isInstanceOf(AccountMovementValidationException.class)
                .hasMessage("The movement query contains unsupported parameters.");

        verifyNoInteractions(service, mapper);
    }
}

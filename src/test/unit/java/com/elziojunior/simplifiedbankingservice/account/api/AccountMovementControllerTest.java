package com.elziojunior.simplifiedbankingservice.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.api.AccountMovementController;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementFilterRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.ListAccountMovementsDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMovementMapper;
import com.elziojunior.simplifiedbankingservice.service.ListAccountMovementsService;

class AccountMovementControllerTest {

    /** Proves the controller only maps, invokes the read use case, and maps its result. */
    @Test
    void shouldMapMovementQueryAndResult() {
        ListAccountMovementsService service = mock(ListAccountMovementsService.class);
        AccountMovementMapper mapper = mock(AccountMovementMapper.class);
        AccountMovementFilterRequest request = new AccountMovementFilterRequest(null, null, null, null);
        ListAccountMovementsDto query = new ListAccountMovementsDto(41L, 0, null, null, null);
        MovementPageDto page = new MovementPageDto(List.of(), 0, 10, 0, 0);
        AccountMovementPageResponse expected = new AccountMovementPageResponse(List.of(), 0, 10, 0, 0);
        when(mapper.toDto(41L, request)).thenReturn(query);
        when(service.list(query)).thenReturn(page);
        when(mapper.toResponse(page)).thenReturn(expected);
        AccountMovementController controller = new AccountMovementController(service, mapper);

        AccountMovementPageResponse response = controller.list(41L, request);

        assertThat(response).isEqualTo(expected);
        verify(mapper).toDto(41L, request);
        verify(service).list(query);
        verify(mapper).toResponse(page);
    }
}

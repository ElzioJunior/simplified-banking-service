package com.elziojunior.simplifiedbankingservice.transfer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.model.api.CreateTransferRequest;
import com.elziojunior.simplifiedbankingservice.api.TransferController;
import com.elziojunior.simplifiedbankingservice.model.api.TransferResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateTransferDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferMapper;
import com.elziojunior.simplifiedbankingservice.service.TransferService;
class TransferControllerTest {

    /** Proves the transport adapter maps the request and stable completed response. */
    @Test
    void shouldMapTransferRequestAndResponse() {
        TransferService service = mock(TransferService.class);
        TransferMapper mapper = mock(TransferMapper.class);
        UUID token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID transferId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, new BigDecimal("12.345"));
        CreateTransferDto input = new CreateTransferDto(token, 1L, 2L, new BigDecimal("12.345"));
        CompletedTransferDto completed = new CompletedTransferDto(transferId, 1L, 2L, new BigDecimal("12.34"));
        TransferResponse expected = new TransferResponse(transferId, "COMPLETED", 1L, 2L, new BigDecimal("12.34"));
        when(mapper.toDto(token, request)).thenReturn(input);
        when(service.createTransfer(input)).thenReturn(completed);
        when(mapper.toResponse(completed)).thenReturn(expected);

        TransferResponse response = new TransferController(service, mapper).create(token, request);

        assertThat(response).isEqualTo(expected);
        verify(mapper).toDto(token, request);
        verify(service).createTransfer(input);
        verify(mapper).toResponse(completed);
    }
}

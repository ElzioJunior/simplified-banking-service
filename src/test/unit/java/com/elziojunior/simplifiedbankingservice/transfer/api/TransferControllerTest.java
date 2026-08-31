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
import com.elziojunior.simplifiedbankingservice.service.CreateTransferService;
import com.elziojunior.simplifiedbankingservice.service.TransferMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class TransferControllerTest {

    /** Proves the transport adapter maps the request and stable completed response. */
    @Test
    void shouldMapTransferRequestAndResponse() {
        CreateTransferService service = mock(CreateTransferService.class);
        UUID token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID transferId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        CreateTransferDto command = new CreateTransferDto(token, 1L, 2L, new BigDecimal("12.345"));
        when(service.create(command)).thenReturn(
                new CompletedTransferDto(transferId, 1L, 2L, new BigDecimal("12.34")));

        TransferResponse response = new TransferController(service, new TransferMetrics(new SimpleMeterRegistry())).create(
                token, new CreateTransferRequest(1L, 2L, new BigDecimal("12.345")));

        assertThat(response).isEqualTo(new TransferResponse(
                transferId, "COMPLETED", 1L, 2L, new BigDecimal("12.34")));
        verify(service).create(command);
    }
}

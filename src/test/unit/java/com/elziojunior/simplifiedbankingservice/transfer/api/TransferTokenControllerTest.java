package com.elziojunior.simplifiedbankingservice.transfer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.api.TransferTokenController;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferTokenMapper;
import com.elziojunior.simplifiedbankingservice.service.IssueTransferTokenService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class TransferTokenControllerTest {

    /** Proves the token adapter maps the generated token and expiry without alteration. */
    @Test
    void shouldMapIssuedToken() {
        IssueTransferTokenService service = mock(IssueTransferTokenService.class);
        TransferTokenMapper mapper = mock(TransferTokenMapper.class);
        UUID token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-08-31T14:10:00Z");
        IssuedTransferTokenDto issued = new IssuedTransferTokenDto(token, expiresAt);
        TransferTokenResponse expected = new TransferTokenResponse(token, expiresAt);
        when(service.issue()).thenReturn(issued);
        when(mapper.toResponse(issued)).thenReturn(expected);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        TransferTokenResponse response = new TransferTokenController(service, mapper, new ApiMetrics(registry)).issue();

        assertThat(response).isEqualTo(expected);
        verify(service).issue();
        verify(mapper).toResponse(issued);
        assertThat(registry.counter(
                "banking.api.requests.successful", "operation", ApiOperation.TRANSFER_TOKEN_ISSUE.metricTag()).count())
                .isOne();
    }
}

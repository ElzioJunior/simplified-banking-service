package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.model.entity.TransferIdempotencyTokenEntity;
import com.elziojunior.simplifiedbankingservice.repository.TransferIdempotencyTokenRepository;

final class TransferTokenServiceTest {

    /** Proves issuance persists a deterministic UUID with exactly ten minutes of validity. */
    @Test
    void shouldIssueTokenWithTenMinuteValidity() {
        TransferIdempotencyTokenRepository repository = mock(TransferIdempotencyTokenRepository.class);
        UUID tokenId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TransferTokenService service = new TransferTokenService(
                repository,
                () -> tokenId,
                Clock.fixed(Instant.parse("2026-08-31T19:00:00.123456789Z"), ZoneOffset.UTC));

        IssuedTransferTokenDto result = service.issueTransferToken();

        assertThat(result.token()).isEqualTo(tokenId);
        assertThat(result.expiresAt()).isEqualTo(OffsetDateTime.parse("2026-08-31T19:10:00.123456Z"));
        verify(repository).save(any(TransferIdempotencyTokenEntity.class));
    }
}

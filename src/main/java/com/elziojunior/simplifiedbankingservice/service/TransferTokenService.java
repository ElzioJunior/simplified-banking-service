package com.elziojunior.simplifiedbankingservice.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.model.entity.TransferIdempotencyTokenEntity;
import com.elziojunior.simplifiedbankingservice.repository.TransferIdempotencyTokenRepository;

/** Issues the short-lived server token required before transfer submission. */
@Service
public class TransferTokenService {

    private static final long TOKEN_VALIDITY_MINUTES = 10;

    private final TransferIdempotencyTokenRepository tokenRepository;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    public TransferTokenService(
            TransferIdempotencyTokenRepository tokenRepository,
            UuidGenerator uuidGenerator,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.uuidGenerator = uuidGenerator;
        this.clock = clock;
    }

    /** Persists a unique token with the ADR-0026 lifetime so retries have durable identity. */
    @Transactional
    public IssuedTransferTokenDto issueTransferToken() {
        OffsetDateTime createdAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        TransferIdempotencyTokenEntity token = new TransferIdempotencyTokenEntity(
                uuidGenerator.generate(), createdAt, createdAt.plusMinutes(TOKEN_VALIDITY_MINUTES));
        TransferIdempotencyTokenEntity saved = tokenRepository.save(token);
        return new IssuedTransferTokenDto(saved.getToken(), saved.getExpiresAt());
    }
}

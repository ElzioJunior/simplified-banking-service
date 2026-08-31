package com.elziojunior.simplifiedbankingservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.elziojunior.simplifiedbankingservice.model.entity.TransferIdempotencyTokenEntity;

import jakarta.persistence.LockModeType;

/** Persistence and first-use serialization boundary for transfer tokens. */
public interface TransferIdempotencyTokenRepository
        extends JpaRepository<TransferIdempotencyTokenEntity, UUID> {

    /** Locks one token so concurrent retries cannot establish duplicate operations. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from TransferIdempotencyTokenEntity token where token.token = :token")
    Optional<TransferIdempotencyTokenEntity> findByTokenForUpdate(@Param("token") UUID token);
}

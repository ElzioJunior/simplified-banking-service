package com.elziojunior.simplifiedbankingservice.model.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Persistence state for one server-issued transfer idempotency token. */
@Entity
@Table(name = "transfer_idempotency_tokens")
public class TransferIdempotencyTokenEntity {

    /** Server-issued token identity. */
    @Id
    private UUID token;

    /** UTC issuance instant. */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** Exclusive UTC expiration instant. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Successful first-use instant, absent before association. */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /** Established transfer identifier, absent before association. */
    @Column(name = "operation_id", unique = true)
    private UUID operationId;

    /** Established source account ID, absent before association. */
    @Column(name = "source_account_id")
    private Long sourceAccountId;

    /** Established destination account ID, absent before association. */
    @Column(name = "destination_account_id")
    private Long destinationAccountId;

    /** Established normalized transfer amount, absent before association. */
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    protected TransferIdempotencyTokenEntity() {
    }

    public TransferIdempotencyTokenEntity(UUID token, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** Associates the successful operation and payload atomically with its financial effects. */
    public void associate(
            UUID operationId,
            Long sourceAccountId,
            Long destinationAccountId,
            BigDecimal amount,
            OffsetDateTime usedAt) {
        this.operationId = operationId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.usedAt = usedAt;
    }

    public UUID getToken() { return token; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public UUID getOperationId() { return operationId; }
    public Long getSourceAccountId() { return sourceAccountId; }
    public Long getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
}

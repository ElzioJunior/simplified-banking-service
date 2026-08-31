package com.elziojunior.simplifiedbankingservice.model.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Immutable persistence record for one side of a financial operation. */
@Entity
@Table(name = "movements")
public class MovementEntity {

    /** Database-generated movement identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Account affected by this movement, loaded only when explicitly needed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    /** UUID shared by the debit and credit of one transfer. */
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    /** Financial direction constrained to CREDIT or DEBIT. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private MovementType type;

    /** Positive scale-two monetary amount. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** UTC occurrence instant shared by the transfer effects. */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected MovementEntity() {
    }

    public MovementEntity(
            AccountEntity account,
            UUID operationId,
            MovementType type,
            BigDecimal amount,
            OffsetDateTime createdAt) {
        this.account = account;
        this.operationId = operationId;
        this.type = type;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public MovementType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

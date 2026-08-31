package com.elziojunior.simplifiedbankingservice.model.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persistence representation of an account stored by the Flyway-managed
 * {@code accounts} table.
 */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    /** Database-generated account identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Account-holder name retained exactly as accepted by the application. */
    @Column(nullable = false, length = 255)
    private String name;

    /** Current account balance stored with the project's monetary precision. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** UTC account-creation instant stored as a timezone-aware timestamp. */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AccountEntity() {
    }

    public AccountEntity(String name, BigDecimal balance, OffsetDateTime createdAt) {
        this.name = name;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /** Replaces the balance already validated by the transactional financial use case. */
    public void updateBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

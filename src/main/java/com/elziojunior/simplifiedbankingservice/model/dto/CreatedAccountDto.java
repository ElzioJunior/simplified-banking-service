package com.elziojunior.simplifiedbankingservice.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Application result returned after an account is durably created.
 *
 * @param id database-generated account identifier
 * @param name persisted account-holder name
 * @param balance normalized opening balance
 * @param createdAt persisted UTC creation instant
 */
public record CreatedAccountDto(Long id, String name, BigDecimal balance, OffsetDateTime createdAt) {
}

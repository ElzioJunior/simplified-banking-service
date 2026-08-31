package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Public representation returned for a newly created account.
 *
 * @param id database-generated account identifier
 * @param name persisted account-holder name
 * @param balance normalized opening balance
 * @param createdAt persisted creation instant with its UTC offset
 */
public record AccountResponse(
        Long id,
        String name,
        BigDecimal balance,
        OffsetDateTime createdAt) {
}

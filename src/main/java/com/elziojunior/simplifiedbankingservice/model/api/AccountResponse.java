package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public representation returned for a newly created account.
 *
 * @param id database-generated account identifier
 * @param name persisted account-holder name
 * @param balance normalized opening balance
 * @param createdAt persisted creation instant with its UTC offset
 */
public record AccountResponse(
        @Schema(description = "Generated account identifier", example = "41") Long id,
        @Schema(description = "Persisted account-holder name", example = "Ada Lovelace") String name,
        @Schema(description = "Normalized account balance", example = "1000.00") BigDecimal balance,
        @Schema(description = "UTC account creation instant", example = "2026-09-01T12:00:00Z")
        OffsetDateTime createdAt) {
}

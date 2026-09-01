package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public representation of one financial movement.
 *
 * @param id movement identifier
 * @param operationId financial operation correlation identifier
 * @param type credit or debit direction
 * @param amount positive scale-two monetary amount
 * @param createdAt movement occurrence instant
 */
public record AccountMovementResponse(
        @Schema(description = "Movement identifier", example = "84") Long id,
        @Schema(
                description = "Financial operation correlation identifier",
                example = "f6608b62-b6ba-4da2-864d-b8d49c48fb85")
        UUID operationId,
        @Schema(description = "Credit or debit direction", example = "CREDIT") AccountMovementType type,
        @Schema(description = "Positive normalized monetary amount", example = "100.00") BigDecimal amount,
        @Schema(description = "Movement occurrence instant", example = "2026-09-01T12:00:00Z")
        OffsetDateTime createdAt) {
}

package com.elziojunior.simplifiedbankingservice.model.api;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional HTTP filters for one account-movement page.
 *
 * @param page zero-based page, defaulting to zero when absent
 * @param start optional inclusive ISO 8601 occurrence lower bound
 * @param end optional exclusive ISO 8601 occurrence upper bound
 * @param type optional CREDIT or DEBIT direction
 */
public record AccountMovementFilterRequest(
        @Schema(description = "Zero-based page, defaulting to zero", example = "0", minimum = "0")
        @Min(0) Integer page,
        @Schema(description = "Optional inclusive occurrence lower bound", example = "2026-08-01T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
        @Schema(description = "Optional exclusive occurrence upper bound", example = "2026-09-01T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
        @Schema(description = "Optional movement direction", example = "CREDIT") AccountMovementType type) {
}

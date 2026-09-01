package com.elziojunior.simplifiedbankingservice.model.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional HTTP filters for one account-movement page.
 *
 * @param page zero-based page, defaulting to zero when absent
 * @param period recent-history period, defaulting to one day when absent
 * @param type optional CREDIT or DEBIT direction
 */
public record AccountMovementFilterRequest(
        @Schema(description = "Zero-based page, defaulting to zero", example = "0", minimum = "0")
        @Min(0) Integer page,
        @Schema(description = "Recent-history period, defaulting to 1d", example = "1d", allowableValues = {
                "1d", "1w", "1M"
        })
        @Pattern(regexp = "1d|1w|1M") String period,
        @Schema(description = "Optional movement direction", example = "CREDIT") AccountMovementType type) {
}

package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public account-creation input.
 *
 * @param name required account-holder name
 * @param initialBalance nonnegative balance normalized by the application service
 */
public record CreateAccountRequest(
        @Schema(description = "Account-holder name", example = "Ada Lovelace", maxLength = 255)
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Non-negative opening balance", example = "1000.00", minimum = "0")
        @NotNull @DecimalMin("0.0") BigDecimal initialBalance) {
}

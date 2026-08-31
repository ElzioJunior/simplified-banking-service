package com.elziojunior.simplifiedbankingservice.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Public account-creation input.
 *
 * @param name required account-holder name
 * @param initialBalance nonnegative balance normalized by the application service
 */
public record CreateAccountRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin("0.0") BigDecimal initialBalance) {
}

package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public transfer payload.
 *
 * @param sourceAccountId account to debit
 * @param destinationAccountId account to credit
 * @param amount positive amount normalized by the application service
 */
public record CreateTransferRequest(
        @Schema(description = "Account to debit", example = "41") @NotNull Long sourceAccountId,
        @Schema(description = "Different account to credit", example = "42") @NotNull Long destinationAccountId,
        @Schema(description = "Positive transfer amount", example = "100.00", minimum = "0", exclusiveMinimum = true)
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount) {
}

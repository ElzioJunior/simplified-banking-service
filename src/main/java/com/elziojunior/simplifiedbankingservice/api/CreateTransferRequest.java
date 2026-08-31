package com.elziojunior.simplifiedbankingservice.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Public transfer payload.
 *
 * @param sourceAccountId account to debit
 * @param destinationAccountId account to credit
 * @param amount positive amount normalized by the application service
 */
public record CreateTransferRequest(
        @NotNull Long sourceAccountId,
        @NotNull Long destinationAccountId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount) {
}

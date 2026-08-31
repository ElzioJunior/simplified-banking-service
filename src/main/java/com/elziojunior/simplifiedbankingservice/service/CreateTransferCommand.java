package com.elziojunior.simplifiedbankingservice.service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application input for one idempotent transfer attempt.
 *
 * @param token server-issued idempotency token
 * @param sourceAccountId account to debit
 * @param destinationAccountId account to credit
 * @param amount requested monetary amount
 */
public record CreateTransferCommand(
        UUID token,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount) {
}

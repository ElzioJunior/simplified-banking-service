package com.elziojunior.simplifiedbankingservice.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Established result of a first transfer or identical idempotent replay.
 *
 * @param transferId operation UUID shared by both movements
 * @param sourceAccountId debited account
 * @param destinationAccountId credited account
 * @param amount normalized scale-two amount
 */
public record CompletedTransferDto(
        UUID transferId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount) {
}

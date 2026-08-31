package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public completed-transfer representation.
 *
 * @param transferId operation UUID shared by the financial movements
 * @param status stable completed status
 * @param sourceAccountId debited account
 * @param destinationAccountId credited account
 * @param amount normalized scale-two amount
 */
public record TransferResponse(
        UUID transferId,
        String status,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount) {
}

package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "Financial operation identifier", example = "f6608b62-b6ba-4da2-864d-b8d49c48fb85")
        UUID transferId,
        @Schema(description = "Stable transfer status", example = "COMPLETED", allowableValues = "COMPLETED")
        String status,
        @Schema(description = "Debited account", example = "41") Long sourceAccountId,
        @Schema(description = "Credited account", example = "42") Long destinationAccountId,
        @Schema(description = "Normalized transfer amount", example = "100.00") BigDecimal amount) {
}

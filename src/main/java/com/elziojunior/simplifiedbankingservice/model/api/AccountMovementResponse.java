package com.elziojunior.simplifiedbankingservice.model.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

/**
 * Public representation of one financial movement.
 *
 * @param id movement identifier
 * @param operationId financial operation correlation identifier
 * @param type credit or debit direction
 * @param amount positive scale-two monetary amount
 * @param createdAt movement occurrence instant
 */
public record AccountMovementResponse(
        Long id,
        UUID operationId,
        MovementType type,
        BigDecimal amount,
        OffsetDateTime createdAt) {
}

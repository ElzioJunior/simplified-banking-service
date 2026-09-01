package com.elziojunior.simplifiedbankingservice.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

/**
 * Application representation of one movement in account history.
 *
 * @param id database-generated movement identifier
 * @param operationId financial operation correlation identifier
 * @param type credit or debit direction
 * @param amount positive scale-two monetary amount
 * @param createdAt movement occurrence instant
 */
public record MovementItemDto(
        Long id,
        UUID operationId,
        MovementType type,
        BigDecimal amount,
        OffsetDateTime createdAt) {
}

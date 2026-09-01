package com.elziojunior.simplifiedbankingservice.model.dto;

import java.time.OffsetDateTime;

import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

/**
 * Application input for one bounded account-movement query.
 *
 * @param accountId account whose movements may be returned
 * @param page zero-based requested page
 * @param start optional inclusive occurrence lower bound
 * @param end optional exclusive occurrence upper bound
 * @param type optional financial movement direction
 */
public record ListAccountMovementsDto(
        Long accountId,
        int page,
        OffsetDateTime start,
        OffsetDateTime end,
        MovementType type) {
}

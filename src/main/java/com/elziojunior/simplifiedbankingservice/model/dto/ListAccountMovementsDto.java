package com.elziojunior.simplifiedbankingservice.model.dto;

import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

/**
 * Application input for one bounded account-movement query.
 *
 * @param accountId account whose movements may be returned
 * @param page zero-based requested page
 * @param period recent-history period resolved against the application clock
 * @param type optional financial movement direction
 */
public record ListAccountMovementsDto(
        Long accountId,
        int page,
        MovementLookbackPeriod period,
        MovementType type) {
}

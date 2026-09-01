package com.elziojunior.simplifiedbankingservice.model.api;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

import jakarta.validation.constraints.Min;

/**
 * Optional HTTP filters for one account-movement page.
 *
 * @param page zero-based page, defaulting to zero when absent
 * @param start optional inclusive ISO 8601 occurrence lower bound
 * @param end optional exclusive ISO 8601 occurrence upper bound
 * @param type optional CREDIT or DEBIT direction
 */
public record AccountMovementFilterRequest(
        @Min(0) Integer page,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
        MovementType type) {
}

package com.elziojunior.simplifiedbankingservice.model.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public server-issued transfer token.
 *
 * @param token token supplied later through Idempotency-Key
 * @param expiresAt exclusive UTC expiration instant
 */
public record TransferTokenResponse(UUID token, OffsetDateTime expiresAt) {
}

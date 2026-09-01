package com.elziojunior.simplifiedbankingservice.model.api;

import java.time.OffsetDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public server-issued transfer token.
 *
 * @param token token supplied later through Idempotency-Key
 * @param expiresAt exclusive UTC expiration instant
 */
public record TransferTokenResponse(
        @Schema(
                description = "Token supplied through Idempotency-Key",
                example = "4e80db4d-ce8c-40a6-b839-b45fd45b1461")
        UUID token,
        @Schema(description = "Exclusive UTC expiration instant", example = "2026-09-01T12:10:00Z")
        OffsetDateTime expiresAt) {
}

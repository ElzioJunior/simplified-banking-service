package com.elziojunior.simplifiedbankingservice.model.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted server-issued transfer token.
 *
 * @param token unique token identity
 * @param expiresAt exclusive UTC expiration instant
 */
public record IssuedTransferTokenDto(UUID token, OffsetDateTime expiresAt) {
}

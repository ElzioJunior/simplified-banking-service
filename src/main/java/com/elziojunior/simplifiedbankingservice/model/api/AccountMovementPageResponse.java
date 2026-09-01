package com.elziojunior.simplifiedbankingservice.model.api;

import java.util.List;

/**
 * Public fixed-size page envelope for account movement history.
 *
 * @param content movements in deterministic newest-first order
 * @param page zero-based returned page
 * @param size fixed maximum page size
 * @param totalElements total matching movements
 * @param totalPages total matching pages
 */
public record AccountMovementPageResponse(
        List<AccountMovementResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

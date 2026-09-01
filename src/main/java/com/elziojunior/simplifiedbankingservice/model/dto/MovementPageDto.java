package com.elziojunior.simplifiedbankingservice.model.dto;

import java.util.List;

/**
 * Application result for one fixed-size movement page.
 *
 * @param content movements in deterministic newest-first order
 * @param page zero-based returned page
 * @param size fixed maximum page size
 * @param totalElements total matching movements
 * @param totalPages total matching pages
 */
public record MovementPageDto(
        List<MovementItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

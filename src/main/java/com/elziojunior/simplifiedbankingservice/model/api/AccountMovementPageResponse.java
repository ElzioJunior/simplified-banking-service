package com.elziojunior.simplifiedbankingservice.model.api;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "Movements ordered newest first") List<AccountMovementResponse> content,
        @Schema(description = "Returned zero-based page", example = "0") int page,
        @Schema(description = "Fixed maximum page size", example = "10") int size,
        @Schema(description = "Total matching movements", example = "1") long totalElements,
        @Schema(description = "Total matching pages", example = "1") int totalPages) {
}

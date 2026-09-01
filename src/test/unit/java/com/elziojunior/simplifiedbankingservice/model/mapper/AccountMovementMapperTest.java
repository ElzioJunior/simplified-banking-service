package com.elziojunior.simplifiedbankingservice.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementFilterRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementType;
import com.elziojunior.simplifiedbankingservice.model.dto.ListAccountMovementsDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementItemDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

class AccountMovementMapperTest {

    private final AccountMovementMapper mapper = Mappers.getMapper(AccountMovementMapper.class);

    /** Proves absent pagination defaults to zero while all supplied HTTP filters cross the boundary unchanged. */
    @Test
    void shouldMapHttpFiltersToApplicationQuery() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        AccountMovementFilterRequest request =
                new AccountMovementFilterRequest(null, start, end, AccountMovementType.CREDIT);

        ListAccountMovementsDto result = mapper.toDto(41L, request);

        assertThat(result).isEqualTo(new ListAccountMovementsDto(41L, 0, start, end, MovementType.CREDIT));
    }

    /** Proves page metadata and movement fields are exposed without leaking an entity or account graph. */
    @Test
    void shouldMapApplicationPageToPublicResponse() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-31T18:45:00Z");
        UUID operationId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        MovementPageDto page = new MovementPageDto(
                List.of(new MovementItemDto(42L, operationId, MovementType.DEBIT, new BigDecimal("10.00"), createdAt)),
                1,
                10,
                12,
                2);

        AccountMovementPageResponse response = mapper.toResponse(page);

        assertThat(response.page()).isOne();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(12);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.content()).singleElement().satisfies(movement -> {
            assertThat(movement.id()).isEqualTo(42L);
            assertThat(movement.operationId()).isEqualTo(operationId);
            assertThat(movement.type()).isEqualTo(AccountMovementType.DEBIT);
            assertThat(movement.amount()).isEqualByComparingTo("10.00");
            assertThat(movement.createdAt()).isEqualTo(createdAt);
        });
    }
}

package com.elziojunior.simplifiedbankingservice.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.elziojunior.simplifiedbankingservice.model.dto.MovementLookbackPeriod;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementEntity;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

class AccountMovementMapperTest {

    private final AccountMovementMapper mapper = Mappers.getMapper(AccountMovementMapper.class);

    /** Proves absent pagination and period receive their defaults while the supplied type crosses the boundary. */
    @Test
    void shouldMapHttpFiltersToApplicationQuery() {
        AccountMovementFilterRequest request = new AccountMovementFilterRequest(null, null, AccountMovementType.CREDIT);

        ListAccountMovementsDto result = mapper.toDto(41L, request);

        assertThat(result).isEqualTo(
                new ListAccountMovementsDto(41L, 0, MovementLookbackPeriod.ONE_DAY, MovementType.CREDIT));
    }

    /** Proves each case-sensitive public period maps to its explicit application value. */
    @Test
    void shouldMapEverySupportedPeriod() {
        assertThat(mapPeriod("1d")).isEqualTo(MovementLookbackPeriod.ONE_DAY);
        assertThat(mapPeriod("1w")).isEqualTo(MovementLookbackPeriod.ONE_WEEK);
        assertThat(mapPeriod("1M")).isEqualTo(MovementLookbackPeriod.ONE_MONTH);
    }

    /** Proves persisted movement fields map to the application DTO without loading the lazy account relationship. */
    @Test
    void shouldMapMovementEntityWithoutTraversingAccount() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-31T18:45:00Z");
        UUID operationId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        MovementEntity movement = mock(MovementEntity.class);
        when(movement.getId()).thenReturn(42L);
        when(movement.getOperationId()).thenReturn(operationId);
        when(movement.getType()).thenReturn(MovementType.DEBIT);
        when(movement.getAmount()).thenReturn(new BigDecimal("10.00"));
        when(movement.getCreatedAt()).thenReturn(createdAt);

        MovementItemDto result = mapper.toDto(movement);

        assertThat(result).isEqualTo(
                new MovementItemDto(42L, operationId, MovementType.DEBIT, new BigDecimal("10.00"), createdAt));
        verify(movement, never()).getAccount();
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

    private MovementLookbackPeriod mapPeriod(String period) {
        return mapper.toDto(41L, new AccountMovementFilterRequest(0, period, null)).period();
    }
}

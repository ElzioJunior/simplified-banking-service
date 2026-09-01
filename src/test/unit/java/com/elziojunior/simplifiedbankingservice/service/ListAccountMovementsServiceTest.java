package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.elziojunior.simplifiedbankingservice.exception.AccountMovementNotFoundException;
import com.elziojunior.simplifiedbankingservice.exception.AccountMovementValidationException;
import com.elziojunior.simplifiedbankingservice.model.dto.ListAccountMovementsDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementEntity;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;
import com.elziojunior.simplifiedbankingservice.repository.AccountRepository;
import com.elziojunior.simplifiedbankingservice.repository.MovementRepository;

@ExtendWith(MockitoExtension.class)
class ListAccountMovementsServiceTest {

    private static final long ACCOUNT_ID = 41L;
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-08-01T00:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-09-01T00:00:00Z");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    private ListAccountMovementsService service;

    @BeforeEach
    void setUp() {
        service = new ListAccountMovementsService(accountRepository, movementRepository);
    }

    /**
     * Proves the service enforces page size and deterministic ordering while
     * exposing only approved movement fields.
     */
    @Test
    void shouldReturnDefaultMovementPageInDeterministicOrder() {
        MovementEntity movement = movement(
                8L,
                UUID.fromString("00000000-0000-0000-0000-000000000008"),
                MovementType.CREDIT,
                "12.34",
                END.minusHours(1));
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
        when(movementRepository.findPageByAccountAndFilters(
                org.mockito.ArgumentMatchers.eq(ACCOUNT_ID),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movement), PageRequest.of(0, 10), 1));

        MovementPageDto result = service.list(new ListAccountMovementsDto(ACCOUNT_ID, 0, null, null, null));

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isOne();
        assertThat(result.totalPages()).isOne();
        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(8L);
            assertThat(item.operationId()).isEqualTo(movement.getOperationId());
            assertThat(item.type()).isEqualTo(MovementType.CREDIT);
            assertThat(item.amount()).isEqualByComparingTo("12.34");
            assertThat(item.createdAt()).isEqualTo(END.minusHours(1));
        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(movementRepository).findPageByAccountAndFilters(
                org.mockito.ArgumentMatchers.eq(ACCOUNT_ID),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    /**
     * Proves later-page requests and every approved optional-filter
     * combination reach the single repository query unchanged.
     */
    @ParameterizedTest
    @MethodSource("filters")
    void shouldApplyEveryFilterCombination(OffsetDateTime start, OffsetDateTime end, MovementType type) {
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
        PageRequest expectedPage = PageRequest.of(2, 10, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(movementRepository.findPageByAccountAndFilters(ACCOUNT_ID, start, end, type, expectedPage))
                .thenReturn(new PageImpl<>(List.of(), expectedPage, 21));

        MovementPageDto result = service.list(new ListAccountMovementsDto(ACCOUNT_ID, 2, start, end, type));

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(3);
        verify(movementRepository).findPageByAccountAndFilters(ACCOUNT_ID, start, end, type, expectedPage);
    }

    /** Proves a known account with no movements is a successful empty page rather than a not-found result. */
    @Test
    void shouldReturnEmptyPageForKnownAccount() {
        PageRequest page = PageRequest.of(0, 10, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
        when(movementRepository.findPageByAccountAndFilters(ACCOUNT_ID, null, null, null, page))
                .thenReturn(new PageImpl<>(List.of(), page, 0));

        MovementPageDto result = service.list(new ListAccountMovementsDto(ACCOUNT_ID, 0, null, null, null));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    /** Proves an absent account is distinguished before movement history is queried. */
    @Test
    void shouldRejectUnknownAccountWithoutQueryingMovements() {
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.list(new ListAccountMovementsDto(ACCOUNT_ID, 0, null, null, null)))
                .isInstanceOf(AccountMovementNotFoundException.class)
                .hasMessage("The requested account does not exist.");

        verifyNoInteractions(movementRepository);
    }

    /** Proves null input, negative pages, and non-increasing ranges fail before any persistence access. */
    @Test
    void shouldRejectInvalidQueriesBeforeRepositoryAccess() {
        List<ListAccountMovementsDto> invalidQueries = List.of(
                new ListAccountMovementsDto(ACCOUNT_ID, -1, null, null, null),
                new ListAccountMovementsDto(ACCOUNT_ID, 0, START, START, null),
                new ListAccountMovementsDto(ACCOUNT_ID, 0, END, START, null));

        assertThatThrownBy(() -> service.list(null))
                .isInstanceOf(AccountMovementValidationException.class);
        invalidQueries.forEach(query -> assertThatThrownBy(() -> service.list(query))
                .isInstanceOf(AccountMovementValidationException.class));

        verify(accountRepository, never()).existsById(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(movementRepository);
    }

    private static Stream<Arguments> filters() {
        return Stream.of(
                Arguments.of(null, null, MovementType.CREDIT),
                Arguments.of(null, null, MovementType.DEBIT),
                Arguments.of(START, null, null),
                Arguments.of(null, END, null),
                Arguments.of(START, END, null),
                Arguments.of(START, END, MovementType.CREDIT),
                Arguments.of(START, END, MovementType.DEBIT));
    }

    private MovementEntity movement(
            Long id, UUID operationId, MovementType type, String amount, OffsetDateTime createdAt) {
        MovementEntity movement = mock(MovementEntity.class);
        when(movement.getId()).thenReturn(id);
        when(movement.getOperationId()).thenReturn(operationId);
        when(movement.getType()).thenReturn(type);
        when(movement.getAmount()).thenReturn(new BigDecimal(amount));
        when(movement.getCreatedAt()).thenReturn(createdAt);
        return movement;
    }
}

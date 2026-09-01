package com.elziojunior.simplifiedbankingservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elziojunior.simplifiedbankingservice.exception.AccountMovementNotFoundException;
import com.elziojunior.simplifiedbankingservice.exception.AccountMovementValidationException;
import com.elziojunior.simplifiedbankingservice.model.dto.ListAccountMovementsDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementItemDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementEntity;
import com.elziojunior.simplifiedbankingservice.repository.AccountRepository;
import com.elziojunior.simplifiedbankingservice.repository.MovementRepository;

/** Lists one account's immutable financial history through a bounded read-only query. */
@Service
public class ListAccountMovementsService {

    private static final int PAGE_SIZE = 10;
    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    public ListAccountMovementsService(AccountRepository accountRepository, MovementRepository movementRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    /**
     * Validates the approved range, distinguishes a missing account from empty
     * history, and returns one deterministic fixed-size page without loading
     * the movement-to-account lazy relationship.
     *
     * @param query account, page, and optional movement filters
     * @return matching movement page
     * @throws AccountMovementValidationException when page or range input is invalid
     * @throws AccountMovementNotFoundException when the account does not exist
     */
    @Transactional(readOnly = true)
    public MovementPageDto list(ListAccountMovementsDto query) {
        validate(query);
        if (!accountRepository.existsById(query.accountId())) {
            throw new AccountMovementNotFoundException("The requested account does not exist.");
        }

        PageRequest pageRequest = PageRequest.of(query.page(), PAGE_SIZE, NEWEST_FIRST);
        Page<MovementEntity> movements = movementRepository.findPageByAccountAndFilters(
                query.accountId(), query.start(), query.end(), query.type(), pageRequest);
        List<MovementItemDto> content = movements.getContent().stream()
                .map(this::toDto)
                .toList();
        return new MovementPageDto(
                content,
                movements.getNumber(),
                movements.getSize(),
                movements.getTotalElements(),
                movements.getTotalPages());
    }

    /** Rejects malformed application input before any repository interaction. */
    private void validate(ListAccountMovementsDto query) {
        if (query == null || query.accountId() == null) {
            throw new AccountMovementValidationException("Account movement query data is required.");
        }
        if (query.page() < 0) {
            throw new AccountMovementValidationException("Page must be greater than or equal to zero.");
        }
        if (query.start() != null && query.end() != null && !query.start().isBefore(query.end())) {
            throw new AccountMovementValidationException("Start must be before end.");
        }
    }

    /** Copies approved movement fields into an application result without traversing the account relationship. */
    private MovementItemDto toDto(MovementEntity movement) {
        return new MovementItemDto(
                movement.getId(),
                movement.getOperationId(),
                movement.getType(),
                movement.getAmount(),
                movement.getCreatedAt());
    }
}

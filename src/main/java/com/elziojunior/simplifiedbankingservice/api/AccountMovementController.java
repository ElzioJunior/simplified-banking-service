package com.elziojunior.simplifiedbankingservice.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.metrics.ObservedApiOperation;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementFilterRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMovementMapper;
import com.elziojunior.simplifiedbankingservice.service.ListAccountMovementsService;

import jakarta.validation.Valid;

/** HTTP adapter for the read-only account-movement listing use case. */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountMovementController {

    private final ListAccountMovementsService listAccountMovementsService;
    private final AccountMovementMapper accountMovementMapper;

    public AccountMovementController(
            ListAccountMovementsService listAccountMovementsService,
            AccountMovementMapper accountMovementMapper) {
        this.listAccountMovementsService = listAccountMovementsService;
        this.accountMovementMapper = accountMovementMapper;
    }

    /**
     * Adapts path and query parameters into the bounded read use case and
     * returns only the approved page contract.
     *
     * @param accountId account whose movements are requested
     * @param request optional page, range, and type filters
     * @return matching movement page
     */
    @GetMapping("/{accountId}/movements")
    @ObservedApiOperation(ApiOperation.MOVEMENT_LIST)
    public AccountMovementPageResponse list(
            @PathVariable Long accountId,
            @Valid @ModelAttribute AccountMovementFilterRequest request) {
        MovementPageDto page = listAccountMovementsService.list(accountMovementMapper.toDto(accountId, request));
        return accountMovementMapper.toResponse(page);
    }
}

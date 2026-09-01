package com.elziojunior.simplifiedbankingservice.api;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elziojunior.simplifiedbankingservice.api.documentation.AccountMovementApi;
import com.elziojunior.simplifiedbankingservice.exception.AccountMovementValidationException;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.metrics.ObservedApiOperation;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementFilterRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMovementMapper;
import com.elziojunior.simplifiedbankingservice.service.AccountMovementService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** HTTP adapter for the read-only account-movement listing use case. */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountMovementController implements AccountMovementApi {

    private static final Set<String> SUPPORTED_QUERY_PARAMETERS = Set.of("page", "period", "type");

    private final AccountMovementService accountMovementService;
    private final AccountMovementMapper accountMovementMapper;

    public AccountMovementController(
            AccountMovementService accountMovementService,
            AccountMovementMapper accountMovementMapper) {
        this.accountMovementService = accountMovementService;
        this.accountMovementMapper = accountMovementMapper;
    }

    /**
     * Adapts path and query parameters into the bounded read use case and
     * returns only the approved page contract.
     *
     * @param accountId account whose movements are requested
     * @param filter optional page, recent-period, and type filters
     * @param httpRequest raw request used only to reject unknown query fields
     * @return matching movement page
     */
    @GetMapping("/{accountId}/movements")
    @ObservedApiOperation(ApiOperation.MOVEMENT_LIST)
    @Override
    public AccountMovementPageResponse list(
            @PathVariable Long accountId,
            @Valid @ModelAttribute AccountMovementFilterRequest filter,
            HttpServletRequest httpRequest) {
        validateQueryParameters(httpRequest);
        MovementPageDto page = accountMovementService.listAccountMovements(
                accountMovementMapper.toDto(accountId, filter));
        return accountMovementMapper.toResponse(page);
    }

    /**
     * Rejects removed or unknown query fields before application execution so
     * clients cannot mistake a default-period response for an unsupported query.
     */
    private void validateQueryParameters(HttpServletRequest request) {
        if (!SUPPORTED_QUERY_PARAMETERS.containsAll(request.getParameterMap().keySet())) {
            throw new AccountMovementValidationException("The movement query contains unsupported parameters.");
        }
    }
}

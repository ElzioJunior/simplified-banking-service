package com.elziojunior.simplifiedbankingservice.api;

import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;
import com.elziojunior.simplifiedbankingservice.api.documentation.AccountApi;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.metrics.ObservedApiOperation;
import com.elziojunior.simplifiedbankingservice.model.dto.CreatedAccountDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMapper;
import com.elziojunior.simplifiedbankingservice.service.CreateAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** HTTP adapter for the account-creation use case. */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController implements AccountApi {

    private final CreateAccountService createAccountService;
    private final AccountMapper accountMapper;

    public AccountController(CreateAccountService createAccountService, AccountMapper accountMapper) {
        this.createAccountService = createAccountService;
        this.accountMapper = accountMapper;
    }

    /**
     * Creates one account and returns only the approved creation contract;
     * account lookup links are omitted because query endpoints are out of scope.
     *
     * @param request validated public creation input
     * @return persisted account values
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ObservedApiOperation(ApiOperation.ACCOUNT_CREATE)
    @Override
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        CreatedAccountDto account = createAccountService.create(accountMapper.toDto(request));
        return accountMapper.toResponse(account);
    }
}

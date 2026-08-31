package com.elziojunior.simplifiedbankingservice.account.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.elziojunior.simplifiedbankingservice.account.service.CreateAccountCommand;
import com.elziojunior.simplifiedbankingservice.account.service.CreateAccountService;
import com.elziojunior.simplifiedbankingservice.account.service.CreatedAccount;

import jakarta.validation.Valid;

/** HTTP adapter for the account-creation use case. */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountService createAccountService;

    public AccountController(CreateAccountService createAccountService) {
        this.createAccountService = createAccountService;
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
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        CreatedAccount account = createAccountService.create(
                new CreateAccountCommand(request.name(), request.initialBalance()));
        return new AccountResponse(
                account.id(),
                account.name(),
                account.balance(),
                account.createdAt());
    }
}

package com.elziojunior.simplifiedbankingservice.api;

import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HTTP adapter for the account-creation use case. */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account creation operations")
public class AccountController {

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
    @Operation(
            summary = "Create an account",
            description = "Creates an account with a non-negative opening balance normalized to two decimal places.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateAccountRequest.class),
                            examples = {
                                    @ExampleObject(name = "positiveOpeningBalance", value = ApiExamples.ACCOUNT_CREATE),
                                    @ExampleObject(
                                            name = "zeroOpeningBalance", value = ApiExamples.ACCOUNT_CREATE_ZERO_BALANCE),
                                    @ExampleObject(
                                            name = "blankNameValidation", value = ApiExamples.ACCOUNT_CREATE_BLANK_NAME),
                                    @ExampleObject(
                                            name = "missingBalanceValidation", value = ApiExamples.ACCOUNT_CREATE_MISSING_BALANCE),
                                    @ExampleObject(
                                            name = "negativeBalanceValidation", value = ApiExamples.ACCOUNT_CREATE_NEGATIVE_BALANCE),
                                    @ExampleObject(
                                            name = "unsupportedBalanceValidation",
                                            value = ApiExamples.ACCOUNT_CREATE_UNSUPPORTED_BALANCE)
                            })))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created",
                    content = @Content(
                            schema = @Schema(implementation = AccountResponse.class),
                            examples = @ExampleObject(name = "createdAccount", value = ApiExamples.ACCOUNT_RESPONSE))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or unreadable account request",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = {
                                    @ExampleObject(name = "constraintValidation", value = ApiExamples.INVALID_REQUEST),
                                    @ExampleObject(name = "malformedJson", value = ApiExamples.INVALID_BODY),
                                    @ExampleObject(
                                            name = "unsupportedMonetaryRange",
                                            value = ApiExamples.UNSUPPORTED_ACCOUNT_BALANCE)
                            }))
    })
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        CreatedAccountDto account = createAccountService.create(accountMapper.toDto(request));
        return accountMapper.toResponse(account);
    }
}

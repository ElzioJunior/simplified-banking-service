package com.elziojunior.simplifiedbankingservice.api.documentation;

import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** OpenAPI contract for account operations, separated from the runtime controller adapter. */
@Tag(name = "Accounts", description = "Account creation operations")
public interface AccountApi {

    /** Publishes the account-creation success and validation contract used by API consumers. */
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
                                            name = "zeroOpeningBalance",
                                            value = ApiExamples.ACCOUNT_CREATE_ZERO_BALANCE),
                                    @ExampleObject(
                                            name = "blankNameValidation",
                                            value = ApiExamples.ACCOUNT_CREATE_BLANK_NAME),
                                    @ExampleObject(
                                            name = "missingBalanceValidation",
                                            value = ApiExamples.ACCOUNT_CREATE_MISSING_BALANCE),
                                    @ExampleObject(
                                            name = "negativeBalanceValidation",
                                            value = ApiExamples.ACCOUNT_CREATE_NEGATIVE_BALANCE),
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
                            examples = @ExampleObject(
                                    name = "createdAccount", value = ApiExamples.ACCOUNT_RESPONSE))),
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
    AccountResponse create(CreateAccountRequest request);
}

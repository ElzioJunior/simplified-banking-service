package com.elziojunior.simplifiedbankingservice.api.documentation;

import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementFilterRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;

import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** OpenAPI contract for account movement history, separated from request validation and orchestration. */
@Tag(name = "Account movements", description = "Paginated account movement history")
public interface AccountMovementApi {

    /** Publishes the movement-list parameters, page responses, and principal client-visible failures. */
    @Operation(
            summary = "List account movements",
            description = "Returns a fixed-size page ordered newest first for the selected recent period.")
    @Parameters({
            @Parameter(
                    name = "page",
                    description = "Zero-based page; defaults to zero",
                    examples = {
                            @ExampleObject(name = "firstPage", value = "0"),
                            @ExampleObject(name = "laterPage", value = "2"),
                            @ExampleObject(name = "invalidNegativePage", value = "-1")
                    }),
            @Parameter(
                    name = "period",
                    description = "Recent-history period; defaults to 1d",
                    examples = {
                            @ExampleObject(name = "oneDay", value = "1d"),
                            @ExampleObject(name = "oneWeek", value = "1w"),
                            @ExampleObject(name = "oneMonth", value = "1M"),
                            @ExampleObject(name = "invalidPeriod", value = "30d")
                    }),
            @Parameter(
                    name = "type",
                    description = "Optional movement direction",
                    examples = {
                            @ExampleObject(name = "credit", value = "CREDIT"),
                            @ExampleObject(name = "debit", value = "DEBIT"),
                            @ExampleObject(name = "invalidType", value = "UNKNOWN")
                    })
    })
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Movement page, including an empty page when no movement matches",
                    content = @Content(
                            schema = @Schema(implementation = AccountMovementPageResponse.class),
                            examples = {
                                    @ExampleObject(name = "filteredMovementPage", value = ApiExamples.MOVEMENT_PAGE),
                                    @ExampleObject(
                                            name = "emptyMovementPage", value = ApiExamples.EMPTY_MOVEMENT_PAGE)
                            })),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or unsupported query parameters",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = {
                                    @ExampleObject(name = "transportValidation", value = ApiExamples.INVALID_REQUEST),
                                    @ExampleObject(
                                            name = "invalidPeriod", value = ApiExamples.INVALID_MOVEMENT_PERIOD)
                            })),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "unknownAccount", value = ApiExamples.ACCOUNT_NOT_FOUND))),
            @ApiResponse(
                    responseCode = "503",
                    description = "Movement persistence is temporarily unavailable",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "persistenceUnavailable", value = ApiExamples.MOVEMENT_UNAVAILABLE)))
    })
    AccountMovementPageResponse list(
            @Parameter(description = "Existing account identifier", required = true, example = "41") Long accountId,
            AccountMovementFilterRequest filter,
            @Parameter(hidden = true) HttpServletRequest httpRequest);
}

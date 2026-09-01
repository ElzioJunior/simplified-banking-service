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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HTTP adapter for the read-only account-movement listing use case. */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account movements", description = "Paginated account movement history")
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
    @Operation(
            summary = "List account movements",
            description = "Returns a fixed-size page ordered newest first. Start is inclusive and end is exclusive.")
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
                    name = "start",
                    description = "Optional inclusive ISO 8601 date-time",
                    examples = {
                            @ExampleObject(name = "utcStart", value = "2026-08-01T00:00:00Z"),
                            @ExampleObject(name = "reversedRangeStart", value = "2026-09-02T00:00:00Z"),
                            @ExampleObject(name = "invalidDate", value = "not-a-date")
                    }),
            @Parameter(
                    name = "end",
                    description = "Optional exclusive ISO 8601 date-time; must be after start",
                    examples = {
                            @ExampleObject(name = "utcEnd", value = "2026-09-01T00:00:00Z"),
                            @ExampleObject(name = "reversedRangeEnd", value = "2026-09-01T00:00:00Z")
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
                                    @ExampleObject(name = "emptyMovementPage", value = ApiExamples.EMPTY_MOVEMENT_PAGE)
                            })),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page, date, range, or movement type",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = {
                                    @ExampleObject(name = "transportValidation", value = ApiExamples.INVALID_REQUEST),
                                    @ExampleObject(
                                            name = "invalidDateRange", value = ApiExamples.INVALID_MOVEMENT_RANGE)
                            })),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(name = "unknownAccount", value = ApiExamples.ACCOUNT_NOT_FOUND))),
            @ApiResponse(
                    responseCode = "503",
                    description = "Movement persistence is temporarily unavailable",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "persistenceUnavailable", value = ApiExamples.MOVEMENT_UNAVAILABLE)))
    })
    public AccountMovementPageResponse list(
            @Parameter(description = "Existing account identifier", required = true, example = "41")
            @PathVariable Long accountId,
            @Valid @ModelAttribute AccountMovementFilterRequest request) {
        MovementPageDto page = listAccountMovementsService.list(accountMovementMapper.toDto(accountId, request));
        return accountMovementMapper.toResponse(page);
    }
}

package com.elziojunior.simplifiedbankingservice.api.documentation;

import java.util.UUID;

import com.elziojunior.simplifiedbankingservice.model.api.CreateTransferRequest;
import com.elziojunior.simplifiedbankingservice.model.api.TransferResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** OpenAPI contract for idempotent transfers, separated from the financial HTTP adapter. */
@Tag(name = "Transfers", description = "Idempotent account-to-account transfers")
public interface TransferApi {

    /** Publishes the transfer success, validation, conflict, and temporary-failure contract. */
    @Operation(
            summary = "Transfer funds",
            description = "Creates a transfer or replays its established result when the token and normalized payload "
                    + "match.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateTransferRequest.class),
                            examples = {
                                    @ExampleObject(name = "successfulTransfer", value = ApiExamples.TRANSFER_CREATE),
                                    @ExampleObject(name = "idempotentReplay", value = ApiExamples.TRANSFER_REPLAY),
                                    @ExampleObject(
                                            name = "tokenPayloadMismatch",
                                            value = ApiExamples.TRANSFER_TOKEN_PAYLOAD_MISMATCH),
                                    @ExampleObject(
                                            name = "missingAccountValidation",
                                            value = ApiExamples.TRANSFER_MISSING_ACCOUNT),
                                    @ExampleObject(
                                            name = "zeroAmountValidation", value = ApiExamples.TRANSFER_ZERO_AMOUNT),
                                    @ExampleObject(
                                            name = "unsupportedAmountValidation",
                                            value = ApiExamples.TRANSFER_UNSUPPORTED_AMOUNT),
                                    @ExampleObject(
                                            name = "sameAccountConflict", value = ApiExamples.TRANSFER_SAME_ACCOUNT),
                                    @ExampleObject(
                                            name = "insufficientFundsConflict",
                                            value = ApiExamples.TRANSFER_INSUFFICIENT_FUNDS),
                                    @ExampleObject(
                                            name = "unknownAccount", value = ApiExamples.TRANSFER_UNKNOWN_ACCOUNT)
                            })))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transfer completed or an identical completed transfer replayed",
                    content = @Content(
                            schema = @Schema(implementation = TransferResponse.class),
                            examples = @ExampleObject(
                                    name = "completedTransfer", value = ApiExamples.TRANSFER_RESPONSE))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or malformed token, unreadable body, or invalid transfer fields",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = {
                                    @ExampleObject(
                                            name = "missingIdempotencyKey",
                                            value = ApiExamples.MISSING_IDEMPOTENCY_KEY),
                                    @ExampleObject(
                                            name = "malformedIdempotencyKey",
                                            value = ApiExamples.MALFORMED_IDEMPOTENCY_KEY),
                                    @ExampleObject(name = "constraintValidation", value = ApiExamples.INVALID_REQUEST),
                                    @ExampleObject(name = "malformedJson", value = ApiExamples.INVALID_BODY),
                                    @ExampleObject(
                                            name = "unsupportedMonetaryRange",
                                            value = ApiExamples.UNSUPPORTED_TRANSFER_AMOUNT)
                            })),
            @ApiResponse(
                    responseCode = "404",
                    description = "Source or destination account not found",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "unknownTransferAccount",
                                    value = ApiExamples.TRANSFER_ACCOUNT_NOT_FOUND))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Account, balance, or idempotency token conflicts with the requested transfer",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = {
                                    @ExampleObject(name = "sameAccount", value = ApiExamples.SAME_ACCOUNT_CONFLICT),
                                    @ExampleObject(
                                            name = "insufficientFunds",
                                            value = ApiExamples.INSUFFICIENT_FUNDS_CONFLICT),
                                    @ExampleObject(name = "invalidToken", value = ApiExamples.INVALID_TOKEN_CONFLICT),
                                    @ExampleObject(name = "expiredToken", value = ApiExamples.EXPIRED_TOKEN_CONFLICT),
                                    @ExampleObject(
                                            name = "tokenPayloadMismatch",
                                            value = ApiExamples.TOKEN_PAYLOAD_MISMATCH_CONFLICT)
                            })),
            @ApiResponse(
                    responseCode = "503",
                    description = "The required persistence resources were not available within their bound",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "persistenceUnavailable", value = ApiExamples.TRANSFER_UNAVAILABLE)))
    })
    TransferResponse create(
            @Parameter(
                    description = "Server-issued transfer token",
                    required = true,
                    example = "4e80db4d-ce8c-40a6-b839-b45fd45b1461")
            UUID token,
            CreateTransferRequest request);
}

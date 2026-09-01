package com.elziojunior.simplifiedbankingservice.api.documentation;

import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** OpenAPI contract for server-issued transfer tokens, separated from runtime orchestration. */
@Tag(name = "Transfer tokens", description = "Server-issued idempotency tokens for transfers")
public interface TransferTokenApi {

    /** Publishes the token-issuance success and temporary-failure contract. */
    @Operation(
            summary = "Issue a transfer token",
            description = "Issues a UUID token that must be supplied through Idempotency-Key within 10 minutes.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transfer token issued",
                    content = @Content(
                            schema = @Schema(implementation = TransferTokenResponse.class),
                            examples = @ExampleObject(
                                    name = "issuedTransferToken", value = ApiExamples.TRANSFER_TOKEN_RESPONSE))),
            @ApiResponse(
                    responseCode = "503",
                    description = "Token persistence is temporarily unavailable",
                    content = @Content(
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "persistenceUnavailable",
                                    value = ApiExamples.TRANSFER_PERSISTENCE_UNAVAILABLE)))
    })
    TransferTokenResponse issue();
}

package com.elziojunior.simplifiedbankingservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.metrics.ObservedApiOperation;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferTokenMapper;
import com.elziojunior.simplifiedbankingservice.service.IssueTransferTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HTTP adapter for server-issued transfer idempotency tokens. */
@RestController
@RequestMapping("/api/v1/transfer-tokens")
@Tag(name = "Transfer tokens", description = "Server-issued idempotency tokens for transfers")
public class TransferTokenController {

    private final IssueTransferTokenService issueTransferTokenService;
    private final TransferTokenMapper transferTokenMapper;

    public TransferTokenController(
            IssueTransferTokenService issueTransferTokenService, TransferTokenMapper transferTokenMapper) {
        this.issueTransferTokenService = issueTransferTokenService;
        this.transferTokenMapper = transferTokenMapper;
    }

    /** Issues the prerequisite token so a later transfer can be retried safely. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ObservedApiOperation(ApiOperation.TRANSFER_TOKEN_ISSUE)
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
    public TransferTokenResponse issue() {
        IssuedTransferTokenDto issued = issueTransferTokenService.issue();
        return transferTokenMapper.toResponse(issued);
    }
}

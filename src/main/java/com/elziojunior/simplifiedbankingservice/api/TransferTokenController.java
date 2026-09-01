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

/** HTTP adapter for server-issued transfer idempotency tokens. */
@RestController
@RequestMapping("/api/v1/transfer-tokens")
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
    public TransferTokenResponse issue() {
        IssuedTransferTokenDto issued = issueTransferTokenService.issue();
        return transferTokenMapper.toResponse(issued);
    }
}

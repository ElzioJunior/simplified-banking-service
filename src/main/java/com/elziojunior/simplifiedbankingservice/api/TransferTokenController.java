package com.elziojunior.simplifiedbankingservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;
import com.elziojunior.simplifiedbankingservice.metrics.ApiMetrics;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferTokenMapper;
import com.elziojunior.simplifiedbankingservice.service.IssueTransferTokenService;

/** HTTP adapter for server-issued transfer idempotency tokens. */
@RestController
@RequestMapping("/api/v1/transfer-tokens")
public class TransferTokenController {

    private final IssueTransferTokenService issueTransferTokenService;
    private final TransferTokenMapper transferTokenMapper;
    private final ApiMetrics apiMetrics;

    public TransferTokenController(
            IssueTransferTokenService issueTransferTokenService,
            TransferTokenMapper transferTokenMapper,
            ApiMetrics apiMetrics) {
        this.issueTransferTokenService = issueTransferTokenService;
        this.transferTokenMapper = transferTokenMapper;
        this.apiMetrics = apiMetrics;
    }

    /** Issues the prerequisite token so a later transfer can be retried safely. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferTokenResponse issue() {
        IssuedTransferTokenDto issued = apiMetrics.observe(
                ApiOperation.TRANSFER_TOKEN_ISSUE,
                issueTransferTokenService::issue);
        return transferTokenMapper.toResponse(issued);
    }
}

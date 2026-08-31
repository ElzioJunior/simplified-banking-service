package com.elziojunior.simplifiedbankingservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;
import com.elziojunior.simplifiedbankingservice.service.IssueTransferTokenService;

/** HTTP adapter for server-issued transfer idempotency tokens. */
@RestController
@RequestMapping("/api/v1/transfer-tokens")
public class TransferTokenController {

    private final IssueTransferTokenService issueTransferTokenService;

    public TransferTokenController(IssueTransferTokenService issueTransferTokenService) {
        this.issueTransferTokenService = issueTransferTokenService;
    }

    /** Issues the prerequisite token so a later transfer can be retried safely. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferTokenResponse issue() {
        IssuedTransferTokenDto issued = issueTransferTokenService.issue();
        return new TransferTokenResponse(issued.token(), issued.expiresAt());
    }
}

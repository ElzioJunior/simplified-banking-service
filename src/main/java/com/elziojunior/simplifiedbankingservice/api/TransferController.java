package com.elziojunior.simplifiedbankingservice.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elziojunior.simplifiedbankingservice.model.api.TransferResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.service.CreateTransferCommand;
import com.elziojunior.simplifiedbankingservice.service.CreateTransferService;
import com.elziojunior.simplifiedbankingservice.service.TransferMetrics;

import jakarta.validation.Valid;

/** HTTP adapter for idempotent account-to-account transfers. */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private static final String COMPLETED = "COMPLETED";
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferController.class);

    private final CreateTransferService createTransferService;
    private final TransferMetrics transferMetrics;

    public TransferController(CreateTransferService createTransferService, TransferMetrics transferMetrics) {
        this.createTransferService = createTransferService;
        this.transferMetrics = transferMetrics;
    }

    /** Executes or replays one transfer while keeping HTTP concerns outside financial behavior. */
    @PostMapping
    public TransferResponse create(
            @RequestHeader("Idempotency-Key") UUID token,
            @Valid @RequestBody CreateTransferRequest request) {
        CompletedTransferDto transfer = transferMetrics.observe(() -> createTransferService.create(
                new CreateTransferCommand(
                        token, request.sourceAccountId(), request.destinationAccountId(), request.amount())));
        LOGGER.info("Transfer request completed with operationId={}", transfer.transferId());
        return new TransferResponse(
                transfer.transferId(),
                COMPLETED,
                transfer.sourceAccountId(),
                transfer.destinationAccountId(),
                transfer.amount());
    }
}

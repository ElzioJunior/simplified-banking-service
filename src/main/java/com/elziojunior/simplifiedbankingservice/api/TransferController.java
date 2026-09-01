package com.elziojunior.simplifiedbankingservice.api;

import java.util.UUID;

import com.elziojunior.simplifiedbankingservice.model.api.CreateTransferRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elziojunior.simplifiedbankingservice.model.api.TransferResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.TransferMapper;
import com.elziojunior.simplifiedbankingservice.service.CreateTransferService;
import com.elziojunior.simplifiedbankingservice.metrics.ApiOperation;
import com.elziojunior.simplifiedbankingservice.metrics.ObservedApiOperation;

import jakarta.validation.Valid;

/** HTTP adapter for idempotent account-to-account transfers. */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferController.class);

    private final CreateTransferService createTransferService;
    private final TransferMapper transferMapper;

    public TransferController(CreateTransferService createTransferService, TransferMapper transferMapper) {
        this.createTransferService = createTransferService;
        this.transferMapper = transferMapper;
    }

    /** Executes or replays one transfer while keeping HTTP concerns outside financial behavior. */
    @PostMapping
    @ObservedApiOperation(ApiOperation.TRANSFER_CREATE)
    public TransferResponse create(
            @RequestHeader("Idempotency-Key") UUID token,
            @Valid @RequestBody CreateTransferRequest request) {
        CompletedTransferDto transfer = createTransferService.create(transferMapper.toDto(token, request));
        LOGGER.info("Transfer request completed with operationId={}", transfer.transferId());
        return transferMapper.toResponse(transfer);
    }
}

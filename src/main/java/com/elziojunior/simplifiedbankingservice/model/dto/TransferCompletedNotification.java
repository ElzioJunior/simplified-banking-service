package com.elziojunior.simplifiedbankingservice.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Stable, minimal RabbitMQ transfer-completed event. */
public record TransferCompletedNotification(
        UUID eventId,
        UUID operationId,
        Long recipientAccountId,
        String eventType,
        BigDecimal amount,
        OffsetDateTime occurredAt) {

    public static final String TRANSFER_COMPLETED = "TRANSFER_COMPLETED";
}

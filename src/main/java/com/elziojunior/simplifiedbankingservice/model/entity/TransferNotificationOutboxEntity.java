package com.elziojunior.simplifiedbankingservice.model.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Durable transfer-completed notification intent awaiting RabbitMQ publication. */
@Entity
@Table(name = "transfer_notification_outbox")
public class TransferNotificationOutboxEntity {

    public static final String TRANSFER_COMPLETED = "TRANSFER_COMPLETED";

    /** Stable event identity used for downstream deduplication. */
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    /** Transfer operation represented by this intent. */
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    /** Source account whose holder receives the notification. */
    @Column(name = "recipient_account_id", nullable = false)
    private Long recipientAccountId;

    /** Stable event discriminator. */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /** Scale-two amount required by the notification contract. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** UTC transfer completion instant. */
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    /** UTC broker-confirmation instant, absent while pending. */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /** Number of publication attempts made. */
    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    /** UTC instant of the latest publication attempt. */
    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    protected TransferNotificationOutboxEntity() {
    }

    public TransferNotificationOutboxEntity(
            UUID eventId,
            UUID operationId,
            Long recipientAccountId,
            BigDecimal amount,
            OffsetDateTime occurredAt) {
        this.eventId = eventId;
        this.operationId = operationId;
        this.recipientAccountId = recipientAccountId;
        this.eventType = TRANSFER_COMPLETED;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    /** Records a failed publication attempt while preserving pending state for retry. */
    public void recordFailedAttempt(OffsetDateTime attemptedAt) {
        publishAttempts++;
        lastAttemptAt = attemptedAt;
    }

    /** Records confirmed broker publication so scheduled work no longer selects the event. */
    public void markPublished(OffsetDateTime publishedAt) {
        publishAttempts++;
        lastAttemptAt = publishedAt;
        this.publishedAt = publishedAt;
    }

    public UUID getEventId() { return eventId; }
    public UUID getOperationId() { return operationId; }
    public Long getRecipientAccountId() { return recipientAccountId; }
    public String getEventType() { return eventType; }
    public BigDecimal getAmount() { return amount; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public int getPublishAttempts() { return publishAttempts; }
    public OffsetDateTime getLastAttemptAt() { return lastAttemptAt; }
}

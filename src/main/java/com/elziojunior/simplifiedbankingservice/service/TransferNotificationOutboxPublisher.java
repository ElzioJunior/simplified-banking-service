package com.elziojunior.simplifiedbankingservice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.elziojunior.simplifiedbankingservice.configuration.TransferNotificationConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;
import com.elziojunior.simplifiedbankingservice.model.entity.TransferNotificationOutboxEntity;
import com.elziojunior.simplifiedbankingservice.repository.TransferNotificationOutboxRepository;

/** Publishes bounded pending outbox batches with broker confirmation and retryable failure. */
@Service
@ConditionalOnProperty(name = "transfer.notifications.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class TransferNotificationOutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferNotificationOutboxPublisher.class);

    private final TransferNotificationOutboxRepository repository;
    private final TransferNotificationOutboxStateService stateService;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;
    private final long confirmationTimeoutMilliseconds;

    public TransferNotificationOutboxPublisher(
            TransferNotificationOutboxRepository repository,
            TransferNotificationOutboxStateService stateService,
            RabbitTemplate rabbitTemplate,
            @Value("${transfer.notifications.publisher.batch-size:50}") int batchSize,
            @Value("${transfer.notifications.publisher.confirm-timeout-ms:5000}") long confirmationTimeoutMilliseconds) {
        this.repository = repository;
        this.stateService = stateService;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
        this.confirmationTimeoutMilliseconds = confirmationTimeoutMilliseconds;
    }

    /** Polls pending intents; a failed event remains pending and does not stop later events. */
    @Scheduled(fixedDelayString = "${transfer.notifications.publisher.fixed-delay-ms:1000}")
    public void publishPending() {
        List<TransferNotificationOutboxEntity> pending = repository
                .findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, batchSize));
        pending.forEach(this::publish);
    }

    private void publish(TransferNotificationOutboxEntity entity) {
        TransferCompletedNotification notification = new TransferCompletedNotification(
                entity.getEventId(), entity.getOperationId(), entity.getRecipientAccountId(), entity.getEventType(),
                entity.getAmount(), entity.getOccurredAt());
        try {
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(
                        TransferNotificationConfiguration.EXCHANGE,
                        TransferNotificationConfiguration.ROUTING_KEY,
                        notification);
                operations.waitForConfirmsOrDie(confirmationTimeoutMilliseconds);
                return null;
            });
            stateService.markPublished(entity.getEventId());
        } catch (AmqpException exception) {
            stateService.recordFailedAttempt(entity.getEventId());
            LOGGER.warn("Transfer notification publication remains pending for eventId={}", entity.getEventId());
        }
    }
}

package com.elziojunior.simplifiedbankingservice.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.elziojunior.simplifiedbankingservice.configuration.TransferNotificationConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;

/** Sends committed transfer notifications to RabbitMQ with count- and time-bounded retry. */
@Service
public class TransferNotificationPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final int maxAttempts;
    private final long maxDurationNanos;

    public TransferNotificationPublisher(RabbitTemplate rabbitTemplate,
            @Value("${transfer.notifications.publisher.max-attempts:3}") int maxAttempts,
            @Value("${transfer.notifications.publisher.max-duration:3s}") Duration maxDuration) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Notification publication max attempts must be positive");
        }
        if (maxDuration.isZero() || maxDuration.isNegative()) {
            throw new IllegalArgumentException("Notification publication max duration must be positive");
        }
        this.rabbitTemplate = rabbitTemplate;
        this.maxAttempts = maxAttempts;
        this.maxDurationNanos = maxDuration.toNanos();
    }

    /**
     * Sends an event directly and contains exhausted broker failures so notification
     * availability never changes the completed financial result.
     */
    public void publish(TransferCompletedNotification notification) {
        long startedAt = nanoTime();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                rabbitTemplate.convertAndSend(
                        TransferNotificationConfiguration.EXCHANGE,
                        TransferNotificationConfiguration.ROUTING_KEY,
                        notification);
                return;
            } catch (AmqpException exception) {
                if (attempt == maxAttempts || nanoTime() - startedAt >= maxDurationNanos) {
                    LOGGER.warn(
                            "Transfer notification publication failed within retry bounds for eventId={}",
                            notification.eventId());
                    return;
                }
            }
        }
    }

    /** Supplies monotonic elapsed time so retry duration is unaffected by wall-clock changes. */
    long nanoTime() {
        return System.nanoTime();
    }
}

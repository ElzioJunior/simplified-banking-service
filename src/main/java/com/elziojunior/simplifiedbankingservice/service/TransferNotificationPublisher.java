package com.elziojunior.simplifiedbankingservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.elziojunior.simplifiedbankingservice.configuration.TransferNotificationConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;

/** Sends transfer notifications directly to RabbitMQ with bounded in-memory retry. */
@Service
public class TransferNotificationPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final int maxAttempts;

    public TransferNotificationPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${transfer.notifications.publisher.max-attempts:3}") int maxAttempts) {
        this.rabbitTemplate = rabbitTemplate;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Sends an event directly and contains exhausted broker failures so notification
     * availability never changes the completed financial result.
     */
    public void publish(TransferCompletedNotification notification) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                rabbitTemplate.convertAndSend(
                        TransferNotificationConfiguration.EXCHANGE,
                        TransferNotificationConfiguration.ROUTING_KEY,
                        notification);
                return;
            } catch (AmqpException exception) {
                if (attempt == maxAttempts) {
                    LOGGER.warn(
                            "Transfer notification publication failed after retries for eventId={}",
                            notification.eventId());
                }
            }
        }
    }
}

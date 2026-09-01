package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.elziojunior.simplifiedbankingservice.configuration.TransferNotificationConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;

class TransferNotificationPublisherTest {

    /** Proves a notification is sent directly without polling or confirmation state. */
    @Test
    void shouldPublishEventDirectly() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        TransferNotificationPublisher publisher = new TransferNotificationPublisher(rabbitTemplate, 3);
        TransferCompletedNotification notification = notification();

        publisher.publish(notification);

        verify(rabbitTemplate).convertAndSend(
                TransferNotificationConfiguration.EXCHANGE,
                TransferNotificationConfiguration.ROUTING_KEY,
                notification);
    }

    /** Proves transient broker failures are retried in memory until one send succeeds. */
    @Test
    void shouldRetryFailedPublication() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        TransferNotificationPublisher publisher = new TransferNotificationPublisher(rabbitTemplate, 3);
        TransferCompletedNotification notification = notification();
        doThrow(brokerFailure()).doThrow(brokerFailure()).doNothing()
                .when(rabbitTemplate).convertAndSend(
                        TransferNotificationConfiguration.EXCHANGE,
                        TransferNotificationConfiguration.ROUTING_KEY,
                        notification);

        publisher.publish(notification);

        verify(rabbitTemplate, times(3)).convertAndSend(
                TransferNotificationConfiguration.EXCHANGE,
                TransferNotificationConfiguration.ROUTING_KEY,
                notification);
    }

    /** Proves exhausted publication attempts do not escape and invalidate financial completion. */
    @Test
    void shouldContainExhaustedPublicationFailure() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        TransferNotificationPublisher publisher = new TransferNotificationPublisher(rabbitTemplate, 3);
        TransferCompletedNotification notification = notification();
        doThrow(brokerFailure()).when(rabbitTemplate).convertAndSend(
                TransferNotificationConfiguration.EXCHANGE,
                TransferNotificationConfiguration.ROUTING_KEY,
                notification);

        assertThatCode(() -> publisher.publish(notification)).doesNotThrowAnyException();

        verify(rabbitTemplate, times(3)).convertAndSend(
                TransferNotificationConfiguration.EXCHANGE,
                TransferNotificationConfiguration.ROUTING_KEY,
                notification);
    }

    private TransferCompletedNotification notification() {
        return new TransferCompletedNotification(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                1L,
                TransferCompletedNotification.TRANSFER_COMPLETED,
                new BigDecimal("10.00"),
                OffsetDateTime.parse("2026-08-31T14:00:00Z"));
    }

    private AmqpIOException brokerFailure() {
        return new AmqpIOException(new IOException("broker unavailable"));
    }
}

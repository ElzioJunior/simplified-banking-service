package com.elziojunior.simplifiedbankingservice.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.elziojunior.simplifiedbankingservice.configuration.TransferNotificationConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;
import com.elziojunior.simplifiedbankingservice.service.TransferNotificationPublisher;

@Testcontainers
@Tag("integrated")
@SpringBootTest(
        classes = TransferNotificationPublisherIntegratedFunctionalTest.RabbitTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "transfer.notifications.publisher.max-attempts=1")
class TransferNotificationPublisherIntegratedFunctionalTest {

    @Container
    @ServiceConnection
    private static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:4.1.4-management-alpine");

    @Autowired
    private TransferNotificationPublisher publisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** Proves the production publisher, topology, routing, and JSON conversion preserve the stable event contract. */
    @Test
    void shouldPublishAndConsumeTransferCompletedNotification() {
        TransferCompletedNotification expected = new TransferCompletedNotification(
                UUID.fromString("00000000-0000-0000-0000-000000000031"),
                UUID.fromString("00000000-0000-0000-0000-000000000032"),
                41L,
                TransferCompletedNotification.TRANSFER_COMPLETED,
                new BigDecimal("12.34"),
                OffsetDateTime.parse("2026-09-01T12:30:00Z"));

        publisher.publish(expected);

        Object received = await().atMost(Duration.ofSeconds(5)).until(
                () -> rabbitTemplate.receiveAndConvert(TransferNotificationConfiguration.QUEUE),
                value -> value != null);
        assertThat(received).isInstanceOf(TransferCompletedNotification.class);
        TransferCompletedNotification actual = (TransferCompletedNotification) received;
        assertThat(actual.eventId()).isEqualTo(expected.eventId());
        assertThat(actual.operationId()).isEqualTo(expected.operationId());
        assertThat(actual.recipientAccountId()).isEqualTo(expected.recipientAccountId());
        assertThat(actual.eventType()).isEqualTo(expected.eventType());
        assertThat(actual.amount()).isEqualByComparingTo(expected.amount());
        assertThat(actual.occurredAt()).isEqualTo(expected.occurredAt());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import({TransferNotificationConfiguration.class, TransferNotificationPublisher.class})
    static class RabbitTestApplication {
    }
}

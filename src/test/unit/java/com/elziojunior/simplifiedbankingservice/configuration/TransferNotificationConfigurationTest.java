package com.elziojunior.simplifiedbankingservice.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;

import com.rabbitmq.client.ConnectionFactory;

class TransferNotificationConfigurationTest {

    /** Proves every potentially blocking RabbitMQ connection stage receives a finite configured timeout. */
    @Test
    void shouldApplyFiniteRabbitMqClientTimeouts() {
        TransferNotificationConfiguration configuration = new TransferNotificationConfiguration();
        ConnectionFactoryCustomizer customizer = configuration.transferNotificationConnectionFactoryCustomizer(
                Duration.ofMillis(700), Duration.ofMillis(800), Duration.ofMillis(900));
        ConnectionFactory connectionFactory = new ConnectionFactory();

        customizer.customize(connectionFactory);

        assertThat(connectionFactory.getConnectionTimeout()).isEqualTo(700);
        assertThat(connectionFactory.getHandshakeTimeout()).isEqualTo(800);
        assertThat(connectionFactory.getChannelRpcTimeout()).isEqualTo(900);
    }

    /** Proves zero and sub-millisecond waits cannot disable or bypass the finite RabbitMQ timeout contract. */
    @Test
    void shouldRejectNonpositiveOrSubMillisecondTimeouts() {
        TransferNotificationConfiguration configuration = new TransferNotificationConfiguration();

        assertThatThrownBy(() -> configuration.transferNotificationConnectionFactoryCustomizer(
                Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> configuration.transferNotificationConnectionFactoryCustomizer(
                Duration.ofNanos(1), Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

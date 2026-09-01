package com.elziojunior.simplifiedbankingservice.configuration;

import java.time.Duration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declares the RabbitMQ boundary used by direct transfer notification events. */
@Configuration
@EnableRabbit
public class TransferNotificationConfiguration {

    public static final String EXCHANGE = "banking.transfer.notifications";
    public static final String QUEUE = "banking.transfer.notifications.completed";
    public static final String ROUTING_KEY = "transfer.completed";

    @Bean
    DirectExchange transferNotificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue transferNotificationQueue() {
        return new Queue(QUEUE, true, false, false);
    }

    @Bean
    Binding transferNotificationBinding(DirectExchange transferNotificationExchange, Queue transferNotificationQueue) {
        return BindingBuilder.bind(transferNotificationQueue)
                .to(transferNotificationExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter transferNotificationMessageConverter() {
        return new Jackson2JsonMessageConverter("com.elziojunior.simplifiedbankingservice.model.dto");
    }

    /**
     * Applies finite client waits so one direct send cannot retain the request thread
     * beyond unbounded TCP, handshake, or channel RPC operations.
     */
    @Bean
    ConnectionFactoryCustomizer transferNotificationConnectionFactoryCustomizer(
            @Value("${transfer.notifications.publisher.connection-timeout:1s}") Duration connectionTimeout,
            @Value("${transfer.notifications.publisher.handshake-timeout:1s}") Duration handshakeTimeout,
            @Value("${transfer.notifications.publisher.channel-rpc-timeout:1s}") Duration channelRpcTimeout) {
        int connectionTimeoutMillis = positiveMilliseconds(connectionTimeout, "connection timeout");
        int handshakeTimeoutMillis = positiveMilliseconds(handshakeTimeout, "handshake timeout");
        int channelRpcTimeoutMillis = positiveMilliseconds(channelRpcTimeout, "channel RPC timeout");
        return connectionFactory -> {
            connectionFactory.setConnectionTimeout(connectionTimeoutMillis);
            connectionFactory.setHandshakeTimeout(handshakeTimeoutMillis);
            connectionFactory.setChannelRpcTimeout(channelRpcTimeoutMillis);
        };
    }

    /** Converts a configured finite duration to the positive millisecond range required by the RabbitMQ client. */
    private int positiveMilliseconds(Duration duration, String setting) {
        long milliseconds = duration.toMillis();
        if (milliseconds < 1 || milliseconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Transfer notification " + setting + " must fit positive milliseconds");
        }
        return (int) milliseconds;
    }
}

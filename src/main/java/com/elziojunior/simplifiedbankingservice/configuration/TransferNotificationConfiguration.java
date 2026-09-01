package com.elziojunior.simplifiedbankingservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Declares the durable RabbitMQ boundary used by transfer notification intents. */
@Configuration
@EnableRabbit
@EnableScheduling
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
    Binding transferNotificationBinding(DirectExchange transferNotificationExchange,
            Queue transferNotificationQueue) {
        return BindingBuilder.bind(transferNotificationQueue)
                .to(transferNotificationExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter transferNotificationMessageConverter() {
        return new Jackson2JsonMessageConverter(
                "com.elziojunior.simplifiedbankingservice.model.dto");
    }
}

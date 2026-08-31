package com.elziojunior.simplifiedbankingservice.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies the application clock used for auditable creation timestamps. */
@Configuration
public class TimeConfiguration {

    /**
     * Uses UTC as the production time source so persisted instants and API
     * responses remain unambiguous across runtime time zones.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

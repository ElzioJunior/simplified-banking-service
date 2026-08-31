package com.elziojunior.simplifiedbankingservice.configuration;

import java.time.Clock;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.elziojunior.simplifiedbankingservice.service.UuidGenerator;

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

    /** Supplies random UUIDs while allowing financial tests to control identity generation. */
    @Bean
    UuidGenerator uuidGenerator() {
        return UUID::randomUUID;
    }
}

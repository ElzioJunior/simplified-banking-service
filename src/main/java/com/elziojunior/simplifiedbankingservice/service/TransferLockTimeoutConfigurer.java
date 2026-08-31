package com.elziojunior.simplifiedbankingservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;

/** Applies the configured PostgreSQL lock wait bound to the current transfer transaction. */
@Component
public class TransferLockTimeoutConfigurer {

    private final EntityManager entityManager;
    private final long timeoutMilliseconds;

    public TransferLockTimeoutConfigurer(
            EntityManager entityManager,
            @Value("${spring.jpa.properties.jakarta.persistence.lock.timeout:5000}") long timeoutMilliseconds) {
        this.entityManager = entityManager;
        this.timeoutMilliseconds = timeoutMilliseconds;
    }

    /** Uses transaction-local configuration so pooled connections do not retain the transfer timeout. */
    public void configureCurrentTransaction() {
        entityManager.createNativeQuery("SELECT set_config('lock_timeout', :timeout, true)", String.class)
                .setParameter("timeout", timeoutMilliseconds + "ms")
                .getSingleResult();
    }
}

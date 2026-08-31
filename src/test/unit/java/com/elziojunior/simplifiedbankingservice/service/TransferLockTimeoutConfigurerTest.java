package com.elziojunior.simplifiedbankingservice.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

class TransferLockTimeoutConfigurerTest {

    /** Proves the external millisecond bound is applied transaction-locally for PostgreSQL locks. */
    @Test
    void shouldConfigureCurrentTransactionLockTimeout() {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<String> query = mock(TypedQuery.class);
        when(entityManager.createNativeQuery(
                "SELECT set_config('lock_timeout', :timeout, true)", String.class)).thenReturn(query);
        when(query.setParameter("timeout", "750ms")).thenReturn(query);

        new TransferLockTimeoutConfigurer(entityManager, 750).configureCurrentTransaction();

        verify(query).getSingleResult();
    }
}

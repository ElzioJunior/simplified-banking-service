package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.CannotAcquireLockException;

import com.elziojunior.simplifiedbankingservice.exception.TransferConflictException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class TransferMetricsTest {

    /** Proves successful calls contribute total, success, and latency observations. */
    @Test
    void shouldRecordSuccess() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TransferMetrics metrics = new TransferMetrics(registry);

        assertThat(metrics.observe(() -> "completed")).isEqualTo("completed");
        assertCount(registry, "banking.transfer.requests.total", 1);
        assertCount(registry, "banking.transfer.requests.successful", 1);
        assertThat(registry.timer("banking.transfer.request.latency").count()).isOne();
    }

    /** Proves expected client failures are rejected rather than operational failures. */
    @Test
    void shouldRecordRejectedOutcomes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TransferMetrics metrics = new TransferMetrics(registry);

        assertThatThrownBy(() -> metrics.observe(() -> {
            throw new TransferConflictException("conflict");
        })).isInstanceOf(TransferConflictException.class);

        assertCount(registry, "banking.transfer.requests.rejected", 1);
        assertCount(registry, "banking.transfer.requests.failed", 0);
    }

    /** Proves transient persistence failures expose database, timeout, and contention signals. */
    @Test
    void shouldRecordTransientDatabaseFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TransferMetrics metrics = new TransferMetrics(registry);

        assertThatThrownBy(() -> metrics.observe(() -> {
            throw new CannotAcquireLockException("lock detail");
        })).isInstanceOf(CannotAcquireLockException.class);

        assertCount(registry, "banking.transfer.requests.failed", 1);
        assertCount(registry, "banking.transfer.database.errors", 1);
        assertCount(registry, "banking.transfer.timeouts", 1);
        assertCount(registry, "banking.transfer.lock.contention", 1);
    }

    /** Proves permanent database and unexpected runtime failures increment bounded failure metrics. */
    @Test
    void shouldRecordOtherFailures() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TransferMetrics metrics = new TransferMetrics(registry);

        assertThatThrownBy(() -> metrics.observe(() -> {
            throw new DataIntegrityViolationException("database detail");
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> metrics.observe(() -> {
            throw new IllegalStateException("unexpected");
        })).isInstanceOf(IllegalStateException.class);

        assertCount(registry, "banking.transfer.requests.failed", 2);
        assertCount(registry, "banking.transfer.database.errors", 1);
    }

    private void assertCount(SimpleMeterRegistry registry, String name, double expected) {
        assertThat(registry.counter(name).count()).isEqualTo(expected);
    }
}

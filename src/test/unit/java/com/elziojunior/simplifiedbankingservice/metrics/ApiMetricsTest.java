package com.elziojunior.simplifiedbankingservice.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;

import com.elziojunior.simplifiedbankingservice.exception.AccountCreationValidationException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ApiMetricsTest {

    /** Proves all supported APIs share meter names while remaining distinguishable by bounded operation tags. */
    @Test
    void shouldRecordSuccessForEachApiOperation() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        for (ApiOperation operation : ApiOperation.values()) {
            assertThat(metrics.observe(operation, () -> operation.metricTag())).isEqualTo(operation.metricTag());
            assertCount(registry, "banking.api.requests.total", operation, 1);
            assertCount(registry, "banking.api.requests.successful", operation, 1);
            assertThat(registry.timer(
                    "banking.api.request.latency", "operation", operation.metricTag()).count()).isOne();
        }
    }

    /** Proves expected application validation is classified as rejection rather than operational failure. */
    @Test
    void shouldRecordRejectedOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        assertThatThrownBy(() -> metrics.observe(ApiOperation.ACCOUNT_CREATE, () -> {
            throw new AccountCreationValidationException("invalid account");
        })).isInstanceOf(AccountCreationValidationException.class);

        assertCount(registry, "banking.api.requests.rejected", ApiOperation.ACCOUNT_CREATE, 1);
        assertCount(registry, "banking.api.requests.failed", ApiOperation.ACCOUNT_CREATE, 0);
    }

    /** Proves lock failures expose database, timeout, and contention signals for their originating API. */
    @Test
    void shouldRecordLockContention() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        assertThatThrownBy(() -> metrics.observe(ApiOperation.TRANSFER_CREATE, () -> {
            throw new CannotAcquireLockException("lock detail");
        })).isInstanceOf(CannotAcquireLockException.class);

        assertCount(registry, "banking.api.requests.failed", ApiOperation.TRANSFER_CREATE, 1);
        assertCount(registry, "banking.api.database.errors", ApiOperation.TRANSFER_CREATE, 1);
        assertCount(registry, "banking.api.timeouts", ApiOperation.TRANSFER_CREATE, 1);
        assertCount(registry, "banking.api.lock.contention", ApiOperation.TRANSFER_CREATE, 1);
    }

    /** Proves timeout, transient, permanent database, and unexpected failures use bounded failure meters. */
    @Test
    void shouldRecordOtherFailures() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        assertFailure(metrics, new QueryTimeoutException("timeout"));
        assertFailure(metrics, new TransientDataAccessResourceException("transient"));
        assertFailure(metrics, new DataIntegrityViolationException("database detail"));
        assertFailure(metrics, new IllegalStateException("unexpected"));

        assertCount(registry, "banking.api.requests.failed", ApiOperation.TRANSFER_TOKEN_ISSUE, 4);
        assertCount(registry, "banking.api.database.errors", ApiOperation.TRANSFER_TOKEN_ISSUE, 3);
        assertCount(registry, "banking.api.timeouts", ApiOperation.TRANSFER_TOKEN_ISSUE, 1);
    }

    private void assertFailure(ApiMetrics metrics, RuntimeException failure) {
        assertThatThrownBy(() -> metrics.observe(ApiOperation.TRANSFER_TOKEN_ISSUE, () -> {
            throw failure;
        })).isSameAs(failure);
    }

    private void assertCount(SimpleMeterRegistry registry, String name, ApiOperation operation, double expected) {
        assertThat(registry.counter(name, "operation", operation.metricTag()).count()).isEqualTo(expected);
    }
}

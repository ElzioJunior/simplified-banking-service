package com.elziojunior.simplifiedbankingservice.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

class ApiMetricsTest {

    /** Proves all supported APIs share meter names while remaining distinguishable by bounded operation tags. */
    @Test
    void shouldRecordSuccessForEachApiOperation() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        for (ApiOperation operation : ApiOperation.values()) {
            metrics.recordOutcome(operation, 200, metrics.start());
            assertCount(registry, "banking.api.requests.total", operation, 1);
            assertCount(registry, "banking.api.requests.successful", operation, 1);
            assertThat(registry.timer(
                    "banking.api.request.latency", "operation", operation.metricTag()).count()).isOne();
        }
    }

    /** Proves client and server HTTP outcomes are classified without inspecting controller exceptions. */
    @Test
    void shouldClassifyRejectedAndFailedOutcomes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        metrics.recordOutcome(ApiOperation.ACCOUNT_CREATE, 400, metrics.start());
        metrics.recordOutcome(ApiOperation.ACCOUNT_CREATE, 503, metrics.start());

        assertCount(registry, "banking.api.requests.total", ApiOperation.ACCOUNT_CREATE, 2);
        assertCount(registry, "banking.api.requests.rejected", ApiOperation.ACCOUNT_CREATE, 1);
        assertCount(registry, "banking.api.requests.failed", ApiOperation.ACCOUNT_CREATE, 1);
        assertThat(registry.timer(
                "banking.api.request.latency", "operation", ApiOperation.ACCOUNT_CREATE.metricTag()).count())
                .isEqualTo(2);
    }

    /** Proves lock failures expose database, timeout, and contention signals for their originating API. */
    @Test
    void shouldRecordLockContention() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        metrics.recordDatabaseFailure(ApiOperation.TRANSFER_CREATE, new CannotAcquireLockException("lock detail"));

        assertCount(registry, "banking.api.database.errors", ApiOperation.TRANSFER_CREATE, 1);
        assertCount(registry, "banking.api.timeouts", ApiOperation.TRANSFER_CREATE, 1);
        assertCount(registry, "banking.api.lock.contention", ApiOperation.TRANSFER_CREATE, 1);
    }

    /** Proves timeout, transient, and permanent database failures use bounded technical meters. */
    @Test
    void shouldRecordOtherFailures() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiMetrics metrics = new ApiMetrics(registry);

        metrics.recordDatabaseFailure(ApiOperation.TRANSFER_TOKEN_ISSUE, new QueryTimeoutException("timeout"));
        metrics.recordDatabaseFailure(
                ApiOperation.TRANSFER_TOKEN_ISSUE, new TransientDataAccessResourceException("transient"));
        metrics.recordDatabaseFailure(
                ApiOperation.TRANSFER_TOKEN_ISSUE, new DataIntegrityViolationException("database detail"));

        assertCount(registry, "banking.api.database.errors", ApiOperation.TRANSFER_TOKEN_ISSUE, 3);
        assertCount(registry, "banking.api.timeouts", ApiOperation.TRANSFER_TOKEN_ISSUE, 1);
    }

    /** Proves the existing bounded meters receive stable Prometheus names and labels for dashboard queries. */
    @Test
    void shouldPublishStablePrometheusSeries() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        ApiMetrics metrics = new ApiMetrics(registry);

        metrics.recordOutcome(ApiOperation.ACCOUNT_CREATE, 200, metrics.start());

        assertThat(registry.scrape())
                .contains("banking_api_requests_total{operation=\"account.create\"} 1.0")
                .contains("banking_api_requests_successful_total{operation=\"account.create\"} 1.0")
                .contains("banking_api_request_latency_seconds_count{operation=\"account.create\"} 1");
    }

    private void assertCount(MeterRegistry registry, String name, ApiOperation operation, double expected) {
        assertThat(registry.counter(name, "operation", operation.metricTag()).count()).isEqualTo(expected);
    }
}

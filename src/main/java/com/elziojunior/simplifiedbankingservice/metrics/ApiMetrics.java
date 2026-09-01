package com.elziojunior.simplifiedbankingservice.metrics;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Records bounded-cardinality outcomes and latency for application API operations. */
@Component
public class ApiMetrics {

    private static final String OPERATION_TAG = "operation";
    private static final String REQUESTS_TOTAL = "banking.api.requests.total";
    private static final String REQUESTS_SUCCESSFUL = "banking.api.requests.successful";
    private static final String REQUESTS_REJECTED = "banking.api.requests.rejected";
    private static final String REQUESTS_FAILED = "banking.api.requests.failed";
    private static final String DATABASE_ERRORS = "banking.api.database.errors";
    private static final String TIMEOUTS = "banking.api.timeouts";
    private static final String LOCK_CONTENTION = "banking.api.lock.contention";
    private static final String REQUEST_LATENCY = "banking.api.request.latency";

    private final MeterRegistry registry;

    public ApiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Starts latency measurement at the MVC boundary before request arguments are resolved. */
    public Timer.Sample start() {
        return Timer.start(registry);
    }

    /** Records the final HTTP outcome and complete MVC latency for one bounded API operation. */
    public void recordOutcome(ApiOperation operation, int status, Timer.Sample sample) {
        counter(REQUESTS_TOTAL, operation).increment();
        if (status < 400) {
            counter(REQUESTS_SUCCESSFUL, operation).increment();
        } else if (status < 500) {
            counter(REQUESTS_REJECTED, operation).increment();
        } else {
            counter(REQUESTS_FAILED, operation).increment();
        }
        sample.stop(registry.timer(REQUEST_LATENCY, OPERATION_TAG, operation.metricTag()));
    }

    /** Records persistence-specific failure signals while the MVC boundary owns the common outcome. */
    public void recordDatabaseFailure(ApiOperation operation, DataAccessException exception) {
        counter(DATABASE_ERRORS, operation).increment();
        if (exception instanceof PessimisticLockingFailureException) {
            counter(TIMEOUTS, operation).increment();
            counter(LOCK_CONTENTION, operation).increment();
        } else if (exception instanceof QueryTimeoutException) {
            counter(TIMEOUTS, operation).increment();
        }
    }

    private Counter counter(String name, ApiOperation operation) {
        return registry.counter(name, OPERATION_TAG, operation.metricTag());
    }
}

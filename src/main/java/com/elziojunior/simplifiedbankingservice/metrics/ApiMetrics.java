package com.elziojunior.simplifiedbankingservice.metrics;

import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import com.elziojunior.simplifiedbankingservice.exception.RejectedRequestException;

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

    /**
     * Observes one complete API operation so service and transaction-completion failures are
     * classified consistently across controllers.
     */
    public <T> T observe(ApiOperation operation, Supplier<T> invocation) {
        counter(REQUESTS_TOTAL, operation).increment();
        Timer.Sample sample = Timer.start(registry);
        try {
            T result = invocation.get();
            counter(REQUESTS_SUCCESSFUL, operation).increment();
            return result;
        } catch (PessimisticLockingFailureException exception) {
            recordDatabaseFailure(operation);
            counter(TIMEOUTS, operation).increment();
            counter(LOCK_CONTENTION, operation).increment();
            throw exception;
        } catch (QueryTimeoutException exception) {
            recordDatabaseFailure(operation);
            counter(TIMEOUTS, operation).increment();
            throw exception;
        } catch (TransientDataAccessException exception) {
            recordDatabaseFailure(operation);
            throw exception;
        } catch (DataAccessException exception) {
            recordDatabaseFailure(operation);
            throw exception;
        } catch (RuntimeException exception) {
            counter(exception instanceof RejectedRequestException ? REQUESTS_REJECTED : REQUESTS_FAILED, operation)
                    .increment();
            throw exception;
        } finally {
            sample.stop(registry.timer(REQUEST_LATENCY, OPERATION_TAG, operation.metricTag()));
        }
    }

    /** Records the shared failure counters emitted for every persistence failure. */
    private void recordDatabaseFailure(ApiOperation operation) {
        counter(REQUESTS_FAILED, operation).increment();
        counter(DATABASE_ERRORS, operation).increment();
    }

    private Counter counter(String name, ApiOperation operation) {
        return registry.counter(name, OPERATION_TAG, operation.metricTag());
    }
}

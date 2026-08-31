package com.elziojunior.simplifiedbankingservice.service;

import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Records bounded-cardinality transfer outcomes and latency. */
@Component
public class TransferMetrics {

    private final MeterRegistry registry;
    private final Counter total;
    private final Counter successful;
    private final Counter rejected;
    private final Counter failed;
    private final Counter databaseErrors;
    private final Counter timeouts;
    private final Counter lockContention;
    private final Timer latency;

    public TransferMetrics(MeterRegistry registry) {
        this.registry = registry;
        total = registry.counter("banking.transfer.requests.total");
        successful = registry.counter("banking.transfer.requests.successful");
        rejected = registry.counter("banking.transfer.requests.rejected");
        failed = registry.counter("banking.transfer.requests.failed");
        databaseErrors = registry.counter("banking.transfer.database.errors");
        timeouts = registry.counter("banking.transfer.timeouts");
        lockContention = registry.counter("banking.transfer.lock.contention");
        latency = registry.timer("banking.transfer.request.latency");
    }

    /** Observes one complete service invocation, including transaction completion failures. */
    public <T> T observe(Supplier<T> operation) {
        total.increment();
        Timer.Sample sample = Timer.start(registry);
        try {
            T result = operation.get();
            successful.increment();
            return result;
        } catch (TransferValidationException | TransferNotFoundException | TransferConflictException exception) {
            rejected.increment();
            throw exception;
        } catch (PessimisticLockingFailureException exception) {
            failed.increment();
            databaseErrors.increment();
            timeouts.increment();
            lockContention.increment();
            throw exception;
        } catch (QueryTimeoutException exception) {
            failed.increment();
            databaseErrors.increment();
            timeouts.increment();
            throw exception;
        } catch (TransientDataAccessException exception) {
            failed.increment();
            databaseErrors.increment();
            throw exception;
        } catch (DataAccessException exception) {
            failed.increment();
            databaseErrors.increment();
            throw exception;
        } catch (RuntimeException exception) {
            failed.increment();
            throw exception;
        } finally {
            sample.stop(latency);
        }
    }
}

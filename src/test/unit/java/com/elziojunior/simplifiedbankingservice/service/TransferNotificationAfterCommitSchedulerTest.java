package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;

class TransferNotificationAfterCommitSchedulerTest {

    /** Clears thread-bound synchronization state so one transaction scenario cannot affect another. */
    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    /** Proves notification publication remains deferred until the financial transaction commits. */
    @Test
    void shouldPublishOnlyAfterCommit() {
        TransferNotificationPublisher publisher = mock(TransferNotificationPublisher.class);
        TransferNotificationAfterCommitScheduler scheduler = new TransferNotificationAfterCommitScheduler(publisher);
        TransferCompletedNotification notification = notification();
        startTransactionSynchronization();

        scheduler.schedule(notification);

        verify(publisher, never()).publish(notification);
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(publisher).publish(notification);
    }

    /** Proves rollback completion cannot invoke a transfer-completed publication. */
    @Test
    void shouldNotPublishAfterRollback() {
        TransferNotificationPublisher publisher = mock(TransferNotificationPublisher.class);
        TransferNotificationAfterCommitScheduler scheduler = new TransferNotificationAfterCommitScheduler(publisher);
        TransferCompletedNotification notification = notification();
        startTransactionSynchronization();

        scheduler.schedule(notification);
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(publisher, never()).publish(notification);
    }

    /** Proves callers cannot silently fall back to pre-commit publication outside a synchronized transaction. */
    @Test
    void shouldRejectSchedulingWithoutAnActiveTransaction() {
        TransferNotificationAfterCommitScheduler scheduler =
                new TransferNotificationAfterCommitScheduler(mock(TransferNotificationPublisher.class));

        assertThatThrownBy(() -> scheduler.schedule(notification()))
                .isInstanceOf(IllegalStateException.class);
    }

    /** Creates the transaction state required to exercise Spring's registered callbacks deterministically. */
    private void startTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private TransferCompletedNotification notification() {
        return new TransferCompletedNotification(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                1L,
                TransferCompletedNotification.TRANSFER_COMPLETED,
                new BigDecimal("10.00"),
                OffsetDateTime.parse("2026-09-01T12:00:00Z"));
    }
}

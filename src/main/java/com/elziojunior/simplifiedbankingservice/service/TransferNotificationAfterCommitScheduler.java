package com.elziojunior.simplifiedbankingservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;

/** Registers best-effort transfer notification publication after the financial commit. */
@Service
public class TransferNotificationAfterCommitScheduler {

    private final TransferNotificationPublisher notificationPublisher;

    public TransferNotificationAfterCommitScheduler(TransferNotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    /**
     * Registers one synchronous after-commit callback so RabbitMQ waits never retain
     * token or account locks and rolled-back transfers never emit completion events.
     */
    public void schedule(TransferCompletedNotification notification) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Transfer notification requires an active transaction synchronization");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationPublisher.publish(notification);
            }
        });
    }
}

package com.elziojunior.simplifiedbankingservice.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.elziojunior.simplifiedbankingservice.model.entity.TransferNotificationOutboxEntity;
import com.elziojunior.simplifiedbankingservice.repository.TransferNotificationOutboxRepository;

/** Persists each broker attempt independently from the completed transfer transaction. */
@Service
public class TransferNotificationOutboxStateService {

    private final TransferNotificationOutboxRepository repository;
    private final Clock clock;

    public TransferNotificationOutboxStateService(TransferNotificationOutboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Marks a pending event published only after broker confirmation. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId) {
        repository.findById(eventId).filter(event -> event.getPublishedAt() == null)
                .ifPresent(event -> event.markPublished(now()));
    }

    /** Leaves an event pending while durably recording a failed attempt. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(UUID eventId) {
        repository.findById(eventId).filter(event -> event.getPublishedAt() == null)
                .ifPresent(event -> event.recordFailedAttempt(now()));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}

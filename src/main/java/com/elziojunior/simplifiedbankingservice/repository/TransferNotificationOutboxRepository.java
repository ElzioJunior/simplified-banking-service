package com.elziojunior.simplifiedbankingservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.elziojunior.simplifiedbankingservice.model.entity.TransferNotificationOutboxEntity;

/** Persistence boundary for pending and confirmed notification intents. */
public interface TransferNotificationOutboxRepository
        extends JpaRepository<TransferNotificationOutboxEntity, UUID> {

    /** Returns the oldest bounded pending batch for asynchronous publication. */
    List<TransferNotificationOutboxEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);
}

package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.model.entity.TransferNotificationOutboxEntity;
import com.elziojunior.simplifiedbankingservice.repository.TransferNotificationOutboxRepository;

class TransferNotificationOutboxStateServiceTest {

    /** Proves confirmation and failed attempts use the application clock and preserve pending semantics. */
    @Test
    void shouldRecordPublicationStates() {
        TransferNotificationOutboxRepository repository = mock(TransferNotificationOutboxRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-31T15:00:00.123456Z"), ZoneOffset.UTC);
        TransferNotificationOutboxStateService service = new TransferNotificationOutboxStateService(repository, clock);
        UUID failedId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID publishedId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        TransferNotificationOutboxEntity failed = entity(failedId);
        TransferNotificationOutboxEntity published = entity(publishedId);
        when(repository.findById(failedId)).thenReturn(Optional.of(failed));
        when(repository.findById(publishedId)).thenReturn(Optional.of(published));

        service.recordFailedAttempt(failedId);
        service.markPublished(publishedId);

        assertThat(failed.getPublishAttempts()).isOne();
        assertThat(failed.getPublishedAt()).isNull();
        assertThat(published.getPublishAttempts()).isOne();
        assertThat(published.getPublishedAt()).isEqualTo(OffsetDateTime.parse("2026-08-31T15:00:00.123456Z"));
        verify(repository).findById(failedId);
        verify(repository).findById(publishedId);
    }

    private TransferNotificationOutboxEntity entity(UUID eventId) {
        return new TransferNotificationOutboxEntity(eventId, UUID.randomUUID(), 1L,
                new BigDecimal("10.00"), OffsetDateTime.parse("2026-08-31T14:00:00Z"));
    }
}

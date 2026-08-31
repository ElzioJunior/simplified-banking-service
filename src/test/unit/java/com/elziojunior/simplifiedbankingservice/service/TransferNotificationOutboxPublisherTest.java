package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import com.elziojunior.simplifiedbankingservice.configuration.TransferNotificationConfiguration;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;
import com.elziojunior.simplifiedbankingservice.model.entity.TransferNotificationOutboxEntity;
import com.elziojunior.simplifiedbankingservice.repository.TransferNotificationOutboxRepository;

class TransferNotificationOutboxPublisherTest {

    /** Proves a bounded pending event is mapped and marked only after confirmation. */
    @Test
    void shouldPublishAndMarkConfirmedEvent() {
        Fixture fixture = fixture();

        fixture.publisher.publishPending();

        ArgumentCaptor<TransferCompletedNotification> event = ArgumentCaptor.forClass(TransferCompletedNotification.class);
        verify(fixture.operations).convertAndSend(eq(TransferNotificationConfiguration.EXCHANGE),
                eq(TransferNotificationConfiguration.ROUTING_KEY), event.capture());
        assertThat(event.getValue().eventId()).isEqualTo(fixture.entity.getEventId());
        assertThat(event.getValue().recipientAccountId()).isEqualTo(1L);
        verify(fixture.operations).waitForConfirmsOrDie(2500L);
        verify(fixture.stateService).markPublished(fixture.entity.getEventId());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(fixture.repository).findByPublishedAtIsNullOrderByOccurredAtAsc(page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(7);
    }

    /** Proves broker failure records an attempt and leaves publication pending. */
    @Test
    void shouldRecordRetryableFailure() {
        Fixture fixture = fixture();
        doThrow(new AmqpIOException(new IOException("broker down")))
                .when(fixture.operations).waitForConfirmsOrDie(2500L);

        fixture.publisher.publishPending();

        verify(fixture.stateService).recordFailedAttempt(fixture.entity.getEventId());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Fixture fixture() {
        TransferNotificationOutboxRepository repository = mock(TransferNotificationOutboxRepository.class);
        TransferNotificationOutboxStateService stateService = mock(TransferNotificationOutboxStateService.class);
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        TransferNotificationOutboxEntity entity = new TransferNotificationOutboxEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                1L, new BigDecimal("10.00"), OffsetDateTime.parse("2026-08-31T14:00:00Z"));
        when(repository.findByPublishedAtIsNullOrderByOccurredAtAsc(any())).thenReturn(List.of(entity));
        when(template.invoke(any())).thenAnswer(invocation -> {
            RabbitOperations.OperationsCallback callback = invocation.getArgument(0);
            return callback.doInRabbit(operations);
        });
        return new Fixture(repository, stateService, operations, entity,
                new TransferNotificationOutboxPublisher(repository, stateService, template, 7, 2500));
    }

    private record Fixture(
            TransferNotificationOutboxRepository repository,
            TransferNotificationOutboxStateService stateService,
            RabbitOperations operations,
            TransferNotificationOutboxEntity entity,
            TransferNotificationOutboxPublisher publisher) {
    }
}

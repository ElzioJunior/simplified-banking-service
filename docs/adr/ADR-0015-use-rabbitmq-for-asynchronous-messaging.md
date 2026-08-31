# ADR-0015 — Use RabbitMQ for Asynchronous Messaging

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Notifications and similar asynchronous work should not unnecessarily extend the synchronous financial transaction path.

## Decision

Use RabbitMQ as the initial message broker. Successful financial operations may publish notification messages for asynchronous processing.

## Consequences

### Positive

- Decouples asynchronous consumers.
- Notification processing can scale independently.
- Mature routing and queueing capabilities.

### Negative or trade-offs

- Adds infrastructure and operational complexity.
- Retries, idempotency, delivery guarantees, and dead-letter handling require design.

## Alternatives considered

- Synchronous notifications — couples transaction latency to notification processing.
- Kafka — not required for the initial queue-oriented use case.

## Validation

RabbitMQ must run in the local environment. Integration/load testing must validate required delivery behavior before this ADR is accepted.

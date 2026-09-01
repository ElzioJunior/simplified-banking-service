# ADR-0030 — Publish Transfer Notifications Directly

- Status: Accepted
- Date: 2026-08-31
- Deciders: User and Engineering Team
- Supersedes: ADR-0028
- Superseded by: none
- Related: ADR-0015, ADR-0025, BDR-0005

## Context

The transactional outbox requires an entity, repository, table, scheduler,
publication-state service, broker confirmations, and recovery tests. The
current scope prefers a simple best-effort RabbitMQ event boundary and accepts
weaker delivery guarantees.

## Decision

The transfer application service calls a dedicated publisher once for each new
successful transfer. The publisher sends the immutable transfer-completed event
directly with `RabbitTemplate.convertAndSend` and may retry `AmqpException`
failures a configured bounded number of times in memory.

The publisher does not poll storage, persist attempts, wait for publisher
confirmations, or propagate an exhausted send failure into the financial
transfer. Flyway V3 removes the obsolete outbox table; V2 remains immutable as
historical migration evidence.

Direct publication occurs before the surrounding database transaction returns
to its caller. Therefore publication and database commit are not atomic: an
event can be lost after commit or can be observed before a later commit
failure. Those trade-offs are explicitly accepted for the simplified scope.

## Consequences

### Positive

- Removes the outbox entity, repository, state service, scheduler, and database structure.
- Reduces publisher behavior to direct event sending with bounded retry.
- Removes broker-confirmation and pending-state coordination.

### Negative or trade-offs

- No durable retry or restart recovery exists.
- Delivery is best effort and may be lost or duplicated.
- The database transaction and RabbitMQ publication have no atomic boundary.

## Alternatives considered

- Transactional outbox — superseded because its guarantees exceed the desired complexity.
- Transaction synchronization or a local application-event relay — rejected because it retains additional coordination machinery without durable recovery.
- Broker failure rolling back the transfer — rejected by BDR-0005.

## Validation

Unit tests verify direct event publication, bounded retry, swallowed exhausted
failure, and transfer-service invocation rules. Integrated tests verify the
real successful HTTP/PostgreSQL/RabbitMQ path without outbox persistence.

# ADR-0030 — Publish Transfer Notifications Directly After Commit

- Status: Accepted
- Date: 2026-08-31
- Last updated: 2026-09-01
- Deciders: User and Engineering Team
- Supersedes: ADR-0028
- Superseded by: none
- Related: ADR-0015, ADR-0025, ADR-0029, BDR-0005

## Context

The transactional outbox requires an entity, repository, table, scheduler,
publication-state service, broker confirmations, and recovery tests. The
current scope prefers a simple best-effort RabbitMQ event boundary and accepts
weaker delivery guarantees.

The initial direct implementation published inside the financial transaction.
A silent broker connection failure could therefore block each retry while the
transaction retained token and account locks. Count-bounded retries were not
necessarily time-bounded and could amplify contention across otherwise valid
transfers.

## Decision

A newly completed transfer registers one synchronous transaction callback that
invokes `TransferNotificationPublisher` only after the PostgreSQL commit
succeeds. An identical idempotent replay and every rejected or rolled-back
transfer register no publication. The callback is not dispatched to an
asynchronous executor and no notification intent is persisted.

The publisher sends the immutable transfer-completed event directly with
`RabbitTemplate.convertAndSend`. It retains a small configurable maximum
attempt count and a configurable monotonic total retry budget. RabbitMQ TCP
connection, AMQP handshake, and channel RPC operations each use explicit finite
timeouts. The total budget prevents another retry from starting after the
elapsed budget; the client timeouts bound an individual in-progress send.

The publisher does not poll storage, persist attempts, wait for publisher
confirmations, or propagate an exhausted send failure into the already
committed financial transfer. Flyway V3 removes the obsolete outbox table; V2
remains immutable as historical migration evidence. The HTTP response waits for
the bounded after-commit callback.

## Evolution

The decision accepted on 2026-08-31 used the same direct best-effort publisher
before the surrounding transaction returned. On 2026-09-01, the decision was
refined to execute synchronously after commit and to bound retries by time as
well as count. This amendment preserves the direct, non-durable delivery model
while removing RabbitMQ waits from the financial lock window.

## Consequences

### Positive

- Removes the outbox entity, repository, state service, scheduler, and database structure.
- PostgreSQL releases transfer locks before any RabbitMQ connection wait.
- Rolled-back transfers cannot emit transfer-completed notifications.
- Retry impact is bounded by both attempt count and explicit time limits.
- Removes broker-confirmation and pending-state coordination.

### Negative or trade-offs

- No durable retry or restart recovery exists.
- The HTTP response can still be delayed by the bounded after-commit sequence.
- A process failure between commit and callback execution loses the event.
- Delivery is best effort and may still be duplicated.
- The database transaction and RabbitMQ publication have no atomic boundary.
- Transaction synchronization adds a small local coordination boundary.

## Alternatives considered

- Transactional outbox — superseded because its guarantees exceed the desired complexity.
- Publication inside the transaction with only connection timeouts — rejected
  because even bounded waits unnecessarily retain financial locks.
- Asynchronous after-commit publication — rejected because executor capacity,
  shutdown loss, rejection handling, and task lifecycle add complexity without
  durable recovery.
- Future cancellation of blocking sends — rejected because cancellation may
  not stop the underlying socket operation and can permit late publication or
  blocked-thread accumulation.
- Broker failure rolling back the transfer — rejected by BDR-0005.

## Validation

Tests verify registration only for a newly completed transfer, no execution
before commit or after rollback, and no publication for replay or rejection.
Publisher tests verify success, attempt-count exhaustion, elapsed-budget
exhaustion, contained failure, and finite RabbitMQ connection, handshake, and
channel RPC configuration. Isolated functional tests verify the application-side
publication request after a committed transfer with RabbitMQ mocked. One focused
integrated test verifies the production publisher, topology, routing,
conversion, and consumption against a disposable RabbitMQ broker without
PostgreSQL.

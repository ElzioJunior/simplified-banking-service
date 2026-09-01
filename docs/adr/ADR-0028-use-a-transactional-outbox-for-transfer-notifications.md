# ADR-0028 — Use a Transactional Outbox for Transfer Notifications

- Status: Accepted
- Date: 2026-08-31
- Deciders: User and Engineering Team
- Supersedes: none
- Superseded by: none
- Related: ADR-0015, ADR-0025, BDR-0004

## Context

A completed transfer must create an asynchronous notification intent, but a
RabbitMQ outage must not roll back or invalidate committed financial effects.
Publishing directly inside the database transaction couples broker availability
to money movement, while publishing only after commit can lose the event if the
process fails between those actions.

## Decision

Persist one transfer-completed notification outbox record in the same
`READ_COMMITTED` transaction as the account balances, movements, and consumed
idempotency token. The record identifies the event, transfer operation, source
account recipient, amount, occurrence time, and publication state without
including account balances or customer names.

A background publisher sends pending records to RabbitMQ after the financial
transaction commits. It marks a record as published only after broker
confirmation. Publication failure leaves the record pending for a later
configurable retry and never changes the completed transfer outcome.

The database permits only one transfer-completed notification intent for the
same operation and recipient. RabbitMQ delivery is at least once: every message
contains a stable event ID so a future channel-specific consumer can deduplicate
delivery. Implementing an email, SMS, push, or other notification consumer is
outside the current scope.

## Consequences

### Positive

- Financial commit and notification-intent creation are atomic.
- RabbitMQ outages do not lose notification intent or roll back money movement.
- Stable event identity supports downstream idempotency.

### Negative or trade-offs

- A migration, publisher, retry scheduling, and publication observability are
  required.
- Delivery can be delayed while RabbitMQ is unavailable.
- At-least-once publication requires consumers to deduplicate by event ID.

## Alternatives considered

- Publish inside the financial transaction — rejected because broker failure
  would affect transfer correctness and extend database lock duration.
- Publish once after commit without persistence — rejected because a process
  failure could lose the notification.
- Synchronous channel delivery — rejected by BDR-0004.

## Validation

Integrated tests must prove that a successful transfer commits exactly one
outbox record, a rejected or rolled-back transfer commits none, broker failure
does not alter financial state, and pending publication succeeds when RabbitMQ
is available without creating another intent.

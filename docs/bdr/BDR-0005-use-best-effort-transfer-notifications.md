# BDR-0005 — Use Best-Effort Transfer Notifications

- Status: Accepted
- Date: 2026-08-31
- Deciders: User and Engineering Team
- Supersedes: BDR-0004
- Superseded by: none

## Context

The durable notification intent and duplicate-delivery guarantees introduced
operational and persistence complexity beyond the desired simplified banking
scope. Transfer correctness remains more important than notification delivery.

## Decision

Each newly completed transfer makes one best-effort request to publish a
transfer-completed event for the source account holder. Identical idempotent
replays do not request another publication.

The publisher may retry a failed RabbitMQ send a small bounded number of times
in memory. Exhausted publication attempts do not roll back or invalidate the
financial transfer. The application does not persist notification intent or
publication state and does not promise recovery after broker outage, process
failure, or restart. Exactly-once delivery and downstream duplicate prevention
are outside the current scope.

## Consequences

### Positive

- Removes notification persistence, polling, confirmation state, and recovery machinery.
- Keeps notification failure secondary to the completed financial operation.
- Makes the current notification boundary easier to understand and maintain.

### Negative or trade-offs

- A notification can be lost after retries are exhausted or the process stops.
- Publication is not atomic with the financial database transaction.
- RabbitMQ or network duplication can produce more than one delivered event.

## Alternatives considered

- Retain the transactional outbox — rejected for the current simplified scope.
- Fail the transfer when RabbitMQ is unavailable — rejected because notification availability must not determine financial correctness.
- Remove notifications entirely — rejected because a best-effort event remains useful.

## Validation

Tests must verify one direct publication request for a newly completed transfer,
no publication for a replay or rejected transfer, bounded publisher retry, and
preservation of the completed transfer outcome after publication failure.

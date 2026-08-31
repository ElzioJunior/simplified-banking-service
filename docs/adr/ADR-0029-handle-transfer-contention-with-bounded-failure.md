# ADR-0029 — Handle Transfer Contention with Bounded Failure

- Status: Accepted
- Date: 2026-08-31
- Deciders: User and Engineering Team
- Supersedes: none
- Superseded by: none
- Related: ADR-0011, ADR-0024, ADR-0025, ADR-0026

## Context

Pessimistic account locking can make a transfer wait behind another operation.
The API needs bounded behavior when lock acquisition times out or PostgreSQL
selects a transaction as a deadlock victim, without obscuring whether a client
retry could duplicate financial effects.

## Decision

Transfer account locks use a positive, externally configurable timeout with a
documented local default. Locks remain ordered by ascending account ID under
ADR-0024 to prevent expected cross-transfer deadlocks.

The application does not automatically retry a complete transfer after a lock
timeout, deadlock, or unexpected database failure. The database transaction
rolls back and the API returns safe RFC 9457 Problem Details with `503 Service
Unavailable`; the client may retry with the same idempotency token.

Other transfer failures map as follows:

- `400 Bad Request` — malformed input, missing idempotency header, or a
  nonpositive/unsupported monetary value.
- `404 Not Found` — source or destination account does not exist.
- `409 Conflict` — same-account transfer, insufficient funds, or unknown,
  expired, already-associated-with-another-payload idempotency token.
- `503 Service Unavailable` — lock timeout, deadlock victim, or unexpected
  database unavailability.

Problem details must remain stable and safe; SQL, lock, broker, credentials,
payloads, balances, and stack details are not returned to clients.

## Consequences

### Positive

- Contention cannot leave requests waiting indefinitely.
- Retry responsibility is explicit and safe through idempotency.
- Clients receive predictable failure categories without infrastructure leaks.

### Negative or trade-offs

- Clients must decide when to retry a `503` response.
- A short timeout can reject work during contention; a long timeout increases
  latency and lock occupancy.

## Alternatives considered

- Unbounded waiting — rejected because it makes latency and resource use
  uncontrolled.
- Automatic whole-transfer retry — rejected because it complicates latency,
  contention, and response semantics in the initial scope.
- Expose raw database errors — rejected because it leaks implementation details
  and creates an unstable API contract.

## Validation

Isolated tests must verify exception-to-Problem-Detail mapping. Integrated
concurrency tests must prove bounded timeout/deadlock failure, full rollback,
safe same-token retry, and preservation of all balances and movements.

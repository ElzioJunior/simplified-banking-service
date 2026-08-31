# ADR-0024 — Use Pessimistic Write Locking for Transfer Accounts

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Concurrent transfers can attempt to modify the same account balance at the same time. Balance validation must occur against the latest protected state so the same funds cannot be spent concurrently.

## Decision

Use database-level pessimistic write locking for accounts participating in a transfer.

Both the source and destination accounts must be locked before balance changes are applied.

Locks must be acquired in deterministic account-ID order, using the lower account ID first, regardless of which account is the source or destination.

The source account balance must be validated only after the required locks have been acquired.

## Consequences

### Positive

- Prevents concurrent transfers from operating on stale balances.
- Protects against lost updates and overspending.
- Deterministic lock ordering reduces deadlock risk.
- Behavior is straightforward to reason about for the scope of this project.

### Negative or trade-offs

- Contended accounts may cause requests to wait.
- Pessimistic locking can reduce throughput for hot accounts.
- Database locks increase coupling between transaction duration and concurrency.

## Alternatives considered

- Optimistic locking — rejected for the initial implementation because transfer conflicts would require explicit retry handling.
- Application-level in-memory locking — rejected because it does not safely coordinate multiple application instances.
- Lock only the source account — rejected because both account balances are modified and deterministic two-account locking simplifies consistency guarantees.

## Validation

Concurrent integration and load tests must verify that balances never become negative, no updates are lost, and cross-transfers between the same accounts do not produce inconsistent results.

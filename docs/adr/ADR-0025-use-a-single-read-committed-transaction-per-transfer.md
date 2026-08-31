# ADR-0025 — Use a Single READ_COMMITTED Transaction per Transfer

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

A transfer changes two account balances and creates the related debit and credit movements. These changes must succeed or fail together while remaining simple enough for the exercise.

## Decision

Execute each transfer inside a single database transaction.

Use the database `READ_COMMITTED` isolation level.

Within the transaction:

1. Load and lock both accounts in deterministic ID order.
2. Validate the source account balance after locks are acquired.
3. Apply the debit to the source account.
4. Apply the credit to the destination account.
5. Persist the two corresponding movements.
6. Commit all changes together.

Any failure before commit must roll back the entire operation.

## Consequences

### Positive

- Provides a clear atomic boundary for transfers.
- Works with pessimistic account locking to protect balances.
- Avoids the additional complexity of stronger global isolation levels.
- Ensures debit, credit, and movements remain consistent.

### Negative or trade-offs

- Long-running transfer logic increases lock duration.
- READ_COMMITTED alone does not provide all concurrency guarantees; correctness depends on the explicit locking strategy.
- Database failures roll back the whole operation and must be surfaced cleanly.

## Alternatives considered

- SERIALIZABLE — rejected because it adds unnecessary contention for this scope.
- REPEATABLE_READ — not required when explicit pessimistic locks protect the modified rows.
- Separate transactions for debit and credit — rejected because partial transfers would become possible.

## Validation

Integration tests must inject or simulate failures during the transfer flow and verify full rollback. Concurrent tests must validate that the transaction and locking strategy preserves balances.

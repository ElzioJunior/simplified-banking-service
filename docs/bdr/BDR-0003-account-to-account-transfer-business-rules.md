# BDR-0003 — Account-to-Account Transfer Business Rules

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering and Product Team
- Supersedes: none
- Superseded by: none

## Context

The core banking capability is transferring money between two accounts. Transfers modify financial balances and therefore require explicit business invariants that prevent invalid operations, duplicate processing, partial financial effects, and loss of traceability.

## Decision

The system must support transferring a positive monetary amount from one existing source account to one existing destination account.

The following business rules apply:

- Source and destination accounts must both exist.
- Source and destination accounts must be different.
- The transfer amount must be greater than zero.
- A transfer must not be completed when the resulting source-account balance would be below zero.
- Duplicate transfers must not be processed more than once.
- Each accepted transfer must receive a unique transfer identifier.
- A successful transfer must update the balances of both accounts.
- A successful transfer must generate exactly two financial movements:
  - A `DEBIT` movement on the source account.
  - A `CREDIT` movement on the destination account.
- The debit and credit must represent the same transfer amount.
- The transfer must be atomic from a business perspective: either all financial effects are completed or none of them are.
- The resulting balances must be immediately reflected after a successfully completed transfer.
- The transfer must record the information required for traceability, including its unique identifier and occurrence date/time.

Concurrency must never allow the same available balance to be spent more than once.

## Consequences

### Positive

- Protects account balances from invalid financial states.
- Provides complete traceability between a transfer and its financial movements.
- Ensures money is neither created nor lost during internal transfers.
- Defines deterministic behavior for invalid and duplicate requests.
- Supports safe concurrent financial activity.

### Negative or trade-offs

- Transfers may be rejected because of insufficient funds or invalid account relationships.
- Duplicate-prevention behavior introduces additional request-processing rules.
- Atomicity and concurrency guarantees increase implementation complexity.

## Alternatives considered

- Allow overdrafts — not selected because overdraft behavior is outside the exercise scope.
- Allow transfers to the same account — not selected because they provide no valid movement of funds and complicate history.
- Allow partial transfer completion — rejected because it can create inconsistent financial balances.
- Allow zero or negative transfer values — rejected because they do not represent valid transfer semantics.

## Validation

Automated tests must cover successful transfers, exact-balance transfers, insufficient funds, zero and negative amounts, missing source and destination accounts, same-account transfers, duplicate requests, balance updates, debit/credit movement creation, atomic failure behavior, and concurrent transfers competing for the same balance.

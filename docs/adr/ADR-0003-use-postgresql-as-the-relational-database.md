# ADR-0003 — Use PostgreSQL as the Relational Database

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Financial data requires ACID transactions, relational integrity, precise data types, and reliable concurrency control.

## Decision

Use PostgreSQL as the primary persistent relational database for accounts, transfers, and financial movements.

## Consequences

### Positive

- Strong ACID guarantees.
- Mature locking and concurrency control.
- Rich relational constraints and indexing.

### Negative or trade-offs

- Requires database operations and tuning.
- Horizontal write scaling can be complex.

## Alternatives considered

- MySQL — viable but not selected.
- NoSQL — not preferred for the core transactional financial domain.

## Validation

Integration tests must run against PostgreSQL and validate persistence, constraints, and transactional behavior.

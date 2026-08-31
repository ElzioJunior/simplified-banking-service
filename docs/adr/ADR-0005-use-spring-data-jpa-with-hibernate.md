# ADR-0005 — Use Spring Data JPA with Hibernate

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Persistence code must integrate cleanly with PostgreSQL and Spring transaction management while minimizing repository boilerplate.

## Decision

Use Spring Data JPA for repository abstractions with Hibernate as the JPA implementation.

## Consequences

### Positive

- Reduces persistence boilerplate.
- Integrates with Spring transactions.
- Mature ORM and repository ecosystem.

### Negative or trade-offs

- ORM behavior can hide inefficient queries.
- Performance-sensitive queries may require explicit SQL.

## Alternatives considered

- Plain JDBC — more boilerplate.
- jOOQ — strong SQL-oriented option but not selected as the primary abstraction.

## Validation

Repository integration tests must execute against PostgreSQL and validate persistence and transaction behavior.

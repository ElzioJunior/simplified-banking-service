# ADR-0010 — Use Flyway for Database Migrations

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Database schema changes must be versioned, reviewable, repeatable, and consistently applied across environments.

## Decision

Use Flyway for versioned, deterministic PostgreSQL schema migrations.

## Consequences

### Positive

- Schema history is version-controlled.
- Deterministic migration ordering.
- Native Spring Boot integration.

### Negative or trade-offs

- Applied migrations require disciplined maintenance.
- Migration mistakes can affect startup and deployment.

## Alternatives considered

- Liquibase — valid alternative, but Flyway is simpler for this project.
- Manual migrations — difficult to reproduce reliably.

## Validation

A clean PostgreSQL instance must be fully initialized by Flyway during automated integration testing.

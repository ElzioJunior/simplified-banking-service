# ADR-0009 — Use Docker Compose for Local Infrastructure

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: ADR-0034

## Context

Developers need a repeatable way to start required local infrastructure without manual installations.

## Decision

Use Docker Compose to start local infrastructure dependencies such as PostgreSQL and RabbitMQ.

## Consequences

### Positive

- One-command local infrastructure startup.
- Consistent service versions across developers.
- Easy onboarding and repeatable development.

### Negative or trade-offs

- Requires Docker Compose locally.
- Local topology does not represent production orchestration.

## Alternatives considered

- Manual local installation — creates environment drift.
- Local Kubernetes — unnecessary complexity.

## Validation

A developer must be able to start documented local dependencies through Docker Compose and run the application against them.

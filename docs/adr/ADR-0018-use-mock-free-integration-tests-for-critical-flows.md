# ADR-0018 — Use Mock-Free Integration Tests for Critical Flows

- Status: Superseded
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: ADR-0031

## Context

Critical flows must prove that real persistence, transactions, migrations, messaging, serialization, and framework configuration work together.

## Decision

Integration tests must start the real application context and exercise real infrastructure without mocks. Required accounts, balances, users, and other data must be created through real persistence/application mechanisms.

## Consequences

### Positive

- Validates real component integration.
- Detects configuration and transactional issues.
- Provides high confidence in critical flows.

### Negative or trade-offs

- Slower than isolated tests.
- Requires real test infrastructure and data lifecycle management.

## Alternatives considered

- Mock-based integration tests — do not validate real integrations.
- Manual-only E2E testing — not repeatable enough for engineering gates.

## Validation

Integration tests must start the application with real required infrastructure and execute critical flows without mocks.

# ADR-0017 — Use Isolated Tests with Mocks and WireMock

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Fast isolated tests are needed for focused behavior and controlled simulation of HTTP dependencies.

## Decision

Use mocks in isolated tests when collaborators must be isolated. Use WireMock as the preferred HTTP mock server for external HTTP dependencies. Mocks are not used in integration tests.

## Consequences

### Positive

- Fast deterministic feedback.
- Failures are easy to localize.
- WireMock provides realistic HTTP simulation.

### Negative or trade-offs

- Mocks can diverge from real behavior.
- Over-mocking can couple tests to implementation details.

## Alternatives considered

- Real external systems in isolated tests — slow and nondeterministic.
- Integration tests for every path — unnecessarily slow feedback.

## Validation

Isolated tests must run without real infrastructure or external network services; WireMock contracts must cover required HTTP behavior.

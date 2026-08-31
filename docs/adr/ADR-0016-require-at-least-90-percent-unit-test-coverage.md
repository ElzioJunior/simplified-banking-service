# ADR-0016 — Require at Least 90 Percent Unit Test Coverage

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Core banking logic requires strong automated regression protection while avoiding low-value tests written only to satisfy coverage metrics.

## Decision

Require at least 90% unit-test coverage for meaningful application logic. Narrow exclusions may include enums, entities, simple DTO/data-only models, generated code, and framework configuration.

## Consequences

### Positive

- Strong regression protection.
- Measurable quality gate.
- Focuses testing on meaningful behavior.

### Negative or trade-offs

- Coverage percentage does not guarantee test quality.
- High thresholds can encourage low-value tests if poorly governed.

## Alternatives considered

- 100% coverage — likely to create low-value tests.
- No threshold — insufficient quality enforcement.

## Validation

The build must calculate unit-test coverage and fail below 90% after documented exclusions.

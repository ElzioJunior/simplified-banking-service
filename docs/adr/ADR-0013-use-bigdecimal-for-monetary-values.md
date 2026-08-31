# ADR-0013 — Use BigDecimal for Monetary Values

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Financial calculations require exact decimal arithmetic and must not rely on binary floating-point values.

## Decision

Use Java BigDecimal and PostgreSQL fixed-precision numeric types for monetary values, with explicit scale and rounding rules.

## Consequences

### Positive

- Exact decimal arithmetic.
- Prevents floating-point precision errors.
- Maps naturally to PostgreSQL numeric columns.

### Negative or trade-offs

- Scale and rounding must be explicit.
- BigDecimal requires disciplined comparison and arithmetic.

## Alternatives considered

- double/float — unsafe for exact financial arithmetic.
- Integer minor units — viable but not selected for the initial model.

## Validation

Tests must cover decimal arithmetic, persistence round trips, boundary values, and unsupported precision.

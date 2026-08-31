# ADR-0022 — Use BigDecimal with HALF_EVEN Rounding for Monetary Values

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The transfer domain requires exact decimal arithmetic. Monetary values must not rely on binary floating-point behavior, and a consistent rounding rule is needed for any operation that requires rounding.

## Decision

Use Java `BigDecimal` for monetary values.

Monetary values should use a scale of two decimal places for the current scope.

When rounding is required, use `RoundingMode.HALF_EVEN`.

The database representation must use a fixed-precision decimal type compatible with the same monetary precision.

## Consequences

### Positive

- Provides exact decimal arithmetic for balances and transfer amounts.
- Defines deterministic rounding behavior.
- Avoids floating-point precision errors.

### Negative or trade-offs

- Requires explicit handling of scale and comparison semantics.
- Developers must avoid implicit or inconsistent rounding.

## Alternatives considered

- `double` or `float` — rejected because binary floating-point is unsuitable for financial values.
- `HALF_UP` rounding — valid, but `HALF_EVEN` was selected as the project-wide financial rounding rule.
- No rounding rule — rejected because future calculations could otherwise behave inconsistently.

## Validation

Unit and integration tests must cover decimal persistence, arithmetic, scale handling, and scenarios that require rounding.

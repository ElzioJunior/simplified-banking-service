# ADR-0011 — Standardize REST API Error Responses

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

API clients need predictable and machine-readable errors for validation, business, concurrency, and infrastructure failures.

## Decision

Use Problem Details for HTTP APIs aligned with RFC 9457 and Spring ProblemDetail where appropriate.

## Consequences

### Positive

- Consistent error contract.
- Standards-based representation.
- Simplifies centralized exception handling.

### Negative or trade-offs

- Domain details must be mapped carefully.
- Responses must avoid leaking sensitive information.

## Alternatives considered

- Custom error schema — unnecessary proprietary contract.
- Plain string errors — unsuitable for reliable client processing.

## Validation

API tests must validate RFC 9457-compatible structures, status codes, and safe error details.

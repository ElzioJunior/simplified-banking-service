# ADR-0012 — Version the REST API

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The API contract will evolve and requires a strategy for introducing breaking changes safely.

## Decision

Expose public REST endpoints under a major-version path beginning with /api/v1. Breaking API changes require a new major version.

## Consequences

### Positive

- Explicit contract evolution.
- Simple routing and documentation.
- Breaking changes can coexist with older versions.

### Negative or trade-offs

- Version information appears in URLs.
- Multiple versions may temporarily require maintenance.

## Alternatives considered

- Header versioning — less visible and navigable.
- No explicit versioning — makes breaking changes difficult to manage.

## Validation

Tests and API documentation must verify that public endpoints use the expected versioned paths.

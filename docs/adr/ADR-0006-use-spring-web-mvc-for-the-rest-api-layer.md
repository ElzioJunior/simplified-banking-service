# ADR-0006 — Use Spring Web MVC for the REST API Layer

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The system exposes synchronous REST APIs and primarily performs conventional database and infrastructure I/O.

## Decision

Use Spring Web MVC for synchronous REST APIs.

## Consequences

### Positive

- Mature REST programming model.
- Strong Spring integration.
- Compatible with Virtual Threads if adopted.

### Negative or trade-offs

- Uses a blocking programming model by default.
- Not intended for end-to-end reactive processing.

## Alternatives considered

- Spring WebFlux — reactive complexity is not currently required.
- JAX-RS — would move the API layer outside the selected Spring stack.

## Validation

API tests must verify routing, validation, serialization, status codes, and error handling.

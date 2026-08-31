# ADR-0019 — Use Maven for Build and Dependency Management

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The Java project requires deterministic builds, dependency management, testing, packaging, and CI integration.

## Decision

Use Apache Maven and commit the Maven Wrapper for deterministic builds and dependency management.

## Consequences

### Positive

- Mature Java ecosystem.
- Standard lifecycle and dependency management.
- Straightforward CI integration.

### Negative or trade-offs

- XML configuration can be verbose.
- Complex custom build logic can be cumbersome.

## Alternatives considered

- Gradle — valid alternative but not selected.
- Manual dependency management — not reproducible.

## Validation

`./mvnw verify` must execute the required build, tests, quality checks, and packaging successfully.

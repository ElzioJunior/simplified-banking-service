# ADR-0008 — Use Spring Boot Actuator and Micrometer for Observability

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The system must expose operational data to evaluate health, performance, failures, and concurrency behavior under load.

## Decision

Use Spring Boot Actuator and Micrometer for health information, JVM/application metrics, latency, throughput, failures, and custom metrics.

## Consequences

### Positive

- Vendor-neutral instrumentation.
- Native Spring integration.
- Supports JVM, HTTP, application, and custom metrics.

### Negative or trade-offs

- Metric naming and cardinality require discipline.
- A monitoring backend is still needed for storage and visualization.

## Alternatives considered

- Vendor-specific metrics SDK — creates unnecessary coupling.
- Custom instrumentation abstraction — duplicates Micrometer capabilities.

## Validation

Health and metrics must be exposed according to security rules, and key metrics must be visible during load tests.

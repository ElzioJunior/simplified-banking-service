# ADR-0002 — Use Spring Boot 3.5 or Later

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The application needs a mature Java framework integrated with the selected web, persistence, security, testing, and observability stack.

## Decision

Use Spring Boot 3.5.x as the minimum framework baseline, with later compatible versions allowed after validation.

## Consequences

### Positive

- Mature ecosystem and Spring integrations.
- Convention-over-configuration reduces boilerplate.
- Strong production and testing support.

### Negative or trade-offs

- Creates coupling to Spring conventions.
- Major upgrades may require migration work.

## Alternatives considered

- Plain Spring Framework — requires more manual setup.
- Quarkus/Micronaut — valid alternatives, but not selected for this project.

## Validation

The application context must start successfully on the supported Java baseline and all automated tests must pass.

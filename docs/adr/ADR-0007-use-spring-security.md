# ADR-0007 — Use Spring Security

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The REST API requires a standard, extensible security layer.

## Decision

Use Spring Security as the application security framework for authentication, authorization, and endpoint protection.

## Consequences

### Positive

- Native Spring integration.
- Extensible authentication and authorization.
- Mature security ecosystem.

### Negative or trade-offs

- Security configuration requires care.
- Misconfiguration can expose or incorrectly block endpoints.

## Alternatives considered

- Custom security — increases security risk and maintenance.
- Gateway-only security — insufficient for all application authorization needs.

## Validation

Automated security tests must verify protected/public endpoint behavior and authorization rules.

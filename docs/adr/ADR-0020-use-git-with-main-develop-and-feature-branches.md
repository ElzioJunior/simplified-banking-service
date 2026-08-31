# ADR-0020 — Use Git with Main, Develop, and Feature Branches

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

Source changes require a predictable version-control, integration, and release workflow.

## Decision

Use Git. main is the stable branch, develop is the integration branch, and feature branches merge into develop. Validated releases are promoted from develop to main.

## Consequences

### Positive

- Clear integration and release path.
- Supports parallel feature work.
- Protects the stable branch.

### Negative or trade-offs

- A long-lived develop branch can accumulate integration risk.
- Requires branch discipline.

## Alternatives considered

- Trunk-based development — valid alternative but not selected for this workflow.
- Full GitFlow — more process than currently required.

## Validation

Branch protections and pull-request practices should enforce feature -> develop -> main promotion.

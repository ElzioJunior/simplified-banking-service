# ADR-0021 — Use AI Agents Through Vendor-Agnostic Workflows and Skills

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

AI agents will be used during development, but the engineering process must not depend on a particular AI product or vendor.

## Decision

AI agents may assist development, but workflows, skills, engineering standards, and repository instructions must remain vendor-, model-, IDE-, and agent-agnostic.

## Consequences

### Positive

- Reduces AI vendor lock-in.
- Makes engineering knowledge reusable.
- Allows agents/models to be replaced over time.

### Negative or trade-offs

- Agent capabilities differ, so perfect portability is impossible.
- Shared workflow and skill documentation requires maintenance.

## Alternatives considered

- Agent-specific repository design — creates tool lock-in.
- No structured AI guidance — reduces consistency and repeatability.

## Validation

Project workflows and skills must be usable without requiring one named AI vendor or agent; vendor-specific configuration must remain optional and isolated.

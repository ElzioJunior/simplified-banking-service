# ADR-0001 — Use Java 21 or Later

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The project requires a modern LTS Java baseline suitable for a high-concurrency banking API.

## Decision

Use Java 21 as the minimum supported Java version. Later compatible versions may be adopted after validation.

## Consequences

### Positive

- Stable LTS baseline.
- Access to modern JVM capabilities, including Virtual Threads.
- Reduces dependence on legacy Java versions.

### Negative or trade-offs

- CI and developer environments must provide Java 21 or later.
- Newer Java versions require compatibility validation.

## Alternatives considered

- Java 17 — older LTS baseline with fewer modern capabilities.
- Non-LTS Java baseline — increases upgrade pressure.

## Validation

The Maven build and automated test suite must pass on Java 21 or a validated later version.

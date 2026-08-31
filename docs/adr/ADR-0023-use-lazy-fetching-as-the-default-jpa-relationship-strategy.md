# ADR-0023 — Use LAZY Fetching as the Default JPA Relationship Strategy

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

The persistence model includes relationships such as movements referencing accounts. Loading related entities eagerly by default can cause unnecessary queries, excessive object graphs, and unpredictable performance.

## Decision

Use `LAZY` fetching as the default strategy for JPA relationships.

Relationships should only be fetched eagerly for a specific use case when that behavior is explicitly justified.

The Account-to-Movement relationship must not cause an account query to automatically load its complete movement history.

## Consequences

### Positive

- Avoids unnecessary related-data loading.
- Reduces the risk of large object graphs and excessive queries.
- Keeps query behavior aligned with the data required by each use case.

### Negative or trade-offs

- Accessing lazy relationships outside a valid persistence context can fail.
- Use cases that require related data may need explicit fetch queries or projections.
- Developers must remain aware of N+1 query risks.

## Alternatives considered

- EAGER fetching by default — rejected because it can load unnecessary data and degrade performance.
- No explicit project convention — rejected because JPA defaults vary by relationship type and can lead to inconsistent behavior.

## Validation

Repository and integration tests must verify that normal account retrieval does not automatically load movement collections and that use cases requiring related data fetch it explicitly.

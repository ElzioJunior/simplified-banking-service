# BDR-0002 — Financial Movement Query Rules

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering and Product Team
- Supersedes: none
- Superseded by: none

## Context

Clients need to inspect the financial activity of a specific account. The query must remain efficient as movement history grows and must provide enough filtering to inspect relevant credits and debits without introducing unnecessary search capabilities.

## Decision

Provide a financial movement query scoped to a specific account, conceptually exposed as:

`GET /accounts/{accountId}/movements`

The result must be paginated.

Business query rules:

- A page may contain a maximum of 10 movements.
- Movements belong to the account identified by the request path.
- The client may filter movements by a date/time range using a start and end value.
- The client may filter by movement type.
- Supported movement types are:
  - `CREDIT`
  - `DEBIT`
- Date/time and movement-type filters are optional unless otherwise required by API validation.
- No additional business filters are part of the current scope.

## Consequences

### Positive

- Prevents unbounded movement-history responses.
- Supports the primary ways a client needs to inspect financial activity.
- Keeps the query contract intentionally small.
- Makes large histories manageable through pagination.

### Negative or trade-offs

- Clients requiring more than 10 results must request additional pages.
- Search capabilities are intentionally limited.
- Additional filters will require future contract evolution.

## Alternatives considered

- Return the complete movement history — not selected because response size would grow without bound.
- Support arbitrary page sizes — not selected to keep response behavior predictable and bounded.
- Provide advanced search filters — not selected because they are unnecessary for the current exercise.

## Validation

Automated API tests must verify account-scoped movement retrieval, the 10-item maximum page size, pagination across larger histories, date/time-range filtering, CREDIT filtering, DEBIT filtering, combined filters, empty results, and invalid account behavior.

# BDR-0006 — Use Fixed Lookback Periods for Movement Queries

- Status: Accepted
- Date: 2026-09-01
- Deciders: Engineering and Product Team
- Supersedes: BDR-0002
- Superseded by: none

## Context

The movement-listing API currently accepts arbitrary start and end timestamps.
That contract exposes more flexibility than the product needs and relies on an
unrelated historical placeholder when a boundary is absent. Clients only need
small, predictable recent-history windows.

## Decision

Keep the account-scoped, fixed-size paginated movement query and the optional
`CREDIT` or `DEBIT` filter, but replace arbitrary `start` and `end` request
parameters with one optional `period` parameter.

The only accepted period values are:

- `1d` — from one day before the request instant.
- `1w` — from one week before the request instant.
- `1M` — from one calendar month before the request instant.

When `period` is absent, the API uses `1d`. Each request resolves one current
instant and queries movements in the half-open interval from that instant minus
the selected period through that instant. Clients cannot submit arbitrary date
boundaries. Removed `start` and `end` parameters, and every other unsupported
query field, must return `400 Bad Request` rather than being silently ignored.

All other BDR-0002 rules remain unchanged: results belong to the account in the
path, pages contain at most 10 movements, movement type remains optional, and no
additional filters enter the current scope.

## Consequences

### Positive

- The public contract is smaller and easier to use correctly.
- Every query is bounded to a recent, product-approved period.
- Default requests have explicit one-day semantics without sentinel dates.
- Calendar-month behavior remains distinct from a fixed number of days.

### Negative or trade-offs

- Clients can no longer request arbitrary historical ranges.
- Retrieving history older than one month is outside the current API scope.
- Results near a period boundary depend on the request instant.

## Alternatives considered

- Keep arbitrary `start` and `end` timestamps — rejected because the flexibility
  is unnecessary and makes the contract and validation more complex.
- Use a fixed sentinel date when no lower bound is supplied — rejected because
  it hides default behavior and can scan unrelated history.
- Interpret one month as 30 days — rejected because `1M` explicitly represents
  a calendar month.

## Validation

Automated tests must verify the default `1d` period, explicit `1d`, `1w`, and
`1M` periods, calendar-month calculation, combination with `CREDIT` and `DEBIT`,
rejection of every unsupported period, pagination, ownership isolation, empty
results, explicit rejection of removed or unknown query fields, and unchanged
persisted financial state.

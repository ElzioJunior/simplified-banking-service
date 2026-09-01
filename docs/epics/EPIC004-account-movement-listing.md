# EPIC004 — Account Movement Listing

## Objective

Provide a simple REST API for listing the financial movements of one account.

The list must be paginated and may be filtered only by:

- Recent period: `1d`, `1w`, or `1M`.
- Movement type: `CREDIT` or `DEBIT`.

The endpoint is read-only. It does not modify accounts, balances, movements, or
transfer state.

## Delivery status

Completed on 2026-09-01, including the contract revision that replaced the
original arbitrary date range with the fixed periods defined in BDR-0006.

---

## Governing context

- [BDR-0006 — Use Fixed Lookback Periods for Movement Queries](../bdr/BDR-0006-use-fixed-lookback-periods-for-movement-queries.md)
- [ADR-0011 — Standardize REST API Error Responses](../adr/ADR-0011-standardize-rest-api-error-responses.md)
- [ADR-0012 — Version the REST API](../adr/ADR-0012-version-the-rest-api.md)
- [ADR-0023 — Use LAZY Fetching as the Default JPA Relationship Strategy](../adr/ADR-0023-use-lazy-fetching-as-the-default-jpa-relationship-strategy.md)
- [Logical Data Model](../database/logical-data-model.md)

The existing `Movement` entity and Flyway V1 indexes already represent the
required account, date/time, and movement-type data. No logical data-model
change or database migration is expected for this scope.

---

## API

### List Account Movements

`GET /api/v1/accounts/{accountId}/movements`

### Query Parameters

| Parameter | Required | Description |
| --- | --- | --- |
| `page` | No | Zero-based page number. Defaults to `0`. |
| `period` | No | Recent period: `1d`, `1w`, or `1M`. Defaults to `1d`. |
| `type` | No | Movement type: `CREDIT` or `DEBIT`. |

Each page contains at most 10 movements. A custom page-size parameter is not
part of this Epic.

Results are ordered from newest to oldest by movement date/time, with movement
ID descending as the deterministic tie-breaker.

Example request:

```http
GET /api/v1/accounts/1/movements?page=0&period=1w&type=CREDIT
```

### Successful Response

`200 OK`

```json
{
  "content": [
    {
      "id": 42,
      "operationId": "f6608b62-b6ba-4da2-864d-b8d49c48fb85",
      "type": "CREDIT",
      "amount": 100.00,
      "createdAt": "2026-08-31T18:45:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

An account with no matching movements returns `200 OK` with an empty `content`
array and zero totals.

### Error Contract

Errors use safe RFC 9457 Problem Details:

- `400 Bad Request` — invalid page, period, or movement type.
- `404 Not Found` — the account does not exist.

The endpoint remains temporarily unauthenticated under
[ADR-0027](../adr/ADR-0027-defer-api-authentication-for-the-initial-scope.md).

---

# Epic Acceptance Criteria

- [x] A client can list movements for one existing account.
- [x] Only movements belonging to the account in the path are returned.
- [x] Results are paginated with at most 10 movements per page.
- [x] Page numbering starts at zero and defaults to the first page.
- [x] Results are ordered deterministically from newest to oldest.
- [x] The client may select only `1d`, `1w`, or `1M`, with `1d` as the default.
- [x] The client may filter by `CREDIT` or `DEBIT`.
- [x] Period and type filters may be combined.
- [x] An empty result returns a successful empty page.
- [x] An unknown account returns `404 Not Found`.
- [x] Invalid query parameters return safe Problem Details with `400 Bad Request`.
- [x] The API does not expose persistence entities directly.
- [x] The query does not modify account, movement, or transfer data.

---

# User Stories

## US-001 — List Account Movements

**As an** API client
**I want to** list the movements of an account
**So that** I can inspect its financial activity.

### Scenario

**Given:**

- An account exists with recorded movements.

**When:**

- The first movement page is requested.

**Then:**

- The API returns only that account's movements.
- The page contains at most 10 items.
- The newest movements appear first.

---

## US-002 — Navigate Movement Pages

**As an** API client
**I want to** request another movement page
**So that** I can navigate a history larger than 10 items.

### Scenario

**Given:**

- An account has more than 10 movements.

**When:**

- The client requests page `1`.

**Then:**

- The API returns the next page of movements.
- No page contains more than 10 items.

---

## US-003 — Filter Movements by Recent Period

**As an** API client
**I want to** select a supported recent period
**So that** I can inspect activity from a specific period.

### Scenario

**Given:**

- An account has movements inside and outside a requested period.

**When:**

- The client supplies `period=1d`, `period=1w`, or `period=1M`.

**Then:**

- Only movements within the selected period are returned.
- When the period is omitted, only movements from the last day are returned.

---

## US-004 — Filter Credit Movements

**As an** API client
**I want to** list only credit movements
**So that** I can inspect incoming funds.

### Scenario

**Given:**

- An account has credit and debit movements.

**When:**

- The client filters by `type=CREDIT`.

**Then:**

- Only `CREDIT` movements are returned.

---

## US-005 — Filter Debit Movements

**As an** API client
**I want to** list only debit movements
**So that** I can inspect outgoing funds.

### Scenario

**Given:**

- An account has credit and debit movements.

**When:**

- The client filters by `type=DEBIT`.

**Then:**

- Only `DEBIT` movements are returned.

---

## US-006 — Return an Empty Movement Page

**As an** API client
**I want to** receive an empty page when no movement matches
**So that** the absence of results is represented normally.

### Scenario

**Given:**

- An account exists.
- No movement matches the supplied filters.

**When:**

- The movement list is requested.

**Then:**

- The API returns `200 OK`.
- The response contains an empty `content` array.

---

# Out of Scope

- Listing movements across multiple accounts.
- Filtering by amount, operation ID, movement ID, customer name, or balance.
- Client-selected sorting or custom page sizes.
- Creating, updating, deleting, reversing, or correcting movements.
- Returning or recalculating account balances.
- Exporting movement history.
- Aggregations, totals, statements, reports, or analytics.
- Streaming or real-time movement updates.

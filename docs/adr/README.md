# Architecture Decision Records

ADRs capture durable architecture and technical decisions. Use the base status
values `Proposed`, `Accepted`, `Deprecated`, `Superseded`, or `Rejected`.

Naming: `ADR-XXXX-short-title.md`.

An ADR explains why the system is built this way, its consequences, and the
alternatives considered. Local and reversible implementation details do not
need an ADR. When a later ADR changes an accepted decision, both records must
state the supersession relationship explicitly.

Start from [the ADR template](ADR-template.md) and maintain an index below.

## Index

- [ADR-0022 — Use BigDecimal with HALF_EVEN Rounding for Monetary Values](ADR-0022-use-bigdecimal-with-half-even-rounding-for-monetary-values.md)
- [ADR-0023 — Use LAZY Fetching as the Default JPA Relationship Strategy](ADR-0023-use-lazy-fetching-as-the-default-jpa-relationship-strategy.md)
- [ADR-0024 — Use Pessimistic Write Locking for Transfer Accounts](ADR-0024-use-pessimistic-write-locking-for-transfer-accounts.md)
- [ADR-0025 — Use a Single READ_COMMITTED Transaction per Transfer](ADR-0025-use-a-single-read-committed-transaction-per-transfer.md)
- [ADR-0026 — Use Server-Issued Idempotency Tokens for Transfers](ADR-0026-use-server-issued-idempotency-tokens-for-transfers.md)
- [ADR-0027 — Defer API Authentication for the Initial Scope](ADR-0027-defer-api-authentication-for-the-initial-scope.md)
- [ADR-0028 — Use a Transactional Outbox for Transfer Notifications](ADR-0028-use-a-transactional-outbox-for-transfer-notifications.md)
- [ADR-0029 — Handle Transfer Contention with Bounded Failure](ADR-0029-handle-transfer-contention-with-bounded-failure.md)
- [ADR-0030 — Publish Transfer Notifications Directly](ADR-0030-publish-transfer-notifications-directly.md)

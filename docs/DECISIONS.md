# Decision Register

Use this register to make unresolved decisions visible. Link each resolved item
to its governing BDR, ADR, engineering standard, or product document.

## Resolved

- EPIC001 monetary scale and rounding follow
  [ADR-0022](adr/ADR-0022-use-bigdecimal-with-half-even-rounding-for-monetary-values.md):
  scale two with `HALF_EVEN` whenever normalization requires rounding.
- API authentication is intentionally deferred for the initial simplified
  scope by
  [ADR-0027](adr/ADR-0027-defer-api-authentication-for-the-initial-scope.md).
- Transfer clients obtain a 10-minute server-issued token from
  `POST /api/v1/transfer-tokens` and submit it through `Idempotency-Key` under
  [ADR-0026](adr/ADR-0026-use-server-issued-idempotency-tokens-for-transfers.md).
- Successful transfers create one durable source-account notification intent,
  published asynchronously through the transactional outbox defined by
  [BDR-0004](bdr/BDR-0004-successful-transfer-notification-policy.md) and
  [ADR-0028](adr/ADR-0028-use-a-transactional-outbox-for-transfer-notifications.md).
- Transfer lock contention has a configurable bound, no automatic
  whole-transfer retry, and a safe RFC 9457 failure mapping under
  [ADR-0029](adr/ADR-0029-handle-transfer-contention-with-bounded-failure.md).

## Open

- None affecting the currently planned EPIC002 delivery.

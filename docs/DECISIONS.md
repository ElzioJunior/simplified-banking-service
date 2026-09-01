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
- Newly completed transfers request one synchronous best-effort RabbitMQ
  publication after the financial commit, with count- and time-bounded retry
  and no durable notification state under
  [BDR-0005](bdr/BDR-0005-use-best-effort-transfer-notifications.md) and
  [ADR-0030](adr/ADR-0030-publish-transfer-notifications-directly.md).
- Transfer lock contention has a configurable bound, no automatic
  whole-transfer retry, and a safe RFC 9457 failure mapping under
  [ADR-0029](adr/ADR-0029-handle-transfer-contention-with-bounded-failure.md).
- Complete application flows using guarded disposable PostgreSQL belong to the
  isolated functional lifecycle, while opt-in integrated tests qualify named
  real adapter or external-system boundaries under
  [ADR-0031](adr/ADR-0031-separate-isolated-application-tests-from-real-boundary-integration-tests.md).

## Open

- None affecting the currently documented delivery scope.

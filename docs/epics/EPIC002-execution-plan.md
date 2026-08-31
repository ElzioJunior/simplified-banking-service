# EPIC002 — Execution Plan

## Preconditions and decisions

- Scope is limited to server-issued transfer tokens, account-to-account
  transfers, their debit/credit movements, and the source-account notification
  intent defined by [EPIC002](EPIC002-account-to-account-transfer.md),
  [BDR-0003](../bdr/BDR-0003-account-to-account-transfer-business-rules.md),
  and [BDR-0004](../bdr/BDR-0004-successful-transfer-notification-policy.md).
- Public contracts are `POST /api/v1/transfer-tokens` and
  `POST /api/v1/transfers`; the latter requires the server-issued token in
  `Idempotency-Key` under [ADR-0026](../adr/ADR-0026-use-server-issued-idempotency-tokens-for-transfers.md).
- Transfer IDs are UUID `operationId` values shared by exactly one debit and
  one credit movement. Account IDs remain PostgreSQL-generated `BIGINT` values;
  no separate Transfer entity or table is introduced.
- Monetary input uses `BigDecimal`, scale two, and `HALF_EVEN` under
  [ADR-0022](../adr/ADR-0022-use-bigdecimal-with-half-even-rounding-for-monetary-values.md).
  Reject nonpositive input before financial mutation and reject a positive
  sub-cent value if normalization produces zero. Reject values outside
  `NUMERIC(19,2)` safely.
- Every transfer uses one `READ_COMMITTED` transaction. Lock the idempotency
  token, then both accounts individually in ascending account-ID order; validate
  the source balance only after both account locks are held, as required by
  [ADR-0024](../adr/ADR-0024-use-pessimistic-write-locking-for-transfer-accounts.md)
  and [ADR-0025](../adr/ADR-0025-use-a-single-read-committed-transaction-per-transfer.md).
- The account lock timeout is externally configurable with a five-second local
  default. The application does not retry whole transfers automatically;
  contention/database failures roll back and use the safe status mapping from
  [ADR-0029](../adr/ADR-0029-handle-transfer-contention-with-bounded-failure.md).
- One notification outbox record is committed atomically with the transfer.
  RabbitMQ publication occurs after commit with publisher confirmation and
  retry of pending records under
  [ADR-0028](../adr/ADR-0028-use-a-transactional-outbox-for-transfer-notifications.md).
- `/api/v1/**` remains temporarily unauthenticated under
  [ADR-0027](../adr/ADR-0027-defer-api-authentication-for-the-initial-scope.md).
  EPIC002 does not design authentication and must preserve the traceable TODO
  for future bearer-token validation through the `Authorization` header.
- Spring Boot Actuator and Micrometer remain the observability boundary under
  [ADR-0008](../adr/ADR-0008-use-spring-boot-actuator-and-micrometer-for-observability.md).
  Metrics use bounded outcome/failure tags; account, token, event, and operation
  identifiers must never become metric labels.
- There are no remaining open EPIC002 decisions. The approved changes require
  a Flyway V2 migration, logical-model update, RabbitMQ Testcontainers support,
  and an opt-in Gatling load-test profile.

## Acceptance criteria

- Token issuance returns a unique UUID and an expiration instant exactly 10
  minutes after issuance.
- A valid first use atomically updates both balances, persists exactly one
  debit and credit with the same UUID, associates the normalized payload with
  the token, and creates exactly one source-account notification intent.
- An identical retry returns the established completed result without another
  financial or notification effect; expired, unknown, or payload-mismatched
  token use is rejected.
- Missing accounts, same-account transfer, nonpositive/unsupported amount, and
  insufficient funds make no persistent change.
- Any failure after mutation begins rolls back balances, movements, token
  association, and outbox insertion together.
- Concurrent transfers never overspend, lose updates, create/destroy money, or
  deadlock through inconsistent lock ordering. Timeout/deadlock victims fail
  within the configured bound and can be retried with the same token.
- RabbitMQ unavailability never changes a completed transfer; the unique outbox
  intent remains pending and is publishable after recovery.
- The API uses the documented RFC 9457 `400`, `404`, `409`, and `503` contract
  without exposing balances, payloads, SQL, locks, credentials, or stack data.
- Gatling reports throughput, latency, error rate, and consistency results for
  contended and independently distributed transfers.
- Micrometer exposes transfer totals by success/rejection/failure, latency,
  database errors, timeouts, and lock contention. Successful-operation logs are
  correlatable by operation ID without logging tokens, names, balances, request
  payloads, credentials, or other sensitive values.

## Ordered slices

1. **Physical persistence foundation.** Add immutable Flyway V2 tables for
   transfer idempotency tokens and transfer notification outbox records,
   including UUID identities, 10-minute-expiration support, all-or-none token
   association constraints, operation/event uniqueness, retention-safe account
   references, and indexes for unused-token and unpublished-outbox lookup.
   Extend the existing migration integrated suite to verify metadata,
   constraints, foreign keys, uniqueness, and V1-to-V2 migration idempotence.
2. **Persistence mappings and locking boundaries.** Add data-only JPA entities
   for movements, tokens, and outbox records; add the movement type enum and
   Spring Data repositories. Extend `AccountEntity` only with the minimal
   balance state mutation needed by the service. Implement repository methods
   that lock a token and then lock each account in ascending ID order with the
   configured timeout. Repository/integrated tests prove actual PostgreSQL lock
   behavior and that account retrieval does not load movement history.
3. **Token issuance use case.** Add an injectable UUID boundary and reuse the
   application clock to issue persisted tokens with exact 10-minute expiry.
   Add command/result DTOs, service validation, controller request/response,
   and unit/isolated API tests for uniqueness, timestamp precision, empty-body
   issuance, serialization, and temporarily unauthenticated access.
4. **Atomic transfer use case.** Add transfer command/result types and one
   transactional service that validates and normalizes input, locks the token,
   handles completed identical retries, locks both accounts deterministically,
   checks existence and latest funds, mutates balances, creates the two
   movements, associates the token, and creates the outbox intent. Focused unit
   tests cover the complete success path, exact-balance transfer, both
   `HALF_EVEN` ties, positive sub-cent-to-zero rejection, range overflow,
   missing/same accounts, insufficient funds, token states/payload mismatch,
   ordering, and collaborator non-invocation on early failure.
5. **Transfer HTTP and safe failures.** Add versioned request/response DTOs and
   `POST /api/v1/transfers`, requiring `Idempotency-Key`. Extend centralized
   Problem Details translation for validation, account absence, business/token
   conflict, lock timeout/deadlock, and unexpected database availability while
   preserving the existing account-creation mappings. Instrument bounded
   Micrometer counters/timers for total, successful, rejected, failed,
   database-error, timeout, and lock-contention outcomes, and add safe
   operation-ID correlation after a transfer identity exists. Isolated MockMvc
   and registry tests verify success/replay serialization, required header/body
   validation, `400`/`404`/`409`/`503`, safe details, unsupported methods,
   metrics, and the temporary unauthenticated API boundary.
6. **Transactional outbox publisher.** Configure a durable RabbitMQ transfer
   notification exchange, queue, and binding plus publisher confirmations. Add
   a scheduled publisher that reads pending intents in bounded batches,
   publishes stable event IDs, and marks success only after confirmation;
   failure records the attempt and leaves the intent pending. Unit tests cover
   mapping, confirmation, retryable failure, bounded batches, and the rule that
   publication never changes transfer state.
7. **Mock-free critical-flow verification.** Add an opt-in
   `TransferIntegratedFunctionalTest` using the real random-port application,
   Flyway V2, PostgreSQL 17.6, and RabbitMQ 4.1.4 Testcontainers without mocks.
   Reuse the real account-creation API for fixtures. Verify success, exact
   balance, replay and payload mismatch, token expiry, all invalid cases,
   movement/outbox invariants, forced database rollback, broker outage/recovery,
   same-source competition, 100-debit exhaustion, simultaneous incoming and
   outgoing transfers, cross-transfers, bounded lock timeout, and total-money
   conservation. Use clocks, barriers, futures, database locks, and broker
   control rather than sleeps.
8. **Gatling and lifecycle completion.** Add an opt-in `load-tests` Maven
   profile and Java simulations under `src/test/gatling/java`. One simulation
   targets a hot source account; another distributes work across independent
   accounts. Seed through public APIs, issue real tokens, expose base URL and
   load parameters through environment/system properties, and report
   throughput, percentiles, errors, completed operations, and post-run money
   conservation. Run configured gates, perform independent review, apply
   findings, rerun affected checks, then synchronize EPIC002 and this shared
   execution report.

## Expected components and documentation

- `src/main/resources/db/migration/V2__*.sql` — token and outbox schema only;
  V1 remains unchanged.
- `src/main/java/.../model/entity/` — movement, idempotency-token, and outbox
  persistence mappings plus the existing account mapping.
- `src/main/java/.../repository/` — token/account locking, movement, and outbox
  persistence boundaries.
- `src/main/java/.../service/` — token issuance, atomic transfer, and outbox
  publication orchestration.
- `src/main/java/.../api/` and `model/api/` — versioned token/transfer
  controllers, transport records, and safe Problem Details mapping.
- `src/main/java/.../configuration/` — UUID/timeout, RabbitMQ topology,
  confirmation, and scheduling configuration without business behavior.
- `src/test/unit/java/.../transfer/` — deterministic process-local behavior.
- `src/test/isolated/java/.../transfer/` — isolated MVC and publisher wiring.
- `src/test/integrated/java/.../transfer/` — real HTTP/PostgreSQL/RabbitMQ,
  rollback, idempotency, recovery, and concurrency scenarios.
- `src/test/gatling/java/.../transfer/` — opt-in load simulations.
- `docs/database/logical-data-model.md`, ADR-0026, ADR-0028, ADR-0029,
  BDR-0004, EPIC002, and the shared report — planned contract sources.
- Movement-query endpoints, notification channel consumers, authentication,
  overdrafts, fees, exchange rates, scheduled transfers, and a persisted
  Transfer entity remain out of scope.

All methods that validate, normalize, order/obtain locks, mutate financial
state, orchestrate the transaction, evaluate idempotency, translate failures,
publish/retry events, or coordinate concurrency will have JavaDoc stating what
they do and why the boundary exists. Application-owned record/DTO/entity/config
properties receive concise property documentation. Every automated test method
states the invariant or failure it proves and why it matters. Lock ordering,
rollback scope, at-least-once publication, stable event identity, and the
temporary authentication limitation remain documented next to their smallest
relevant code boundary with links to governing decisions.

## Quality strategy

- `./mvnw -B -ntp clean test` — unit tests and at least 90% eligible line
  coverage.
- `./mvnw -B -ntp clean verify` — unit plus isolated functional tests without
  Docker, PostgreSQL, RabbitMQ, or another external process.
- `./mvnw -B -ntp clean -Pintegrated-functional-tests verify` — existing
  account/schema regression plus mock-free transfer HTTP/PostgreSQL/RabbitMQ,
  rollback, recovery, and concurrency scenarios.
- `docker compose config --quiet` — unchanged/local infrastructure validity.
- `./mvnw -B -ntp -Pload-tests gatling:test` — opt-in authorized load suite
  against an explicitly configured dedicated environment.
- Review verifies business invariants, token replay semantics, deterministic
  lock order, transaction boundaries, no network call inside the financial
  transaction, outbox uniqueness, safe errors/logging, sensitive-data
  minimization, bounded metric cardinality, absence of unrelated endpoints,
  and unnecessary complexity.
- No lint, static-analysis, dependency/security scanner, or standalone schema
  quality tool is currently configured; the plan does not claim or weaken an
  absent gate.

## Integrated and load-test strategy

The normal integrated suite uses only disposable local containers and a random
HTTP port. PostgreSQL and RabbitMQ state is isolated per suite, fixtures enter
through real public APIs, concurrency is coordinated deterministically, and
owned rows/queues are cleared between scenarios. This local disposable suite
has no shared state, credentials, cost, or consequential external effect, so it
does not require a Workflow 05 authorization pause.

Gatling intentionally generates sustained load against a separately running
application. Preparation may add the profile, simulations, seed logic, and
preflight checks, but Workflow 05 must stop immediately before the first load
run and request explicit authorization for the exact base URL, environment,
concurrency, duration, and cleanup. The preflight must reject a production-like
or ambiguous target, missing dedicated dataset, or absent post-run consistency
access. Authorization for local integrated tests does not authorize Gatling.

## Source-control behavior

Development authorization covers coherent non-destructive commits and pushes
on `feature/ep002` for approved slices, fixes, tests, review, and finalization.
Each checkpoint stages only EPIC002 files and preserves unrelated worktree
changes, including the existing `.gitkeep` deletions. Amend, squash,
force-push, pull request creation, merge, deployment, and release remain
excluded unless separately requested.

## Checkpoint

- Status: implementation and local verification complete; stopped before the
  separately authorized Gatling execution
- Completed slices: 1–7; slice 8 preparation is complete and its load
  execution/finalization remains pending
- Documentation prepared: BDR-0004, ADR-0026, ADR-0028, ADR-0029, logical data
  model, EPIC002 contract, this plan, and shared execution report
- Decision impact: approved business, API, persistence, messaging, contention,
  and data-model changes are explicit; no open EPIC002 decision remains
- Validation evidence:
  - `./mvnw -B -ntp clean -Pintegrated-functional-tests -Dit.test=DatabaseMigrationIntegratedFunctionalTest verify`
    — 7 PostgreSQL 17.6 migration/schema scenarios passed at Flyway version 2
  - `./mvnw -B -ntp clean verify` — 40 unit tests, 12 isolated MVC tests, and
    the 90% eligible-line coverage gate passed
  - `./mvnw -B -ntp clean -Pintegrated-functional-tests verify` — 20 real
    transfer, account-regression, Flyway, PostgreSQL 17.6, and RabbitMQ 4.1.4
    scenarios passed
  - `./mvnw -B -ntp -Pload-tests -DskipTests test-compile` — both Gatling
    simulations compiled; no load was executed
  - `docker compose config --quiet` and `git diff --check` passed
- Review: no unresolved BLOCKER or HIGH issue; timeout, metric classification,
  rollback, recovery, cross-transfer, and 100-request contention findings were
  applied and revalidated
- Delivery commits: `3ec5035`, `ebad351`, and `26d9879`
- Next action: request explicit authorization for the exact dedicated Gatling
  target, rate, duration, consistency access, and cleanup before running it

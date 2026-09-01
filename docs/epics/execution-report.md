# Development Execution Report

This is the single execution report for all epic execution plans.

## Portfolio status

| Epic | Execution plan | Status | Latest checkpoint |
| --- | --- | --- | --- |
| EPIC000 — Core Database Schema | [Plan](EPIC000-execution-plan.md) | Completed | Migration and 6 real PostgreSQL tests passed |
| EPIC001 — Account Creation | [Plan](EPIC001-execution-plan.md) | Completed | API, review, and all configured gates completed |
| EPIC002 — Account-to-Account Transfer | [Plan](EPIC002-execution-plan.md) | Completed | Local gates and both authorized Gatling simulations passed |

## Completed foundation work

The feature-free Java 21/Spring Boot foundation was delivered before the epic
artifact convention was standardized. It includes the Maven Wrapper, separated
test lifecycles, environment-backed configuration, PostgreSQL/RabbitMQ Compose
services, and a non-root Docker image. Validation passed for the normal and
opt-in Maven lifecycles, Compose configuration, host startup, Actuator health,
and container startup. The implementation and documentation commits are
`0e0490a` and `5f72528`.

## Completed plan: EPIC000

### What was implemented

One initial Flyway migration for `accounts` and `movements`, including approved
PostgreSQL types, row constraints, retention-safe foreign keys, operation
uniqueness, and movement-query indexes. An opt-in Testcontainers suite proves
the real migration without introducing business feature code.

### Delivery order

1. Implemented and smoke-tested the versioned schema migration.
2. Added and executed real PostgreSQL migration/constraint tests.
3. Passed quality gates, completed AI review, fixed the metadata-test finding,
   and synchronized documentation.

### Decisions and risks

The approved physical mapping is recorded in the logical data-model document.
Cross-row guarantees such as exactly two equal and opposite movements remain an
application-transaction responsibility because ordinary PostgreSQL checks do
not safely express them. Token and notification persistence remain deferred
rather than being guessed into the initial schema.

### Validation

The default Maven lifecycles remain infrastructure-independent. On 2026-08-31,
`./mvnw -B -ntp clean test` and `./mvnw -B -ntp clean verify` passed without
Docker. `./mvnw -B -ntp clean -Pintegrated-functional-tests verify` passed all
6 tests against a disposable PostgreSQL 17.6 container, covering schema
metadata, indexes, accepted rows, rejected rows, foreign-key behavior,
uniqueness, and migration idempotence. `docker compose config --quiet` also
passed. No consequential real-boundary suite applies.

No unit-test coverage percentage was produced because EPIC000 adds no eligible
application logic; JaCoCo reported no execution data and skipped its check.
No lint, static-analysis, or dependency/security check is configured in the
repository. Independent review found no unresolved BLOCKER or HIGH issue.

### Delivery commits

- `0df5348` — core schema migration and EPIC000 planning artifacts.
- `6ceeb2b` — execution-plan placeholders for the remaining epics.
- `f9dfa04` — opt-in PostgreSQL migration verification suite.

## Completed plan: EPIC001

### What was implemented

Only `POST /api/v1/accounts`: an application service, JPA mapping and
repository over the existing V1 schema, versioned MVC API, RFC 9457 validation
errors, and explicit temporary unauthenticated API security configuration. No
account query/update/delete, transfer, movement-query, messaging, or migration
work is included.

### Delivery order

1. Implement the account persistence mapping, repository, creation use case,
   deterministic clock boundary, and focused unit tests.
2. Implement request/response contracts, controller, Problem Details handling,
   temporary security configuration, and isolated MVC tests.
3. Add and run mock-free HTTP/PostgreSQL Testcontainers scenarios against the
   real application and Flyway V1 schema.
4. Run quality gates, perform independent review and fixes, synchronize
   documentation, and finalize delivery.

### Decisions and risks

ADR-0022 governs scale-two `HALF_EVEN` monetary normalization, with negative
input rejected before rounding. ADR-0027 records the user-approved temporary
absence of authentication and CSRF enforcement for `/api/v1/**`; the
implementation must contain a traceable TODO for future bearer-token
authentication and must not relax operational endpoints. This makes the
service unsuitable for untrusted-network exposure until authentication is
implemented. The API will return `201 Created`, safe RFC 9457 errors, and an
offset-bearing timestamp. The existing V1 schema is sufficient, so no
migration or new dependency is planned.

### Validation

Unit tests cover validation, normalization, timestamps, mapping, and invalid
repository interactions with at least 90% eligible line coverage. Isolated MVC
tests cover routing, serialization, Problem Details, unsupported routes, and
unauthenticated access without Docker. The opt-in integrated profile exercises
the real HTTP application and disposable PostgreSQL 17.6 without mocks,
including successful persistence, unique IDs, monetary round trips, timestamps,
and invalid-request atomicity. These are local disposable boundaries, so no
consequential Workflow 05 authorization gate applies.

The persistence/use-case, HTTP/security, and integrated slices are complete.
`./mvnw -B -ntp clean verify` passed 17 unit tests and 7 isolated MVC
scenarios, including the 90% eligible-code line-coverage gate. The opt-in
integrated lifecycle also passed 5 account-creation scenarios and the existing
6 migration scenarios against disposable PostgreSQL 17.6 containers. During
integrated verification, timestamp values were aligned to PostgreSQL
microsecond precision and servlet security configuration was scoped away from
non-web application contexts.

Final quality and review reruns passed on 2026-08-31: 17 unit tests, 7 isolated
MVC tests, 5 real account HTTP/PostgreSQL tests, the existing 6 real migration
tests, the 90% eligible-code coverage gate, and
`docker compose config --quiet`. Review found no unresolved BLOCKER or HIGH
issue. Its completeness
findings added explicit list/update-route absence checks, Problem Details media
type assertions, and the required exception-mapping JavaDoc. No lint,
static-analysis, dependency/security scanner, or separate schema-quality tool
is configured, so none is claimed.

### Delivery commits

- `adc8391` — account persistence/use-case core, unit tests, ADR-0027, and
  approved planning artifacts.
- `2d84765` — versioned creation API, Problem Details, temporary security
  configuration, and isolated tests.
- `50ef551` — mock-free HTTP/PostgreSQL suite and integrated compatibility
  fixes.
- Final review fixes and documentation are recorded by the subsequent
  finalization commit in branch history.

## Completed plan: EPIC002

### Planned scope

Deliver only server-issued 10-minute transfer tokens, atomic
account-to-account transfers through `POST /api/v1/transfers`, exactly two
correlated movements, and one best-effort direct RabbitMQ notification event
for the source account holder. Movement-query APIs, notification delivery channels,
authentication, overdrafts, fees, scheduled transfers, and a separate Transfer
entity remain excluded.

### Delivery order

1. Keep Flyway V2 history, add V3 outbox removal, and verify real PostgreSQL constraints.
2. Add persistence mappings, repositories, deterministic pessimistic locking,
   and configurable bounded lock waiting.
3. Implement and expose token issuance.
4. Implement the single-transaction idempotent transfer use case.
5. Expose the versioned transfer API and safe RFC 9457 failures.
6. Publish notifications directly to RabbitMQ with bounded in-memory retry.
7. Prove rollback, direct publication, idempotency, and concurrency through real
   HTTP/PostgreSQL/RabbitMQ Testcontainers.
8. Prepare Gatling load simulations, then complete quality gates, review, and
   documentation.

### Decisions and risks

ADR-0026 fixes the token endpoint and `Idempotency-Key` contract. ADR-0024 and
ADR-0025 require ascending-ID pessimistic locks within one `READ_COMMITTED`
transaction. ADR-0029 bounds lock waiting, prohibits automatic whole-transfer
retry, and defines `400`/`404`/`409`/`503` Problem Details. ADR-0030 uses direct
best-effort RabbitMQ publication with bounded in-memory retry and no durable
recovery or atomic database/broker boundary. Hot accounts intentionally serialize;
distributed accounts should retain independent throughput. The API remains
temporarily unauthenticated under ADR-0027 and is unsuitable for untrusted
network exposure. ADR-0008 supplies bounded Micrometer outcome, latency,
database-error, timeout, and contention metrics without financial identifiers
as metric labels; logs correlate successful operations by operation ID without
including tokens, payloads, balances, or customer data.

### Validation and authorization boundaries

Default `test` and `verify` remain infrastructure-independent and enforce the
90% eligible-code coverage gate. The opt-in integrated profile uses disposable
PostgreSQL 17.6 and RabbitMQ 4.1.4 containers to verify real migrations,
transactions, broker recovery, concurrent overspend prevention, cross-transfer
lock order, 100-transfer exhaustion, and money conservation without mocks.
Those resources are local and disposable, so they do not require a
consequential-boundary pause.

Gatling targets a separately running dedicated environment and can create
sustained load. Its first execution was authorized for a disposable local
environment after the base URL, environment identity, request rate, duration,
destination count, consistency access, and cleanup behavior were stated. No
lint, static-analysis, dependency/security scanner, or standalone
schema-quality tool is configured.

## Source control

EPIC001 was delivered in coherent non-destructive commits on `feature/ep001`.
If EPIC002 development is authorized, coherent non-destructive commits and
pushes on `feature/ep002` are included through review and finalization. Existing
unrelated `.gitkeep` deletions remain outside EPIC002 commits. Force-push,
history rewriting, pull requests, merges, deployments, releases, and Gatling
execution remain excluded unless their respective authorization is explicit.

## Authorization

Development authorization was granted on 2026-08-31. It covers the planned
implementation, local quality and disposable integrated tests, review/fix
loops, documentation, and normal non-destructive commits and pushes. Flyway V3
and its 7 PostgreSQL 17.6 schema scenarios are complete. Separate Gatling
authorization was granted on 2026-08-31 for `http://localhost:18080`, the
`dedicated-load-test` identity, 10 requests/second for 30 seconds per
simulation, 20 destinations, direct consistency access, and whole-container
cleanup.

## EPIC002 implementation checkpoint

Slices 1–7 and the preparation portion of slice 8 are complete. The application
now issues 10-minute UUID tokens, executes idempotent `HALF_EVEN` transfers in
one `READ_COMMITTED` transaction, applies a transaction-local PostgreSQL lock
timeout, persists movements and token association atomically, and requests a
direct best-effort RabbitMQ event publication for each new transfer. Safe Problem
Details, bounded Micrometer metrics, and operation-ID-only success correlation
are included. The temporary unauthenticated API boundary and bearer-token TODO
remain unchanged.

The `load-tests` profile contains hot-source and distributed Gatling
simulations. They seed through public APIs, issue real tokens, require an
explicit `dedicated-load-test` environment, reject production-like targets,
require consistency access, and verify total money after the run. Both
simulations ran successfully against a disposable application, PostgreSQL
17.6 database, and RabbitMQ 4.1.4 broker; cleanup discarded the complete
containers without clearing database tables.

Validation on 2026-08-31:

- `./mvnw -B -ntp verify` passed 43 unit tests, 12 isolated MVC tests,
  and the 90% eligible-line coverage gate.
- `./mvnw -B -ntp -Pintegrated-functional-tests verify` passed 18 real
  scenarios: 6 transfer HTTP/PostgreSQL/RabbitMQ scenarios, 5 account
  regressions, and 7 Flyway/schema scenarios. Transfer coverage includes
  replay/mismatch, atomic rejection, 100 competing debits, money conservation,
  cross-transfers, bounded lock failure and metrics, direct RabbitMQ
  publication, and Flyway V3 outbox removal.
- `DistributedTransferSimulation` passed 300/300 requests at 10 requests/second
  with zero failures, 9 ms mean latency, 14 ms p95, and conserved total money.
- `HotSourceTransferSimulation` passed 300/300 requests at 10 requests/second
  with zero failures, 6 ms mean latency, 9 ms p95, and conserved total money.
- Gatling 3.15 initially exposed Spring Boot dependency-management selecting
  incompatible Netty 4.1 modules. The `load-tests` profile now selects Netty
  4.2.14.Final only for load execution; both simulations passed after the fix.
- `docker compose config --quiet` and `git diff --check` passed.

The original delivery review found no unresolved BLOCKER or HIGH issue. Review
fixes made the PostgreSQL timeout transaction-local, separated generic
transient-database metrics from lock-contention metrics, restricted RabbitMQ
deserialization to the event package, and added rollback, recovery,
cross-transfer, and 100-request contention evidence. The later outbox recovery
mechanism was explicitly superseded by ADR-0030; direct publication and Flyway
V3 were revalidated through the configured unit, isolated, and integrated
gates. No absent quality gate is claimed.

Delivery checkpoints:

- `3ec5035` — EPIC002 persistence foundation and approved design artifacts.
- `ebad351` — transfer API, transaction, outbox, metrics, tests, and prepared
  Gatling profile.
- `26d9879` — strengthened rollback, cross-transfer, lock-metric, and
  100-request concurrency verification.

EPIC002 implementation, local verification, authorized load execution,
subsequent direct-publication simplification, consistency checks, cleanup, and
documentation are complete.

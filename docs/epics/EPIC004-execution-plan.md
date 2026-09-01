# EPIC004 — Execution Plan

## Approved contract revision — fixed lookback periods

BDR-0006 supersedes the original arbitrary `start` and `end` contract described
later in this historical plan. The active revision keeps the endpoint,
pagination, ordering, account scope, movement-type filter, and response envelope
unchanged while replacing those parameters with `period=1d|1w|1M`, defaulting
to `1d`.

### Revision acceptance criteria

- The request exposes `period` and no longer exposes `start` or `end`.
- Omitted `period` resolves to one day before the request instant.
- `1d`, `1w`, and `1M` resolve respectively to one day, one week, and one
  calendar month before the same request instant.
- Unsupported or case-mismatched period values return safe `400` Problem
  Details before the service is called.
- Period and `CREDIT`/`DEBIT` filters may be combined.
- The repository receives real computed bounds and contains no sentinel date.
- Existing pagination, ordering, ownership, empty-result, error, metrics, and
  read-only behavior remains unchanged.

### Revision slices

1. Supersede BDR-0002 with BDR-0006 and align EPIC004, the decision register,
   Swagger descriptions, and public documentation with the approved contract.
2. Replace API date fields with the validated period string, map it to an
   application period enum, resolve deterministic bounds through the existing
   application `Clock`, and remove the repository sentinel-date path.
3. Update unit tests for mapping and exact day/week/calendar-month bounds;
   update MVC tests for defaults, accepted values, combinations, and rejection;
   update PostgreSQL functional tests for real boundary filtering without table
   cleanup; update OpenAPI contract assertions.
4. Run focused tests, configured quality gates, independent review, affected
   reruns, and final documentation synchronization.

No migration, logical data-model change, new dependency, RabbitMQ behavior, or
real-boundary integrated scenario is required. Every computed-boundary method,
application-owned model property, and changed test scenario retains the
project's mandatory JavaDoc baseline.

## Preconditions and decisions

- Scope is limited to the read-only
  `GET /api/v1/accounts/{accountId}/movements` contract defined by
  [EPIC004](EPIC004-account-movement-listing.md) and
  [BDR-0002](../bdr/BDR-0002-financial-movement-query-rules.md).
- [EPIC003](EPIC003-functional-test-suite-simplification.md) must be completed
  first so PostgreSQL-backed application flows use the canonical isolated
  functional source set and lifecycle.
- The endpoint returns only movements owned by the account in the path. It does
  not return balances or modify account, movement, token, transfer, or
  notification state.
- Pagination is zero-based with page `0` as the default and a fixed maximum of
  10 items. There is no client-selected page size.
- Optional `start` and `end` filters use offset-bearing ISO 8601 date/time
  values. `start` is inclusive, `end` is exclusive, and a supplied range must
  have `start` before `end`.
- The optional public movement types are exactly `CREDIT` and `DEBIT`, matching
  the existing persistence enum and database constraint. No translation to
  additional public values is planned.
- Results use deterministic newest-first ordering by `createdAt DESC`, followed
  by movement `id DESC` as a tie-breaker.
- The custom response envelope contains `content`, `page`, `size`,
  `totalElements`, and `totalPages`; persistence entities and Spring Data page
  implementations are not exposed through the HTTP contract.
- An existing account with no matching movements returns an empty `200 OK`
  page. A missing account returns `404`; invalid page, date/time, range, or type
  input returns safe RFC 9457 Problem Details with `400`.
- `/api/v1/**` remains temporarily unauthenticated under
  [ADR-0027](../adr/ADR-0027-defer-api-authentication-for-the-initial-scope.md).
  EPIC004 does not expand or redesign authentication.
- Flyway V1 already stores every required field and includes account/date and
  account/type/date indexes. The logical model already requires this query
  path, so no migration or logical data-model change is planned.
- Existing Spring MVC, validation, Spring Data JPA, MapStruct, Micrometer, and
  Testcontainers dependencies are sufficient. No new dependency is planned.
- The decision register contains no open decision blocking this scope.

## Acceptance criteria

- `GET /api/v1/accounts/{accountId}/movements` returns only movements belonging
  to the requested existing account.
- The default request returns page zero with at most 10 items.
- Later pages return the corresponding non-overlapping slice in deterministic
  newest-first order.
- `start`, `end`, `type=CREDIT`, and `type=DEBIT` work independently and in
  combination.
- Date/time range boundaries are applied as `[start, end)`.
- Empty matches return `200 OK` with empty content and zero totals.
- Unknown accounts return safe `404` Problem Details.
- Negative page values, malformed dates, invalid/equal/reversed ranges, and
  unsupported movement types return safe `400` Problem Details.
- The endpoint is read-only and does not mutate financial data.
- API metrics record the movement-list operation with bounded cardinality and
  no account, movement, operation, amount, or date value as a metric tag.
- Existing account creation, transfer, notification, migration, and security
  behavior remains unchanged.

## Ordered slices

1. **Persistence query and read-only application use case.** Extend
   `MovementRepository` with one pageable account-scoped query whose optional
   predicates cover `start`, `end`, and `MovementType`. Reuse
   `AccountRepository` to distinguish an absent account from an empty movement
   history. Add purpose-specific input, item, and page DTOs under `model.dto`
   and a `@Transactional(readOnly = true)` listing service that validates the
   range, enforces the fixed page size and deterministic sort, invokes the
   repository, and maps results without initializing the lazy Account graph.
   Unit tests cover default pagination, later pages, every filter combination,
   ordering, empty content, unknown accounts, invalid ranges, and collaborator
   non-invocation after validation failure.
2. **Versioned HTTP contract, mapping, errors, and metrics.** Add the account
   movement filter and response records under `model.api`, plus a MapStruct
   mapper under `model.mapper` for API-to-DTO and DTO-to-response conversion.
   Add a thin movement-list controller under `api`, extend centralized Problem
   Details translation for query validation and absent accounts, and add
   `movement.list` to the existing bounded API metrics operations. Unit and
   isolated MockMvc tests cover the exact response envelope, default values,
   ISO 8601 parsing, all supported filters, combined filters, empty pages,
   `400`/`404`, metrics, unauthenticated access, and rejection of unsupported
   methods without exposing persistence or financial details.
3. **Mock-free PostgreSQL verification.** Add an isolated
   `AccountMovementListingFunctionalTest` using the real random-port
   application, Flyway V1–V3, and a disposable PostgreSQL 17.6 Testcontainer.
   Create uniquely identifiable accounts and movement fixtures without clearing
   tables. Verify ownership isolation, more-than-10-item pagination,
   deterministic order including equal timestamps, both date boundaries,
   `CREDIT`, `DEBIT`, combined filters, empty results, invalid input, unknown
   accounts, and unchanged persisted rows after reads.
4. **Quality, review, and documentation completion.** Run configured quality
   gates, review scope, query behavior, lazy-loading safety, API exposure,
   sensitive-data handling, and unnecessary complexity. Apply findings, rerun
   affected checks, then synchronize EPIC004, the README, and the shared
   execution report with actual implementation and validation evidence.

## Expected components and documentation

- `src/main/java/.../repository/MovementRepository.java` — one pageable,
  account-scoped filtered query.
- `src/main/java/.../service/` — cohesive read-only movement listing use case.
- `src/main/java/.../model/dto/` — listing input, movement item, and page DTOs.
- `src/main/java/.../model/api/` — query/filter and response records.
- `src/main/java/.../model/mapper/` — MapStruct boundary mappings.
- `src/main/java/.../api/` — thin versioned listing controller and safe error
  translation integration.
- `src/main/java/.../metrics/ApiOperation.java` — bounded `movement.list`
  operation tag.
- `src/test/unit/java/.../` — service, mapper, controller, metrics, and error
  scenarios.
- `src/test/isolated/java/.../` — isolated MVC contract scenarios plus the
  mock-free HTTP/PostgreSQL listing scenarios.
- `docs/epics/EPIC004-account-movement-listing.md`, this plan, the README, and
  the shared execution report — planned and final public documentation.
- No Flyway migration, new dependency, entity relationship, RabbitMQ behavior,
  Gatling simulation, write use case, or additional filter is included.

The repository query, listing service, mapper methods that transform page
content, controller orchestration, and exception translation will receive
JavaDoc stating what each boundary does and why it exists. Every application-
owned record property will have concise property documentation. Every new or
changed test method will state the behavior it proves and why that scenario
matters. The fixed page size, `[start, end)` range semantics, deterministic
ordering, absent-account distinction, and read-only behavior will remain
documented next to their smallest relevant code boundaries.

## Quality strategy

- `./mvnw -B -ntp clean test` — process-local unit tests.
- `./mvnw -B -ntp clean verify` — unit plus isolated functional tests and the
  at-least-90% eligible line-coverage gate. Docker is required for the
  disposable PostgreSQL functional scenarios.
- `./mvnw -B -ntp clean -Pintegrated-functional-tests verify` — optional
  existing RabbitMQ adapter regression; EPIC004 adds no integrated scenario.
- `docker compose config --quiet` — unchanged local infrastructure validity.
- Review verifies fixed pagination, deterministic ordering, optional-filter
  combinations, safe errors, no entity exposure, no N+1/lazy graph loading, no
  sensitive logging or metric tags, unchanged write behavior, and absence of
  speculative filters or abstractions.
- No lint, static-analysis, dependency/security scanner, or standalone schema
  quality tool is currently configured; the plan does not claim an absent gate.

## Integrated strategy

No EPIC004 integrated test applies. The production controller, mapper, service,
repository, JPA mapping, security chain, serialization, Flyway history, and
disposable PostgreSQL execute in the isolated functional suite established by
EPIC003. Scenarios use unique fixtures, scope every assertion to those
identifiers, never clear tables, and discard the whole test-owned container.
RabbitMQ, external applications, and Gatling are outside this read-only scope,
so no Workflow 05 authorization pause is expected.

## Source-control behavior

Development authorization covered coherent non-destructive commits and pushes
for the planned slices, quality/review fixes, isolated verification, and final
documentation. Only EPIC004 files were staged; unrelated
worktree changes were preserved. Amend, squash, force-push, pull request creation, merge,
deployment, and release remain excluded unless separately requested.

## Checkpoint

- Status: fixed-lookback contract revision in progress on 2026-09-01.
- Completed revision slices: BDR/contract documentation, application and
  repository implementation, unit tests, MVC contract tests, OpenAPI checks,
  and real HTTP/PostgreSQL period scenarios.
- Focused validation: 18 affected unit tests passed; the focused PostgreSQL
  lifecycle passed all 68 unit tests and 5 movement scenarios; the focused MVC
  and OpenAPI lifecycle passed all 68 unit tests and 9 isolated scenarios. The
  configured coverage check passed in both `verify` executions.
- Active next action: run comprehensive quality gates, independent review, and
  final documentation synchronization.
- Original delivery status: completed on 2026-09-01.
- Completed work: the account-scoped pageable query, read-only service, DTOs,
  public API models, MapStruct mapper, controller, safe errors, bounded
  `movement.list` metrics, unit/MVC/PostgreSQL tests, review fixes, and public
  documentation.
- Focused PostgreSQL validation: `./mvnw -B -ntp
  -Dit.test=com.elziojunior.simplifiedbankingservice.account.AccountMovementListingFunctionalTest
  verify` passed 69 unit tests and 5 real movement-listing scenarios against a
  disposable PostgreSQL 17.6 container with no RabbitMQ dependency.
- Comprehensive validation: `./mvnw -B -ntp clean test` passed 69 unit tests;
  `./mvnw -B -ntp clean verify` passed 69 unit and 42 isolated functional tests
  with the coverage gate; `./mvnw -B -ntp clean
  -Pintegrated-functional-tests verify` additionally passed the existing single
  RabbitMQ integration scenario.
- Review corrected stable UUID mismatch classification, separated the public
  movement enum from persistence, and added movement-specific safe database
  failure wording and verification. No review finding remains open.
- Decision impact: no new BDR, ADR, engineering standard, logical data-model
  update, Flyway migration, or dependency is required.
- Expected integrated boundary: none; PostgreSQL belongs to the isolated
  functional environment after EPIC003, and no consequential authorization
  gate is expected.

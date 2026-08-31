# EPIC001 — Execution Plan

## Preconditions and decisions

- Scope is limited to `POST /api/v1/accounts`; account read, list, update, and
  delete operations remain excluded by
  [BDR-0001](../bdr/BDR-0001-account-management-scope-and-account-creation-rules.md)
  and [EPIC001](EPIC001-account-creation.md).
- The existing Flyway V1 `accounts` table is the physical schema source of
  truth. EPIC001 adds no migration and must keep Hibernate in validation-only
  mode.
- Account IDs remain PostgreSQL-generated `BIGINT` values, balances map through
  `BigDecimal` to `NUMERIC(19,2)`, and creation instants map to `TIMESTAMPTZ`.
- Initial balances are normalized to scale two with `RoundingMode.HALF_EVEN`
  under
  [ADR-0022](../adr/ADR-0022-use-bigdecimal-with-half-even-rounding-for-monetary-values.md).
  Negative input is rejected before normalization; zero is accepted, and
  values exceeding the `NUMERIC(19,2)` range are rejected safely.
- The successful contract is `201 Created` with generated ID, name, normalized
  balance, and an offset-bearing creation timestamp. Invalid requests use safe
  RFC 9457 Problem Details and persist no account.
- [ADR-0027](../adr/ADR-0027-defer-api-authentication-for-the-initial-scope.md)
  temporarily permits `/api/v1/**` without authentication or CSRF enforcement.
  Security configuration will retain a traceable TODO for future bearer-token
  authentication through the `Authorization` header; operational endpoints are
  not included in this exception.
- Existing Spring MVC, Spring Data JPA, Spring Security, validation, PostgreSQL,
  Flyway, Testcontainers, and test dependencies are sufficient. No production
  dependency is planned.
- There are no unresolved EPIC001 decisions or conflicts in the decision
  register.

## Ordered slices

1. Add the Account persistence model and Spring Data repository mapped exactly
   to Flyway V1, then add an application-facing account-creation command,
   result, and transactional service. Keep validation and monetary
   normalization in the application/domain path rather than the controller or
   persistence entity;
   inject a clock so creation time is deterministic in unit tests. Unit tests
   cover positive and zero balances, pre-rounding negative rejection including
   negative sub-cent values, `HALF_EVEN` scale-two normalization, numeric range,
   required/nonblank/maximum-length names, generated result mapping, and
   repository non-invocation on invalid input.
2. Add request/response DTOs, the versioned Spring MVC controller, centralized
   validation-to-Problem-Detail handling, and the narrowly scoped temporary
   security configuration from ADR-0027. Isolated MockMvc tests cover `201`
   serialization, missing/blank/oversized names, null/negative balances, safe
   RFC 9457 errors, unauthenticated/CSRF-token-free access, numeric overflow,
   and `404` behavior for unsupported account-management routes.
3. Add an opt-in mock-free `AccountCreationIntegratedFunctionalTest` that
   starts the real web application with disposable PostgreSQL 17.6, enters
   through HTTP, applies Flyway V1, and verifies positive/zero creation,
   independent generated IDs, normalized monetary round trips, persisted UTC
   instants, and absence of rows after invalid requests. Reuse the existing
   integrated Maven profile and Testcontainers dependencies.
4. Run all configured quality gates, review business/architecture/security/data
   compliance and unnecessary complexity, apply findings, rerun affected
   checks, then synchronize EPIC001, the shared execution report, and public
   development documentation.

## Expected components and documentation

- `src/main/java/.../account/model/` — data-only JPA Account mapping.
- `src/main/java/.../account/repository/` — Spring Data persistence boundary.
- `src/main/java/.../account/service/` — creation command/result and use case.
- `src/main/java/.../account/api/` — request/response DTOs, controller, and
  RFC 9457 exception mapping.
- `src/main/java/.../configuration/` — clock and temporary API security
  configuration.
- `src/test/unit/java/.../account/` — application/domain unit scenarios.
- `src/test/isolated/java/.../account/` — isolated MVC contract scenarios.
- `src/test/integrated/java/.../account/` — real HTTP/PostgreSQL scenarios.
- No new migration, messaging component, transfer behavior, or account-query
  endpoint is included.

Methods containing validation, normalization, mapping decisions, exception
translation, or security behavior will receive JavaDoc explaining what they do
and why the boundary exists. Application-owned DTO/command/result/entity
properties will have concise property documentation. Every test method will
state the behavior it proves and why the scenario matters. The temporary
authentication TODO will reference ADR-0027 and describe its removal condition.

## Quality strategy

- `./mvnw -B -ntp clean test` runs process-local unit tests and enforces at
  least 90% line coverage for eligible application logic.
- `./mvnw -B -ntp clean verify` runs unit and isolated functional tests without
  Docker or another external process.
- `./mvnw -B -ntp clean -Pintegrated-functional-tests verify` runs the existing
  migration suite plus real account-creation HTTP/PostgreSQL scenarios.
- `docker compose config --quiet` validates unchanged local infrastructure
  configuration.
- Review verifies layer boundaries, absence of business behavior in controller
  and persistence mapping, exact monetary handling, safe error details,
  timestamp semantics, unsupported endpoint absence, no sensitive logging, and
  the narrow scope of the temporary authentication exception.
- No lint, static-analysis, or dependency/security scanner is currently
  configured; this plan does not weaken or misreport those absent gates.

## Integrated strategy

The integrated suite uses a random local HTTP port and its own PostgreSQL 17.6
Testcontainer. Each scenario creates minimal data through the real public API,
observes the response and final database state, and truncates owned tables
between scenarios. It uses the production controller, validation, service,
repository, Hibernate mapping, security chain, serialization, and Flyway
migration without mocks.

The container and HTTP server are disposable local resources with no shared
state, credentials, cost, or consequential external side effect. Therefore no
Workflow 05 real-boundary authorization gate applies; after development
authorization, the lifecycle may execute this suite and continue through
finalization.

## Checkpoint

- Status: in progress
- Completed slices: 1–2
- Validation evidence:
  - `./mvnw -B -ntp clean test` — 13 core unit scenarios passed at slice 1
  - `./mvnw -B -ntp clean verify` — 17 unit and 7 isolated functional
    scenarios passed with the 90% eligible-code coverage gate at slice 2
- Next action: implement the mock-free HTTP/PostgreSQL integrated scenarios

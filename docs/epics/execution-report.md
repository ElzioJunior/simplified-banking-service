# Development Execution Report

This is the single execution report for all epic execution plans.

## Portfolio status

| Epic | Execution plan | Status | Latest checkpoint |
| --- | --- | --- | --- |
| EPIC000 — Core Database Schema | [Plan](EPIC000-execution-plan.md) | Completed | Migration and 6 real PostgreSQL tests passed |
| EPIC001 — Account Creation | [Plan](EPIC001-execution-plan.md) | In progress | HTTP contract and isolated tests completed |
| EPIC002 — Account-to-Account Transfer | [Plan](EPIC002-execution-plan.md) | Backlog | Workflow 01 not started |

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

## Active plan: EPIC001

### What will be implemented

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

At the latest checkpoint, the persistence/use-case slice and HTTP/security
slice are complete. `./mvnw -B -ntp clean verify` passed 17 unit tests and 7
isolated MVC scenarios, including the 90% eligible-code line-coverage gate.

## Source control

EPIC001 planning changes remain uncommitted for review. After authorization,
normal coherent non-destructive commits and pushes are part of this plan.
Force-push, history rewriting, pull requests, deployments, and releases remain
excluded unless explicitly requested.

## Authorization request

Development authorization was granted on 2026-08-31. It covers the planned
implementation, validation, review fixes, local disposable integrated suite,
final documentation, and normal non-destructive commits and pushes. No
consequential external-boundary authorization gate applies to EPIC001.

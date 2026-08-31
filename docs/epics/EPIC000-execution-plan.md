# EPIC000 — Execution Plan

## Preconditions and decisions

- The user approved `BIGINT` identity IDs, UUID operation IDs,
  `NUMERIC(19,2)`, `TIMESTAMPTZ`, `VARCHAR(255)`, the two-table scope, database
  constraints, query indexes, and PostgreSQL Testcontainers validation.
- Existing proposed BDRs and ADRs remain the active baseline; their statuses do
  not change in this epic.
- The canonical logical data model records the approved physical mapping before
  production schema work begins.

## Ordered slices

1. Add `V1__create_accounts_and_movements.sql` with named tables, columns,
   primary/foreign keys, checks, uniqueness, and query-supporting indexes; run
   the normal Maven build and a local Flyway startup smoke check.
2. Add the minimum Spring Boot Testcontainers and PostgreSQL test dependencies,
   then create an opt-in integrated test that starts the application against a
   disposable PostgreSQL 17.6 instance and verifies migration idempotence,
   metadata, indexes, valid rows, and constraint failures.
3. Run all quality gates, review schema correctness/security/maintainability,
   apply findings, rerun affected checks, and synchronize final documentation.

## Quality strategy

- `./mvnw test` proves the unit lifecycle remains independent of infrastructure.
- `./mvnw verify` proves the normal build and isolated lifecycle remain
  PostgreSQL-independent.
- `./mvnw -Pintegrated-functional-tests verify` executes the real PostgreSQL
  migration suite.
- `docker compose config --quiet` and a clean local PostgreSQL startup provide a
  separate Flyway/application smoke check.
- Review verifies named constraints, non-cascading retention, fixed precision,
  index coverage, absence of feature tables, and absence of secrets or seed data.

## Integrated strategy

The test creates and removes its own PostgreSQL 17.6 container, enters through
the real Spring Boot application context, applies the real Flyway migration, and
uses JDBC assertions against PostgreSQL. No mocks, shared database, credentials,
or consequential external system are involved.

## Checkpoint

- Status: in progress
- Completed slices: 1 and 2
- Validation evidence:
  - `./mvnw -B -ntp clean verify` — passed
  - `./mvnw -B -ntp test` — passed without Docker
  - `./mvnw -B -ntp verify` — passed without Docker
  - `./mvnw -B -ntp -Pintegrated-functional-tests verify` — 6 tests passed against disposable PostgreSQL 17.6
  - `docker compose config --quiet` — passed
  - Clean PostgreSQL 17.6 application startup — V1 applied successfully
  - Schema inspection — `accounts`, `movements`, Flyway history, and expected indexes present
- Next action: run final quality gates, review, and documentation synchronization

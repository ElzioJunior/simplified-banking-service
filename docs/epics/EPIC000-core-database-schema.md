# EPIC000 — Core Database Schema

## Outcome

Initialize a clean PostgreSQL database with the Account and Movement storage
required by the documented banking domain, using a deterministic Flyway
migration and database-enforced structural invariants.

## Scope

- Create the `accounts` and `movements` tables.
- Generate Account and Movement identifiers with PostgreSQL identity columns.
- Store monetary values as `NUMERIC(19,2)` and timestamps as `TIMESTAMPTZ`.
- Represent transfer operation identifiers as UUID values.
- Enforce required values, nonblank account names, nonnegative balances,
  positive movement amounts, supported movement types, referential integrity,
  and at most one movement of each type per operation.
- Add indexes for operation correlation and account-scoped movement queries by
  date/time and type.
- Prove the migration and constraints against a disposable real PostgreSQL
  instance.

## Out of scope

- JPA entities, repositories, services, controllers, or business features.
- Seed or fixture data.
- A persisted Transfer entity.
- Transfer idempotency-token storage.
- Notification, queue, retry, dead-letter, or outbox storage.
- Corrective financial processes or deletion workflows.

## Governing context

- Product: [repository scope](../../README.md),
  [account creation](EPIC001-account-creation.md), and
  [account-to-account transfer](EPIC002-account-to-account-transfer.md)
- BDRs: [BDR-0001](../bdr/BDR-0001-account-management-scope-and-account-creation-rules.md),
  [BDR-0002](../bdr/BDR-0002-financial-movement-query-rules.md), and
  [BDR-0003](../bdr/BDR-0003-account-to-account-transfer-business-rules.md)
- ADRs: [ADR-0003](../adr/ADR-0003-use-postgresql-as-the-relational-database.md),
  [ADR-0005](../adr/ADR-0005-use-spring-data-jpa-with-hibernate.md),
  [ADR-0010](../adr/ADR-0010-use-flyway-for-database-migrations.md),
  [ADR-0013](../adr/ADR-0013-use-bigdecimal-for-monetary-values.md),
  [ADR-0018](../adr/ADR-0018-use-mock-free-integration-tests-for-critical-flows.md),
  [ADR-0022](../adr/ADR-0022-use-bigdecimal-with-half-even-rounding-for-monetary-values.md),
  [ADR-0023](../adr/ADR-0023-use-lazy-fetching-as-the-default-jpa-relationship-strategy.md),
  [ADR-0024](../adr/ADR-0024-use-pessimistic-write-locking-for-transfer-accounts.md), and
  [ADR-0025](../adr/ADR-0025-use-a-single-read-committed-transaction-per-transfer.md)
- Data model: [logical data model](../database/logical-data-model.md)
- Open decisions: none affecting the two-table initial schema

## Requirements and acceptance criteria

- [x] Flyway initializes an empty PostgreSQL database from one versioned migration.
- [x] `accounts.id` and `movements.id` are generated `BIGINT` identity primary keys.
- [x] Account names are required, limited to 255 characters, and cannot be blank.
- [x] Account balances are required `NUMERIC(19,2)` values greater than or equal to zero.
- [x] Movement amounts are required `NUMERIC(19,2)` values greater than zero.
- [x] Movement types are restricted to `CREDIT` and `DEBIT`.
- [x] Every movement references an existing account and account deletion does not cascade to movements.
- [x] Operation identifiers are required UUID values and support efficient correlation queries.
- [x] An operation cannot contain duplicate movements of the same type.
- [x] Account/date and account/type/date query paths have supporting indexes.
- [x] Both tables require unambiguous timestamp-with-time-zone values.
- [x] Re-running Flyway against the migrated database is idempotent.
- [x] No feature code, seed data, idempotency-token table, notification table, or outbox table is introduced.

## Risks and failure behavior

The database can enforce row-level validity and prevent duplicate movement
types within one operation, but it cannot express the complete multi-row
transfer invariant with ordinary checks. The application transaction must still
create exactly one debit and one credit with equal amounts on different
accounts. Schema creation must fail atomically if any migration statement is
invalid. Applied migration history must never be edited in place.

## Integrated-test scope

An opt-in `*IntegratedFunctionalTest` starts the real Spring application context
against a disposable PostgreSQL 17.6 Testcontainer. It verifies Flyway startup,
schema metadata, indexes, representative valid inserts, and rejection of invalid
rows. The container is local and disposable, so no consequential external-boundary
authorization gate applies.

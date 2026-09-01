# ADR-0031 — Separate Isolated Application Tests from Real-Boundary Integration Tests

- Status: Accepted
- Date: 2026-09-01
- Deciders: User and Engineering Team
- Supersedes: ADR-0017, ADR-0018
- Superseded by: none
- Related: ADR-0003, ADR-0009, ADR-0010, ADR-0015, ADR-0016, ADR-0019

## Context

The previous taxonomy equated isolation with an absence of real infrastructure
and required every integration test to start the complete application without
mocks. That distinction does not describe the boundary the project needs to
qualify.

PostgreSQL is owned by this application. A complete application flow using a
disposable, guarded PostgreSQL instance can remain isolated from other
applications and external systems while proving HTTP, Spring wiring, Flyway,
JPA, transactions, locking, and persistence together. RabbitMQ, in contrast,
is an independent messaging boundary whose production adapter and topology
need focused compatibility evidence.

## Decision

Use three test categories:

- Unit tests are process-local and exercise focused behavior without Spring,
  sockets, databases, containers, or external processes.
- Isolated functional tests exercise meaningful application flows through real
  application wiring. They may use application-owned disposable infrastructure
  such as a PostgreSQL Testcontainer, but external applications and messaging
  systems are replaced by controlled test doubles. Every database instance is
  guarded as test-owned, fixtures are unique and scenario-scoped, tables are
  never cleared, and the whole instance is discarded after the suite.
- Integrated functional tests are opt-in and qualify a named real adapter or
  external-system boundary through its production implementation. The boundary
  under qualification is never mocked; collateral boundaries may be omitted or
  controlled so the test remains focused and safe.

PostgreSQL-backed account, schema, and transfer application flows belong to the
isolated lifecycle. The integrated source set contains one focused RabbitMQ
publisher/topology compatibility test. This test uses a disposable broker and
does not start PostgreSQL or duplicate the complete transfer flow.

## Consequences

### Positive

- Test names describe the boundary being isolated or qualified.
- Complete application and persistence behavior remains covered with real PostgreSQL.
- RabbitMQ compatibility remains proven without duplicating business-flow assertions.
- Failures localize more clearly to application behavior or the messaging adapter.
- External systems do not become dependencies of the normal isolated lifecycle.

### Negative or trade-offs

- Full isolated verification requires Docker because PostgreSQL remains real.
- The isolated suite is slower than a mock-only suite.
- Mocking the publisher in transfer flows no longer proves one continuous HTTP-to-broker path.
- Developers and CI must distinguish the Docker-backed isolated lifecycle from the opt-in RabbitMQ profile.

## Alternatives considered

- Treat every container as integration — rejected because ownership and the
  boundary under qualification matter more than whether a process is separate.
- Keep one complete HTTP/PostgreSQL/RabbitMQ suite — rejected because it mixes
  application behavior with adapter compatibility and makes failures harder to
  diagnose.
- Mock PostgreSQL in isolated flows — rejected because it would remove evidence
  for migrations, constraints, JPA mappings, transactions, and locking.
- Require integrated tests to be universally mock-free — rejected because
  collateral infrastructure can obscure the specific real boundary being
  qualified.

## Validation

The default unit lifecycle remains process-local. The isolated lifecycle runs
complete application flows against an exact disposable PostgreSQL instance
without RabbitMQ. The opt-in integrated lifecycle runs exactly one production
RabbitMQ publisher/topology test against a disposable broker without
PostgreSQL. Source-set searches verify the naming and membership rules, and no
functional test clears database tables.

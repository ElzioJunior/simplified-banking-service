# FEATURE-001 — Project Foundation

## Outcome

Provide a reproducible, bootable application foundation on which the documented
banking features can be implemented without first establishing build, runtime,
test, or local-infrastructure conventions.

## Scope

- Java 21 and Spring Boot 3.5 Maven application.
- Maven Wrapper and separated unit, isolated functional, and opt-in integrated
  functional test source sets.
- Dependencies selected by the active architecture records for web, validation,
  persistence, security, observability, messaging, and database migrations.
- Environment-backed application configuration.
- Local PostgreSQL and RabbitMQ services through Docker Compose.
- A non-root application container image.
- Build and local-run documentation.

## Out of scope

- Account, movement, transfer, notification, or other business behavior.
- REST controllers and transport contracts.
- Domain models, persistence entities, repositories, and messaging contracts.
- Security policy beyond Spring Boot's framework defaults.
- Flyway SQL migrations and database schema creation.
- WireMock, Testcontainers, Gatling, or provider-specific test harnesses before a
  concrete feature requires them.

## Governing context

- Product: [repository scope](../../README.md),
  [account creation](EPIC001-account-creation.MD), and
  [account-to-account transfer](EPIC002-account-to-account-transfer.MD)
- BDRs: none; this feature adds no business behavior
- ADRs: [ADR-0001](../adr/ADR-0001-use-java-21-or-later.md),
  [ADR-0002](../adr/ADR-0002-use-spring-boot-3-5-or-later.md),
  [ADR-0003](../adr/ADR-0003-use-postgresql-as-the-relational-database.md),
  [ADR-0004](../adr/ADR-0004-use-docker-for-containerization.md),
  [ADR-0005](../adr/ADR-0005-use-spring-data-jpa-with-hibernate.md),
  [ADR-0006](../adr/ADR-0006-use-spring-web-mvc-for-the-rest-api-layer.md),
  [ADR-0007](../adr/ADR-0007-use-spring-security.md),
  [ADR-0008](../adr/ADR-0008-use-spring-boot-actuator-and-micrometer-for-observability.md),
  [ADR-0009](../adr/ADR-0009-use-docker-compose-for-local-infrastructure.md),
  [ADR-0010](../adr/ADR-0010-use-flyway-for-database-migrations.md),
  [ADR-0014](../adr/ADR-0014-use-a-monorepo-with-layered-architecture.md),
  [ADR-0015](../adr/ADR-0015-use-rabbitmq-for-asynchronous-messaging.md),
  [ADR-0016](../adr/ADR-0016-require-at-least-90-percent-unit-test-coverage.md),
  [ADR-0017](../adr/ADR-0017-use-isolated-tests-with-mocks-and-wiremock.md),
  [ADR-0018](../adr/ADR-0018-use-mock-free-integration-tests-for-critical-flows.md), and
  [ADR-0019](../adr/ADR-0019-use-maven-for-build-and-dependency-management.md)
- Open decisions: none affecting this feature-free foundation

## Requirements and acceptance criteria

- [x] The Maven Wrapper builds and packages the application on Java 21.
- [x] `test`, `verify`, and the integrated test profile use the documented test
  source-set boundaries.
- [x] The application starts against local PostgreSQL and RabbitMQ.
- [x] Flyway validates an empty schema without a migration file.
- [x] Hibernate does not create or update the schema.
- [x] The Actuator health endpoint reports a healthy application.
- [x] PostgreSQL and RabbitMQ can be started reproducibly with Docker Compose.
- [x] The application container image builds and starts as a non-root user.
- [x] No feature behavior or migration is introduced.

## Risks and failure behavior

Local ports may already be occupied. Every exposed Compose port and the
application port can be overridden without editing tracked configuration.
Committed credentials are local-development defaults only; runtime values remain
externalized. The absence of migrations is deliberate, and Flyway reports zero
validated migrations until a persistence feature supplies the first schema.

## Integrated-test scope

No consequential external boundary applies. PostgreSQL and RabbitMQ startup and
application connectivity are covered by a local, disposable smoke check; no
real-provider integrated suite or Workflow 05 authorization gate is required.

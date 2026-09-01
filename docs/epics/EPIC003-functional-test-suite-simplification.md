# EPIC003 — Functional Test Suite Simplification

## Outcome

Align the functional-test suites with the project's intended boundary model:
tests that exercise the complete application with its own disposable PostgreSQL
database are isolated functional tests, while RabbitMQ compatibility is proven
by one focused opt-in integration test.

## Delivery status

Completed on 2026-09-01. The test-suite reclassification, build configuration,
real RabbitMQ adapter scenario, documentation, and validation were delivered as
defined in the
[EPIC003 execution plan](EPIC003-execution-plan.md).

## Scope

- Treat the application, its Spring wiring, Flyway migrations, JPA mappings,
  transactions, and its disposable PostgreSQL Testcontainer as one isolated
  functional-test environment.
- Move the existing account HTTP/PostgreSQL, database migration/constraint, and
  transfer HTTP/PostgreSQL scenarios from `src/test/integrated/java` to
  `src/test/isolated/java`, with names matching the isolated convention.
- Keep PostgreSQL real and test-owned in those scenarios. Preserve the
  datasource guard, unique fixtures, scenario-scoped assertions, and
  whole-container disposal without clearing database tables.
- Replace RabbitMQ in the isolated transfer scenarios with a mocked
  `TransferNotificationPublisher`. Capture and verify the requested event,
  exact invocation count, absence of publication after rejected transfers, and
  absence of an additional publication for idempotent replay.
- Keep the publisher's retry and exhausted-failure behavior in focused unit
  tests.
- Retain one opt-in RabbitMQ integration test using the real publisher,
  production AMQP configuration, and a disposable RabbitMQ Testcontainer. It
  verifies exchange, queue, binding, routing, serialization, publication, and
  consumption without starting PostgreSQL or executing a financial transfer.
- Update Maven, engineering standards, applicable ADRs, agent workflows/skills,
  README instructions, and delivery documentation so the terminology and
  commands describe the resulting suites consistently.

Existing focused MVC slice tests remain part of the isolated source set and
continue to provide fast API-contract diagnostics alongside the complete
PostgreSQL-backed functional scenarios.

## Out of scope

- Production behavior, APIs, persistence mappings, migrations, and messaging
  contracts.
- Replacing PostgreSQL with an in-memory database or mocking repositories in
  complete functional flows.
- Clearing or reusing the development, transactional, shared, staging, or
  production database.
- Running RabbitMQ for isolated functional scenarios.
- Adding a real notification consumer application or another external business
  service.
- Changing Gatling simulations or their dedicated environment.

## Governing context

- Engineering standard:
  [Testing Standards](../engineering/testing-standards.md).
- Existing decisions to supersede or clarify before implementation:
  [ADR-0017](../adr/ADR-0017-use-isolated-tests-with-mocks-and-wiremock.md)
  and
  [ADR-0018](../adr/ADR-0018-use-mock-free-integration-tests-for-critical-flows.md).
- Integrated-test workflow:
  [Workflow 05](../../.agents/workflows/05-integrated-functional-tests.md).
- Product, business rules, APIs, and logical data model: unchanged.
- Open product or data decisions: none.

The execution plan must introduce a superseding architecture decision and
update the recurring testing standard together with the suite implementation;
the current records must not be silently reinterpreted.

## Requirements and acceptance criteria

- [x] Account creation and database migration/constraint scenarios execute as
      isolated functional tests against disposable PostgreSQL.
- [x] Transfer HTTP, transaction, persistence, rollback, idempotency, locking,
      and concurrency scenarios execute as isolated functional tests against
      disposable PostgreSQL without starting or contacting RabbitMQ.
- [x] A successful new transfer requests exactly one notification containing
      the approved event data.
- [x] Rejected transfers request no notification, and an idempotent replay does
      not request an additional notification.
- [x] Publisher unit tests continue to prove routing invocation, bounded retry,
      and contained exhausted failures.
- [x] Exactly one focused integrated test proves the production publisher and
      AMQP topology against a disposable RabbitMQ broker.
- [x] The RabbitMQ integration test publishes and consumes the approved event
      with matching identifiers, type, recipient, amount, and timestamp.
- [x] No functional test clears database tables or connects to a non-test-owned
      PostgreSQL instance.
- [x] Unit tests remain process-local; the isolated functional lifecycle may
      require Docker for PostgreSQL; RabbitMQ remains opt-in.
- [x] Maven configuration, test names, documentation, workflows, and skills use
      one consistent test-boundary definition.
- [x] Existing production behavior and observable contracts remain unchanged.

## Risks and failure behavior

Moving PostgreSQL-backed scenarios into the normal isolated lifecycle makes
Docker availability a prerequisite for full `verify` execution and increases
its duration. Failures must clearly distinguish unavailable Docker from an
application assertion failure. Every persistence scenario must fail before
execution unless its datasource is proven to be its exact running disposable
container.

Mocking the notification publisher removes the continuous
HTTP-to-PostgreSQL-to-broker path. The focused RabbitMQ integration test and
publisher unit tests preserve the lost adapter evidence, while argument capture
in the transfer functional suite proves the application-side contract. The
isolated Spring context must be configured so AMQP infrastructure does not
attempt a background broker connection.

## Integrated-test scope

Only the RabbitMQ adapter boundary remains in the integrated source set. One
focused scenario starts a disposable local RabbitMQ container, applies the real
exchange/queue/binding and message converter configuration, publishes through
`TransferNotificationPublisher`, and consumes the routed event. The broker is
local, test-owned, cost-free, and discarded whole, so no consequential
Workflow 05 authorization pause is expected after development authorization.

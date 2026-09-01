# EPIC003 — Execution Plan

## Preconditions and decisions

- Scope is limited to the test taxonomy and suite restructuring defined by
  [EPIC003](EPIC003-functional-test-suite-simplification.md). Production
  behavior and contracts must remain unchanged.
- A complete functional test may start the real application and its owned,
  disposable PostgreSQL infrastructure while remaining isolated from external
  applications and messaging systems.
- PostgreSQL-backed functional tests may require Docker and belong to the
  isolated lifecycle. They must retain the exact-container datasource guard,
  unique fixtures, scoped assertions, and whole-container disposal without
  table clearing.
- RabbitMQ is replaced by a mocked `TransferNotificationPublisher` in transfer
  functional scenarios. The mock verifies application intent; it does not
  claim broker delivery.
- One focused opt-in integration test owns the real RabbitMQ compatibility
  evidence. It exercises the production publisher, topology, routing, and JSON
  conversion without PostgreSQL or a complete transfer flow.
- Existing publisher unit tests remain responsible for deterministic retry and
  failure-exhaustion branches.
- ADR-0017 and ADR-0018 currently express the superseded taxonomy. A new ADR
  must explicitly supersede them before the implementation slice is considered
  complete; accepted history must not be rewritten.
- The testing standard, build configuration, workflows, skills, README, and
  delivery artifacts must be updated together so no conflicting convention
  remains.
- No BDR, product document, logical data-model, Flyway migration, production
  dependency, or production configuration change is required.

## Acceptance criteria

- `src/test/integrated/java` contains exactly one test class, focused on the
  RabbitMQ publisher and AMQP topology.
- Account, migration/constraint, and transfer application flows reside under
  `src/test/isolated/java` and use the `*FunctionalTest` suffix.
- Transfer functional tests start PostgreSQL but neither start nor contact
  RabbitMQ.
- Successful, replayed, and rejected transfer scenarios verify the correct
  `TransferNotificationPublisher` interactions and captured event data.
- The RabbitMQ integration scenario starts no PostgreSQL container and proves
  that a published `TransferCompletedNotification` can be consumed unchanged
  from the configured queue.
- Default unit execution remains Docker-free. Full isolated verification may
  require Docker for PostgreSQL and reports that prerequisite explicitly.
- The integrated profile remains opt-in and adds RabbitMQ compatibility
  verification.
- No scenario clears tables or can resolve to the application's transactional
  or another shared database.
- Existing unit, functional, migration, concurrency, messaging, and coverage
  assertions remain equivalent or stronger after reclassification.

## Ordered slices

1. **Superseding decision and canonical test standard.** Add a new ADR that
   explicitly supersedes ADR-0017 and ADR-0018 with the approved test-boundary
   taxonomy. Update `docs/engineering/testing-standards.md`, Workflow 05, and
   the applicable test skills so isolated functional tests may use guarded
   disposable PostgreSQL, integrated tests remain opt-in for real adapter or
   external-system compatibility, and commands/prerequisites remain explicit.
2. **PostgreSQL-backed isolated source set.** Move and rename the account
   creation and database migration suites, plus `EphemeralPostgresGuard`, into
   `src/test/isolated/java`. Move the non-messaging transfer scenarios into the
   same source set. Adjust Maven includes, excludes, source sets, and lifecycle
   documentation so `test` stays process-local and `verify` executes all
   isolated scenarios with a clear Docker prerequisite.
3. **Mocked notification boundary in transfer flows.** Replace the transfer
   suite's RabbitMQ container and `RabbitTemplate` consumption with a Spring
   mock of `TransferNotificationPublisher`. Capture the event on a new
   completion and verify its exact values and single invocation. Verify no
   publication for rejected operations and no additional publication for an
   identical replay. Prevent the isolated application context from attempting
   AMQP topology declaration or a background broker connection.
4. **Single RabbitMQ integration test.** Add one
   `TransferNotificationPublisherIntegratedFunctionalTest` using only a
   disposable RabbitMQ Testcontainer and the smallest production AMQP context.
   Publish a deterministic notification, consume it from the configured queue,
   and assert all stable fields. Keep retry/exhaustion tests process-local and
   avoid duplicating business-flow assertions.
5. **Quality, review, and documentation completion.** Run the process-local,
   isolated PostgreSQL, and opt-in RabbitMQ lifecycles; verify source-set
   membership and absence of unintended Rabbit connections; review safety,
   concurrency evidence, wiring, and coverage; apply findings and rerun
   affected checks. Synchronize the README, engineering and agent guidance,
   EPIC003, renumbered EPIC004 references, and the shared execution report with
   actual results.

## Expected components and documentation

- `pom.xml` — source-set membership, naming filters, and lifecycle behavior.
- `src/test/isolated/java/...` — MVC slices plus complete account, schema, and
  transfer functional scenarios using disposable PostgreSQL where applicable.
- `src/test/integrated/java/...` — one RabbitMQ publisher/topology scenario.
- `src/test/unit/java/.../TransferNotificationPublisherTest.java` — unchanged
  ownership of retry and exhausted-failure branches, with focused adjustments
  only if review finds a coverage gap.
- `docs/adr/` — one superseding test-boundary ADR and updated index.
- `docs/engineering/testing-standards.md` — canonical recurring conventions and
  prerequisites.
- `.agents/workflows/05-integrated-functional-tests.md` and relevant test skills
  — workflow terminology and execution behavior aligned with the standard.
- `README.md`, this Epic, its execution plan, and `execution-report.md` —
  developer commands, scope, authorization, and final evidence.

## Quality strategy

- `./mvnw -B -ntp clean test` — process-local unit tests and the configured
  eligible-line coverage instrumentation without Docker.
- `./mvnw -B -ntp clean verify` — unit, MVC slice, and complete isolated
  functional scenarios against disposable PostgreSQL; Docker is required.
- `./mvnw -B -ntp clean -Pintegrated-functional-tests verify` — the preceding
  gates plus the single real RabbitMQ publisher/topology integration scenario.
- A focused Maven invocation for the RabbitMQ class verifies that it starts no
  PostgreSQL container and does not depend on transfer fixtures.
- `docker compose config --quiet` — unchanged local infrastructure validity.
- `git diff --check` and repository link/reference searches validate the
  documentation and EPIC003/EPIC004 renumbering.
- Review verifies that no business assertion was dropped, the publisher mock is
  reset and verified per scenario, concurrency remains deterministic, AMQP is
  never contacted by isolated tests, and database safety guards remain intact.
- No lint, static-analysis, dependency/security scanner, or standalone schema
  quality tool is currently configured; the plan does not claim an absent gate.

## Integrated strategy

The integrated suite contains one local infrastructure-adapter scenario. It
starts a disposable RabbitMQ container, loads the production notification
exchange, queue, binding, message converter, and publisher, publishes one
deterministic event, and consumes the routed value with bounded polling. It
does not start the complete banking application, PostgreSQL, or a real consumer
application and does not claim downstream business delivery.

The broker has no shared state, credential, cost, or production-like effect and
is discarded whole after the suite. Therefore no consequential Workflow 05
authorization pause is expected after development authorization. Any future
test against a real external application remains separately opt-in and must
define its environment, credentials, effects, cleanup, and authorization
boundary.

## Source-control behavior

Development authorization will cover coherent non-destructive commits and
pushes for the planned slices, quality/review fixes, and final documentation.
Stage only EPIC003 implementation files and preserve unrelated worktree
changes. Amend, squash, force-push, history rewriting, pull request creation,
merge, deployment, and release remain excluded unless separately requested.

## Checkpoint

- Status: implementation in progress; development authorized on 2026-09-01.
- Completed work: EPIC003 scope, approved test-boundary direction,
  decision-impact analysis, execution plan, ADR-0031 superseding ADR-0017 and
  ADR-0018, aligned testing standard, Workflow 05, and test skills, PostgreSQL
  suite migration into `src/test/isolated/java`, and mocked publisher
  verification for successful, replayed, rejected, contended, and concurrent
  transfer flows.
- Focused validation: `./mvnw -B -ntp
  -Dit.test=AccountEntityCreationFunctionalTest,DatabaseMigrationFunctionalTest,TransferFunctionalTest
  verify` passed 54 unit tests and 30 selected isolated scenarios against three
  disposable PostgreSQL 17.6 containers; no RabbitMQ connection was configured
  or attempted, and the coverage gate passed.
- Focused RabbitMQ validation: `./mvnw -B -ntp
  -Pintegrated-functional-tests
  -Dit.test=TransferNotificationPublisherIntegratedFunctionalTest verify`
  passed 54 unit tests and the single integrated scenario against one
  disposable RabbitMQ 4.1.4 broker with no PostgreSQL container. Maven tag
  filters kept the integrated scenario out of the isolated execution.
- Remaining work: comprehensive quality execution, review, documentation
  finalization, commits, and pushes.
- Decision impact: one superseding ADR and coordinated engineering-standard,
  workflow, skill, Maven, README, and delivery-document updates are required.
- Product, API, production runtime, business behavior, and logical data model:
  unchanged.
- Expected integrated boundary: one disposable local RabbitMQ Testcontainer;
  no consequential real-boundary authorization gate is expected.
- Next action: run the complete default and opt-in lifecycles, then perform the
  independent review and final documentation synchronization.

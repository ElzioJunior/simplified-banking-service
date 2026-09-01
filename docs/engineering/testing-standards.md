# Testing Standards

## Project configuration

This baseline assumes Maven and separate test source sets. An adopting project
may use Gradle or a different layout, but must update this document, build
configuration, workflows, and skills together so that one canonical convention
remains.

Default conventions:

- Unit sources: `src/test/unit/java`; classes end in `*Test`.
- Isolated functional sources: `src/test/isolated/java`; classes end in
  `*FunctionalTest`.
- Real-boundary integrated sources: `src/test/integrated/java`; classes end in
  `*IntegratedFunctionalTest`.
- Default unit command: `./mvnw test`.
- Default unit plus isolated command: `./mvnw verify`.
- Default integrated command: `./mvnw -Pintegrated-functional-tests verify`.
- Default eligible line-coverage target: 90%. A different target must be
  explicit, justified, configured in the build, and reflected throughout the
  engineering standards.

Shared test utilities must not use a test-suite suffix. Do not keep Java tests
in a legacy source tree once the project adopts the separated layout.

## Functional-test database safety

- Isolated and integrated functional tests must never connect to the
  application's transactional database or any shared database.
- Persistence tests must provision a disposable, test-owned database such as a
  PostgreSQL Testcontainer and fail before each scenario unless the resolved
  datasource is proven to be that exact running test instance.
- Functional tests must not clear or reset database tables through
  `TRUNCATE`, bulk `DELETE`, Flyway clean, schema drops, repository-wide delete
  operations, or equivalent cleanup. Create uniquely identifiable fixtures,
  scope assertions to those fixtures, and discard the whole disposable
  database after the suite.
- A fixture-scoped mutation that exists to verify a database constraint is
  permitted when the test asserts its expected outcome and cannot affect rows
  outside that fixture.

## Unit tests

- Unit tests remain process-local: do not start Spring, open network sockets,
  start databases, or require external processes.
- Test observable behavior and decisions rather than implementation trivia.
- Cover happy paths, important branches and edge cases, and expected failures.
- Prefer deterministic fixtures and avoid excessive mocking.
- Do not weaken assertions or production design merely to meet coverage.
- Every test method has concise JavaDoc explaining what it proves and why the
  scenario matters.

## Coverage

Coverage is a guardrail, not a substitute for meaningful assertions. Exclusions
must be narrow, configured, and technically justified. Reasonable candidates
include generated code, behavior-free persistence entities, pure configuration
holders, behavior-free enums, and framework bootstrap glue.

Do not exclude code because it is inconvenient to test. Tangled, untestable
responsibilities are a design finding.

## Isolated functional tests

- Validate important flows across real application wiring while replacing
  external applications and messaging systems with controlled test doubles.
- Application-owned infrastructure may remain real when it is material to the
  flow. PostgreSQL-backed scenarios use a disposable Testcontainer and may
  therefore require Docker.
- Do not depend on shared mutable environments or manual preparation.
- Enter through a meaningful application boundary and assert final business
  outcomes, not only transport success.
- Cover important successful and failure flows with explicit fixtures.
- Keep tests repeatable locally and in CI.
- Every test method documents the flow/failure and why it matters.

When a scenario is conversational or event-driven, a chronological,
human-readable transcript may accompany the Java assertions. It is evidence,
not an execution framework: it must not hide actions, assertions, or duplicate
production rules.

## Integrated functional tests

Integrated tests qualify one or more consequential real boundaries and are
always opt-in. They supplement rather than replace isolated functional tests.

- Keep the integrated suite outside default `test` and `verify` lifecycles;
  activate it only through the dedicated profile.
- Enter through the real public or adapter boundary named by the scenario and
  use its production implementation.
- Never mock the boundary under qualification. Omit or control collateral
  boundaries when doing so keeps the scenario focused without weakening its
  compatibility claim.
- Use real schema migrations and isolated infrastructure when persistence is in
  scope.
- Keep provider details in infrastructure adapters and test harnesses; assert
  application-owned contracts and stable domain invariants.
- Externalize endpoints, credentials, models/resources, and environment
  selection. Never commit or print secrets.
- Perform actionable preflights and fail clearly when an explicitly requested
  runtime is unavailable.
- Make scenarios independent, seed only minimal uniquely identifiable data,
  and scope observations to the scenario fixtures.
- Do not accept an HTTP acknowledgement or queue publication as sufficient when
  the scenario promises an observable downstream business result.
- Document the exact command and authorization boundary in the feature plan.
- Obtain explicit authorization immediately before the first consequential
  real-provider request or external side effect, as defined by Workflow 05.

## Test quality

- Use descriptive test names and Arrange/Act/Assert structure when it improves
  readability.
- One test should communicate one scenario, though it may assert all outcomes
  needed to prove that scenario.
- Avoid brittle assertions on incidental formatting, generated prose, random
  IDs, timestamps, or unordered collections unless contractually significant.
- Prefer builders and fixtures that expose meaningful inputs; avoid opaque
  fixture magic.
- Do not use sleeps for synchronization when polling, clocks, latches, or
  observable state provide a deterministic alternative.
- Test retries, idempotency, concurrency, and failure recovery when the feature
  relies on those guarantees.

## Required reporting

For every quality or integrated run, record:

- exact command;
- pass/fail;
- meaningful result;
- reason when an applicable check was not executed;
- external provider/environment and date for real-boundary runs.

Never claim a test or check passed unless it actually executed successfully.

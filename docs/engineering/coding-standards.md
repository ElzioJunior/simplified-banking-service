# Coding Standards

This reusable baseline assumes Java 21+ and provides Spring/JPA/Flyway defaults.
Sections tied to a framework apply only when that framework is selected by the
adopting project's active ADRs. Replace them with equally explicit standards
when using another Java stack.

## General

- Optimize for clarity, maintainability, and explicit domain language.
- Prefer the simplest design that correctly satisfies the current requirements.
- Keep business rules out of controllers, transport DTOs, persistence entities, repositories, and configuration classes.
- Prefer small, cohesive components over large utility classes.
- Avoid speculative abstractions and premature generalization.
- Make responsibilities and boundaries explicit.
- Prefer code that another engineer or AI agent can understand and modify safely.

## Pragmatic SOLID

Follow SOLID principles pragmatically. SOLID is a design guide, not a reason to introduce unnecessary layers, interfaces, factories, indirection, or abstractions.

- Single Responsibility: classes and methods should have one clear reason to change.
- Open/Closed: introduce extension points only when there is a real or clearly imminent need.
- Liskov Substitution: implementations must preserve the behavioral contract of their abstractions.
- Interface Segregation: prefer small, focused interfaces when an interface is justified.
- Dependency Inversion: business/application logic should depend on meaningful abstractions at external or architectural boundaries.

Every additional abstraction must have a clear purpose. When two designs satisfy the same requirements, prefer the simpler one.

Do not create an interface merely because a class exists. Interfaces are appropriate for architectural boundaries, multiple meaningful implementations, external dependency isolation, or another concrete design need.

## Internal Service Structure

Services should follow clear internal layers and dependency direction.

Typical packages/layers:

- `api` — HTTP controllers and transport adapters only.
- `model.api` — HTTP request and response models; prefer Java records.
- `model.dto` — application/service input and output DTOs; prefer Java records.
- `model.entity` — persistence entities and persistence-owned enums.
- `model.mapper` — MapStruct mappings between API models and application DTOs.
- `exception` — application-specific exception types shared across layers.
- `metrics` — reusable Micrometer instrumentation with bounded metric names and tags.
- `service` — application orchestration and business behavior.
- `repository` — persistence access.
- `configuration` — Spring and infrastructure configuration.

Additional domain or integration packages may be introduced when genuinely required.

Dependencies should generally flow from API/integration boundaries toward application/domain behavior, not the reverse. Do not create ceremonial layers without a real responsibility.

## API Layer

- Controllers adapt transport concerns to application use cases.
- Controllers must not own business rules.
- Controllers should remain thin.
- Place endpoint-specific OpenAPI metadata in resource interfaces under
  `api.documentation`, implemented by the corresponding controllers. Keep
  runtime Spring MVC mapping, binding, validation, and orchestration on the
  concrete controller.
- Validate transport-level input at the boundary when appropriate.
- Convert external representations into application/domain inputs before executing business behavior.
- Do not expose persistence entities directly through APIs.
- Keep HTTP-specific concepts out of business logic.

## DTOs

- Use Java records by default for immutable request, response, DTO, event, and transport structures when appropriate.
- DTOs contain data and transport-level validation; they do not contain business behavior.
- Use explicit names that communicate direction and purpose.
- Place HTTP request and response models under `model.api`, not alongside
  controllers in `api`. Name them with the `Request` or `Response` suffix that
  describes their boundary role.
- Place application/service input and output DTOs under `model.dto` and name
  them with the `Dto` suffix. Do not introduce `Command`-suffixed application
  input types; represent those inputs as purpose-specific DTOs instead.
- Event and notification records may retain purpose-specific event names when
  they are not service input or output DTOs.
- Keep API-model-to-DTO and DTO-to-API-model construction out of controllers.
  Define use-case-specific MapStruct interfaces under `model.mapper`, generate
  them as Spring components, and fail compilation for unmapped target fields.
- Do not reuse one generic DTO across unrelated use cases merely to reduce class count.
- Do not expose persistence entities as DTOs.

## Service Layer

- Services coordinate application use cases and business behavior.
- Keep services cohesive and focused on a clear capability.
- Avoid large service classes that accumulate unrelated use cases.
- Express business rules with explicit domain language.
- Access external providers and infrastructure through appropriate boundaries.
- Transaction boundaries should be deliberate and as narrow as correctness allows.

## Repository Layer

- Repositories are responsible for persistence access, not business decisions.
- Keep query and persistence concerns inside the repository boundary.
- Do not place workflow orchestration or business rules in repositories.
- Persistence details should not leak unnecessarily into application APIs.
- Prefer Spring Data JPA capabilities before custom persistence infrastructure.

## Configuration

- Keep Spring/infrastructure configuration under clear configuration boundaries.
- Externalize environment-specific configuration.
- Never hard-code credentials, secrets, tokens, or environment-specific endpoints.
- Avoid field injection; prefer constructor injection.
- Configuration classes must not contain business logic.

## Java

- Baseline: OpenJDK 21+.
- Prefer immutable data where practical.
- Use records for immutable transport/value structures when appropriate.
- Use explicit domain types when primitive strings or numbers would obscure meaning.
- Treat nullability deliberately; avoid uncontrolled null propagation.
- Prefer standard Java capabilities before adding dependencies.
- Use modern Java features when they improve readability without obscuring behavior.
- Avoid clever code when straightforward code communicates intent better.

## Naming

- Use names that describe business intent rather than technical mechanics.
- Classes should represent clear responsibilities or domain concepts.
- Methods should describe actions or queries precisely.
- Avoid vague names such as `Manager`, `Helper`, `Utils`, `Processor`, or `Handler` when a more specific domain name exists.
- Avoid unexplained abbreviations.
- Keep naming consistent across API, service, persistence, messaging, and tests.

## Methods and Control Flow

- Prefer small methods with a single clear responsibility.
- Extract meaningful behavior when a method becomes difficult to understand as one unit.
- Avoid deeply nested conditionals.
- Prefer guard clauses and early returns when they improve readability.
- Avoid long parameter lists; introduce a meaningful parameter object/domain type when useful.
- Do not split simple logic into tiny methods solely to satisfy arbitrary size targets.
- Optimize for cognitive simplicity.

## Comments and JavaDoc

Code should communicate its normal control flow primarily through cohesive
responsibilities, explicit domain names, meaningful intermediate values, and
small methods. Comments are required when important intent, constraints, or
consequences cannot be understood safely from the code alone. JavaDoc is also
a mandatory navigation aid for methods with light or greater cognitive
complexity and for data carried by application-owned objects.

### Mandatory JavaDoc baseline

- Add JavaDoc to every method that performs at least one decision, branch,
  iteration, transformation, normalization, validation, orchestration, state
  change, persistence operation, external call, failure translation, or other
  non-trivial behavior. This is the project's threshold for **light or greater
  complexity**; it does not depend on a particular static-analysis score.
- Method JavaDoc must state both **what the method does** and **why the method
  exists** in the surrounding application flow. Add `@param`, `@return`, and
  `@throws` only where they communicate useful contract information rather than
  repeating names and types.
- Trivial accessors, delegating constructors, framework-required no-op methods,
  and self-evident constant getters do not require JavaDoc unless they expose a
  non-obvious contract.
- Document properties of application-owned records, DTOs, entities,
  configuration objects, events, and results. Keep property
  documentation deliberately short: a phrase describing the value, unit, or
  role is sufficient. Prefer record-level `@param` tags for record components
  and field JavaDoc for entity/configuration fields.
- Every automated test method must have concise JavaDoc explaining the
  behavior, scenario, invariant, or failure it proves and why that proof is
  relevant. Do not restate the test implementation line by line.
- Test fixtures and helper methods require JavaDoc only when they encode a
  non-obvious fixture convention or reusable test contract.

### What to document

- Explain **why** a non-obvious implementation choice or business rule exists,
  rather than narrating the syntax.
- Reference the governing BDR, ADR, product rule, external contract, or
  operational constraint when that traceability helps prevent an unsafe future
  change.
- Document invariants, preconditions, postconditions, units, boundary cases,
  failure semantics, and side effects when they are not evident from types and
  names.
- Explain non-obvious algorithms, ranking, normalization, deduplication,
  correlation, matching, scheduling, retry, idempotency, concurrency, and
  transactional behavior.
- Document intentional limitations, safe-failure behavior, provider quirks,
  compatibility requirements, and workarounds. A workaround comment must state
  why it exists and, when known, what condition allows its removal.
- Use JavaDoc for public or architectural boundaries whose contract is not
  fully obvious from the signature, especially gateways, integration ports,
  reusable services, message contracts, extension points, and APIs consumed by
  another module or service. This complements, rather than replaces, the
  mandatory complexity baseline above.
- Keep comments close to the smallest code element they explain.

### What to avoid

- Do not add comments that merely translate the next line of code into prose.
- Do not add JavaDoc mechanically to trivial accessors or boilerplate outside
  the mandatory method, property, and test baseline above.
- Do not use comments to compensate for vague names, large methods, excessive
  nesting, hidden side effects, or mixed responsibilities. Refactor the code
  first, then document only the remaining non-obvious intent.
- Do not preserve commented-out code, speculative TODOs, historical narration,
  or generated boilerplate comments in production sources.
- Do not duplicate durable documentation from BDRs, ADRs, product documents, or
  integration contracts. Reference the source and explain only its local
  implementation consequence.
- Do not include secrets, credentials, personal data, sensitive payloads, or
  misleading examples in comments or JavaDoc.

### Maintenance requirement

Comments and JavaDoc are part of the implementation contract. Update or remove
them in the same change when behavior changes. A stale, contradictory, or
unverifiable comment is a defect, not harmless documentation debt.

During review, use the following order:

1. Verify that naming, structure, and responsibility make the normal flow
   understandable without commentary.
2. Refactor unnecessary cognitive complexity.
3. Verify that remaining non-obvious intent, constraints, contracts, and side
   effects are documented.
4. Verify that every existing comment remains accurate and useful.

Examples:

```java
// BDR-0004 caps a single user's active reservations to preserve fair access.
.limit(MAX_ACTIVE_RESERVATIONS_PER_USER)
```

Avoid:

```java
// Limit the result to five.
.limit(5)
```

## Classes and Files

- Prefer one top-level production class, interface, enum, or record per file.
- A source file must not exceed **1,000 lines**.
- Approaching the limit should trigger a responsibility review rather than code compression.
- The 1,000-line limit is not permission to create oversized classes; prefer substantially smaller cohesive classes.

## Line Length and Formatting

- Maximum line length: **200 characters**.
- Keep a method or constructor declaration's complete parameter list on the
  same line when the resulting line is at most **120 characters**. Break the
  parameter list only when that line would exceed 120 characters.
- Apply the same 120-character rule to method and constructor invocations:
  keep arguments on the same line while the complete invocation fits, and wrap
  only when it would exceed the threshold. Syntax that is inherently
  multiline, such as text blocks or multiline lambdas, is exempt.
- When wrapping is necessary, group parameters or arguments into the fewest
  readable continuation lines that stay within 120 characters; one item per
  line is not required.
- For parameter and argument lists, this 120-character rule takes precedence
  over the general preference for shorter lines.
- Prefer shorter lines when they improve readability.
- Follow configured formatter and static-analysis rules.
- Do not manually format code in conflict with automated formatting.
- Avoid unrelated formatting churn in feature diffs.

## Error Handling

- Place application-specific exception types under the root `exception`
  package rather than in `service`, `api`, or model packages.
- Fail explicitly when an operation cannot satisfy its contract.
- Do not silently swallow exceptions.
- Do not catch broad exceptions without a concrete recovery, translation, logging, or boundary-handling reason.
- Translate infrastructure/provider exceptions at appropriate boundaries.
- Preserve diagnostic context without exposing secrets or sensitive information.
- Do not use exceptions for ordinary control flow.

## Logging

- Log operationally useful information.
- Prefer structured/contextual logging where supported.
- Include correlation identifiers for asynchronous/distributed processing when available.
- Never log passwords, credentials, access tokens, secrets, or unnecessary sensitive payloads.
- Avoid noisy logging without operational value.

## Spring

- Controllers adapt transport to application use cases; they do not own business rules.
- Persistence details must not leak unnecessarily into domain/application APIs.
- Configuration must be externalized.
- Use constructor injection.
- Prefer explicit dependencies.
- Avoid unnecessary Spring coupling in pure business/domain logic.
- Use framework capabilities where they reduce boilerplate without hiding important behavior.

## Persistence and Database Changes

- Database schema changes must be performed through Flyway migrations.
- Do not rely on automatic schema mutation as the production schema-management strategy.
- Keep JPA mappings consistent with `docs/database/`.
- A feature must not silently redefine the logical data model.
- Significant persistence strategy or ownership changes require the appropriate ADR.

## Dependencies

- Reuse Java, Spring, and existing platform capabilities before introducing libraries.
- Every new dependency must solve a concrete problem.
- Avoid overlapping libraries that solve the same problem.
- Major framework, database, broker, serialization, workflow, or infrastructure choices require an ADR.
- Do not introduce dependencies merely to save a few lines of straightforward code.

## Complexity

- Minimize accidental complexity.
- Avoid premature optimization.
- Avoid generic frameworks for hypothetical future requirements.
- Prefer explicit code over hidden conventions when conventions obscure behavior.
- Refactor when responsibilities become unclear, not merely to maximize abstraction.
- If a pattern makes code harder to understand without solving a concrete problem, do not apply it.

## Scope Discipline

- Keep feature changes focused on requested behavior.
- Avoid unrelated refactors unless required for correctness or safe implementation.
- Do not silently change public contracts, business behavior, architectural boundaries, or data models.
- Surface durable architectural or business decisions through the appropriate ADR or BDR process.

# ADR-0032 — Publish the REST Contract with OpenAPI and Swagger UI

- Status: Accepted
- Date: 2026-09-01
- Deciders: User and Engineering Team
- Supersedes: none
- Superseded by: none
- Related: ADR-0006, ADR-0011, ADR-0012, ADR-0027

## Context

The public REST contract is currently described through controller code, tests,
and duplicated examples in the repository README. That makes endpoint discovery
and interactive experimentation harder, and duplicated examples can drift from
the executable contract. Consumers also need the principal success, transport
validation, business conflict, not-found, and temporary-failure scenarios in
one navigable place.

The service uses Spring Web MVC and Spring Boot 3.5. Springdoc can derive an
OpenAPI 3 description from that application model and serve Swagger UI without
introducing a separate hand-maintained specification.

## Decision

Generate the public OpenAPI 3 contract with the Springdoc Web MVC Swagger UI
starter compatible with Spring Boot 3.5. Publish only routes under
`/api/v1/**` in the generated contract.

Expose the interactive documentation at `/swagger-ui.html` and the machine-
readable description at `/v3/api-docs`. These documentation routes are
unauthenticated in the current simplified scope, consistently with the public
API governed by ADR-0027. Operational routes remain protected. This exposure
must be reconsidered together with API authentication before the service is
placed on an untrusted network.

Every public operation must document its purpose, parameters or request body,
success responses, and principal client-visible failures. Request and response
examples must cover the main successful flows and validation categories,
including malformed or missing transport input, invalid query filters,
business conflicts, absent resources, and temporary persistence failure where
the endpoint exposes that outcome. Examples must use fictional values and must
not contain credentials, secrets, or real customer data.

Define one OpenAPI documentation interface per public API resource under
`api.documentation`. Endpoint-specific OpenAPI metadata, including tags,
operation descriptions, parameters, responses, schemas, and examples, belongs
in these interfaces, which are implemented by the corresponding Spring MVC
controllers. Keep runtime mappings, request binding, validation, response
status, metrics, and orchestration on the concrete controllers. These
interfaces represent the documented HTTP contract and must not contain runtime
or business behavior.

Swagger UI and the generated OpenAPI description are the canonical interactive
API and example reference. The README keeps a concise endpoint inventory and
links to those resources instead of duplicating HTTP request and response
payloads. Automated tests must verify that every public route and its principal
examples remain present in the generated specification and that documentation
routes are reachable under the intended security policy.

## Consequences

### Positive

- Developers and API consumers can discover and exercise all public operations from one interface.
- Examples stay next to the executable endpoint contracts and are less likely to drift.
- OpenAPI JSON enables client generation and automated contract inspection.
- README maintenance no longer requires duplicating request and response payloads.

### Negative or trade-offs

- Documentation interfaces and API-model annotations add maintenance work.
- Each public controller and its documentation interface must evolve together.
- Springdoc and Swagger UI increase the application dependency and runtime footprint.
- Public documentation exposes the API shape, so its access policy must change when authentication is introduced.
- Generated documentation still requires tests because inference alone cannot express every business failure accurately.

## Alternatives considered

- Keep examples only in the README — rejected because they are duplicated,
  non-interactive, and disconnected from generated endpoint metadata.
- Maintain a handwritten OpenAPI file — rejected because it would duplicate
  Spring MVC mappings and validation constraints and create another drift-prone source.
- Generate OpenAPI without Swagger UI — rejected because it would not provide
  the requested interactive, developer-friendly execution surface.
- Document only successful requests — rejected because consumers need the
  principal validation and failure contracts to integrate safely.

## Validation

Unit tests validate explicit OpenAPI metadata. MVC contract tests request the
generated specification without authentication and assert all `/api/v1/**`
operations, success examples, validation examples, and documented failure
responses. Security tests verify the Swagger UI and OpenAPI routes are public
while operational endpoints remain protected. The normal Maven quality gates
must remain green. Compilation must also verify that each controller implements
its documentation interface, keeping their method signatures aligned.

# ADR-0027 — Defer API Authentication for the Initial Scope

- Status: Accepted
- Date: 2026-08-31
- Deciders: User and Engineering Team
- Supersedes: none
- Superseded by: none
- Related: ADR-0007

## Context

ADR-0007 selects Spring Security as the long-term authentication and
authorization framework, but the simplified account-creation scope does not
yet define identities, token issuance, validation, roles, or permissions.
Inventing those contracts inside EPIC001 would materially expand the feature.

## Decision

Public application endpoints under `/api/v1/**` will not require
authentication in the current simplified scope. They will be explicitly
permitted and excluded from CSRF enforcement because this stateless API does
not use browser-session credentials. The Spring Security configuration must
include a traceable TODO stating that these endpoints require bearer-token
authentication through the `Authorization` header when the authentication
feature is designed.

The temporary API exception does not remove Spring Security, define a token
format, create an identity model, or relax operational endpoints outside
`/api/v1/**`. Authentication and authorization must be introduced through a
separately approved epic and governing decisions.

## Consequences

### Positive

- Keeps EPIC001 focused on account creation.
- Avoids inventing incomplete identity and token contracts.
- Makes the temporary security limitation visible in code and documentation.

### Negative or trade-offs

- Application API endpoints are accessible without caller authentication.
- The service is unsuitable for exposure to an untrusted network until the
  authentication feature is delivered.
- Future authentication work must replace the explicit temporary exception.

## Alternatives considered

- Implement bearer-token authentication in EPIC001 — rejected because token
  issuance, validation, identities, roles, and permissions are not defined.
- Leave Spring Security defaults active — rejected because generated local
  credentials would create undocumented API behavior.
- Remove Spring Security — rejected because ADR-0007 remains the intended
  framework and removing it would obscure the deferred requirement.

## Validation

API tests must prove that `POST /api/v1/accounts` works without an
`Authorization` header. Review must verify that the temporary security
configuration, CSRF exclusion, and TODO are narrowly scoped to `/api/v1/**`,
while operational endpoints remain outside that exception.

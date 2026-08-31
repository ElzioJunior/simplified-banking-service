# ADR-0026 — Use Server-Issued Idempotency Tokens for Transfers

- Status: Accepted
- Date: 2026-08-31
- Deciders: Engineering Team
- Supersedes: none
- Superseded by: none

## Context

A real banking backend must prevent an accidental retry of the same transfer request from creating duplicate financial effects. Deriving duplicates from amount, accounts, or time windows is unreliable because two legitimate transfers can have identical business data.

## Decision

Introduce a server-issued idempotency token for transfer creation.

The flow is:

1. The client requests a transfer idempotency token through
   `POST /api/v1/transfer-tokens`.
2. The server generates a UUID token and returns it with its expiration instant.
3. The token is valid for 10 minutes.
4. The client submits the token in the `Idempotency-Key` header of
   `POST /api/v1/transfers`.
5. The first valid use of the token is associated with the resulting transfer operation.
6. Reusing the same token for the same completed operation must return the previously established result and must not create duplicate financial effects.
7. An expired token must not authorize a new transfer.
8. A token must not be reused to execute a different transfer payload.

Missing tokens are invalid request input. Unknown, expired, or payload-mismatched
tokens are idempotency conflicts. Token issuance and expiration timestamps use
the application clock so behavior remains deterministic in tests.

The exact storage implementation is an internal technical detail, but token uniqueness, expiration, and one-logical-operation semantics are required.

## Consequences

### Positive

- Prevents duplicate financial effects caused by client retries.
- Does not require clients to generate their own idempotency identifiers.
- Avoids unreliable duplicate detection based on account, amount, or time-window heuristics.
- Provides explicit lifecycle and expiration semantics.

### Negative or trade-offs

- Requires an additional API call before transfer creation.
- Requires temporary server-side idempotency state.
- Clients must manage the issued token until transfer submission.
- Expired tokens require requesting a new token.

## Alternatives considered

- Client-generated idempotency key — valid and common, but not selected because the project will centralize token generation on the server.
- Detect identical transfers within a time window — rejected because legitimate repeated transfers may have the same source, destination, and amount.
- Server-generated transfer ID only after submission — rejected because it does not protect against duplicate submission of the initial request.
- No idempotency — rejected because accidental retries could duplicate financial effects.

## Validation

Integration tests must verify token generation, 10-minute expiration behavior, first-use transfer execution, duplicate submission with the same token, payload mismatch using an already-associated token, expired token rejection, and preservation of a single set of financial movements.

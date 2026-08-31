# BDR-0001 — Account Management Scope and Account Creation Rules

- Status: Proposed
- Date: 2026-08-31
- Deciders: Engineering and Product Team
- Supersedes: none
- Superseded by: none

## Context

The digital banking exercise requires accounts to exist before financial movements and transfers can be performed. The account capability should remain intentionally small so the implementation focuses on the core financial scenarios rather than a complete account-management product.

## Decision

For the current scope, account management will support **account creation only**.

The account creation API must receive:

- Customer name.
- Initial balance.

A successful creation must return:

- Generated account ID.
- Customer name.
- Initial balance.

The account ID may be generated as an auto-incrementing integer.

The initial balance must be greater than or equal to zero. Creating an account with a negative initial balance is not allowed.

The following account operations are explicitly outside the current scope:

- Update account.
- Delete account.
- Get account by ID.
- List accounts.

These operations may be introduced later only if the product scope requires them.

## Consequences

### Positive

- Keeps the exercise focused on the financial capabilities that depend on accounts.
- Provides the minimum data required to execute transfer and movement scenarios.
- Prevents accounts from starting in an invalid negative-balance state.
- Reduces unnecessary CRUD implementation.

### Negative or trade-offs

- Account information cannot be updated through the API.
- Accounts cannot be explicitly queried or deleted through account-management endpoints.
- The simplified lifecycle is not representative of a complete banking product.

## Alternatives considered

- Implement full CRUD — not selected because update, read, list, and delete operations do not contribute to the core goals of the exercise.
- Allow negative initial balances — not selected because accounts must not start in an overdrawn state.
- Use externally supplied account IDs — not selected because generated IDs simplify account creation for the exercise.

## Validation

Automated API tests must verify successful account creation, generated identifiers, response data, zero and positive initial balances, rejection of negative initial balances, and the absence of unsupported account-management operations.

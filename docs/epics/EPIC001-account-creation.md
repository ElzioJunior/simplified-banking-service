# EPIC001 — Account Creation

## Objective

Implement a REST API that provides the minimum account management capability required by the digital banking application.

The scope of this Epic is intentionally limited to **account creation**. Account retrieval, update, deletion, and listing are outside the current scope.

The API must allow an account to be created with:

- Customer name.
- Initial balance.

The system must:

- Generate a unique account identifier.
- Accept an initial balance greater than or equal to zero.
- Reject negative initial balances.
- Persist the account creation timestamp.
- Return the created account information.

## Delivery status

Completed on 2026-08-31. The delivered scope is limited to the documented
creation endpoint and uses the existing Flyway V1 schema; no database migration
or additional account-management operation was introduced.

---

## API

### Create Account

`POST /api/v1/accounts`

### Request

```json
{
  "name": "John Doe",
  "initialBalance": 1000.00
}
```

### Successful Response

`201 Created`

```json
{
  "id": 1,
  "name": "John Doe",
  "balance": 1000.00,
  "createdAt": "2026-08-31T14:00:00Z"
}
```

### Temporary authentication scope

`POST /api/v1/accounts` does not require an `Authorization` header in the
current simplified scope. Bearer-token authentication remains required future
work and is explicitly deferred by
[ADR-0027](../adr/ADR-0027-defer-api-authentication-for-the-initial-scope.md).

---

# Epic Acceptance Criteria

- An account can be created with a valid customer name and initial balance.
- The system must generate a unique account ID.
- The generated ID must be returned to the client.
- The initial balance must be greater than or equal to zero.
- An initial balance of zero must be accepted.
- A negative initial balance must be rejected.
- The customer name must be required.
- The account creation date/time must be persisted.
- A successfully created account must be available for subsequent financial operations.
- Invalid requests must not persist an account.
- Monetary values must preserve their decimal precision.
- Initial balances are normalized to two decimal places with `HALF_EVEN`
  rounding as required by
  [ADR-0022](../adr/ADR-0022-use-bigdecimal-with-half-even-rounding-for-monetary-values.md).
- Account creation must follow the standardized API error contract for invalid requests.
- Account creation succeeds without an authentication token in the current scope.
- Account retrieval, listing, update, and deletion are outside the scope of this Epic.

---

# User Stories

## US-001 — Create an Account With a Positive Initial Balance

**As a** banking client  
**I want to** create an account with an initial balance  
**So that** the account can participate in financial operations.

### Scenario

**Given:**

- The customer name is `John Doe`.
- The requested initial balance is `$1,000.00`.

**When:**

- An account creation request is submitted.

**Then:**

- A new account must be created.
- A unique account ID must be generated.
- The account name must be `John Doe`.
- The account balance must be `$1,000.00`.
- The account creation timestamp must be recorded.
- The created account must be returned to the client.

---

## US-002 — Create an Account With Zero Initial Balance

**As a** banking client  
**I want to** create an account without depositing money initially  
**So that** the account can receive funds later.

### Scenario

**Given:**

- The customer name is `John Doe`.
- The requested initial balance is `$0.00`.

**When:**

- An account creation request is submitted.

**Then:**

- The account must be created successfully.
- A unique account ID must be generated.
- The account balance must be exactly `$0.00`.
- The account creation timestamp must be recorded.

---

## US-003 — Reject an Account With a Negative Initial Balance

**As a** banking platform  
**I want to** reject accounts with negative initial balances  
**So that** accounts cannot start in an invalid financial state.

### Scenario

**Given:**

- The customer name is `John Doe`.
- The requested initial balance is `-$100.00`.

**When:**

- An account creation request is submitted.

**Then:**

- The request must be rejected.
- No account must be created.
- No account ID must be generated for a persisted account.
- The API must return an appropriate validation error.

---

## US-004 — Reject Account Creation Without a Customer Name

**As a** banking platform  
**I want to** require a customer name  
**So that** every account has an identified account holder.

### Scenario

**Given:**

- No customer name is provided.
- The requested initial balance is `$100.00`.

**When:**

- An account creation request is submitted.

**Then:**

- The request must be rejected.
- No account must be persisted.
- The API must return an appropriate validation error.

---

## US-005 — Reject Account Creation With a Blank Customer Name

**As a** banking platform  
**I want to** reject blank customer names  
**So that** accounts cannot be created with meaningless account-holder information.

### Scenario

**Given:**

- The customer name is blank.
- The requested initial balance is `$100.00`.

**When:**

- An account creation request is submitted.

**Then:**

- The request must be rejected.
- No account must be persisted.
- The API must return an appropriate validation error.

---

## US-006 — Generate Unique Account Identifiers

**As a** banking platform  
**I want** every account to have a unique identifier  
**So that** accounts can be referenced unambiguously by financial operations.

### Scenario

**When:**

- Multiple valid accounts are created.

**Then:**

- Every persisted account must receive a unique ID.
- No two accounts may share the same ID.

---

## US-007 — Preserve Monetary Precision

**As a** banking client  
**I want** the initial balance to retain its monetary precision  
**So that** the account is created with the exact requested amount.

### Scenario

**Given:**

- The requested initial balance is `$123.45`.

**When:**

- The account is created.

**Then:**

- The persisted balance must be exactly `$123.45`.
- The API response must represent the same monetary value.
- No floating-point precision loss may occur.

---

## US-008 — Record Account Creation Time

**As a** banking platform  
**I want** the account creation time to be recorded  
**So that** the account lifecycle can be traced.

### Scenario

**When:**

- A valid account is successfully created.

**Then:**

- A creation timestamp must be persisted.
- The timestamp must represent the account creation event.

---

## US-009 — Do Not Persist Invalid Account Requests

**As a** banking platform  
**I want** invalid account creation requests to leave no persisted data  
**So that** rejected operations cannot create partial or invalid accounts.

### Scenario

**Given:**

- An account creation request violates a business or validation rule.

**When:**

- The request is processed.

**Then:**

- No account must be persisted.
- No partial account data may remain in the database.

---

## US-010 — Create Multiple Independent Accounts

**As a** banking client  
**I want to** create multiple accounts  
**So that** they can later participate in transfers.

### Scenario

**When:**

- Account A is created with `$1,000.00`.
- Account B is created with `$500.00`.

**Then:**

- Both accounts must be persisted independently.
- Account A must retain `$1,000.00`.
- Account B must retain `$500.00`.
- Each account must have a different unique ID.

---

# Out of Scope

The following account-management operations are explicitly outside this Epic:

- `GET /accounts/{id}`
- `GET /accounts`
- `PUT /accounts/{id}`
- `PATCH /accounts/{id}`
- `DELETE /accounts/{id}`

The initial account capability provides only:

`POST /api/v1/accounts`

Additional account lifecycle operations require future business scope.

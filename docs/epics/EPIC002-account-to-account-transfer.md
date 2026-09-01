# EPIC002 — Account-to-Account Money Transfers

## Objective

Implement a REST API for transferring funds between bank accounts while ensuring:

- Financial consistency
- Transaction atomicity
- Balance integrity
- Resilience in failure scenarios
- Safe behavior under high concurrency
- Adequate performance under heavy load
- Protection against race conditions
- Full rollback in case of failure
- Retry safety through server-issued idempotency tokens
- Best-effort RabbitMQ notification event for the source account holder

A transfer must be handled as a single transactional operation:

1. Identify the source account.
2. Identify the destination account.
3. Ensure consistent access to the accounts involved.
4. Validate the available balance of the source account.
5. Debit the source account.
6. Credit the destination account.
7. Record the financial movements.
8. Commit the transaction.

If any step fails, no financial change must remain persisted.

---

## API

All endpoints remain temporarily unauthenticated under
[ADR-0027](../adr/ADR-0027-defer-api-authentication-for-the-initial-scope.md).
The traceable authentication TODO remains required until bearer-token behavior
is delivered by a separately approved scope.

### Issue Transfer Token

`POST /api/v1/transfer-tokens`

Successful status: `201 Created`.

The endpoint accepts no business payload and returns:

```json
{
  "token": "4bc9a5ab-6bb8-4c45-b8ca-b15cae27e722",
  "expiresAt": "2026-08-31T19:10:00Z"
}
```

The token is valid for 10 minutes and may authorize only one normalized
transfer payload.

### Transfer Funds

`POST /api/v1/transfers`

Successful first-use and identical-replay status: `200 OK`.

Required header:

```http
Idempotency-Key: 4bc9a5ab-6bb8-4c45-b8ca-b15cae27e722
```

### Request

```json
{
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": 100.00
}
```

### Successful Response

```json
{
  "transferId": "d068799f-c8ab-4be2-9b49-92f7d8c33f44",
  "status": "COMPLETED",
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": 100.00
}
```

Reusing the same token with the same normalized payload returns the established
successful response without another debit, credit, movement pair, or
publication request. Reusing it for another payload is rejected.

### Error contract

Errors use safe RFC 9457 Problem Details:

- `400 Bad Request` — malformed input, missing `Idempotency-Key`, or invalid amount.
- `404 Not Found` — source or destination account does not exist.
- `409 Conflict` — same account, insufficient funds, or token conflict/expiration.
- `503 Service Unavailable` — bounded lock timeout, deadlock victim, or database unavailability.

---

# Epic Acceptance Criteria

- A successful transfer must debit the source account and credit the destination account.
- Debit and credit operations must occur atomically.
- A transfer must never create or destroy money.
- An account balance must never become negative.
- Balance validation must take concurrent operations into account.
- Concurrent operations against the same account must not cause lost updates.
- Any failure during the operation must trigger a full rollback.
- Invalid transfers must not modify any account balance.
- Data consistency must be maintained under high-concurrency tests.
- Lock contention, timeouts, and deadlocks must be handled in a controlled manner.
- Load and concurrency tests must be executed using Gatling.
- Throughput, latency, and error metrics must be available to evaluate the API under load.
- A server-issued idempotency token must be required for every transfer.
- An identical retry with the same token must not duplicate financial effects.
- A token must not authorize another payload and expires after 10 minutes.
- A newly completed transfer must request one best-effort notification event
  publication for the source account holder.
- RabbitMQ publication failure must not roll back or invalidate a successful transfer.
- Replayed and rejected transfers must not request another notification publication.

---

# User Stories

## US-001 — Successful Transfer

**As an** account holder with sufficient funds  
**I want to** transfer money to another account  
**So that** I can move funds between accounts.

### Scenario

**Given:**

- Account A has a balance of $1,000.
- Account B has a balance of $500.

**When:**

- Account A transfers $200 to Account B.

**Then:**

- Account A must have a balance of $800.
- Account B must have a balance of $700.
- The transfer status must be `COMPLETED`.
- A $200 debit must be recorded for Account A.
- A $200 credit must be recorded for Account B.

---

## US-002 — Transfer the Entire Available Balance

**As an** account holder  
**I want to** transfer my entire available balance  
**So that** I can move all available funds to another account.

### Scenario

**Given:**

- Account A has a balance of $1,000.
- Account B has a balance of $500.

**When:**

- Account A transfers $1,000 to Account B.

**Then:**

- Account A must have a balance of $0.
- Account B must have a balance of $1,500.
- The transfer must be completed successfully.

---

## US-003 — Transfer Rejected Due to Insufficient Funds

**As an** account holder  
**I want** transfers exceeding my available balance to be rejected  
**So that** my account cannot enter an invalid negative balance.

### Scenario

**Given:**

- Account A has a balance of $100.
- Account B has a balance of $500.

**When:**

- Account A attempts to transfer $200 to Account B.

**Then:**

- The transfer must be rejected.
- Account A must remain at $100.
- Account B must remain at $500.
- No debit or credit must be persisted.

---

## US-004 — Source Account Does Not Exist

**As a** client of the transfer API  
**I want** transfers from nonexistent accounts to be rejected  
**So that** invalid financial operations cannot be created.

### Scenario

**When:**

- A transfer is requested using a nonexistent source account.

**Then:**

- The transfer must be rejected.
- No financial changes must occur in the destination account.

---

## US-005 — Destination Account Does Not Exist

**As an** account holder  
**I want** transfers to nonexistent accounts to be rejected  
**So that** money cannot be sent to an invalid destination.

### Scenario

**Given:**

- Account A has a balance of $1,000.

**When:**

- Account A attempts to transfer $100 to a nonexistent account.

**Then:**

- The transfer must be rejected.
- Account A must remain at $1,000.
- No debit must be performed.

---

## US-006 — Zero Transfer Amount

**As a** client of the transfer API  
**I want** zero-value transfers to be rejected  
**So that** meaningless financial transactions are not created.

### Scenario

**When:**

- A transfer with an amount of $0 is requested.

**Then:**

- The transfer must be rejected.
- No account balance must be modified.

---

## US-007 — Negative Transfer Amount

**As a** client of the transfer API  
**I want** negative transfer amounts to be rejected  
**So that** invalid operations cannot manipulate account balances.

### Scenario

**When:**

- A transfer with a negative amount is requested.

**Then:**

- The transfer must be rejected.
- No account balance must be modified.

---

## US-008 — Transfer to the Same Account

**As an** account holder  
**I want** transfers to the same account to be rejected  
**So that** unnecessary or ambiguous financial movements are not created.

### Scenario

**Given:**

- Account A has a balance of $1,000.

**When:**

- Account A attempts to transfer $100 to Account A.

**Then:**

- The operation must be rejected.
- Account A must remain at $1,000.

---

# Concurrency User Stories

## US-009 — Two Concurrent Transfers With Sufficient Funds

**As an** account holder  
**I want** concurrent valid transfers to be processed correctly  
**So that** simultaneous operations do not corrupt my balance.

### Scenario

**Given:**

- Account A has a balance of $1,000.
- Account B has a balance of $0.
- Account C has a balance of $0.

**When simultaneously:**

- Account A transfers $200 to Account B.
- Account A transfers $300 to Account C.

**Then:**

- Both transfers must be processed successfully.
- Account A must end with $500.
- Account B must end with $200.
- Account C must end with $300.
- No update must be lost.

---

## US-010 — Concurrent Transfers Competing for the Same Balance

**As an** account holder  
**I want** concurrent transfers to respect my actual available balance  
**So that** simultaneous operations cannot overdraw my account.

### Scenario

**Given:**

- Account A has a balance of $100.
- Account B has a balance of $0.
- Account C has a balance of $0.

**When simultaneously:**

- Account A attempts to transfer $80 to Account B.
- Account A attempts to transfer $80 to Account C.

**Then:**

- Only one transfer may succeed.
- The second transfer must evaluate the updated balance and be rejected.
- Account A must end with $20.
- The combined balance increase of Accounts B and C must be exactly $80.
- Account A must never have a negative balance.

---

## US-011 — Transfer Waiting for a Concurrent Operation

**As an** account holder  
**I want** transfers involving an account already being modified to use its latest valid state  
**So that** concurrent transactions cannot operate on stale balances.

### Scenario

**Given:**

- A transfer involving Account A is already in progress.

**When:**

- A second transfer requiring modification of Account A is initiated.

**Then:**

- The second operation must not operate on a stale or inconsistent balance.
- It must wait or follow the defined contention policy.
- Once access is obtained, it must use the latest committed balance.
- Its result must reflect the previously completed operation.

---

## US-012 — High Concurrency Against the Same Account

**As an** account holder  
**I want** my balance to remain consistent under extreme concurrent access  
**So that** high transaction volume cannot corrupt financial data.

### Scenario

**Given:**

- Account A has a balance of $10,000.

**When:**

- 100 concurrent transfers of $100 are requested from Account A.

**Then:**

- Account A must end with exactly $0.
- Exactly $10,000 must be credited to the destination accounts.
- No money may be created or lost.
- No lost updates may occur.

---

## US-013 — Concurrent Debits and Credits Against the Same Account

**As an** account holder  
**I want** simultaneous incoming and outgoing transfers to be processed consistently  
**So that** my final balance reflects every committed transaction.

### Scenario

**Given:**

- Account A is receiving transfers while simultaneously sending transfers.

**When:**

- Concurrent debit and credit operations are executed.

**Then:**

- Every committed operation must be reflected in the final balance.
- No valid update may overwrite another valid update.
- The final balance must equal:

`initial balance + committed credits - committed debits`

---

## US-014 — Cross Transfers

**As an** account holder  
**I want** simultaneous transfers between the same accounts to complete safely  
**So that** cross-account operations cannot cause inconsistent balances.

### Scenario

**Given:**

- Account A has a balance of $1,000.
- Account B has a balance of $1,000.

**When simultaneously:**

- Account A transfers $100 to Account B.
- Account B transfers $200 to Account A.

**Then:**

- Both operations must be processed without data inconsistency.
- Account A must end with $1,100.
- Account B must end with $900.
- The system must prevent or safely recover from deadlocks.

---

# Atomicity and Failure User Stories

## US-015 — Failure After Debit but Before Credit

**As an** account holder  
**I want** incomplete transfers to be fully rolled back  
**So that** money cannot disappear because of a processing failure.

### Scenario

**Given:**

- Account A has a balance of $1,000.
- Account B has a balance of $500.

**When:**

- A $200 transfer is initiated.
- Account A is debited.
- Processing fails before Account B is credited.

**Then:**

- The entire transaction must be rolled back.
- Account A must remain at $1,000.
- Account B must remain at $500.
- No partially completed transfer may exist.

---

## US-016 — Failure While Recording the Financial Movement

**As an** account holder  
**I want** transfer failures to preserve the previous account state  
**So that** balances and transaction records cannot become inconsistent.

### Scenario

**When:**

- The debit and credit are being processed.
- An error occurs while recording the financial movement.

**Then:**

- The entire operation must be rolled back.
- All balances must remain in their pre-transfer state.

---

## US-017 — Unexpected Database Failure

**As a** client of the transfer API  
**I want** database failures to be handled safely  
**So that** infrastructure failures cannot corrupt financial data.

### Scenario

**When:**

- A database failure occurs during a transfer.

**Then:**

- No partially persisted transfer may remain.
- The API must return a controlled error response.
- Financial integrity must be preserved.

---

## US-018 — Timeout Due to Resource Contention

**As a** client of the transfer API  
**I want** excessive transaction waiting times to be handled safely  
**So that** blocked operations do not remain indefinitely pending.

### Scenario

**When:**

- A transfer cannot proceed within the configured maximum waiting time because another transaction holds a required resource.

**Then:**

- The operation must fail in a controlled manner.
- No partial changes may remain.
- The client must receive an appropriate response.

---

## US-019 — Deadlock Between Transfers

**As a** client of the transfer API  
**I want** deadlock situations to be safely handled  
**So that** concurrent transfers cannot compromise availability or consistency.

### Scenario

**When:**

- Concurrent transfers create a potential deadlock situation.

**Then:**

- The system must prevent or recover from the deadlock.
- No partial changes may remain.
- Account consistency must be preserved.

---

# Financial Integrity User Stories

## US-020 — Money Conservation

**As a** banking platform  
**I want** the total amount of money to remain unchanged by internal transfers  
**So that** transfers cannot create or destroy funds.

### Scenario

**Given:**

- The initial combined balance of all involved accounts is `X`.

**When:**

- `N` transfers are executed between those accounts.

**Then:**

- The final combined balance must remain `X`.
- Internal transfers must never create or destroy money.

---

## US-021 — Monetary Precision

**As an** account holder  
**I want** monetary values to retain their exact precision  
**So that** transfers cannot introduce rounding errors.

### Scenario

**Given:**

- A transfer contains a valid monetary amount with decimal precision.

**When:**

- The transfer is processed.

**Then:**

- No monetary precision may be lost.
- The debit amount must exactly match the credit amount.

---

# Performance and Load Testing User Stories

## US-022 — Gatling Load Test

**As an** engineer  
**I want to** execute concurrent transfer requests using Gatling  
**So that** I can validate the API behavior under load.

### Acceptance Criteria

- Financial consistency must be maintained.
- Throughput must be measured.
- Latency must be measured.
- Error rates must be measured.
- Concurrency-related failures must be identifiable.

---

## US-023 — Transfers Distributed Across Multiple Accounts

**As an** engineer  
**I want** the system to process independent transfers concurrently  
**So that** the consistency strategy does not unnecessarily limit throughput.

### Scenario

**Given:**

- Multiple accounts exist with sufficient funds.

**When:**

- A high volume of transfers is distributed across different accounts.

**Then:**

- Independent operations should be processed concurrently.
- Independent accounts must not be unnecessarily serialized.
- The consistency strategy must not turn all transfers into a single sequential processing flow.

---

## US-024 — Hot Account Scenario

**As an** engineer  
**I want** to test extreme contention against a single account  
**So that** the system behavior under abnormal concurrency can be evaluated.

### Scenario

**Given:**

- One account receives an unusually high number of concurrent operations.

**When:**

- Gatling generates heavy contention against that account.

**Then:**

- Balance integrity must be maintained.
- No race condition may occur.
- Contention behavior must be measurable.
- The API must remain stable.

---

# Observability User Stories

## US-025 — Transfer Traceability

**As an** engineer  
**I want to** trace a transfer end-to-end  
**So that** failures and performance problems can be investigated.

### Acceptance Criteria

- Every transfer must have a unique identifier.
- Logs must allow requests to be correlated with transfers.
- Failures must contain sufficient diagnostic context.
- Sensitive information must not be exposed in logs.

---

## US-026 — Transfer API Metrics

**As an** engineer  
**I want to** monitor transfer API metrics  
**So that** I can evaluate its behavior under load.

### Acceptance Criteria

At minimum, the following must be observable:

- Total transfer requests
- Successful transfers
- Rejected transfers
- Failed transfers
- Request latency
- Throughput
- Database errors
- Timeouts
- Concurrency/lock contention

---

## Epic vs. ADR Responsibility

The Epic defines **what guarantees and behavior the system must provide**.

The ADRs define **how those guarantees will be technically implemented**.

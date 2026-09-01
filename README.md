# Simplified Banking Service

Simplified Banking Service is a Java/Spring Boot REST API for a deliberately
small digital-banking domain. Its documented scope covers account creation,
safe account-to-account transfers, and bounded account movement history, with
financial consistency and traceability as primary constraints.

The service currently implements account creation, server-issued transfer
idempotency tokens, atomic account-to-account transfers, paginated financial
movement queries, best-effort RabbitMQ notifications, operational metrics, and
automated functional and load-test suites.

## Implemented capabilities

- Create an account with a generated numeric ID, customer name, non-negative
  opening balance, and UTC creation timestamp.
- Normalize monetary values to scale two with `HALF_EVEN` rounding and persist
  them as PostgreSQL `NUMERIC(19,2)` values.
- Issue a server-generated UUID idempotency token that is valid for 10 minutes.
- Transfer a positive monetary amount between two different existing accounts.
- Debit and credit both accounts atomically, without creating or destroying
  money.
- Replay the established result when a completed transfer is retried with the
  same token and normalized payload, without duplicating financial effects.
- Prevent overdrafts, token reuse with another payload, lost updates, and
  partial financial effects under concurrent requests.
- Record each successful transfer as one debit and one credit movement sharing
  the same operation identifier.
- List one account's movements in fixed pages of 10 with optional date range
  and `CREDIT`/`DEBIT` filters.
- Lock transfer accounts in ascending ID order inside one `READ_COMMITTED`
  transaction with a configurable lock timeout.
- Request synchronous best-effort publication of a `TRANSFER_COMPLETED` event
  to RabbitMQ after each newly completed transfer commits.
- Expose operational metrics for throughput, latency, failures, timeouts, and
  lock contention.
- Validate database migrations, real HTTP/PostgreSQL/RabbitMQ behavior,
  concurrency, and sustained load through separate automated test suites.

## Current limitations

- Account retrieval, listing, update, and deletion endpoints are not provided.
- Overdrafts, fees, exchange rates, scheduled transfers, and corrective
  financial operations are outside the current scope.
- `/api/v1/**` is temporarily unauthenticated and excluded from CSRF checks.
  The service must not be exposed to an untrusted network until authentication
  and authorization are implemented.
- RabbitMQ delivery is best effort. There is no outbox, durable retry,
  publisher confirmation, exactly-once guarantee, or recovery after process or
  broker failure.
- No notification consumer or customer-facing delivery channel is included.

## REST API

Public endpoints are versioned under `/api/v1`.

### Create an account

```http
POST /api/v1/accounts
Content-Type: application/json

{
  "name": "John Doe",
  "initialBalance": 1000.00
}
```

Successful response: `201 Created`.

```json
{
  "id": 1,
  "name": "John Doe",
  "balance": 1000.00,
  "createdAt": "2026-08-31T18:45:00Z"
}
```

The name is required, nonblank, and limited to 255 characters. The initial
balance is required and must be non-negative.

### List account movements

```http
GET /api/v1/accounts/1/movements?page=0&start=2026-08-01T00:00:00Z&end=2026-09-01T00:00:00Z&type=CREDIT
```

Successful response: `200 OK`.

```json
{
  "content": [
    {
      "id": 42,
      "operationId": "f6608b62-b6ba-4da2-864d-b8d49c48fb85",
      "type": "CREDIT",
      "amount": 100.00,
      "createdAt": "2026-08-31T18:45:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

`page` defaults to zero. `start` is inclusive, `end` is exclusive, and
`type` accepts only `CREDIT` or `DEBIT`. Results are ordered by occurrence time
and movement ID descending. Invalid parameters return safe `400` Problem
Details, an unknown account returns `404`, and an existing account without
matches returns an empty `200` page.

### Issue a transfer token

Every transfer starts by obtaining a server-issued idempotency token:

```http
POST /api/v1/transfer-tokens
```

Successful response: `201 Created`.

```json
{
  "token": "4e80db4d-ce8c-40a6-b839-b45fd45b1461",
  "expiresAt": "2026-08-31T18:55:00Z"
}
```

The token is valid for 10 minutes and must be sent in the
`Idempotency-Key` header when creating a transfer.

### Transfer funds

```http
POST /api/v1/transfers
Content-Type: application/json
Idempotency-Key: 4e80db4d-ce8c-40a6-b839-b45fd45b1461

{
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": 100.00
}
```

Successful response: `200 OK`.

```json
{
  "transferId": "f6608b62-b6ba-4da2-864d-b8d49c48fb85",
  "status": "COMPLETED",
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": 100.00
}
```

The same token and normalized payload replay this response. Missing or malformed
headers and invalid amounts return `400`; missing accounts return `404`;
same-account transfers, insufficient funds, and invalid, expired, or
payload-mismatched tokens return `409`; bounded lock or database failures return
`503`. Expected failures use safe RFC 9457 Problem Details and do not expose
SQL, credentials, balances, or rejected payloads.

### Transfer notifications

Each newly completed transfer requests publication of this event shape:

```json
{
  "eventId": "fd846da6-67e2-4b0a-868d-551a9ce19f39",
  "operationId": "f6608b62-b6ba-4da2-864d-b8d49c48fb85",
  "recipientAccountId": 1,
  "eventType": "TRANSFER_COMPLETED",
  "amount": 100.00,
  "occurredAt": "2026-08-31T18:45:00Z"
}
```

RabbitMQ topology:

- Exchange: `banking.transfer.notifications`
- Queue: `banking.transfer.notifications.completed`
- Routing key: `transfer.completed`

Identical transfer replays do not request another publication. After the
PostgreSQL commit releases financial locks, publication runs synchronously with
finite connection, handshake, channel RPC, attempt-count, and total-duration
bounds. Exhausted attempts do not invalidate the committed transfer. The event
may still be lost if the process stops after commit or duplicated by RabbitMQ
or network behavior.

## Architecture

- Java 21 and Spring Boot 3.5.16.
- Maven with the Maven Wrapper.
- Spring Web MVC, Bean Validation, Spring Data JPA with Hibernate, MapStruct,
  and Spring Security.
- PostgreSQL 17.6 with immutable Flyway migrations. V1 creates accounts and
  movements, V2 adds transfer idempotency state and the historical outbox, and
  V3 removes the superseded outbox table.
- RabbitMQ 4.1.4 for direct transfer-completed event publication.
- Spring Boot Actuator and Micrometer for observability.
- Docker and Docker Compose for reproducible local environments.
- A layered monorepo with API, mapper, service, repository, entity, DTO, and
  configuration boundaries.
- `BigDecimal` and fixed-precision database types for monetary values.
- A single `READ COMMITTED` transaction with pessimistic account locking for
  each transfer.
- At least 90% unit-test coverage for meaningful application logic, plus
  isolated, integrated, concurrency, migration, and Gatling load tests.

Decision records have mixed statuses. Accepted records govern idempotency,
temporary unauthenticated API access, bounded contention failure, and direct
best-effort notifications; the superseded outbox records remain as history.

## Documentation

- [Documentation map](docs/README.md)
- [Core database schema epic](docs/epics/EPIC000-core-database-schema.md)
- [Development execution report](docs/epics/execution-report.md)
- [Account creation epic](docs/epics/EPIC001-account-creation.md)
- [Account-to-account transfer epic](docs/epics/EPIC002-account-to-account-transfer.md)
- [Functional test suite simplification epic](docs/epics/EPIC003-functional-test-suite-simplification.md)
- [Account movement listing epic](docs/epics/EPIC004-account-movement-listing.md)
- [Business decision records](docs/bdr/README.md)
- [Architecture decision records](docs/adr/README.md)
- [Logical data model](docs/database/logical-data-model.md)
- [Engineering standards](docs/engineering/README.md)
- [AI-assisted delivery workflows](.agents/workflows/README.md)

Repository-wide contribution instructions are defined in [AGENTS.md](AGENTS.md).

## Development

### Prerequisites

- Java 21 or later.
- Docker with Docker Compose for local PostgreSQL and RabbitMQ.

The Maven Wrapper downloads the repository's configured Maven version, so a
host Maven installation is not required.

### Build and test

```bash
./mvnw -B -ntp test
./mvnw -B -ntp verify
```

Unit tests belong under `src/test/unit/java`. Isolated functional tests belong
under `src/test/isolated/java` and join the normal `verify` lifecycle. The
default `verify` runs complete application scenarios against disposable
PostgreSQL Testcontainers and enforces at least 90% eligible line coverage.
Docker is therefore required for `verify`; `test` remains process-local and
Docker-free.

The integrated source set is opt-in and adds exactly one focused compatibility
scenario against a disposable RabbitMQ Testcontainer. It exercises the real
publisher, AMQP topology, routing, JSON conversion, and consumption without
starting PostgreSQL or executing a financial transfer:

```bash
./mvnw -B -ntp -Pintegrated-functional-tests verify
```

PostgreSQL-backed isolated tests prove that their datasource is their exact
test-owned container before execution. No functional test clears database
tables; containers are discarded whole. Docker must be available for both
`verify` commands.

### Run Gatling load tests

With the dedicated load-test environment configured through the
`TRANSFER_LOAD_*` variables in `.env.example`, run:

```bash
./mvnw -B -ntp -Pload-tests gatling:test
```

### Run locally

Start PostgreSQL and RabbitMQ:

```bash
docker compose up -d --wait postgres rabbitmq
```

Then start the application:

```bash
./mvnw spring-boot:run
```

Local infrastructure ports and credentials can be overridden through the
variables documented in `.env.example`. Docker Compose loads a copied `.env`
file automatically; application variables must be exported or passed to the
application process explicitly.

Stop local infrastructure with:

```bash
docker compose down
```

### Configuration

Application runtime settings:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP server port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/simplified_banking` | JDBC connection URL |
| `DATABASE_USERNAME` | `simplified_banking` | Application database user |
| `DATABASE_PASSWORD` | `simplified_banking` | Application database password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `RABBITMQ_USERNAME` | `simplified_banking` | RabbitMQ user |
| `RABBITMQ_PASSWORD` | `simplified_banking` | RabbitMQ password |
| `TRANSFER_LOCK_TIMEOUT_MS` | `5000` | Positive transaction-local PostgreSQL lock timeout |
| `TRANSFER_NOTIFICATION_MAX_ATTEMPTS` | `3` | Maximum RabbitMQ publication attempts after commit |
| `TRANSFER_NOTIFICATION_MAX_DURATION` | `3s` | Total monotonic publication retry budget |
| `TRANSFER_NOTIFICATION_CONNECTION_TIMEOUT` | `1s` | RabbitMQ TCP connection timeout |
| `TRANSFER_NOTIFICATION_HANDSHAKE_TIMEOUT` | `1s` | RabbitMQ AMQP handshake timeout |
| `TRANSFER_NOTIFICATION_CHANNEL_RPC_TIMEOUT` | `1s` | RabbitMQ channel RPC timeout |

Compose infrastructure additionally accepts `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`, `POSTGRES_PORT`, and `RABBITMQ_MANAGEMENT_PORT`. Gatling
uses the separate `TRANSFER_LOAD_*` variables documented in `.env.example` and
in the load-test guide above.

### Observability and security

Actuator exposes `health`, `info`, and `metrics`; operational routes remain
protected by Spring Security. The temporary unauthenticated exception applies
only to `/api/v1/**`.

API operations use bounded `operation` tags (`account.create`,
`transfer-token.issue`, and `transfer.create`) and publish these Micrometer
meters:

- `banking.api.requests.total`
- `banking.api.requests.successful`
- `banking.api.requests.rejected`
- `banking.api.requests.failed`
- `banking.api.database.errors`
- `banking.api.timeouts`
- `banking.api.lock.contention`
- `banking.api.request.latency`

### Build the application image

```bash
docker build -t simplified-banking-service .
```

The image runs as a non-root user and expects PostgreSQL and RabbitMQ connection
settings through the same environment variables used for local execution.

## Branching model

- `main` contains stable, validated releases.
- `develop` is the integration branch.
- Feature branches merge into `develop`; validated releases are promoted from
  `develop` to `main`.

## Development status

The implemented delivery includes:

- Flyway V1 accounts/movements, V2 transfer-idempotency history, and V3 outbox
  removal.
- Account creation with validation and monetary normalization.
- Read-only account movement history with fixed pagination and optional date
  and movement-type filters.
- Server-issued transfer tokens and idempotent, atomic, pessimistically locked
  transfers.
- Direct best-effort RabbitMQ transfer-completed events with bounded in-memory
  retry.
- Safe Problem Details, bounded-cardinality API metrics, Docker Compose local
  infrastructure, and a non-root application image.
- Unit tests, PostgreSQL-backed isolated functional tests, one focused RabbitMQ
  integration test, and Gatling load tests.

Authentication and authorization, account query/update/delete operations, and
notification consumers remain future work.

The canonical local quality command is:

```bash
./mvnw -B -ntp verify
```

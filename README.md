# Simplified Banking Service

Simplified Banking Service is a Java/Spring Boot REST API for a deliberately
small digital-banking domain. Its documented scope covers account creation and
safe account-to-account transfers, with financial consistency and traceability
as primary constraints.

The service currently implements account creation, server-issued transfer
idempotency tokens, atomic account-to-account transfers, financial movements,
best-effort RabbitMQ notifications, operational metrics, and automated
functional and load-test suites.

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
- Lock transfer accounts in ascending ID order inside one `READ_COMMITTED`
  transaction with a configurable lock timeout.
- Request direct best-effort publication of a `TRANSFER_COMPLETED` event to
  RabbitMQ for each newly completed transfer.
- Expose operational metrics for throughput, latency, failures, timeouts, and
  lock contention.
- Validate database migrations, real HTTP/PostgreSQL/RabbitMQ behavior,
  concurrency, and sustained load through separate automated test suites.

## Current limitations

- Account retrieval, listing, update, and deletion endpoints are not provided.
- Movement-query endpoints are not implemented; movements are currently an
  internal persistence and traceability mechanism.
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

Identical transfer replays do not request another publication. Publication may
be retried a bounded number of times in memory, but exhausted attempts do not
invalidate the financial transfer. RabbitMQ publication and the PostgreSQL
commit are not atomic, so an event may be lost, duplicated, or observed before
a later transaction failure.

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
- [Account movement listing epic](docs/epics/EPIC003-account-movement-listing.md)
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
default `verify` also enforces at least 90% eligible line coverage and does not
require external services.

The integrated source set is opt-in, uses disposable PostgreSQL and RabbitMQ
Testcontainers, and covers real HTTP, migrations, persistence, messaging,
rollback, idempotency, and concurrency:

```bash
./mvnw -B -ntp -Pintegrated-functional-tests verify
```

Integrated tests verify that their PostgreSQL target is ephemeral and never
clear database tables. Docker must be available for this profile.

### Run and debug Gatling load tests

Gatling simulations are opt-in and generate real accounts, transfers, database
rows, and RabbitMQ messages. Never point them at the normal development database
or a shared, staging, production, or production-like environment. The examples
below create a separate Compose project with dedicated containers, ports, and
volumes; the simulations never clear database tables.

#### 1. Start dedicated load-test infrastructure

From the repository root, start a second PostgreSQL and RabbitMQ stack:

```bash
POSTGRES_DB=simplified_banking_load \
POSTGRES_USER=simplified_banking_load \
POSTGRES_PASSWORD=simplified_banking_load \
POSTGRES_PORT=5433 \
RABBITMQ_USERNAME=simplified_banking_load \
RABBITMQ_PASSWORD=simplified_banking_load \
RABBITMQ_PORT=5673 \
RABBITMQ_MANAGEMENT_PORT=15673 \
docker compose -p simplified-banking-load up -d --wait postgres rabbitmq
```

The `-p simplified-banking-load` project name is required: it prevents this
stack from sharing the normal development containers or volumes.

#### 2. Start the application against the dedicated stack

Keep this process running in its own terminal:

```bash
SERVER_PORT=18080 \
DATABASE_URL=jdbc:postgresql://localhost:5433/simplified_banking_load \
DATABASE_USERNAME=simplified_banking_load \
DATABASE_PASSWORD=simplified_banking_load \
RABBITMQ_HOST=localhost \
RABBITMQ_PORT=5673 \
RABBITMQ_USERNAME=simplified_banking_load \
RABBITMQ_PASSWORD=simplified_banking_load \
./mvnw spring-boot:run
```

Wait until the application reports that it started on port `18080` before
starting Gatling.

#### 3. Create an IntelliJ Run/Debug configuration

1. Open the **Maven** tool window, activate the `load-tests` profile under
   **Profiles**, and reload the Maven project. This makes IntelliJ index
   `src/test/gatling/java` and the Gatling dependencies.
2. Open **Run > Edit Configurations**.
3. Select **+ > Maven**.
4. Set **Name** to `Gatling - Hot source`.
5. Set **Working directory** to `$PROJECT_DIR$`.
6. Set **Run** or **Command line** to:

   ```text
   -Pload-tests gatling:test -Dgatling.sameProcess=true -Dgatling.simulationClass=com.elziojunior.simplifiedbankingservice.load.HotSourceTransferSimulation
   ```

7. Select Java 21 as the JRE.
8. Under **Modify options > Add VM options**, add:

   ```text
   --add-opens=java.base/java.lang=ALL-UNNAMED
   ```

9. Under **Modify options > Environment variables**, add:

   | Variable | Local debug value |
   | --- | --- |
   | `TRANSFER_LOAD_ENVIRONMENT` | `dedicated-load-test` |
   | `TRANSFER_LOAD_BASE_URL` | `http://localhost:18080` |
   | `TRANSFER_LOAD_RATE` | `1` |
   | `TRANSFER_LOAD_DURATION_SECONDS` | `10` |
   | `TRANSFER_LOAD_DESTINATIONS` | `2` |
   | `TRANSFER_LOAD_DATABASE_URL` | `jdbc:postgresql://localhost:5433/simplified_banking_load` |
   | `TRANSFER_LOAD_DATABASE_USERNAME` | `simplified_banking_load` |
   | `TRANSFER_LOAD_DATABASE_PASSWORD` | `simplified_banking_load` |

   `.env.example` is a reference file and is not loaded automatically by
   IntelliJ. Enter these variables in the Run/Debug configuration.

10. Place breakpoints in the simulation or `TransferLoadSupport` and select
   **Debug**. `gatling.sameProcess=true` is required so Gatling runs in the JVM
   to which IntelliJ attaches the debugger.

Use the low rate and duration above while stepping through code. A suspended
thread changes request timing, so use normal non-debug load runs and Gatling
reports to evaluate latency, throughput, timeouts, and lock contention.

To debug the distributed scenario, duplicate the configuration, name it
`Gatling - Distributed`, and replace the simulation class with:

```text
com.elziojunior.simplifiedbankingservice.load.DistributedTransferSimulation
```

The same simulations can be run without the debugger by exporting the
`TRANSFER_LOAD_*` values documented in `.env.example` and running:

```bash
./mvnw -Pload-tests gatling:test \
  -Dgatling.simulationClass=com.elziojunior.simplifiedbankingservice.load.HotSourceTransferSimulation
```

Gatling writes the HTML report under `target/gatling/` after a completed run.

#### Troubleshooting

- Unresolved Gatling imports or inactive breakpoints: activate the Maven
  `load-tests` profile and reload the project.
- `TRANSFER_LOAD_* is required for authorized load execution`: add the missing
  variable to the Run/Debug configuration; `.env.example` is not loaded by the
  IDE.
- `Fixture API is unavailable`: confirm that the dedicated application is still
  running at `http://localhost:18080`.
- `Post-run consistency access is unavailable`: confirm that PostgreSQL is
  running on port `5433` and that the database URL and credentials match the
  dedicated stack.
- A breakpoint is not reached: confirm that the command contains
  `-Dgatling.sameProcess=true` and that the configuration was started with
  **Debug**, not **Run**.

#### 4. Remove the dedicated environment

Stop the application process first. Then remove only the dedicated load-test
containers and their volumes:

```bash
docker compose -p simplified-banking-load down --volumes
```

This removes the whole disposable load-test database instead of clearing tables
and does not affect the normal Compose project.

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
| `TRANSFER_NOTIFICATION_MAX_ATTEMPTS` | `3` | Maximum direct RabbitMQ publication attempts |

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
- Server-issued transfer tokens and idempotent, atomic, pessimistically locked
  transfers.
- Direct best-effort RabbitMQ transfer-completed events with bounded in-memory
  retry.
- Safe Problem Details, bounded-cardinality API metrics, Docker Compose local
  infrastructure, and a non-root application image.
- Unit, isolated functional, integrated, migration, concurrency, and Gatling
  load tests.

Authentication and authorization, account query/update/delete operations,
movement-query APIs, and notification consumers remain future work.

The canonical local quality command is:

```bash
./mvnw -B -ntp verify
```

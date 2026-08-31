# Simplified Banking Service

Simplified Banking Service is a Java/Spring Boot REST API for a deliberately
small digital-banking domain. Its documented scope covers account creation and
safe account-to-account transfers, with financial consistency and traceability
as primary constraints.

The repository is currently documentation-first: the product behavior,
business rules, architecture choices, engineering standards, and AI-assisted
delivery workflows were defined before application implementation began. The
application foundation is now available; account and transfer features remain
planned rather than implemented.

## Current scope

- Create an account with a customer name and a non-negative initial balance.
- Transfer a positive monetary amount between two different existing accounts.
- Debit and credit both accounts atomically, without creating or destroying
  money.
- Prevent overdrafts, duplicate processing, lost updates, and partial financial
  effects under concurrent requests.
- Record each successful transfer as one debit and one credit movement sharing
  the same operation identifier.
- Create successful-transfer notifications asynchronously without making
  notification delivery part of the financial transaction.
- Expose operational metrics for throughput, latency, failures, timeouts, and
  lock contention.

Account retrieval, listing, update, deletion, overdrafts, and a specific
notification delivery channel are outside the currently documented scope.

## Planned API

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

### Transfer funds

```http
POST /api/v1/transfers
Content-Type: application/json

{
  "sourceAccountId": "ACC-001",
  "destinationAccountId": "ACC-002",
  "amount": 100.00
}
```

The API contracts above describe the planned behavior. No application build is
present in the repository yet.

## Architecture baseline

- Java 21 or later and Spring Boot 3.5.x or later.
- Maven with the Maven Wrapper.
- Spring Web MVC, Spring Data JPA with Hibernate, and Spring Security.
- PostgreSQL with Flyway migrations.
- RabbitMQ for asynchronous messaging.
- Spring Boot Actuator and Micrometer for observability.
- Docker and Docker Compose for reproducible local environments.
- A layered monorepo with API, service/domain, and repository boundaries.
- `BigDecimal` and fixed-precision database types for monetary values.
- A single `READ COMMITTED` transaction with pessimistic account locking for
  each transfer.
- At least 90% unit-test coverage for meaningful application logic, plus
  isolated, integration, concurrency, and Gatling load tests where applicable.

Architecture and business records are currently marked as proposed; their
status is visible in the source documents under `docs/`.

## Documentation

- [Documentation map](docs/README.md)
- [Account creation epic](docs/epics/EPIC001-account-creation.MD)
- [Account-to-account transfer epic](docs/epics/EPIC002-account-to-account-transfer.MD)
- [Business decision records](docs/bdr/README.md)
- [Architecture decision records](docs/adr/README.md)
- [Logical data model](docs/database/logical-data-model.md)
- [Engineering standards](docs/engineering/README.md)
- [AI-assisted delivery workflows](ai-operation/workflows/README.md)

Repository-wide contribution instructions are defined in [AGENTS.md](AGENTS.md).

## Development

### Prerequisites

- Java 21 or later.
- Docker with Docker Compose for local PostgreSQL and RabbitMQ.

The Maven Wrapper downloads the repository's configured Maven version, so a
host Maven installation is not required.

### Build and test

```bash
./mvnw test
./mvnw verify
```

Unit tests belong under `src/test/unit/java`. Isolated functional tests belong
under `src/test/isolated/java` and join the normal `verify` lifecycle. The
real-boundary integrated source set is opt-in:

```bash
./mvnw -Pintegrated-functional-tests verify
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

Local ports and credentials can be overridden through the variables documented
in `.env.example`. Application connection settings can be overridden through
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `RABBITMQ_HOST`,
`RABBITMQ_PORT`, `RABBITMQ_USERNAME`, and `RABBITMQ_PASSWORD`.

Stop local infrastructure with:

```bash
docker compose down
```

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

The bootable application foundation, dependency management, separated test
source sets, and local infrastructure are implemented. No account, transfer,
movement, notification, or database-migration feature has been implemented.

The canonical validation command is:

```bash
./mvnw verify
```

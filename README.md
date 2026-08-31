# Simplified Banking Service

Simplified Banking Service is a Java/Spring Boot REST API for a deliberately
small digital-banking domain. Its documented scope covers account creation and
safe account-to-account transfers, with financial consistency and traceability
as primary constraints.

The repository is currently documentation-first: the product behavior,
business rules, architecture choices, engineering standards, and AI-assisted
delivery workflows are defined before application implementation begins.

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

## Branching model

- `main` contains stable, validated releases.
- `develop` is the integration branch.
- Feature branches merge into `develop`; validated releases are promoted from
  `develop` to `main`.

## Development status

The application has not been implemented yet, so build and local-run commands
will be added with the first implementation slice. Once the Maven Wrapper is
available, the canonical validation command will be:

```bash
./mvnw verify
```

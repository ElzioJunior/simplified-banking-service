# ADR-0034 — Use Docker Compose for Complete Local Product Startup

- Status: Accepted
- Date: 2026-09-01
- Deciders: User and Engineering Team
- Supersedes: ADR-0009
- Superseded by: none
- Related: ADR-0004, ADR-0008, ADR-0032, ADR-0033

## Context

ADR-0009 limits Docker Compose to PostgreSQL and RabbitMQ while developers start
the application separately through Maven. That split requires host Java and
Maven execution, duplicates environment setup, and does not provide the local
metrics demonstration selected by ADR-0033. The project now needs one portable
startup path that demonstrates the complete product on another machine with
only Docker and Docker Compose.

## Decision

Make the root `compose.yaml` the canonical complete local product topology. It
must build and start the application image together with PostgreSQL, RabbitMQ,
Prometheus, and Grafana. A single documented `docker compose up --build --wait`
command must wait for dependency and service health without requiring manual
ordering, local Java, Maven, database migration, datasource provisioning, or
dashboard import.

Use health checks and long-form `depends_on` conditions for startup ordering.
The application waits for healthy PostgreSQL and RabbitMQ; Prometheus waits for
the healthy application; Grafana waits for healthy Prometheus. The application
uses Compose service discovery names for internal database, broker, and scrape
traffic.

Publish the application API, PostgreSQL, RabbitMQ AMQP and management, Prometheus
UI, and Grafana UI through configurable host ports. Bind Prometheus and Grafana
only to host loopback because they are local demonstration tools. Keep named
volumes for database, broker, Prometheus, and Grafana state. `docker compose
down` stops the topology without deleting those volumes; deleting volumes
requires an explicit destructive command and is never part of normal startup or
shutdown documentation.

Retain host-native Maven startup as an optional development path, but do not
make it a prerequisite for trying the complete product. Image and service
versions remain pinned for reproducibility.

## Consequences

### Positive

- A new machine can start the complete product with one Docker Compose command.
- Health-based ordering removes common database and broker startup races.
- The application image and local dependencies use one documented topology.
- Swagger, RabbitMQ management, Prometheus, and Grafana are available immediately after startup.
- Host-native development remains possible when faster edit/restart cycles are useful.

### Negative or trade-offs

- The first startup builds the application and downloads several container images.
- The full stack requires more local memory, disk, and startup time.
- Compose remains a local demonstration topology, not a production orchestrator.
- Developers changing only application code must rebuild or use the optional host-native path.

## Alternatives considered

- Continue starting only infrastructure in Compose — rejected because it does
  not satisfy portable complete-product startup.
- Add separate Compose files for the application and observability — rejected
  because the primary demonstration should remain one command and one topology.
- Use local Kubernetes — rejected because it adds unnecessary orchestration
  complexity for the current scope.
- Require host Java and Maven — rejected because it weakens onboarding and
  cross-machine reproducibility.

## Validation

`docker compose config --quiet` must validate the topology. A clean-environment
startup with `docker compose up --build --wait` must leave every service healthy.
HTTP checks must prove the API documentation, Prometheus target, Prometheus UI,
Grafana health endpoint, and provisioned banking dashboard are reachable at the
documented host URLs. Normal shutdown must preserve named volumes.

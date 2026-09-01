# ADR-0033 — Use Prometheus and Grafana for Local Metrics Dashboards

- Status: Accepted
- Date: 2026-09-01
- Deciders: User and Engineering Team
- Supersedes: none
- Superseded by: none
- Related: ADR-0007, ADR-0008, ADR-0027, ADR-0034

## Context

ADR-0008 selects Spring Boot Actuator and Micrometer for application and JVM
instrumentation, but the current `/actuator/metrics` endpoint is diagnostic and
does not provide time-series retention, PromQL queries, or dashboards. The
project needs a local, reproducible example that makes the existing bounded
banking metrics visible during API use and load tests without introducing a
hosted observability dependency.

The selected backend must preserve Micrometer as the instrumentation boundary,
support counters and latency histograms, require no manual datasource or
dashboard creation, and remain explicitly local rather than imply a production
monitoring topology.

## Decision

Use the Micrometer Prometheus registry to expose the application metrics in
Prometheus scrape format. Use Prometheus for local collection and time-series
storage, and Grafana OSS for local visualization. Pin container image versions
in Docker Compose and provision the Prometheus datasource and project dashboard
from version-controlled files.

Expose `/actuator/prometheus` only when the explicit local Compose
observability setting is enabled. In that environment, serve management traffic
on an internal container port that is not published to the host, and allow the
Prometheus scrape endpoint without application credentials. Keep every other
operational endpoint protected. Prometheus and Grafana web ports must bind only
to the host loopback interface; Grafana provides anonymous Viewer access to the
provisioned read-only dashboard so the example requires no committed or shared
credentials.

Enable a Prometheus histogram for `banking.api.request.latency` with bounded
service-level buckets so the dashboard can calculate latency percentiles.
Retain the existing bounded `operation` tag and do not add account IDs, tokens,
request payloads, exception messages, or other high-cardinality or sensitive
labels.

The initial dashboard must show request throughput, outcomes, success ratio,
p95 latency, database errors, timeouts, lock contention, JVM heap use, process
CPU, and datasource connection use. Prometheus and Grafana data use named local
volumes; provisioned configuration and dashboards remain the source of truth.

## Consequences

### Positive

- Existing Micrometer metrics become visible as historical local dashboards.
- The stack uses the standard Spring Boot Prometheus integration.
- PromQL and provisioned dashboards make load-test behavior demonstrable.
- No hosted account, manual Grafana setup, or committed observability credential is required.
- Bounded tags and histogram buckets keep the demonstration predictable.

### Negative or trade-offs

- The local stack consumes additional CPU, memory, ports, images, and disk space.
- Prometheus naming conventions differ from Micrometer's dot-separated meter names.
- Histogram series increase the number of stored time series.
- Anonymous Grafana viewing and an unauthenticated scrape endpoint are acceptable only within the loopback-bound local topology.
- This stack does not define production retention, alerting, authentication, high availability, or remote storage.

## Alternatives considered

- Grafana without Prometheus — rejected because Grafana is a visualization
  layer and `/actuator/metrics` is not a time-series datasource.
- OpenTelemetry Collector plus a telemetry backend — rejected because unified
  traces and logs are not required for this local metrics example.
- Spring Boot Admin — rejected because it does not provide the requested
  Prometheus-style historical metrics and dashboard workflow.
- A hosted Grafana or Prometheus service — rejected because startup must remain
  portable, local, and account-free.

## Validation

Automated tests must verify the Prometheus dependency, endpoint exposure,
histogram configuration, and default protection outside the Compose setting.
The complete Compose validation must prove Prometheus reports the application
target as healthy, Grafana reports a healthy instance with the provisioned
datasource and dashboard, and the dashboard's principal queries return without
manual configuration.

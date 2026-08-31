# Integrated Functional Test Plan — <Feature or System>

- Status: Proposed
- Governing ADRs: <links>
- Governing test standard: [Testing standards](testing-standards.md)

## Purpose and real boundaries

<Which integration risk cannot be proven by isolated tests? Identify every
boundary that must use its production implementation.>

## Target flow

```text
real entry point
  -> application behavior
  -> persistence/messaging as applicable
  -> production external adapter under qualification
  -> observable final business outcome
```

## Controlled boundaries

<Collateral systems that may be mocked or sandboxed and why doing so does not
invalidate the risk being qualified.>

## Harness

- Source root: `src/test/integrated/java`
- Suffix: `*IntegratedFunctionalTest`
- Opt-in command: `./mvnw -Pintegrated-functional-tests verify`
- Application entry point: <HTTP, message, CLI, scheduled job, etc.>
- Infrastructure: <ephemeral database/broker/container/etc.>
- External runtime/provider: <configurable resource>
- Observability: <how the final outcome is asserted>
- Isolation and cleanup: <strategy>

## Scenario matrix

| Scenario | Input | Context/fixtures | Stable expected outcome |
|---|---|---|---|
| <name> | <input> | <minimal state> | <contract and domain assertions> |

## Configuration and preflights

<Externalized endpoints/credentials, connectivity, quota, capability, resource
availability, cost/rate limits, and actionable failure diagnostics.>

## Authorization boundary

<Exact first command/action that reaches a consequential real boundary. All
preparation stops immediately before this point until the user authorizes it.>

## Failure and cleanup policy

<Safe failure, retries, created external state, cleanup, and escalation.>

## Acceptance criteria

- [ ] Default quality gates remain independent of the real provider.
- [ ] The test fails if the required production boundary is replaced by a mock.
- [ ] Provider/runtime unavailability fails clearly rather than silently skips.
- [ ] A shallow transport acknowledgement cannot pass without the expected
      final application outcome.
- [ ] Secrets remain outside version control and tool output.
- [ ] Each scenario is isolated, repeatable, and cleaned up.

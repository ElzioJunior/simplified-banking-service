---
name: run-integrated-functional-tests
description: Plan, implement, or execute opt-in Java functional tests that exercise the real application and consequential external boundaries without weakening deterministic default quality gates.
---

# Run Integrated Functional Tests

## Inputs

- Requested mode: plan, implement, or execute.
- Feature scenarios and stable expected outcomes.
- Relevant product rules, BDRs, ADRs, contracts, and testing standards.
- Real-boundary configuration, credentials, availability, cost/rate limits, and
  cleanup requirements when execution is requested.

## Procedure

1. Read the testing standards, applicable contracts, and feature decisions.
2. Identify precisely which production boundaries must remain real and which
   collateral boundaries may be controlled to keep the scenario safe.
3. Confirm the requested mode. Planning-only work creates no test code or build
   configuration unless explicitly requested.
4. Put tests in the repository's dedicated integrated source set and use the
   configured integrated naming convention. Never mix them into unit or
   isolated functional source sets.
5. Define each scenario with actor/input, minimal fixtures, stable expected
   result, observable side effects, cleanup, and deterministic safety checks.
6. Enter through the real public/application boundary named by the scenario;
   do not call an internal service merely to bypass transport or security.
7. Use production implementations for every boundary explicitly under test.
   Do not mock the behavior whose integration is being qualified.
8. Use isolated, ephemeral infrastructure when practical and apply real schema
   migrations when persistence is part of the path.
9. Add test-only observation that exposes the final application outcome without
   adding an unsafe production endpoint or changing product behavior.
10. Assert stable contracts and domain invariants rather than incidental
    provider prose, timestamps, identifiers, ordering, or implementation detail.
11. Keep the suite outside the default build. Provide a dedicated command or
    profile and actionable preflights for connectivity, credentials, runtime
    capability, quotas, and configured resources.
12. Before the first consequential real request or side effect, stop and obtain
    the Workflow 05 authorization. Preparation and compilation do not consume
    that authorization gate.
13. After authorization, execute the approved suite. Report the exact command,
    date, environment/provider, result, duration when useful, and failure point.
14. Add concise JavaDoc to every test method describing the real flow or failure
    it proves and why it matters.

## Outputs

Planning mode returns:

- scenario matrix;
- boundary/topology map;
- fixtures, cleanup, and expected results;
- test naming and opt-in execution mechanism;
- observability requirements;
- prerequisites, risks, failure diagnostics, and open decisions.

Implementation or execution mode additionally returns test artifacts and exact
command results.

## Validation

- The complete approved path is exercised through its real entry point.
- Every boundary claimed as real uses its production implementation.
- Scenarios are independent and do not rely on execution order.
- A requested run fails clearly when a required provider or resource is
  unavailable; it is not silently skipped.
- Default unit and isolated functional quality gates require no real provider.
- Secrets remain outside version control and are never printed.
- Cleanup prevents avoidable external or shared-state residue.
- Assertions prove stable application behavior, not only transport acceptance.

## Stop conditions

- Stop when the expected business outcome is undefined or conflicts with an
  active BDR/ADR.
- Stop implementation when the final outcome cannot be observed without an
  unapproved production behavior or API.
- Stop before destructive, production, personal-data, or materially broader
  external execution that was not in the approved plan.
- Stop execution when a required preflight fails; do not silently downgrade to
  a mock or skip.
- Never make real-provider connectivity part of the default build lifecycle.

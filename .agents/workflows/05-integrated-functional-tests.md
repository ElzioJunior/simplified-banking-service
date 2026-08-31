# Workflow 05 — Integrated Functional Tests

## Objective

Plan, implement when requested, and execute opt-in scenarios through the real
application and the consequential external boundaries identified by the
project's architecture and test plan.

Examples include a real model/provider, paid API, shared production-like
environment, hardware, cloud resource, or third-party sandbox with side effects
or rate/cost consequences.

## Applicability and preconditions

- The complete lifecycle or integrated-test work is explicitly in scope.
- The expected business outcome and stable assertions are defined.
- Relevant real boundaries, credentials, cost/rate limits, cleanup, and safe
  failure behavior are documented.
- Integrated Java sources use the repository's dedicated opt-in source set and
  naming convention.
- Unit and isolated functional suites remain independent of real providers.

If no consequential real-boundary test applies, record that decision with
evidence in the execution report and continue to Workflow 06 without inventing
an external test.

## Skill

Execute `../skills/run-integrated-functional-tests/SKILL.md`.

Do not substitute isolated mocked tests for a real boundary required by the
approved plan, and do not make real-boundary execution part of the default
build lifecycle.

## Preparation output

- Scenario matrix and stable expected outcomes.
- Fixtures, isolation, cleanup, and observability strategy.
- Configuration and preflight requirements.
- Dedicated command/profile and proof that default quality gates remain
  provider-independent.
- Documentation for human-readable flows when it materially improves review.
- Explicit blockers when the outcome cannot be observed safely.

## Failure loop

When a failure exposes an implementation or contract defect, return to Workflow
02, apply the smallest coherent fix, rerun affected parts of Workflows 03 and
04, and repeat Workflow 05. The authorization granted immediately before the
first real execution covers this planned loop unless external scope or
consequences materially expand.

## Authorization boundary

Scenario design, implementation, compilation, local test-double validation, and
safe preflights belong before the gate. Stop immediately before the first
request or side effect against a consequential real boundary and ask for
explicit authorization for the planned suite.

After authorization, execute, diagnose, fix, revalidate, and retest until the
required suite passes, then continue to Workflow 06 without another planned
approval pause.

## Completion boundary

For a request limited to this workflow, stop after the requested scope. For a
complete lifecycle, continue only after every required scenario passes or the
execution report establishes that this workflow is not applicable.

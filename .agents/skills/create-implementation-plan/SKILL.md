---
name: create-implementation-plan
description: Turn approved repository context and decisions into a scoped, ordered, testable implementation plan and shared execution-report update.
---

# Skill — Create Implementation Plan

## Purpose
Turn approved context and decisions into a small, executable coding plan.

## Inputs
- Context summary.
- Decision-impact result.
- Existing code structure.

## Procedure
1. Define acceptance criteria.
2. Identify affected modules/components.
3. Identify domain/application changes.
4. Identify applicable adapters/boundaries, such as:
   - HTTP/API;
   - asynchronous jobs or messaging;
   - third-party providers;
   - persistence;
   - configuration.
5. Identify data/Flyway impact.
6. Identify tests:
   - unit under `src/test/unit/java`;
   - isolated functional under `src/test/isolated/java`;
   - integrated functional under `src/test/integrated/java` only when the
     explicit integrated workflow is requested;
   - affected regression tests.
7. Identify failure/edge cases.
8. Order work into small coherent steps.
9. Avoid speculative abstractions and unrelated refactors.
10. Identify methods expected to have light or greater complexity and plan
    JavaDoc that states what each method does and why it exists.
11. Identify application-owned record/DTO/entity/configuration properties that
    require concise property documentation.
12. Plan concise JavaDoc for every test scenario, stating what it proves and
    why it matters.
13. Identify additional human-readable rationale or contract documentation for
    non-obvious business rules, external boundaries, failure semantics, side
    effects, concurrency, retries, and idempotency.
14. Produce a concise execution report that summarizes the approved scope,
    ordered development slices, material decisions/risks, validation and
    quality strategy, integrated-test scope, and normal commit/push behavior.

## Output
An ordered implementation plan with:
- files/components likely affected;
- behavior per step;
- tests per step;
- required JavaDoc/comment intent where code alone cannot communicate the
  contract safely;
- required method, property, and test-scenario JavaDoc under the mandatory
  coding-standard baseline;
- validation after each meaningful stage.

Also update the single repository-wide `docs/epics/execution-report.md`; never
create a per-epic execution report. The report must identify the active
`EPICxxx-execution-plan.md`, retain concise status/checkpoints for other epic
plans, and be short enough for the user to approve the active development scope
without rereading its detailed backlog. It must end with one explicit request
for development authorization and state that approval covers normal
non-destructive commits and pushes until the integrated-test gate.

## Validation
The plan must not silently introduce a new durable decision.

## Stop conditions
Return to decision/context work if implementation cannot proceed without a new
BDR/ADR or unresolved requirement.

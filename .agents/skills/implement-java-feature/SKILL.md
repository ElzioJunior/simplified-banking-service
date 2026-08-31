---
name: implement-java-feature
description: Implement approved Java and Spring behavior while preserving repository architecture, documentation, data, and scope constraints.
---

# Skill — Implement Java Feature

## Purpose
Implement production Java/Spring behavior according to an approved plan.

## Inputs
- Approved implementation plan.
- Relevant repository context.

## Procedure
1. Inspect existing conventions before creating new abstractions.
2. Implement business behavior in domain/application components.
3. Keep business logic out of controllers, DTOs, persistence entities, and
   provider-specific adapters.
4. Reuse existing abstractions before introducing new ones.
5. Implement boundaries as required:
   - Spring Web MVC;
   - asynchronous job or messaging adapters;
   - provider adapters;
   - Spring Data JPA repositories;
   - configuration.
6. Add Flyway migration when persistence schema changes.
7. Use the Java baseline and framework conventions governed by the repository's
   active ADRs and engineering standards.
8. Before adding or moving Java types, apply the package and type-naming rules
   in `../../../docs/engineering/coding-standards.md`, including its API model,
   DTO, entity, exception, and configuration boundaries.
9. Keep changes scoped and reviewable.
10. Remove dead/debug/speculative code before completion.
11. Optimize first for human-readable names, responsibilities, and control
    flow. Refactor code that requires narration merely to be understood.
12. Add JavaDoc to every method with light or greater complexity. State what
    the method does and why it exists in the application flow.
13. Add concise property documentation to application-owned records, DTOs,
    entities, configuration objects, events, and results.
14. Add or update further comments and JavaDoc for non-obvious rationale,
    invariants, contracts, side effects, failure behavior, and
    architectural/external boundaries; do not narrate self-explanatory code.
15. Recheck every nearby existing comment after behavior changes and remove or
    update anything stale, contradictory, speculative, or misleading.

## Output
Compiling production implementation ready for tests/quality gates.

## Validation
- Compiles.
- Matches BDR/ADR/product behavior.
- No unrelated architectural change.
- No secret/credential exposure.
- Data changes have migrations.
- Normal flow is readable through code structure and names.
- Non-obvious intent and boundary contracts are documented accurately.
- Every method at or above the light-complexity threshold explains what it does
  and why it exists; object properties are documented concisely.

## Stop conditions
Stop and return to planning when implementation reveals a missing durable
decision.

---
name: review-code
description: Perform an independent evidence-based review of completed changes against behavior, architecture, tests, security, reliability, and maintainability requirements.
---

# Skill — Review Code

## Purpose
Perform an independent, evidence-based review of a completed change.

## Inputs
- Diff.
- Tests.
- Quality report.
- Relevant BDRs/ADRs/standards/data model.

## Review dimensions
1. Correctness and edge cases.
2. Architecture and boundaries.
3. Business-rule compliance.
4. Test quality/completeness.
5. Security and sensitive-data handling.
6. Reliability:
   - durable job/message claiming, completion, retry, and recovery when used;
   - duplicate side effects;
   - idempotency where required;
   - external integration failure behavior.
7. Human readability and maintainability:
   - explicit domain naming and cohesive responsibilities;
   - cognitively simple control flow;
   - comments do not compensate for avoidable structural complexity;
   - non-obvious rationale, invariants, contracts, side effects, and failure
     semantics are documented;
   - JavaDoc exists where an architectural/public boundary needs an explicit
     contract;
   - every method with light or greater complexity explains what it does and
     why it exists;
   - record/DTO/entity/configuration properties have concise documentation;
   - every automated test method explains what it proves and why the scenario
     matters;
   - comments and JavaDoc are accurate, useful, non-sensitive, and consistent
     with the implementation and governing decisions.
8. Unnecessary complexity.

## Finding format
Each finding must include:
- severity: BLOCKER / HIGH / MEDIUM / LOW;
- location;
- problem;
- consequence;
- recommended fix.

Do not produce vague stylistic findings.
Readability findings must identify the concrete comprehension or maintenance
risk. Missing comments are findings only when code and existing durable
documentation do not communicate important intent or contract safely.

## Output
Structured review report.

## Validation
Every finding must be grounded in code, tests, or repository decisions.

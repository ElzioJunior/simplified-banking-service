---
name: run-quality-checks
description: Execute and accurately report the repository's configured build, test, coverage, analysis, security, and schema quality gates.
---

# Skill — Run Quality Checks

## Purpose
Execute and report repository quality gates without inventing results.

## Inputs
- Current implementation.
- Repository engineering standards/tooling.

## Procedure
Run applicable configured checks:
1. Verify test source separation: unit `{TestName}Test` under
   `src/test/unit/java`, isolated `{TestName}FunctionalTest` under
   `src/test/isolated/java`, and opt-in
   `{TestName}IntegratedFunctionalTest` under `src/test/integrated/java`.
2. Build/compile.
3. Unit tests.
4. Coverage verification.
5. Functional tests.
6. Lint/style/static analysis.
7. Dependency/security checks when configured.
8. Flyway/schema validation when data changed.

For each check, record:
- command/tool;
- pass/fail;
- meaningful result;
- reason if not executed.

## Output
A concise quality report.

## Validation
- Required checks pass.
- Eligible code meets the repository's configured unit-test coverage standard.
- No failing check is hidden or reclassified as success.

## Failure conditions
Return implementation defects to the implementation workflow/skill.

---
name: create-unit-tests
description: Create deterministic process-local Java unit tests for new or changed behavior under the repository's unit-test conventions.
---

# Skill — Create Unit Tests

## Purpose
Create meaningful unit tests for new or changed production behavior.

## Inputs
- Production changes.
- Applicable testing standards.

## Procedure
1. Place Java tests under `src/test/unit/java`, preserving production package
   paths. Never place unit tests under `src/test/isolated/java` or
   `src/test/integrated/java`.
2. Use the `*Test` suffix and keep the test process-local: do not start Spring,
   open sockets, start databases, or require external processes.
3. Identify observable behavior and decision branches.
4. Cover:
   - happy paths;
   - important branches;
   - edge cases;
   - expected failures.
5. Test business behavior, not implementation trivia.
6. Avoid excessive mocking.
7. Prefer deterministic tests.
8. Do not test excluded trivial structures merely to inflate coverage.
9. Do not weaken existing tests.
10. Run affected unit tests after changes.
11. Add concise JavaDoc to every test method, explaining the behavior, branch,
    invariant, or failure it proves and why that proof matters.

## Output
Unit tests that prove the intended behavior.

## Validation
- Tests pass.
- Assertions prove meaningful behavior.
- Every unit test is located under `src/test/unit/java` and uses `*Test`.
- Coverage contributes to the project's configured eligible-code target.
- Every test method documents its scenario and relevance.

## Failure conditions
If code is difficult to test because responsibilities are tangled, surface the
design problem rather than hiding it with weak tests.

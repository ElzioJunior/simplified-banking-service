---
name: create-functional-tests
description: Create isolated, reproducible Java functional tests across application boundaries without requiring real external systems.
---

# Skill — Create Functional Tests

## Purpose
Validate important feature flows across application boundaries while remaining
isolated and reproducible.

## Inputs
- Feature behavior.
- Existing functional-test conventions.

## Procedure
1. Place Java tests under `src/test/isolated/java`, preserving production
   package paths, and name each class `{TestName}FunctionalTest`. Never place
   isolated functional tests under `src/test/unit/java` or
   `src/test/integrated/java`.
2. Identify the smallest important end-to-end/application flow.
3. Exercise real application wiring where practical.
4. Isolate external applications and messaging systems with controlled test
   doubles. Application-owned persistence may use a disposable Testcontainer
   when migrations, mappings, transactions, constraints, or locking are part
   of the flow.
5. Avoid shared mutable/manual environments.
6. Validate both successful and important failure flows.
7. Keep fixtures explicit and understandable.
8. Run the functional tests.
9. Add concise JavaDoc to every test method, explaining the flow or failure it
   proves and why that scenario matters.

## Output
Isolated functional tests for important feature behavior.

## Validation
Tests are repeatable locally/CI, use only disposable or controlled dependencies,
and do not require manual external preparation. Database scenarios prove the
resolved datasource is their exact test-owned instance, never clear tables, and
discard the whole instance after the suite.
Every test method documents its scenario and relevance.

## Stop conditions
Do not introduce a new heavyweight integration platform/tool without an
accepted architectural/engineering decision when required.

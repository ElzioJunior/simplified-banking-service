# Skill — Load Relevant Context

## Purpose
Load the minimum repository context required to understand a task correctly.

## Inputs
- User request.
- Optional target files/classes/components.

## Procedure
1. Use `docs/README.md` to locate the relevant documentation.
2. Read the relevant product files under `docs/product/`.
3. Find relevant active BDRs under `docs/bdr/`.
4. Find relevant active ADRs under `docs/adr/`.
5. Check `docs/DECISIONS.md` for related open decisions.
6. Read applicable engineering standards under `docs/engineering/`.
7. Read `docs/database/` when persistence/data is involved.
8. Inspect affected source and tests when they exist.
9. Ignore unrelated documentation unless dependencies require expansion.

## Output
A concise context summary containing:
- intended behavior;
- governing BDRs;
- governing ADRs;
- applicable engineering standards;
- data-model constraints;
- related open decisions;
- affected code areas;
- unresolved conflicts or missing decisions.

## Validation
Every material implementation assumption must be traceable to context or clearly
identified as an assumption.

## Stop conditions
Stop when active records conflict or a missing durable decision prevents safe
implementation.

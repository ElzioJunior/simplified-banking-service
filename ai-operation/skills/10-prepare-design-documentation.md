# Skill — Prepare Design Documentation

## Purpose

Make approved durable decisions and design intent explicit before production
implementation begins.

## Inputs

- Context summary.
- Decision-impact report.
- User-approved business/architecture decisions.
- Existing documentation.

## Procedure

1. Add or supersede BDRs when an approved business decision is required.
2. Add or supersede ADRs when an approved architecture decision is required.
3. Update relevant `docs/product/` files when approved behavior, flow, or scope
   changed.
4. Update `docs/database/` when the approved logical data model changed.
5. Update engineering standards only for approved recurring development
   practices.
6. Update the decision register when a decision is resolved or newly identified
   as open.
7. Do not rewrite accepted decision history to conceal a changed decision; use
   explicit supersession or follow-up relationships.
8. Do not describe planned behavior as already implemented.

## Output

- Implementation-ready documentation.
- List of records created, superseded, clarified, or left open.
- Remaining documentation blockers.

## Validation

- The implementation plan can trace every durable decision to an active source.
- Active documentation does not materially conflict.
- Open decisions are visible and do not get silently resolved by the plan.

## Stop conditions

Stop if a required durable decision has not been approved or active records
still conflict.

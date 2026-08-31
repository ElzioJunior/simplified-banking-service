---
name: sync-implementation-documentation
description: Synchronize final implementation documentation with approved behavior, decisions, data models, and operational reality without rewriting history.
---

# Skill — Sync Implementation Documentation

## Purpose

Ensure repository documentation accurately reflects the final implementation
without rewriting decision history.

## Inputs

- Final implementation.
- Approved plan and decision-impact report.
- Existing documentation.

## Procedure

1. Compare implemented behavior with the approved product, BDR, ADR, data-model,
   and engineering sources.
2. Update public/API and operational documentation affected by the
   implementation.
3. Update `docs/database/` when the implemented logical data model changed and
   verify that physical schema changes have Flyway migrations.
4. Update relevant `docs/product/` files only when the approved behavior, flow,
   or scope changed.
5. Add or supersede ADRs/BDRs only for approved durable decisions discovered
   during implementation; do not retroactively invent approval.
6. Update the decision register when implementation exposes or resolves an open
   decision.
7. Do not rewrite historical accepted ADRs/BDRs to hide changed decisions.
8. Do not mirror implementation details that have no durable documentation
   value.

## Output

- Documentation consistent with the final implementation.
- List of documentation changes and any remaining divergence.

## Validation

Implementation and active documentation do not materially contradict each
other.

## Stop conditions

Stop finalization if implementation contradicts an active decision or contains
an unapproved durable decision.

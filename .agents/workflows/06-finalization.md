# Workflow 06 — Finalization and Documentation

## Objective
After the required integrated suite passes, leave the feature clean,
documented, traceable, and ready for delivery.

## Preconditions

- Workflow 05 completed successfully for every required integrated scenario,
  or the approved plan documented why it was not applicable.
- Any integrated-test hotfix repeated the affected quality gates, AI review,
  and integrated execution before this workflow begins.

## Skills
Execute:
1. `../skills/sync-implementation-documentation/SKILL.md`
2. Run any project-specific knowledge/context synchronization skill only when
   the target repository defines and requires one.
3. `../skills/run-quality-checks/SKILL.md` only when final documentation/code
   synchronization changed executable behavior or tests.

Then:
- verify Definition of Done;
- inspect the complete diff;
- remove accidental/debug/generated files that should not be committed;
- produce the final delivery summary.

## Output
A clean repository state and final feature-delivery summary.

## Source control
Preserve incremental commits and pushes; do not squash, amend, rewrite, or
force-push them during finalization. The Workflow 01 development authorization
continues to cover normal finalization commits and pushes; do not request a new
approval. Never open a pull request unless explicitly instructed.

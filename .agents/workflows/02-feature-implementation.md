# Workflow 02 — Feature Implementation

## Objective
Implement the approved feature in the smallest coherent change.

## Skills
Execute:
1. `../skills/implement-java-feature/SKILL.md`
2. `../skills/create-unit-tests/SKILL.md`
3. `../skills/create-functional-tests/SKILL.md` when the feature has an important
   functional flow that should be validated across application boundaries.

Before completing, verify that `*Test` unit tests exist only under
`src/test/unit/java` and `*FunctionalTest` isolated functional tests exist only
under `src/test/isolated/java`. Do not create real-boundary integrated tests
unless Workflow 05 is in scope.

Before handing the implementation to quality gates, verify the documentation
baseline from the coding standard:

- every method with light or greater complexity states in JavaDoc what it does
  and why it exists;
- properties of records, DTOs, entities, configuration objects, commands,
  events, and results have concise documentation;
- every test method explains the behavior or failure it proves and why that
  scenario matters;
- documentation remains accurate after the final code change.

## Incremental delivery

Treat completion of each small, coherent implementation or backlog slice as a
delivery boundary:

1. Run the focused validation appropriate to the completed slice and record
   only evidence that was actually produced.
2. Update the canonical execution plan with the backlog status, resumable
   checkpoint, validation evidence, and exact next action.
3. Inspect the complete slice diff and identify exactly which implementation,
   tests, documentation, and execution-plan changes belong to that slice.
4. When the Workflow 01 development authorization exists, stage only the
   identified slice, commit it with a message that identifies its backlog item
   or behavior, and push it to the current remote branch without force-pushing.
5. Continue directly to the next planned slice without asking for another
   approval. Report concise progress and commit hashes at useful checkpoints.

The Workflow 01 authorization is lifecycle-wide, not slice-specific. If it is
absent, leave validated changes in the worktree and stop before starting the
next slice. A safe, retryable push failure does not create a new authorization
gate; preserve the commit, report/retry the failure, and continue only when
repository provenance remains reliable.

Do not absorb unrelated or user-owned changes into a checkpoint commit. Do not
commit secrets, generated artifacts, debug files, or claims unsupported by the
recorded validation. If an authorized commit or push cannot be completed
safely, preserve the completed work and keep the execution plan as an accurate
resume point.

## Output
Production implementation, required JavaDoc, and documented automated tests,
ready for quality gates, with every completed slice reflected in the execution
plan. Under the Workflow 01 development authorization, validated slices are
committed and pushed without per-slice approval.

## Stop conditions
Return to Workflow 01 if a missing business/architecture decision is discovered.

## Completion boundary
Do not perform the final comprehensive quality review here.

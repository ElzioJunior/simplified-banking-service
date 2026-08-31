# Workflow 01 — Design and Context Update

## Objective
Make the feature implementation-ready before production code is changed.

## Skills
Execute in order:
1. `../skills/load-relevant-context/SKILL.md`
2. `../skills/detect-decision-impact/SKILL.md`
3. `../skills/prepare-design-documentation/SKILL.md` — only when
   decisions/product/data docs must be updated before implementation.
4. `../skills/create-implementation-plan/SKILL.md`

## Output
- Context summary.
- Decision-impact report.
- Required documentation updates.
- Epic describing the approved product/technical scope when one does not already
  exist, using the `EPICxxx` naming convention.
- Ordered implementation plan.
- The single repository-wide `docs/epics/execution-report.md`, updated with the
  active epic plan, ordered delivery slices, material decisions and risks,
  validation/quality strategy, planned integrated scope, authorization state,
  and normal commit/push behavior. Never create a per-epic execution report.
- One explicit request for authorization to execute the development plan.

## Stop conditions
Stop on unresolved business ambiguity, conflicting active records, or a required
durable decision that has not been made.

## Completion boundary
Do not implement production code. After presenting all planning outputs, stop
and request development authorization. If authorization is granted, the full
lifecycle may continue at Workflow 02 without another planned approval pause
until the Workflow 05 real integrated-test gate.

## Source control

Planning output remains in the worktree for human review before development is
authorized. Never stage, commit, or push design, context, decision-record,
backlog, epic, execution-plan, or report changes as part of this workflow. Once
the user authorizes the presented development plan, those reviewed artifacts
may be included in the first coherent development commit under the durable
authorization defined by `AGENTS.md`.

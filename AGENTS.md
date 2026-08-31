# AGENTS.md

## Project context

Before planning, designing, or changing the product, read the relevant files in
`docs/product/`.

Project identity:

- Name: `<PROJECT_NAME>`
- Purpose: `<PROJECT_PURPOSE>`
- Current delivery scope: `<CURRENT_SCOPE>`
- Explicit non-goals: `<NON_GOALS>`

Do not invent product behavior. Preserve human approval at consequential
boundaries, prefer deterministic enforcement of critical rules, and keep the
current delivery scope narrow.

## Required context

Before planning or implementing a non-trivial change, inspect the relevant
documentation in:

- `docs/README.md` — documentation map and canonical-source guidance.
- `docs/product/` — product vision, requirements, flows, and constraints.
- `docs/bdr/` — Business Decision Records.
- `docs/adr/` — Architecture Decision Records.
- `docs/DECISIONS.md` — resolved and open decision register.
- `docs/engineering/` — engineering standards and development practices.
- `docs/database/` — logical data models, when data is involved.
- `ai-operation/workflows/` — reusable feature-delivery workflows.
- `ai-operation/skills/` — reusable capabilities invoked by workflows.

Read only the context relevant to the requested change, then expand when
dependencies, uncertainty, or conflicts appear. Check related open decisions
before non-trivial design or implementation work. Never silently resolve one.

## Decision records

Read relevant BDRs before deciding product behavior, business rules, policy,
scope, user workflows, market assumptions, autonomy boundaries, or operating
behavior.

Read relevant ADRs before deciding architecture, service boundaries,
technologies, databases, messaging, APIs, infrastructure, cloud, security,
deployment, or runtime behavior.

Follow accepted or otherwise active records. Treat superseded, deprecated, or
rejected records as historical context only. When an active decision must
change, add a new record that explicitly supersedes it; do not rewrite history.
Report conflicts between active records instead of guessing.

Use:

- a BDR for durable business, product, policy, scope, or workflow decisions;
- an ADR for durable architecture, technology, integration, data-platform,
  infrastructure, security, deployment, or runtime decisions;
- an engineering standard for a recurring development practice;
- `docs/database/` for changes to logical entities and relationships.

Do not create a durable record for a local, reversible implementation detail.

## Engineering and data standards

Engineering standards in `docs/engineering/` are mandatory unless an explicit,
reviewed change updates them. Do not weaken standards to simplify delivery.

Read the logical data model before changing persistence, entities,
relationships, migrations, repositories, or database-facing APIs. The logical
model expresses intent; versioned migrations are the source of truth for the
implemented physical schema. Do not silently alter the logical model as part of
an unrelated feature.

## AI operation

Workflows define when and in which order activities happen. Skills define how a
reusable capability is performed. The default sequence is:

1. `01-design-and-context-update.md`
2. `02-feature-implementation.md`
3. `03-quality-gates.md`
4. `04-ai-code-review.md`
5. `05-integrated-functional-tests.md`, when applicable
6. `06-finalization.md`

When the user requests one workflow, execute only that workflow, use its skills,
and stop at its completion boundary. Continue to later workflows only when the
user requests a complete lifecycle or explicitly asks to continue.

Skills must remain feature-agnostic, state inputs and outputs, define validation
and stop conditions, follow project decisions and standards, and avoid
duplicating durable business or architecture decisions.

## Authorization gates

The complete lifecycle has at most two planned human authorization gates:

1. Workflow 01 presents the feature scope, execution plan, decision impact,
   risks, validation strategy, integrated-test scope, and source-control
   behavior, then requests authorization to execute development.
2. Workflow 05 prepares the integrated suite, then requests authorization
   immediately before its first consequential real-boundary execution.

A planning or design request does not authorize production implementation,
commits, or pushes. Leave planning artifacts in the worktree for review.

Authorization of the Workflow 01 plan grants durable permission for its
planned implementation, quality gates, review fixes, integrated-test
preparation, and normal non-destructive commits and pushes. It also covers
finalization after the required integrated suite succeeds. Do not ask again for
each slice, fix, commit, or push.

The Workflow 05 confirmation covers the planned real-boundary suite and its
normal defect-fix, revalidation, and retest loop. Ask again only if the external
scope or consequences materially expand.

If no consequential real-boundary test applies, document why and continue from
Workflow 04 to Workflow 06 under the development authorization.

Never amend, squash, force-push, open a pull request, deploy, or publish a
release unless the user separately requests that exact action.

## Source control

Keep commits coherent and report hashes at checkpoints. Stage only files that
belong to the completed slice and preserve unrelated user changes. A standalone
implementation request that did not follow Workflow 01 does not grant
source-control permission; leave validated changes in the worktree unless the
user authorizes publication.

## Source priority

When guidance conflicts, use:

1. The user's explicit current request.
2. Active BDRs for business, product, policy, and scope decisions.
3. Active ADRs for architecture and technical decisions.
4. Relevant product documentation.
5. Applicable engineering standards.
6. Relevant logical data-model documentation.
7. The active workflow.
8. Skills invoked by that workflow.
9. Existing implementation and tests.
10. Clearly stated implementation assumptions.

A workflow or skill cannot override a BDR, ADR, product constraint, or
engineering standard.

## Full feature lifecycle

1. Execute Workflow 01 and stop for development authorization.
2. After authorization, implement coherent slices with tests and checkpoints.
3. Run quality gates; fix and rerun failures without an approval pause.
4. Perform independent AI review; apply findings and rerun affected gates.
5. Prepare integrated tests when applicable and stop immediately before the
   first consequential real-boundary execution for explicit authorization.
6. After authorization, run the suite, fix defects, rerun affected quality and
   review checks, and retest until it passes.
7. Finalize documentation and delivery without another planned approval pause.

Return to Workflow 01 when a missing business/architecture decision or an
active-record conflict appears. Return to Workflow 02 for implementation,
quality, review, or integrated-test defects.

Outside the planned gates, continue automatically unless blocked by an unsafe
or destructive action, missing durable decision, material expansion beyond the
approved plan, or irrecoverable external-state failure.

## Implementation principles

- Keep changes traceable to product documentation, BDRs, ADRs, standards, the
  data model, workflows, or skills.
- Prefer simple, reversible implementations when no durable decision is needed.
- Do not invent missing business requirements.
- Do not add unnecessary abstractions, infrastructure, frameworks, services, or
  dependencies.
- Keep business behavior out of controllers, transport DTOs, persistence
  entities, repositories, and configuration.
- Preserve unrelated worktree changes.
- Never claim a check passed unless it was executed successfully.
- Never expose secrets or sensitive data in source, tests, logs, documentation,
  commits, or tool output.

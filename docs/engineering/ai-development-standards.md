# AI-Assisted Development Standards

## Principle
The project is intentionally developed with AI coding agents, but AI-generated code is held to the same or higher engineering bar as human-generated code.

## Agent Workflow
For non-trivial work, an agent must:
1. Read relevant product documentation.
2. Read relevant BDRs.
3. Read engineering standards.
4. Read relevant ADRs.
5. Inspect affected code/tests.
6. Produce a concise implementation plan.
7. Implement in small, reviewable changes.
8. Run relevant tests and quality checks.
9. Review its own diff for correctness, security, unnecessary complexity, and documentation impact.

## Skills and Workflows
- Reusable engineering procedures should become version-controlled skills/workflows.
- Skills must state inputs, outputs, constraints, validation, and failure conditions.
- Agents and subagents must use repository-defined workflows instead of inventing incompatible processes per feature.

## Guardrails
- Do not invent business requirements.
- Do not silently create architectural decisions.
- Do not add major dependencies without justification.
- Do not commit secrets.
- Do not claim tests passed unless they were actually executed.
- Prefer deterministic tooling for deterministic tasks.

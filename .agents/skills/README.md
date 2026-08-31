# AI Skills

Skills are reusable, feature-agnostic capabilities used by workflows.
They follow the [open Agent Skills standard](https://agentskills.io/specification)
and live in `.agents/skills/<skill-name>/SKILL.md` so compatible agents can
discover them while repository workflows can still invoke them explicitly.

A workflow answers **what sequence to execute**.
A skill answers **how to execute one capability**.

## Initial skills

1. [Load Relevant Context](load-relevant-context/SKILL.md)
2. [Detect Decision Impact](detect-decision-impact/SKILL.md)
3. [Create Implementation Plan](create-implementation-plan/SKILL.md)
4. [Implement Java Feature](implement-java-feature/SKILL.md)
5. [Create Unit Tests](create-unit-tests/SKILL.md)
6. [Create Functional Tests](create-functional-tests/SKILL.md)
7. [Run Quality Checks](run-quality-checks/SKILL.md)
8. [Review Code](review-code/SKILL.md)
9. [Apply Review Findings](apply-review-findings/SKILL.md)
10. [Prepare Design Documentation](prepare-design-documentation/SKILL.md)
11. [Sync Implementation Documentation](sync-implementation-documentation/SKILL.md)
12. [Run Integrated Functional Tests](run-integrated-functional-tests/SKILL.md)

Keep skills small enough to be independently reusable.

Add domain-specific skills only in the adopting project. A reusable skill must
not encode one product's actors, providers, entities, or business rules.

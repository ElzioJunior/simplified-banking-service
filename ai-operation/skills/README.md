# AI Skills

Skills are reusable, feature-agnostic capabilities used by workflows.

A workflow answers **what sequence to execute**.
A skill answers **how to execute one capability**.

## Initial skills

1. [Load Relevant Context](01-load-relevant-context.md)
2. [Detect Decision Impact](02-detect-decision-impact.md)
3. [Create Implementation Plan](03-create-implementation-plan.md)
4. [Implement Java Feature](04-implement-java-feature.md)
5. [Create Unit Tests](05-create-unit-tests.md)
6. [Create Functional Tests](06-create-functional-tests.md)
7. [Run Quality Checks](07-run-quality-checks.md)
8. [Review Code](08-review-code.md)
9. [Apply Review Findings](09-apply-review-findings.md)
10. [Prepare Design Documentation](10-prepare-design-documentation.md)
11. [Sync Implementation Documentation](11-sync-implementation-documentation.md)
12. [Run Integrated Functional Tests](run-integrated-functional-tests/SKILL.md)

Keep skills small enough to be independently reusable.

Add domain-specific skills only in the adopting project. A reusable skill must
not encode one product's actors, providers, entities, or business rules.

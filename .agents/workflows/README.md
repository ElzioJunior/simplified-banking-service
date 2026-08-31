# Development Workflows

Reusable development workflows live here as a repository convention. They
orchestrate open Agent Skills from `../skills/`; the Agent Skills standard
defines skill packaging and discovery, not a separate workflow format.

Current sequence:

1. [Design and Context Update](01-design-and-context-update.md)
2. [Feature Implementation](02-feature-implementation.md)
3. [Quality Gates](03-quality-gates.md)
4. [AI Code Review](04-ai-code-review.md)
5. [Integrated Functional Tests](05-integrated-functional-tests.md)
6. [Finalization and Documentation](06-finalization.md)

The full feature lifecycle has at most two authorization gates. Workflow 01 presents
the epic, execution plan, and execution report, then asks once for development
authorization. Workflow 05 asks once immediately before the first consequential
real-boundary integrated execution. There are no planned approval pauses
between those gates or after the integrated-test authorization.

Planning workflows never stage, commit, or push their artifacts before the
development authorization. That authorization grants durable permission for
the planned implementation and its normal, non-destructive commits and pushes,
including publication of the reviewed planning artifacts, as defined by
`AGENTS.md`.

When applicable, Workflow 05 is the final behavioral gate. A defect found there returns to
implementation and requires the affected quality gates and review to be rerun
before another integrated execution. The one integrated-test authorization
covers this correction and retest loop. Workflow 06 begins only after the
required integrated suite passes, so its documentation reflects the final
behavior.

If the project has no consequential real-boundary suite, the execution plan
must explain why Workflow 05 is not applicable and may proceed from Workflow 04
to Workflow 06 under the original development authorization.

Project-specific standalone workflows may be added after `06` when they are
governed by the same decisions and standards.

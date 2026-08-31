# Definition of Done

A change is done when, as applicable:

- Business behavior matches the relevant product docs and BDRs.
- Architecture complies with accepted ADRs.
- Code complies with engineering standards.
- Automated tests cover the new/changed behavior.
- Unit-test coverage remains at or above the required threshold for eligible code.
- Relevant functional tests pass.
- No known security-sensitive data is exposed.
- Logging and errors are actionable and do not leak secrets.
- Public/API behavior is documented when changed.
- A new ADR/BDR is added when the change introduces a durable architectural/business decision.
- The implementation has been reviewed for unnecessary complexity and dead code.
- The normal execution flow is understandable through explicit names,
  cohesive responsibilities, and cognitively simple structure.
- Non-obvious rationale, invariants, contracts, side effects, failure behavior,
  and architectural/external boundaries have accurate comments or JavaDoc when
  code alone cannot communicate them safely.
- Changed behavior has not left stale, contradictory, speculative, or
  sensitive comments/JavaDoc behind.
- Methods with light or greater complexity explain in JavaDoc what they do and
  why they exist; application-owned object properties have concise
  documentation.
- Every added or changed automated test documents the behavior or failure it
  proves and why the scenario matters.

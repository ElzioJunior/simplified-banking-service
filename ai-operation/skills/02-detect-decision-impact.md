# Skill — Detect Decision Impact

## Purpose
Determine whether a requested change requires documentation/decision updates.

## Inputs
- Context summary.
- Requested feature/change.

## Procedure
Classify each durable impact:

### BDR
Use when the change modifies:
- product behavior;
- business rule;
- scope;
- product or operating policy;
- workflow;
- autonomy/commercial policy.

### ADR
Use when the change modifies:
- architecture;
- technology;
- service boundaries;
- messaging;
- persistence strategy;
- API/integration;
- security;
- deployment/runtime.

### Engineering Standard
Use when the change establishes or modifies a recurring engineering practice.

### Data model
Update `docs/database/` when logical entities, attributes, relationships, or
ownership change.

### Product docs
Update when user journey, flow, delivery scope, behavior, or product constraints
change.

Do not create durable records for local/reversible implementation details.

## Output
A decision-impact report:
- documents to create/update;
- documents not affected;
- decisions that remain open.

## Stop conditions
Stop if the change contradicts an active record and the user has not explicitly
requested a new/superseding decision.

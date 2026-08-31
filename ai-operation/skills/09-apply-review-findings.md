# Skill — Apply Review Findings

## Purpose
Fix actionable code-review findings without expanding scope unnecessarily.

## Inputs
- Review report.
- Current implementation.

## Procedure
1. Fix all BLOCKER findings.
2. Fix all HIGH findings.
3. Fix MEDIUM findings unless consciously deferred with reason.
4. Apply LOW findings only when beneficial and low-risk.
5. Keep fixes narrowly scoped.
6. Add/update tests when findings expose missing behavior.
7. Rerun affected quality checks.
8. Re-review changed areas when fixes are substantial.

## Output
- Updated implementation.
- List of findings fixed.
- Explicitly deferred findings with rationale.
- Updated test/quality results.

## Completion condition
No unresolved BLOCKER or HIGH finding remains.

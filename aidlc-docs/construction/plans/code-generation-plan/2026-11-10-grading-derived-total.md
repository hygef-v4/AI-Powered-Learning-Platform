# Deterministic AI proposal totals

Resume the authorized grading bug fix. Latest proposal version7 still fails TOTAL validation. Provider output duplicates an arithmetic result already available from rubric item scores. Existing normalization derives absent totals and strict validation checks all item scores afterward.

- [x] Trace provider schema, callback merge/normalization and validator; confirm latest failure without modifying grades.
- [x] Two RED failures across12 cases reproduce the redundant provider-total requirement. Coverage includes empty and non-empty rubric paths and5 invalid-score guards.
- [x] Exclude provider total and clarify instructions; reuse existing deterministic calculation. Preserve full MCP proposal contract and validation.
- [x] Final349 AI/grading/diagram/architecture tests pass0failures/errors/skips. Local API/worker refreshed and health200 confirmed. One live Gemini proposal READY7.50 matches12 items and section totals2+3+2.5; UI agrees. Saved grade/state/version/submission hash unchanged.

Scope: existing provider prompt/schema and tests. No dependencies, migration, UI or scoring-policy change. Security: locked rubric validation and authorization retained. Resiliency: deterministic totals remove model arithmetic dependency; provider errors unchanged. PBT disabled. No subagents, official grade actions, commit/push or VPS deployment.

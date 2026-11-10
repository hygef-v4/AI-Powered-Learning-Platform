# Source grounding correction

The earlier warning-only implementation still exposed model questions following an unrelated user prompt. User requests source-based questions; warnings alone do not satisfy this behavior.

- [x] Reproduce mismatched output becoming READY, then require it remain empty/PENDING until source-only regeneration.
- [x] Check both request relevance and generated-question support flags; model assessments remain fallible. Legacy outputs remain readable.
- [x] Persist mismatch warning and retry marker instead of exposing rejected questions. After original delivery commits, submit at most one new AI task with default instructions only, same source IDs/count/type/difficulty/source policy, current authorization/text validation and normal quota checks.
- [x] Serialize retry under a draft row lock, use a deterministic request reference, enqueue after commit and catch submission failures after rollback. Owner polling can resume an interrupted retry; another viewer must not trigger a billed task. Second mismatch fails closed with no proposal.
- [x] Add service, malformed-output, authorization/recovery and PostgreSQL row-lock regressions; run related tests/package and frontend warning tests. Update U10 documentation and applying notes.

No migration or new endpoint. Conditional regeneration can consume one additional AI task. No real provider request or UI acceptance claimed. Prior incorrect READY drafts remain historical and must be discarded/recreated; do not silently rewrite user proposals.

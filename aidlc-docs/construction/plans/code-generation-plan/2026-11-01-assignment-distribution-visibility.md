# Assignment distribution visibility — 2026-11-01

## Approved scope

User requested updating AI-DLC and code after proposing removal of the manager's class toggle and moving control to the assigned teacher for each assignment in each class. Existing unrelated workspace UI changes are preserved. U05 class administration relinquishes this setting; U09 owns persistence and authorization; U07 exposes it in assignment settings. Traceability: UC29, assignment settings, US-RPT-001, BR-U09-62.

Visibility defaults off for every assignment/class pair, including existing classes. Only GRADED assignments qualify. Published subject assignments remain independently configurable in each class. The assigned teacher controls visibility independently of permission to edit assignment content. Archived classes and retired assignments cannot change the preference. Existing enrollment checks, published-score filtering, minimum 20 usable scores and suppression of buckets below five remain mandatory. No Quiz, Practice, AI or grading-score behavior changes.

## Contract

GET and PUT `/api/v1/classes/{classId}/assignments/{assignmentId}/grade-distribution-visibility` return `enabled`, `version`, `canEdit`. PUT accepts `enabled` and `version`; optimistic conflicts reject stale writes. Missing rows mean disabled/version zero. New grading-owned persistence is keyed by assignment and class. The old manager mutation endpoint is removed; the legacy class column is retained as dormant historical storage and never authorizes student distribution access. Existing enabled class flags are deliberately not copied.

## Execution

- [x] 1. Inspect current U05/U07/U09 implementation, callers, contracts and privacy rules; record the approved decision and preserve pending UI changes.
- [x] 2. Implement backend persistence, teacher authorization, endpoint and student gate; test defaults, assignment/class isolation, privacy and concurrent writes.
- [x] 3. Remove manager UI and wire a grading-owned assignment settings section, including subject assignment class context, error handling and focused tests.
- [x] 4. Synchronize current requirements, stories, unit designs and API documentation without rewriting historical evidence.
- [x] 5. Review integration, run relevant backend/frontend verification and production build, and record results and practical limits. See [verification results](../../build-and-test/2026-11-01-assignment-distribution-visibility-results.md).

Security and resiliency scope is retained from the current AI-DLC state. Property-based testing remains disabled. No dependencies, deployment, commit or push are requested.

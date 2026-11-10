# U09 repeated finalize/publish feedback — 2026-11-10

Continue the authorized UI bug fix without subagents. Preserve backend state/permission/version guards and official grades.

1. [x] Reproduce repeat actions and mixed-state selection in frontend regression tests.
2. [x] Filter finalize/publish/AI candidates using raw submission state in both batch-selection callers. Show row state and disable actions with no eligible candidates.
3. [x] Reconcile INVALID_GRADE_STATE using a fresh read only for affected results. Treat already finalized/published grades as skipped, never finalize an already published grade, preserve errors and original-version checks.
4. [x] Show readable Vietnamese outcomes and a close action; verify grading tests, lint/types/build and actual authenticated read-only browser controls without writing live official grades.

Scope: shared batch dialog, gradebook/submission batch controls and tests. No backend change, idempotency/authorization weakening, live finalize/publish, restart, commit or push. Security/Resiliency retained; PBT disabled.

[Results and limits](../../build-and-test/2026-11-10-grading-repeat-actions.md).

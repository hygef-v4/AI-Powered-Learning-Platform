# U09 screenshot UI remediation — verification 2026-10-25

Plan: [2026-10-25-u09-ui-bugfix-plan.md](../plans/2026-10-25-u09-ui-bugfix-plan.md).

## Changes

- Evaluation route renders only GradingWorkspacePage; the teaching parent retains the shared session/role guard/shell.
- Removed evaluation UUID from AI proposal assistive text; kept prop/request/callback contracts.
- Submission page displays assignment title and class code/name from the existing authorized assignment detail GET. Metadata uses a separate lifecycle, validates assignment/class/scope and rejects stale route responses. Error/retry is isolated from submission rows and filters.
- Shared Field/Select controls replace local styles while preserving status/late queries, pagination resets and batch ID/version behavior. Long title/context text can wrap at narrow widths.
- No backend, grading algorithm, dependency, OpenAPI, database, AI provider or publication changes.

## Tests and commands

Working directory: frontend. Tests use a private writable TEMP/TMP and --maxWorkers=1 to avoid the temporary cache ENOENT observed during planning.

| Verification | Result |
|---|---|
| Route composition and UUID tests before fixes | 2 expected failures; proves nested shell/UUID symptoms |
| Route/manual/AI tests after fixes | 16/16 passed |
| Submission context/filter tests before fix | 14 expected failures; missing context |
| Submission context/filter tests after fix | 14/14 passed |
| npm test -- src/features/grading src/features/notifications/Notifications.test.tsx --maxWorkers=1 | 6 suites, 43/43 passed |
| npm test -- --maxWorkers=1 | 84 suites, 501/501 passed, exit 0 |
| npm run typecheck | Passed, exit 0 |
| npm run lint | Passed, exit 0 |
| npm run build | Passed, exit 0; production route /teaching/evaluations/[evaluationId] and submissions route generated |
| git diff --check | Passed |

New coverage: one shell with preserved roles/ID, no evaluation UUID in DOM text, assignment/class labels, pending and 403/404/503 metadata, retry isolation, mismatched assignment/class/scope, empty/long/HTML-looking titles, response races and immediate hiding after route changes, metadata stability across filter/page/list errors, unselectable unsubmitted rows and batch current versions.

## Runtime acceptance limits

- IAB opened http://localhost:8088/login successfully; no authenticated session was present. No credentials were searched or changed.
- The shell could not control Docker: permission denied on dockerDesktopLinuxEngine. The earlier empty listener inventory was sandbox-scoped and is not evidence the host app was stopped.
- Running Docker frontend revision has not been verified or updated. Production browser tests for Teacher/Subject Manager, active SSE count/cleanup, unauthorized Student, and desktop/390px remain pending against the updated frontend.
- These are source/test results, not completed live acceptance. No AI request, grade publication, database write, service replacement, commit, push or deployment.

## Review

Self-reviewed final source and tests. Parent role guard retained; metadata scope and async ordering checked; all mutation handlers unchanged. No deferred source findings. Security reduced subset is preserved by design and tests; runtime compliance not certified. PBT remains disabled; no global AI-DLC stage advancement.

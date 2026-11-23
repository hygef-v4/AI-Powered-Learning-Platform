# Repeated finalize/publish actions — 2026-11-10

## Fix

The gradebook batch selector previously offered every action for every selected submission. The shared publish dialog also treated every INVALID_GRADE_STATE as an unfinalized draft and tried finalizing again, including already published grades. Results exposed raw outcome/error codes.

Both gradebook and submission-detail selection now use one raw-state eligibility rule: AI for PENDING/DRAFT, finalize for DRAFT, publish for DRAFT/FINALIZED. Ineligible items are excluded from mixed requests and buttons disable when none remain. Gradebook selection shows the raw submission's readable state, distinguishing “Đã chốt” from “Đã công bố”.

If the server still returns INVALID_GRADE_STATE because the view is stale, only affected evaluations are read again. Already FINALIZED/PUBLISHED results become skipped notices as appropriate; published or unconfirmed grades never enter automatic finalize. The existing draft-to-finalize-to-publish workflow retains original versions and only publishes successfully finalized items with returned versions. Backend state, permission, review and version guards are unchanged.

Outcomes/errors display readable Vietnamese text. All-completed batches show a notice that preserves already-completed grades; mixed counts separate newly processed and previously completed items. Failed results have a usable “Đóng” button. Genuine errors, incomplete responses and explicit retry after a whole-request failure remain visible. AI queued outcomes remain visible as “Đã gửi yêu cầu AI”.

## Verification

- Regression first: seven expected failures / 23 existing tests passed.
- Final grading scope: **167 tests / 18 files passed**. Covers FINALIZED/PUBLISHED repeat actions, unavailable fresh-state reads, mixed successes/skips, exact original versions and successful-finalization-only publishing, candidate states and mixed submission-page filtering.
- Scoped ESLint, typecheck, production build and scoped diff whitespace checks passed. First integration run exposed the intentionally translated AI queued label and duplicate header/footer close controls in the new test selector; updated the label expectation and test selector before final passing checks.
- Authenticated Chrome: opened the actual class gradebook in a separate tab, expanded batch controls and selected U05 Mock Student, whose state was “Đã công bố”. Verified AI, finalize and publish controls all disabled. No finalize/publish confirmation or mutation was sent.
- Existing CSP console reports concerning inline styles appeared on navigation; no CSP change was made or broader CSP fix claimed. The relevant controls and state checks worked.

Logs: `.tmp/grading-repeat-red.log`, `.tmp/grading-repeat-tests.log`, `.tmp/grading-repeat-lint.log`, `.tmp/grading-repeat-types.log`, `.tmp/grading-repeat-build.log`.

## Acceptance

1. Select an already published student: both finalize and publish remain disabled.
2. Select a finalized student: finalize is disabled and publish remains available.
3. Mix draft/finalized/published students: each action includes only eligible items, retaining current server versions.
4. If a stale view submits an already-completed action, the dialog reads the new state and explains that the grade was already finalized/published and remains unchanged. A failed fresh read does not trigger an automatic finalize.

No backend change, migration, runtime restart, live official-grade mutation, commit/push or subagents. Stale/race cases used mocked API regressions; no actual repeated finalize/publish was performed on live grades. Whole frontend/backend suites were not repeated. Security/Resiliency retained; PBT disabled.

# Student assignment visibility — diagnosis only

User reported that Student Demo 1 can open the hospital group assessment from a notification, but the class assignment menu has zero items in all three tabs. Evidence supplied as screenshot; exact production assignment URL/status is not yet verified. Asked for the full link asynchronously. No production data changed.

## Reproduction

Mounted the real StudentAssignmentsPage with workApi.list returning one group assignment for the selected class, no submission, and a group document. Ran:

`npx vitest run tests/features/assignmentwork/pages/studentListVisibility.probe.test.tsx --maxWorkers=1`

- OPEN row: appears in at least one tab — PASS.
- Identical row with only status=CLOSED: absent from Chưa làm, Đang làm and Đã nộp — FAIL (`expected false to be true`).
- Two cases executed, one pass/one fail. This verifies the frontend omission, not the exact deployed record's state.
- Log: `.tmp/student-list-visibility-probe.log`; temporary harness retained at `.tmp/studentListVisibility.probe.test.tsx`. Removed the diagnostic failing test from the discovered frontend/tests tree; no broken test added to the suite.

## Cause and competing explanations

1. Confirmed source defect: StudentAssignmentsPage.inTab delegates unsubmitted/in-progress classification to upcoming/doing; both reject non-OPEN assignments, while the submitted branch requires submittedCount > 0 or group.submitted. CLOSED + unsubmitted therefore belongs to no tab.
2. Notification pointing to a different class: not demonstrated. Both list and detail use class membership/scope checks; production link is still needed to rule this out for the specific screenshot.
3. Backend omitting all group work from list: not supported by source; StudentAssignmentService.list explicitly includes GROUP assignments and OPEN/CLOSED rows, with optional group progress.

WorkAccess.visible explicitly permits OPEN and CLOSED details. AssignmentRepository.closeDue changes OPEN to CLOSED after the configured lateUntil/closesAt plus the grace cutoff. This explains how a previously valid notification can still open the detail while the frontend list loses the row. The screenshot appears to show a 00:00 13/11 deadline, consistent with this scenario; this is an inference until the deployed assignment status is inspected.

Related label defect: rowStatus maps an unsubmitted GROUP to NOT_STARTED even when its assignment is CLOSED, so the detail shows Chưa làm instead of communicating that submissions are closed.

## Proposed repair scope

- Give closed unsubmitted assessments a discoverable list category/status, for example Đã đóng / Chưa nộp, without treating them as actionable work.
- Keep the active-work predicates used by ClassTodoCard separate from historical list visibility; merely removing the OPEN guard from the shared upcoming helper would change class summary behavior.
- Align list/detail labels and retain scope checks. Cover group and individual unsubmitted CLOSED/MISSED rows, submitted closed rows, active group drafts and focus-based return navigation.

No product source fix, schedule change, UI redesign, backend restart, commit, push, PR or deploy performed. Security/resiliency scope unchanged; PBT remains disabled. No full regression/build required for this diagnosis-only document delta.

## Approved repair — 2026-11-13

User subsequently requested “giúp tôi sửa lỗi đấy $ponytail:ponytail”. Implementation plan: [student visibility](../plans/code-generation-plan/2026-11-13-student-assignment-visibility.md).

- Added Đã đóng tab for CLOSED/MISSED unsubmitted rows from the authorized API list, including group assignments with or without a group document. Closed submitted rows stay in Đã nộp.
- Shared closed/unsubmitted classification also supplies Hết hạn label to the existing detail page via rowStatus. Active upcoming/doing predicates, class todo statistics, API and permission rules are unchanged.
- Existing URL values remain compatible. tab=closed works with filtering and pagination; focus-based return from a notification detail selects the closed tab. Closed rows do not show an actionable countdown.
- RED: new regression cases failed before source changes (9 failures, 7 passing controls across 2 files; `.tmp/student-visibility-red.log`).
- GREEN: all assignmentwork tests pass, 40 tests/8 files; `.tmp/student-visibility-green.log`. Cases cover the original omission, closed group labels, individual MISSED rows, submitted groups with changedSinceSubmit, focus return and URL state.

Final verification:

- Full frontend: **1047 tests / 150 files pass** with two workers; `.tmp/student-visibility-full-tests.log`.
- Full ESLint passes; `.tmp/student-visibility-lint.log`.
- Standard `npm run typecheck` fails on the previously known stale `.next/types` routes for removed admin/mcp, ai/usage and nested submissions pages, not on changed source. No cache deleted or permanent config changed. Isolated TypeScript passes against all src/tests and the existing fresh `.next-ai-chat-files/types` output; `.tmp/student-visibility-types-isolated.log`. The first external temporary config had a type-library resolution error; corrected its location, reran successfully and removed the temporary config.
- Diff whitespace check passes. No backend change: no new backend test/restart needed. No browser/production data verification or production build repeated for this classification-only delta.

Exact production assessment status still unverified without its full link; the reproducible source defect is fixed locally, not deployed. No commit/push/PR or deployment performed in this repair turn. Existing authorization and active-work predicates remain unchanged; PBT stays disabled.

# U09 gradebook and class overview implementation plan

> Owner: existing Gemini session in Orca, currently Gemini4 Argon (Medium). Implement sequentially in this checkout. No new agents, worktrees, commit, push, merge, deployment or automatic UI acceptance. The user will test the UI after receiving the completion report and manual guide.

## Authorization

User: "lập plan sau đưa cho gemini làm, code xong thì báo cao tôi tự test ui, đưa guide test đầy đủ".

This authorizes implementation of the gradebook and class-information layout proposed in `../2026-10-25-u09-feedback-class-layout.md`. Preserve the already implemented four-second action notices. Do not repeat design approval questions or stop after another plan; implement, run automated checks, then report explicitly `Code complete / awaiting user UI testing` with the manual guide.

## Mandatory startup checkpoint

- Workspace `F:/code/git/AI-Powered-Learning-Platform`, branch `mcp-test`. At planning time HEAD changed concurrently to `ea0d2c8e` (merge PR22); re-read AGENTS.md, CLAUDE.md, frontend/AGENTS.md, installed Next docs, git status, staged and unstaged diffs before edits. Do not assume the older b1abc00e tree.
- Current index contains the prior toast change/tests/docs, and `aidlc-docs/audit.md` is unmerged (UU). No MERGE_HEAD/CHERRY_PICK_HEAD/REVERT_HEAD was present when checked. Root deliberately leaves index/conflict untouched. Do not abort, reset, stash, switch branch, clean, stage or commit to make it disappear. Never choose ours/theirs wholesale. Resolve the documentation conflict by retaining both distinct histories only after verifying no concurrent writer owns it; if ownership is unclear, report that exact blocker and continue isolated/new test/plan work that cannot overwrite it. Do not leave conflict markers in the final delivered source/docs or claim a clean tree. Ask only if an actual ownership conflict cannot be resolved safely.
- Existing staged toast files: BatchActionDialog.tsx, ai-source-integration.test.tsx, batch-action-feedback.test.tsx and related state/audit/plan/results. Preserve all staged and unstaged user changes; do not overwrite them with HEAD copies. This request does not grant Git integration actions.
- Prior toast checks: focused68 tests passed (grading58 + subject10), lint/typecheck passed. The previous full run had531pass/1SubjectListPage focus assertion failure; isolated rerun passed. Establish a new baseline at the actual current HEAD; never relabel these old results as a fresh pass.
- Read the manual guide at `../../build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md`. Keep it synchronized with final labels/behavior; do not fill user verdicts.

## Selected design

Use existing indigo tokens and shared Button, Pill, inputs, Pagination and Dialog. No new framework, fonts, dependency, decorative images, backend/API changes, score/permission semantics or new routes. Existing screenshots in the conversation and `.tmp/u09-design-audit/`/`.tmp/u09-ui-2026-10-25/` are visual context; inspect available files. No alternate theme or image-generated mock is required for this agreed, scoped existing-code layout.

### 1. Gradebook: assignment-first plus an optional matrix

- ClassGradebookTab defaults to `Theo bài tập`. A labelled assignment selector shows the existing full assignment titles, once, above the table. Show the selected full title as a wrapping heading/link to its submissions. Select the first available assignment if none is selected; derive a safe fallback if refreshed data removes a selected ID. No invalid/undefined query and no phantom assignment.
- Focused columns: `Sinh viên`, `Điểm`, `Trạng thái`, `Thao tác`. Match cells by assignmentId, not array position. Existing grading status labels/pills and exact decimal formatting remain. No evaluation -> `—` plus actual state, no broken review link. Score0 is a real score, not absence.
- A two-button mode switch `Theo bài tập` / `Tổng hợp` uses semantic buttons with aria-pressed or proper tabs. Switching mode or selected assignment preserves student search and page; changing search resets page1. A different class resets class-scoped selection/query/page safely.
- Matrix: sticky student column approximately220px, equal160px assignment columns, sticky header inside a bounded table scroller (max-height around65vh). Set opaque backgrounds and correct z-index for the corner/header/sticky cells. Horizontal/vertical overflow is confined to the table, not the entire page.
- Each assignment header has an ordinal `Bài 1`, full title clamped to two lines and the existing link. Provide a keyboard/touch-accessible full-title alternative such as a compact `Danh mục bài tập` disclosure listing ordinal + full title/link; native title hover alone is insufficient. Do not mutate stored names, reorder grades or invent grades to fit columns.
- Matrix cells align by assignmentId even when grade arrays differ in order; handle a missing grade safely. Full list titles and student names can be long or duplicated: use UUIDs as identity, no title-based lookup.
- Compact wrapping toolbar: search, mode, assignment selector when relevant, export. Retain pagination and the rule note: GRADED only, no totals/weights, no Practice/Quiz.
- Export keeps GradebookExportDialog's explicit scope selector. Do not silently change export to current search/page or assignment selection. State scope clearly in the dialog; existing CSV/XLSX parameters/download/audit behavior remain. Tests verify the scope passed to the API.
- Loading, retryable errors, no assignments, no students, no search matches and a removed selected assignment all need explicit readable states. Preserve last-request-wins behavior of useResource. Avoid duplicating one request per assignment when all data already exists in GradebookPage.

### 2. Teacher class information: compact summary and two content regions

- Preserve existing PageHeading, back navigation, tab labels, `?tab=info` / `?tab=gradebook` initialization and role restrictions. Do not redesign the student view while changing the teacher layout.
- Summary row: `Sinh viên` from CourseClassView.studentCount, `Nhóm` from the existing group data, `Bài đang mở` from the existing open-assignment response, `Chờ chấm` from the existing progress summaries. Loading/unavailable must not be displayed as a fabricated0. Preserve the existing group semantics and ungrouped count.
- Desktop body: about one-third metadata and group details, two-thirds assignment progress. Metadata is a compact definition list with semester, class code, teacher, status and expandable long description; avoid repeating a huge subject card already identified in the header.
- Narrow/mobile order: summary cards, progress, metadata; no global overflow or hidden labels. Long Vietnamese strings wrap. Do not change app-wide sidebar/header behavior to make this page fit.
- Keep the cross-unit slot architecture: academics must not import grading/groups directly, and do not add concrete unit knowledge to shared SlotOutlet.
- Minimal composition approach: a scoped teacher-info grid in ReadOnlyClassPage with a local student summary and metadata region; render the existing teacherClassTabs outlet once. Allow info-slot registrations to emit named DOM regions through a data attribute, e.g. `data-class-overview-region=groups|open|pending|progress`, placed by the teacher-info grid's scoped CSS. ClassProgressSection may expose `layout?: "standalone" | "overview"` (default standalone) and render its counters/progress as separate siblings in overview mode. TeacherGradingSection passes overview only for the info tab. GroupCountCard may similarly expose `variant?: "default" | "overview"`, used only by the info registration. Existing consumers keep defaults. Render each provider once; no duplicate group/progress requests for multiple slot render passes. Additional unknown slot sections get a sensible full-width fallback.
- If a clearer implementation fits the same slot boundaries, document it, but do not build a new registry/service for this page. Preserve group refresh-on-window-focus and the student/group tabs' onGroupNamesLoaded behavior.

### 3. Assignment progress and distributions

- Replace oversized nested boxes with compact rows/cards showing title, individual/group type, deadline, submitted/total, pending count, themed thin progress and `Xem bài nộp`. Keep group counts labelled as groups, individual counts as students.
- Display at most5 relevant assignments initially, with `Xem tất cả (N)` / `Thu gọn`. Order pending-grading items first, then earliest known deadline, with stable ID tie-break; missing deadline sorts last. Do not silently hide assignments permanently.
- Current ClassProgressSection loads progress, gradebook assignments and open assignments. Reuse those requests. AssignmentPage exposes items/counts/hiddenCount, not totalElements; inspect actual shapes before deriving metrics. Preserve explicit limits if some assignments are hidden by authorization; do not present a visible subset as a universal total.
- For absent progress show `Chưa có dữ liệu`; denominator0 does not produce NaN/Infinity or a made-up100percent. Prefer `<progress>` with an accessible label and theme styling; do not rely on default bright green browser rendering.
- Score distributions move to a separate collapsed section `Phân bố điểm đã công bố`, with an assignment selector. Fetch only after opening and for the selected eligible assignment. Switching selection/class cannot display stale buckets. No eager distribution request for every row on class-page mount.
- Preserve existing privacy suppression/minimum counts and server results; an empty list means insufficient/no published data, not a numeric zero distribution. Keep error/retry states and no unrequested extra stats.

## Files

Expected edits:

- `frontend/src/features/grading/ClassGradebookTab.tsx` — modes, selection, toolbar, focused view, matrix, states.
- `frontend/src/features/grading/ClassProgressSection.tsx` — summary regions, compact progress, show-more, lazy distributions.
- `frontend/src/features/grading/register.ts` — pass overview layout through existing info registration.
- `frontend/src/features/academics/ReadOnlyClassPage.tsx` and `academics.module.css` — teacher-info composition/metadata/responsiveness; keep student behavior intact.
- `frontend/src/features/groups/TeacherGroupWidgets.tsx` and `register.ts` only for the scoped info summary variant, without unrelated group logic changes.
- Add `frontend/src/features/grading/gradebook-layout.test.tsx` and `class-progress-layout.test.tsx`, using actual API types and mock fixtures. Extend existing `frontend/src/features/academics/academics.test.tsx` / integration tests and focused group tests for slot composition as needed. A small pure helper is allowed when shared rendering needs it; no broad refactor.
- Keep the existing toast source/tests unchanged unless a proven integration regression requires a minimal fix. Update guide/report/state/audit with provenance; preserve both audit conflict histories.

Out of scope: backend, migrations, AI schema/gateway, credential sharing, source/model changes, grade edits/publication, new QA seeding, XML-panel removal, renaming the AI apply button, authentication, global visual theme and Git integration.

## Execution checklist

- [x] Step1 — Startup checkpoint and baseline. Verify real HEAD/index/conflict ownership, read relevant rules/docs/source, record baseline and preserve toast changes. Resolve only safe documentation reconciliation, without staging/committing or abandoning other work.
- [x] Step2 — Gradebook focused/matrix behavior. Write failing tests for mode switching, full title access, assignmentId alignment, zero/missing grade, search/page/class changes and empty/error states; implement and verify green. Include long/duplicate titles and differently ordered grade arrays.
- [x] Step3 — Class composition. Add meaningful teacher/student/slot regressions; implement scoped metadata/summary/content regions with real data and no duplicate requests. Verify student and group tabs retain behavior. No CSS-class-only test pretending to prove visual layout.
- [x] Step4 — Progress/distribution. Test ordering/show-more, no-data/zero denominator, correct group/student units, lazy fetching, selected-assignment changes, privacy/empty/error states; implement and verify green. Derive data from current API types only.
- [x] Step5 — Automated verification. Run changed grading/academics/group tests, full frontend suite, ESLint, TypeScript, isolated production build and git diff --check. Do not call provider, mutate grades or run manual browser acceptance. Avoid writing build output to active .next; use existing NEXT_DIST_DIR support, preserve tsconfig changes and clean only verified generated output. Do not blanket git checkout/reset files to revert tool output. Rerun only checks justified by changes/failure.
- [x] Step6 — Report to user. Save `aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-implementation-report.md` with changed files, exact commands/counts/exit codes, initial/final Git state, actual conflict handling, limitations, how to start app and guide link. Explicitly mark all manual UI checks pending user testing. Update guide labels/paths to match implemented code, preserve unchecked user verdicts. Report in the Gemini terminal immediately after code/checks finish; stop and wait for user UI feedback, do not continue endless screenshots/testing.

Commands, adapted only for actual installed tooling:

    npm --prefix frontend test -- src/features/grading src/features/academics src/features/groups --maxWorkers=2
    npm --prefix frontend test -- --maxWorkers=2
    npm --prefix frontend run lint
    npm --prefix frontend run typecheck
    # PowerShell, isolate output from the active dev server:
    $env:NEXT_DIST_DIR=".next-build-u09-overview"; npm --prefix frontend run build
    git diff --check

Restore only the environment variable you set. Inspect generated file diffs before reverting generated-only changes. If the known SubjectListPage focus failure recurs, record the full-run result and focused rerun honestly; do not alter unrelated tests/production behavior to manufacture green. Do not claim a full-suite pass unless the full run passed.

## Completion contract and runtime

- User-facing conclusion must say code/automated checks are ready and UI awaits their test; include guide and exact failing/skipped/unverified checks. No automatic UI screenshot loop or extra provider request.
- API/worker host terminal `term_b21e6d47-b4d9-4b91-9a56-94c2f7cf2d16`, frontend `term_0feceaf6-77ba-420a-b701-347e593d6a20`. Re-list handles/PIDs before actions; do not stop unrelated processes or start Docker worker alongside host worker.
- Class URL `http://localhost:3000/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672?tab=info`; gradebook uses `?tab=gradebook`. Teacher is u05.teacher@school.test; do not record/search credentials. User manages login/manual UI.
- Existing QA assignment aaf2897a-1deb-4987-8349-f1783b6610bc and evaluation826f5c29-b5d1-4a32-9999-9313c242d8d3 are already user-published10.00/version7 at the last read; do not reset them for tests. Original7ca08c41-e9d4-4738-946d-24c049e3d1e5 remains protected DRAFT0/version7. No DB writes or fixture seeding by Gemini.
- Security: preserve authorization/data privacy, no secrets, safe text rendering and score semantics. Resiliency: existing request ordering/retries retained, avoid request fan-out. Deployment rules N/A; PBT extension disabled. Follow the project's reduced enabled extension configuration.

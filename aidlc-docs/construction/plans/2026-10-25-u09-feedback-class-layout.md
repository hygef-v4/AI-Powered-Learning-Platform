# U09 action feedback and class layout review

## Request and scope

User requests a small self-dismissing notice after grade finalization/publication and layout proposals for the gradebook and class information screenshots. Continue directly on the clean current checkout at b1abc00e; no new agent, commit, push, merge or deployment for this change.

Implement only the clearly requested action feedback. The gradebook/class changes below are proposals, not implementation approval. Keep the initial confirmation before finalization/publication. When every requested evaluation succeeds, replace the result dialog with a compact success notice for four seconds; close immediately through its dismiss button if desired. Keep errors, empty/incomplete responses and partial failures in the existing dialog. Preserve AI batch source selection/results and refresh behavior. No backend/API/credential changes.

## Evidence and files

- User screenshot1 shows a completed publication dialog with `Bài 1: SUCCESS`; current BatchActionDialog always renders result rows inside Dialog.
- No reusable toast exists in shared UI. Use a small grading-local notice with existing success colors, check/close icons and polite live status; no dependency or global notification framework.
- Main file: frontend/src/features/grading/BatchActionDialog.tsx. A focused batch-action-feedback.test.tsx covers confirmation, successful finalization/publication, four-second cleanup, dismissal, persistent partial/error/empty results and unchanged callbacks. Update ai-source-integration.test.tsx's old success-result assertion.
- Read installed Next use-client documentation and use existing client state/effect conventions. Timer cleanup must survive rerenders/unmount and must not invoke mutation again.

## Execution

- [x] 1. Write behavior tests and observe failure for the missing success notice.
- [x] 2. Render timed success feedback only for a complete all-success finalize/publish response. Keep onDone refresh and one mutation per confirmation. Added regression for filtered list refresh removing completed rows.
- [x] 3. Run grading tests, lint and TypeScript. Actual host UI verified with an intercepted response fixture in a dedicated tab; no grade writes. Results and the unrelated full-suite focus failure recorded in the results report.
- [x] 4. Update state/audit and evidence; leave changes uncommitted.

## Gradebook proposal

Current ClassGradebookTab renders one min-w-44 column per assignment with full titles. This becomes difficult to scan as assignments/titles increase.

- Default to a focused single-assignment view with an assignment selector above the table: student, score/max, status and review action. Long assignment titles appear once in the selector/header, avoiding one long name per column. This uses existing gradebook data; do not introduce totals/weights absent from the grading rules.
- Offer a matrix as the alternate class-wide comparison view: sticky student column and header; equal 150-170px assignment columns; horizontal scrolling confined to the table. Limit visible title to two lines with a short ordinal such as Bài 1; full title remains available through accessible focus/details and the existing assignment link. Do not rename or truncate the stored assignment title.
- Place search, assignment selector and export in one wrapping toolbar; compact status pills and tabular scores. Mobile uses the focused view or contained table scrolling, with full labels available without hover.

## Class information proposal

- Compact the class identity at the top: subject/class code, semester, teacher and status. Remove redundant oversized empty space; description can expand when long.
- Below it show a summary row: students, groups, open assignments and pending grading. Use only actual data; do not invent aggregate metrics without a source.
- Desktop body: approximately one-third class metadata/group summary, two-thirds assignment progress and upcoming deadlines. On mobile stack summary then progress then metadata.
- Progress is a compact assignment list with title, deadline, submitted/total, pending count and a thin themed progress bar plus review link. Show at most the most relevant few initially with a show-more control; avoid rendering every published distribution chart inline.
- Put published score distributions behind a named assignment selector/disclosure. Preserve privacy thresholds and empty-data explanations. Existing slot boundaries for academics/group/grading must remain intact.

Design proposals are based on the user-provided screenshots and source inspection, not a full mobile/accessibility audit. Retain current indigo design tokens and use text/icons alongside state colors.

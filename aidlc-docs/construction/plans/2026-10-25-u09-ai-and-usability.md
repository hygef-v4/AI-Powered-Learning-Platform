# U09 AI failure and grading usability

## Evidence
Current submitted attempt belongs to U05-A, evaluation 7ca08c41-e9d4-4738-946d-24c049e3d1e5. Proposal #1 failed. The authenticated U06 task endpoint reports Gemini HTTP 400 (model or request parameters invalid); U09 replaced that safe diagnostic with `AI task failed`. This does not prove a bad API key or identify the exact rejected parameter. The generic output schema also leaves checks and section objects without declared properties.

Screenshot 01-workspace-before.png: raw part UUIDs, no live checklist score, small columns, manual diagram reveal, distant action buttons.

## Scope
1. Tests first for diagnostic propagation and explicit grading output schema. Preserve provider, ownership/correlation checks and final server scoring. Never automatically retry a paid task or change models.
2. UI: section names, per-section totals, live estimated total, focus mode, visible save state/actions, independent AI panel with translated states and recovery. Keep save/navigation behavior unless a separately tested flow change is justified.
3. Show safe diagram previews and a large read-only dialog; retain sanitization and original XML in an optional disclosure.
4. Tests, lint, typecheck, build, backend tests, and live read-only visual checks. No save/finalize/publish against the real grade during UI QA.

## Limits
Attached text could not be materialized: extraction is empty and the transfer helper requires os.setxattr, unavailable on Windows. Live authenticated task diagnostics supply the HTTP 400 evidence independently.

## Changes implemented
- Dynamic grading output schema declares each locked rubric UUID, allowed checklist item UUIDs, and section partId/feedback fields. Request DTOs and server-authoritative score validation remain unchanged.
- U09 preserves U06 safe diagnostics for callback and polling failures (including unknown/cancelled states), and enriches legacy generic failures on the next list read. No automatic provider retry, model change or grade mutation.
- Workspace: assignment/class context, named rubric sections, integer-cent subtotals, clamped live total, sticky save/finalize/publish controls, clear stage instructions, focus mode and consistently styled controls.
- Diagram previews appear automatically; a read-only large dialog supports scrolling. The SVG allowlist now retains safe switch containers so Draw.io fallback labels survive while embedded HTML/scripts are removed. Previews recompute if a refreshed snapshot changes SVG.
- AI state labels and recovery guidance are translated; failed suggestions remain unusable and manual checklist grading stays available.

## Audit and visual checks
| Step | Surface | Health / evidence |
| --- | --- | --- |
| 1 | Original workspace | Poor: UUID section labels, hidden diagrams, no live total. `../verification/u09-ui/01-workspace-before.png` |
| 2 | Updated workspace | Improved: clear context, named parts, preview total and sticky action bar. `../verification/u09-ui/02-workspace-after.png` |
| 3 | Diagram review | Fixed missing attribute/method labels; large read-only view and Escape dismissal verified. `../verification/u09-ui/03-diagram-detail-after.png` |
| 4 | AI source selection | Only configured personal Gemini selectable; sending remains disabled until selection. Opened and cancelled, no provider request sent. `../verification/u09-ui/04-ai-source-picker.png` |
| 5 | AI failure recovery | Explains manual grading and links to AI connections; backend needs restart to supply historical HTTP 400 detail. `../verification/u09-ui/05-ai-failure-recovery.png` |
| 6 | Submission list | Correct assignment/class, one pending submission and working grading link. No grade was saved/finalized/published. `../verification/u09-ui/06-submissions.png` |

Visible accessibility improvements: human section labels, larger checklist hit areas, a labelled live score/status, keyboard-focus styles and a dismissible diagram dialog. Mobile breakpoints, full keyboard flow and complete accessibility compliance were not assessed.

## Verification
- Regression tests first reproduced loss of the U06 safe diagnostic, generic schema fields, raw UUID labels, absence of immediate safe previews, missing SVG switch fallback text, and stale preview after snapshot refresh.
- Full backend: 1348 total, 1256 passed, 92 skipped, 0 failures/errors. Skips: database/Redis integration suites (including U09 EvaluationVersionIntegrationTest and GradingPersistenceTest) require unavailable Docker. Architecture checks passed.
- Initial parallel build/test attempts exhausted memory. Final frontend build passed with CIRCLE_NODE_TOTAL=2 (one Next page-data worker), NODE_OPTIONS=--max-old-space-size=1024. No project-wide build setting was changed.
- TypeScript and ESLint passed. Final full frontend suite: 85 test files, 512 tests passed, 0 failures.
- Live desktop UI was checked on localhost:3000; diagram labels were checked both visually and in the displayed image data. The running backend still returns the old `AI task failed` string, proving that it has not loaded the source change yet.

## Remaining runtime step
Restart the existing backend dev terminal (Ctrl+C, then scripts/run-backend.bat from the repo root), reload the grading page, and request a fresh AI proposal using the configured source. The previous failed task stays in history. HTTP 400 is confirmed, but successful provider execution with the corrected schema is not yet verified. No credentials or grading data were changed during QA. No commit or push.

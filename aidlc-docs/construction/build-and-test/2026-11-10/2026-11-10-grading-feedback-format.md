# U09 feedback formatting — 2026-11-10

General feedback now has an explicit “Tách đoạn” action, readable 14px text/24px line spacing, padding and Enter guidance. The action uses native Vietnamese sentence segmentation to add paragraph breaks without changing words or decimal scores. Already multiline text is preserved. It changes only the editable draft and never saves automatically; the 10,000-character boundary is retained.

Editable strengths/improvements now use one bulleted auto-growing field per idea with add/remove controls, five ideas per list and 500 characters per idea. Manual/trailing line breaks survive editing. This replaces the old per-keystroke split/trim/filter operation that erased newlines. An empty group has a compact placeholder; a single populated group uses full width rather than leaving an empty column. Two populated groups share desktop columns and stack on mobile. Read-only summary lists retain newlines and have clearer spacing.

Blank unfinished editing rows are excluded from the save payload. Nonblank multiline entries, structured item feedback, review metadata and historical nextSteps remain intact. No grading API/schema, score or backend changes were needed.

## Verification

- Regression first: five expected failures / 36 existing tests passed, covering the missing paragraph action and per-idea controls.
- Grading/document scope: **210 tests / 20 files passed**. After the final compact-layout refinement, grading scope: **154 tests / 17 files passed**. Counts overlap.
- Scoped ESLint, typecheck, final production build and scoped diff whitespace check passed. Last edits after the build were indentation only.
- Tests cover sentence/decimal preservation and no automatic save; add/delete ideas, retained trailing/blank line breaks, 500-character/five-entry limits, disabled controls and blank-row removal while retaining historical fields at save.
- Real GradingHeaderSection/GradingFeedbackSummary fixture inspected at1440px and390px, with synthetic text comparable to the screenshot. “Tách đoạn” produced four paragraphs. No horizontal overflow or textarea internal overflow; fields grew on narrow widths. Final computed general line-height24px and single-populated-group layout verified after reload.
- Desktop/mobile screenshots inspected and retained as `.tmp/grading-feedback-format-desktop.png` and `.tmp/grading-feedback-format-mobile.png`. Metrics: `.tmp/grading-feedback-format-desktop.json` and `.tmp/grading-feedback-format-mobile.json`. Temporary fixture tab/server closed afterward.

Logs: `.tmp/grading-feedback-format-red.log`, `.tmp/grading-feedback-format-tests.log`, `.tmp/grading-feedback-format-final-tests.log`, `.tmp/grading-feedback-format-lint.log`, `.tmp/grading-feedback-format-types.log`, `.tmp/grading-feedback-format-build.log`.

## Acceptance

1. On the teacher grading workspace, press “Tách đoạn” for a single-paragraph general comment. Review paragraph breaks; manual Enter remains available. Nothing saves until the teacher chooses to save.
2. Add/edit/remove a strength or improvement. Each idea stays separate; Enter within an idea is preserved. Empty unfinished rows are ignored when saving.
3. At five ideas, “Thêm ý” disables; fields and delete buttons disable when the workspace is read-only. Resize to mobile and verify normal page scrolling.

No live grading save/finalize/publish, provider call, backend restart, migration, commit or push. Browser verification used a synthetic component fixture, not a save on the authenticated teacher submission. Whole frontend/backend suites were not repeated. Security/Resiliency retained; PBT disabled.

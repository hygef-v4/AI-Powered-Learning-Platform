# U09 UI polish implementation plan

> Implementer: the already-open Gemini 4 Argon session in Orca. Execute sequentially in the current checkout; do not spawn agents. Follow root AGENTS.md / CLAUDE.md and frontend/AGENTS.md. This plan is the source of truth for this scoped change.

## Authorization and goal

User: "Giúp rôi sửa theo kế hoạch tạo plan sau đưa cho argon làm".

The user authorizes implementing the UI recommendations discussed immediately before this request and handing execution to the existing Argon session. Proceed with this approved scope; no additional generic planning approval is needed. No commit, push, merge, deployment, credential/model change, official-grade finalization or publication is authorized.

Goal: teachers can inspect submitted work, enter fractional rubric points, compare AI suggestions and save drafts with less scrolling and clearer state. Preserve the working partial-score and real Gemini flows.

## Verified starting context

- Workspace: `F:/code/git/AI-Powered-Learning-Platform`, branch `mcp-test`, HEAD `f4a19cf6`. Work directly on this dirty checkout as requested. Inspect status/diff before editing; preserve all existing changes, including backend partial-score work and frontend generated instruction files. Do not reset, stash, clean, switch branches or create a worktree that loses this baseline.
- Read `aidlc-docs/construction/plans/2026-10-25-u09-partial-scores.md` for the completed live acceptance and fixture IDs. Latest scoped tests were 43 frontend + 27 backend passing; earlier full-suite/build totals are historical, not evidence for the coming changes.
- UI reference screenshots: `.tmp/u09-design-audit/01-list.png`, `02-rubric.png`, `03-ai.png`; review notes `.tmp/u09-design-audit/review.md`. Also `.tmp/u09-ui-2026-10-25/01-grading-overview.png`. Inspect these files, then fresh runtime screenshots. These are current-app references, not assets to embed in the product.
- Key layout evidence: actual browser `innerWidth=1077`, `innerHeight=909`, devicePixelRatio=1.5; available workspace grid width=762 CSS px. Existing `GradingWorkspacePage.tsx` already has `xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,1fr)]`, so it falls back to one column at this viewport. Do not report that the grid is absent or infer viewport size from PNG pixels.
- Existing focus mode hides the shell sidebar via `[data-workspace-focus]` in `frontend/src/app/globals.css`. Reuse this behavior; do not duplicate the shell or globally change sidebar sizing.
- Existing tokens are in `frontend/src/app/globals.css`; reuse `primary`, `success`, `warning`, `danger`, `line`, `muted`, `shell`, `canvas`. Reuse shared Button/Pill/Icon/Dialog/input patterns. No new UI framework, fonts, dependencies, decorative assets or unrelated redesign.

## Scope and product decisions

### A. Grading workspace and toolbar

- Put submission and grading beside each other when available content width is sufficient. Prefer container-aware layout: two columns when approximately 736px of content is available, with submission at least 400px, grading at least 320px and a 16px gap. At wider widths favor approximately 60/40. If actual content does not fit, stack. Confirm with measured layout, not CSS class assertions alone.
- In the current 1077 CSS px Orca viewport, aim for two usable columns; focus mode must also make sensible use of freed space. At 390 CSS px stack into one column with no page-level horizontal overflow.
- Compact the sticky toolbar: total, saved/unsaved/busy status and save action form the primary row. Keep previous/next, finalize and publish available with existing gating; wrap gracefully at narrow widths. Move the long workflow instruction to an optional help disclosure outside the sticky area.
- Target sticky toolbar no more than about 104 CSS px high at the current desktop viewport. Do not shrink text excessively to reach this target. Keep correct app-header offset, keyboard focus visibility and content clearance.
- Avoid adding independently scrolling panes unless necessary; no scroll traps. Existing submission diagrams/preview controls must remain usable.

### B. Compact rubric scoring

- Keep the criterion text left and item score plus maximum right on wide rows. Use compact padding; allow long Vietnamese labels to wrap. On narrow widths place the score below the text.
- Each rubric section has a collapsible heading with real part title and live subtotal/max visible while collapsed. Default open to preserve discoverability; collapsing must retain values, dirty state and validation.
- Replace the ambiguous full-credit checkbox with a clearly named `Đủ điểm` button. It awards that item's maximum through the same score/check update path; zero remains directly editable through the input. Provide an accessible name that identifies the item. Mark full-credit state with text or aria-pressed, not color alone.
- Preserve `0.5 / 1`, zero, bounds, maximum two decimals, server-calculated totals, legacy checked-item fallback, invalid draft retention, saved-score reload and readonly/disabled behavior. Do not change data types, APIs or authoritative calculations for styling.
- Inline errors stay tied to the correct input; do not conceal an invalid field in a collapsed section without an error indicator and a route to open/focus it.

### C. AI proposal presentation and comparison

- Latest proposal expanded by default; previous proposals collapsible with status/version/source visible. Never hide a pending/error state only because it is an older version. Preserve task diagnostics, pending polling and source selection behavior.
- Ready status uses success styling plus text/icon, with prominent proposed total and real named section summaries. Model/source are secondary metadata; do not expose new credential material.
- Show `Điểm đang nhập` versus `AI đề xuất` at item or section level, with differences visually highlighted. Use existing `itemPoints`/`checklistPoints` helpers for legacy checks and per-item overrides. Compare the rubric subtotal to the AI rubric score; show any teacher adjustment separately so a delta does not imply an AI calculation error.
- Extend `AiProposalPanel` with the context it needs from the parent: `rubrics: Rubric[]`, `currentValues: GradingFormValues`, and `partTitles: Record<string,string>`. Preserve existing onRequest/onApply semantics and adapt existing callers/tests. A small local comparison component/helper is allowed if it keeps this component understandable.
- For invalid current input show an explicit invalid/unavailable comparison; never render NaN or present it as zero. Do not invent AI item points for missing IDs or report unsupported values as facts.
- Comparison is visible or reachable before applying. Label application `Áp dụng vào bản nháp` and explain briefly that the teacher can edit before saving. Applying only changes the local form and usedProposalId; it must not save, finalize or publish automatically. Preserve protection for official/readonly evaluations and unsaved navigation.
- Long feedback and previous attempts can collapse; keep the latest ready total and apply action discoverable. Do not claim an application was persisted without server evidence.

### D. Submissions and secondary panels

- Reduce four summary cards to compact counters; retain the existing summary values and labels.
- Status uses the existing Pill tones with text: pending neutral/warning, draft information/neutral, finalized primary, published success, superseded neutral. No color-only meaning.
- Keep score on one line with tabular numbers; let name/title wrap. At narrow widths use local table scrolling if needed, not page-wide overflow.
- Render bulk action controls when at least one eligible evaluation is selected; keep selection count/clear action and existing filter/page selection resets. No bulk operation automatically fires when selection changes. Preserve a clear instruction near selection when none are selected if needed.
- Link labels: `Chấm bài` for PENDING, `Tiếp tục chấm` for DRAFT, `Xem điểm` for finalized/published; preserve actual permissions, navigation target and unavailable/no-submission cases.
- Collapse empty activity/history into compact disclosures. Preserve activity reliability/no-cheating warning and actual history content; populated history stays easy to find. Do not alter group/member grading behavior.

## Files and boundaries

Expected modifications:

- `frontend/src/features/grading/GradingWorkspacePage.tsx`: workspace sizing, toolbar, AI context props, secondary placement.
- `frontend/src/features/grading/RubricChecklistPanel.tsx`: compact rows, accessible full-credit action, collapsible sections.
- `frontend/src/features/grading/GradingForm.tsx`: compact form spacing as needed; preserve adjustment/official-change reason rules.
- `frontend/src/features/grading/AiProposalPanel.tsx`: statuses, comparison and proposal disclosure/application wording.
- `frontend/src/features/grading/GradingReviewPanels.tsx`: compact empty states/disclosures.
- `frontend/src/features/grading/SubmissionDetailPage.tsx`: counters, pills, bulk action visibility and row labels.
- Existing grading test files under the same directory; add a focused comparison test only if a new helper/component is extracted.
- `frontend/src/app/globals.css` only for narrowly scoped layout/focus rules if Tailwind/container utilities are insufficient. No global typography/button/token changes.

Backend, database schema, AI gateway/prompt/schema, authentication, provider configuration, routes and DTOs are out of scope. Do not modify those to make a visual test pass. Use fixture data for live draft mutations.

## Sequential execution checklist

- [x] Step 1 — Verify baseline. Read instructions, this plan and partial-score handoff; inspect git status/diff. Read relevant installed Next docs under `frontend/node_modules/next/dist/docs/` before frontend code changes. Confirm actual host ports, running process identities, worker uniqueness and current teacher browser session. Inspect reference screenshots/tokens and record baseline screenshot dimensions.
- [x] Step 2 — Workspace and rubric. Add meaningful failing behavior tests for accessible full-credit action, fractional preservation through collapse/expand, validation visibility and readonly state. Run red, implement compact layout/toolbar and rubric changes, then green. CSS sizes and two-column behavior require browser measurement, not class-name tests.
- [x] Step 3 — AI review. Add failing tests for current-versus-AI fractional comparison, legacy checks, adjustment separation, invalid current values, stale/failed/pending protections and applying to draft without a save request. Implement new presentation and wire current form/rubric context; update existing apply-button expectations without weakening assertions. Verify green.
- [x] Step 4 — Submissions/secondary panels. Add or adapt tests for bulk action visibility after selection, selection reset, state-specific links, score formatting, collapsible empty panels and populated history access. Implement the scoped presentation, preserving real flows and group/member controls. Verify green.
- [x] Step 5 — Automated checks. Run all grading tests, full frontend suite, lint, typecheck and production build; inspect exit codes and output. Use `npm --prefix frontend test -- src/features/grading --maxWorkers=2`, `npm --prefix frontend test -- --maxWorkers=2`, `npm --prefix frontend run lint`, `npm --prefix frontend run typecheck`, `npm --prefix frontend run build`, then `git diff --check`. Type generation/build order may follow the installed Next tooling if necessary; document why. Avoid running build against the active Next dev output directory; use the existing isolated build support or stop only the verified frontend and restart afterward. No full backend rerun needed for frontend-only changes unless a real dependency/failure justifies it.
- [x] Step 6 — Actual UI acceptance. In the authenticated Orca browser verify the checklist below, collect new screenshots and inspect them. Fix actual regressions and repeat relevant checks. Do not mark complete based only on automated tests or snapshots.
- [x] Step 7 — Handoff evidence. Save results under `aidlc-docs/construction/build-and-test/2026-10-25-u09-ui-polish-results.md`, update this checklist plus `aidlc-state.md`/`audit.md`, and report changes, test counts, UI evidence and remaining gaps. Preserve working-tree changes; do not commit/push/deploy. Leave the verified host app available for review.

## UI acceptance checklist

- [x] Current Orca viewport measured in CSS px: two usable columns where available width permits; compact sticky bar does not obscure content/controls. Capture measured viewport, grid width/columns and toolbar height.
- [ ] Actual viewport/zoom acceptance at 1440 CSS px, 1077 CSS px, 390 CSS px and 200% zoom remains incomplete. Desktop viewport 1289px and workspace-only widths 762px/358px were checked; resizing a container is not equivalent to mobile viewport or browser zoom testing. See the results report for the recorded tooling limit.
- [x] Keyboard: tab to item score and full-credit button, activate disclosures and apply action, focus indicators remain visible; collapsed errors can be reached. Labels communicate item/max/state without relying on color.
- [x] QA item max1 accepts0.5, live subtotal/total update, full-credit button awards1, save/reload preserves fractional points. Above-max and >2-decimal input is blocked without losing draft.
- [x] QA AI latest READY proposal shows named comparison, its actual per-item scores and Vietnamese feedback. Apply populates draft only; edit an item and save/reload successfully. Existing READY QA result is sufficient to validate presentation/application; make at most one fresh provider request if an actual regression requires it. Do not repeatedly spend tokens for screenshots.
- [x] Original evaluation grade stays DRAFT/0.00/version7 unless the user explicitly changes it. Do not use the real target to save/finalize/publish. No finalization/publication even on QA for this visual scope.
- [x] List: compact counters, readable state badges and score; selecting a row reveals actions, deselection hides them, filter/page reset remains correct.
- [x] Old proposal diagnostics and non-ready states remain readable; group/member/history behavior is preserved by tests. Do not claim group/mobile/accessibility acceptance unless actually checked.

## Runtime and fixture handoff

- Frontend host terminal: `term_0feceaf6-77ba-420a-b701-347e593d6a20`, command `npm --prefix frontend run dev`, port3000.
- API/worker host terminal: `term_b21e6d47-b4d9-4b91-9a56-94c2f7cf2d16`, command `.\scripts\run-backend.bat`, profiles local,worker, port8080. Docker `platform-worker-1` was deliberately stopped; leave it stopped while the host worker is active. Verify names/PIDs before any lifecycle action.
- Existing teacher is signed in through Orca. Use `orca skills get orca-cli --reference references/browser.md --json` and current `orca tab list --json`; no password is recorded here. If session expires, ask the user to sign in; never guess/reset credentials or search logs for them.
- QA grading: `http://localhost:3000/teaching/evaluations/826f5c29-b5d1-4a32-9999-9313c242d8d3`, currently DRAFT9.50/version2. Assignment `aaf2897a-1deb-4987-8349-f1783b6610bc`, class `4d18e255-1789-45d6-8fbb-351e2188f672`. Existing READY proposal `6e4eea09-6d45-49cc-bccd-c6f8318d03bd` contains item scores and feedback.
- QA list: `http://localhost:3000/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672/assignments/aaf2897a-1deb-4987-8349-f1783b6610bc/submissions`.
- Original protected grade: evaluation `7ca08c41-e9d4-4738-946d-24c049e3d1e5`; no writes.
- Browser commands sometimes returned clicked without the resulting state change. Always verify fresh DOM/screenshot. The documented Orca `eval` can activate an observed DOM control when needed; do not mistake a click receipt for completed behavior. Quote refs such as `'@e40'` in PowerShell.

## Limits and compliance

Existing partial-score work is the baseline, not discarded code. Real Gemini requests succeeded twice after runtime cleanup with unchanged key/model; historical HTTP400 exact cause remains unproven. Do not reopen schema speculation during UI polish. Source review may reveal real blockers; document evidence before expanding scope.

Security configuration remains the project's reduced enabled scope: preserve authorization, validation, safe diagnostics and secret handling. Resiliency: retain existing AI timeout/polling/worker behavior; no new external dependency. Deployment-specific rules N/A because no deployment is requested. PBT extension is disabled. This plan is a frontend presentation change with explicit behavior regressions and live UI acceptance.

# U09 Structured Grading Feedback Implementation Plan

> Owner: the existing Gemini 4 Argon Orca session. Execute sequentially after the active AI-result-delivery fix reaches its verification/report checkpoint. The user explicitly requested this plan and immediate implementation handoff. Preserve the other task's work; do not spawn another agent or checkout. Update checkboxes as work completes.

**Goal:** Explain earned points with evidence and actionable feedback per rubric item, let teachers edit and approve it, and show students only the published result.

**Spec:** `aidlc-docs/construction/plans/2026-10-26-u09-feedback-recommendations.md`, now approved for the implementation scope below by the user's follow-up request.

**Architecture:** Add optional typed structured feedback to the existing grading JSON and proposal payload. Keep item scores and current backend score calculation authoritative. Use the same feedback renderer for proposal preview, teacher draft and published student results, with separate editing and visibility permissions.

**Tech stack:** Java/Spring Boot/Jackson/PostgreSQL; React/Next.js/TypeScript/Vitest. Use existing dependencies and design tokens.

## Scope and decisions

- Implement teacher and published student feedback end to end, including normal manual grading, AI proposals, saved drafts, history, and existing group grade behavior.
- Overall structure: "Làm tốt", "Cần sửa", "Gợi ý tiếp theo". Each rubric item can show evidence, reason for earned points and a concrete improvement.
- Add teacher review flags for uncertain/unreadable content or an alternative valid approach. Never equate unreadable evidence with a demonstrated error or invent confidence percentages.
- Link only to a known question/document block in this version. Draw.io shape highlighting, configurable issue taxonomy, class analytics, report exports and Practice feedback (U10) are deferred.
- Existing free-text feedback and old records/proposals remain valid. Do not parse historical prose to fabricate structured evidence.
- The reference PDF stays local; use the written recommendations and synthetic examples. Do not send the PDF/student identities to Gemini API or use them as fixtures.
- No commits, push, merge, deploy, new keys/connections/model changes, real grade overwrites, finalization or publication through live UI tests. Tests may finalize/publish isolated fixtures only.
- This task follows the delivery fix. Do not mix its diagnosis with feedback changes or replace its tests/report.

## Design contract

Introduce `structuredFeedback` as an optional field on proposal content, teacher save input and persisted evaluation. Use a domain value object/record and matching TypeScript type rather than duplicating ad hoc JSON validation.

Version 1 shape:

```json
{
  "schemaVersion": 1,
  "summary": {
    "strengths": ["Specific supported strength"],
    "improvements": ["Highest-priority issue"],
    "nextSteps": ["Concrete action"]
  },
  "items": [
    {
      "rubricId": "known-rubric-uuid",
      "itemId": "known-item-uuid",
      "evidence": "Observation actually present in the supplied submission",
      "reason": "Explanation tied to the actual criterion",
      "suggestion": "Specific improvement or optional practice advice",
      "evidenceBlockId": null,
      "requiresReview": false,
      "reviewReason": null
    }
  ]
}
```

- Earned/max values come from the grading score maps and locked rubric; do not accept another AI subtotal or penalty in this object.
- Validate rubric/item pairs and uniqueness. `evidenceBlockId`, if supplied, must resolve in the same submitted snapshot. Derive question/part ownership from the locked rubric, not AI-authored labels.
- Bound input: at most 5 entries per summary list, 500 characters per entry; evidence/reason/suggestion each at most 1000 characters; reviewReason at most 500 characters; total structured feedback JSON at most 60000 characters. Item count cannot exceed the number of rubric items. Reject unknown fields/types/IDs and duplicate item entries with a stable 400 error.
- New AI schema requires feedback for every scored rubric item, with evidence or an explicit uncertainty/review reason. Old proposals and manual saves may omit the entire object. Do not require teachers to fill every item to use legacy manual grading.
- Null/absent structuredFeedback means legacy behavior; it must not accidentally erase an existing structured draft on an old-client save. Define explicit clearing with a documented nullable field or dedicated clear action and test absent versus explicit clear behavior.
- AI output must not invent subcriteria for partial credit. If the rubric does not support a confident rationale, flag teacher review. Suggestions without deductions must be labelled optional.
- Applying AI fills the teacher draft only. Preserve the current overwrite-confirmation behavior and teacher adjustment; do not auto-save or change lifecycle state on arrival.
- After a score changes, mark its existing explanation for review until the teacher edits or explicitly confirms it. Use saved reviewed-score metadata outside AI-authored content, or an equivalent tested mechanism. Enforce unresolved review checks at finalization on the server for structured feedback, with an actionable error. Draft saves and manual grading must remain possible.
- Teacher review acknowledgement is explicit; unchecked AI requiresReview flags cannot be silently cleared. Validate review metadata against the current saved item scores and rubric/submission version.
- Keep a bounded token budget appropriate to the new schema. Inspect the existing 4096 output-token limit and worst-case rubric size; choose a justified bound or explicit supported-size error. Do not silently truncate JSON, loop provider calls or change models.

## Task 1: Domain, persistence, and save/publish contracts

Existing files to inspect/modify:
- `backend/src/main/java/edu/aiplatform/grading/domain/Evaluation.java`
- `backend/src/main/java/edu/aiplatform/grading/domain/EvaluationHistory.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingCommands.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingService.java`
- `backend/src/main/java/edu/aiplatform/grading/application/EvaluationLifecycleService.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingViews.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradebookService.java`
- `backend/src/main/java/edu/aiplatform/grading/infrastructure/EvaluationRepositoryAdapter.java`
- Add focused `GradingFeedback` domain type and a validation service in the grading unit.

- [x] Trace Save constructors/callers, strict unknown-field handling, grading_document mapping, published student query guards, group inheritance/overrides and evaluation history before editing.
- [x] Add failing tests for structured feedback validation, absent/clear semantics, legacy JSON roundtrip, draft save/reload, history and student publication privacy.
- [x] Implement additive persistence inside existing grading JSON; no migration unless a proven storage constraint requires one. Retain old Save constructors/caller behavior deliberately.
- [x] Save and return teacher-edited feedback; preserve prior feedback in history when existing history rules apply. Reset stale feedback/review metadata on resubmission or rubric snapshot replacement.
- [x] Preserve group root-to-member inheritance without aliasing mutable collections. Show group rubric feedback separately from individual member score overrides and never imply AI assessed individual contribution. Test override/reset paths.
- [x] Extend the published StudentGradeView with sanitized feedback and the minimal scored rubric-item labels/points needed for display. Do not expose grading_document, private teacher flags/notes, raw XML, proposals or unpublished content. Keep free-text fallback and ownership checks.
- [x] Add server-side finalization validation for unacknowledged structured-feedback review flags and explanations whose reviewed score no longer matches; keep draft saves valid.
- [x] Run domain/mapping/commands/service/lifecycle/group/gradebook tests plus a real PostgreSQL roundtrip test for the new optional document fields.

## Task 2: AI schema, prompt, validation and proposal application

Files:
- `backend/src/main/java/edu/aiplatform/grading/application/GradingPromptBuilder.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalValidator.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalTransactions.java`
- `backend/src/main/java/edu/aiplatform/grading/domain/GradingProposal.java`
- Tests: GradingPromptSchemaTest, GradingProposalValidatorTest, GradingProposalResultHandlerTest, GradingProposalServiceTest.

- [x] Write failing tests for valid 0.50/1.00 feedback, full marks with optional advice, unreadable evidence, invalid/duplicate item IDs, invalid block anchors, payload limits and historical proposals without structured feedback.
- [x] Extend both actual outputSchema generation and any static schema consumers; update strict allowed-field sets and reuse the feedback validator. Inspect provider schema compatibility rather than assuming all JSON-schema keywords are accepted by Gemini.
- [x] Prompt for Vietnamese evidence-based feedback, item score consistency, strengths and concrete next steps. Submission text/XML remains untrusted data; never execute instructions embedded in it.
- [x] Preserve checks/full-credit semantics, deterministic backend total validation, two decimal places and current callback snapshot/authorization rules. Avoid unsupported double penalties.
- [x] On malformed AI feedback, expose an actionable safe diagnostic/invalid-proposal state rather than a stuck PENDING result. Do not log submissions, output payloads, keys or authorization headers.
- [x] Confirm duplicate result delivery still does not repeat provider usage or mutate teacher drafts. Extend the delivery-fix regression coverage only where the new payload changes the boundary.
- [x] Run focused backend tests and prompt/schema checks. Include a realistic maximum-size supported fixture to catch truncation/token-budget issues.

## Task 3: Teacher UI and draft state

Files:
- `frontend/src/features/grading/types.ts`, `api.ts`
- `frontend/src/features/grading/AiProposalPanel.tsx`
- `frontend/src/features/grading/RubricChecklistPanel.tsx`
- `frontend/src/features/grading/GradingForm.tsx`
- `frontend/src/features/grading/useGradingWorkspace.ts`
- `frontend/src/features/grading/GradingWorkspacePage.tsx`
- Create small shared `GradingFeedbackSummary`, `RubricItemFeedback`, and teacher editor components where appropriate.

- [x] Add failing UI tests for AI preview, Apply-to-draft behavior, editing, score-change review warnings, save/reload, old text-only feedback and unresolved-review finalization errors.
- [x] Add compact strengths/improvements/next-steps sections and expandable per-item evidence/reason/suggestion under existing earned/max scores. Keep the work/grade two-column layout and readable narrow-screen stacking.
- [x] Show review-required feedback prominently with the specific reason and explicit teacher acknowledgement. Clearly distinguish suggestions from deductions.
- [x] Implement accessible labels, keyboard-operable disclosure controls, empty/loading/error states and live validation. Do not rely on color alone or fill the page with always-expanded text.
- [x] Link known evidence blocks/questions to real anchors. Unknown/unavailable references show plain text, not broken links.
- [x] Apply keeps teacher input protected through existing confirmation behavior, fills optional structured data and leaves Save explicit. A proposal arriving must never overwrite current scores/feedback automatically.
- [x] Score edits invalidate the relevant reviewed explanation only; teachers can edit or confirm it without requesting AI again. Handle adjustment/group overrides explicitly.
- [x] Preserve legacy feedback textarea and prevent duplicate rendering when a structured summary and compatibility free-text representation coexist. Do not render AI HTML unsafely.

## Task 4: Student result UI and lifecycle tests

Files:
- `frontend/src/features/grading/StudentGradePanel.tsx`
- `frontend/src/features/grading/StudentGradeCell.tsx` / `StudentGradesSection.tsx` as needed
- Shared read-only feedback components and focused tests.

- [x] Render published total and expandable rubric-item points with approved feedback and actionable next steps. Full marks may show optional advice without implying loss of points.
- [x] Preserve old feedback-only results and unpublished/no-result states. Do not expose teacher review controls, internal diagnostics or pending AI proposals to students.
- [x] Test ownership/role restrictions, DRAFT and FINALIZED privacy, publication, post-publication edits according to existing rules, legacy results and group-member overrides.
- [x] Verify labels and scores agree across teacher preview/draft/save and student published fixtures, without adding a second score calculation.

## Task 5: Verification, report and manual guide

- [x] Re-read current git diff and active delivery-fix report before final verification; preserve all work from that task.
- [x] Run backend focused tests above, relevant grading integration tests, frontend grading tests, lint, typecheck and git diff --check. Broaden only for changed cross-unit contracts or failures.
- [x] Run isolated frontend production build if required; never replace the live .next output. Stop owned backend before Maven clean/test compilation or isolate its output; do not let IDE autobuild replace classes under the running JVM.
- [x] Write `aidlc-docs/construction/build-and-test/2026-10-26-u09-structured-feedback-report.md` with exact changes, constraints, test commands/counts, known gaps and live UI evidence or explicit Pending status. Preserve the separate delivery-fix report.
- [x] Write `aidlc-docs/construction/build-and-test/2026-10-26-u09-structured-feedback-manual-test-guide.md` with IDs, roles, steps, expected result and Pass/Fail/Pending for each case below. Do not claim unrun UI cases passed.
- [x] Update this plan and append state/audit entries. Report in Vietnamese when code and checks are ready for the user to test. No commit/push.

Manual cases (use dedicated disposable fixtures; never publish/overwrite live grades as an agent):

1. Legacy text-only grade and proposal still display correctly.
2. AI partial score 0.50/1.00 has specific evidence/reason/suggestion and sums correctly.
3. Full marks with optional advice do not show a deduction.
4. Unreadable evidence and a different valid design require explicit teacher review; no unsupported claim of incorrectness.
5. AI arrival changes no teacher input; Apply fills a draft and Save remains explicit.
6. Edit feedback and save/reload; text, item IDs, review state and scores persist.
7. Change item score after applying AI: review warning appears, finalize is rejected until confirmed/edited; final text matches new score.
8. Add a teacher adjustment: rubric subtotal and adjustment remain separate and clear.
9. Reference a valid evidence block, then test missing/stale references; no broken navigation or stale application.
10. Invalid item IDs, duplicate entries, oversized text, forged acknowledgement or another student's result are rejected.
11. Students cannot retrieve DRAFT/FINALIZED feedback via UI or API; only approved published fixture content is visible.
12. Group root inheritance, member override and resubmission clear/preserve the correct fields.
13. Existing historical records remain readable; new history preserves prior feedback without mutation.
14. Keyboard navigation, long Vietnamese feedback and narrow viewport remain usable.
15. One live AI proposal uses the existing connection; record delivery timings and feedback completeness. Do not apply/save/finalize/publish it as a test without a disposable fixture authorization.

## Approved feedback placement follow-up - 2026-10-26
User requested removing summary-level suggestions and keeping improvement guidance at each rubric item.
- [x] Hide summary next steps in teacher preview/editor and published student view; retain legacy data without destructive migration.
- [x] Use two-field summary layout and Cách cải thiện for item-level guidance. Update AI prompt to leave summary.nextSteps empty and provide actionable item suggestions.
- [x] Verify historical-data preservation, no empty summary card, partial-item suggestions and prompt contract; 79 frontend tests and 8 backend tests pass; lint/typecheck pass.
- [x] Verified live teacher UI: proposal version 6 READY, summary.nextSteps empty, all 9 items include suggestions; opened the first item's Cách cải thiện. No grade applied/saved/published.

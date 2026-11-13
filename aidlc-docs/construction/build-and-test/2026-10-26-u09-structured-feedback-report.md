# U09 Structured Grading Feedback Implementation & Verification Report

**Date**: 2026-10-26
**Unit**: `U09 - Evaluation, Grading, and Feedback`
**Plan**: `aidlc-docs/construction/plans/2026-10-26-u09-structured-feedback.md`
**Reference Spec**: `aidlc-docs/construction/plans/2026-10-26-u09-feedback-recommendations.md`
**Branch**: `mcp-test`

---

## 1. Summary of Architecture & Design Implementation

1. **Typed Structured Feedback Value Object (`GradingFeedback`)**:
   - Added `edu.aiplatform.grading.domain.GradingFeedback` (`schemaVersion: 1`, `summary: { strengths, improvements, nextSteps }`, `items: [{ rubricId, itemId, evidence, reason, suggestion, evidenceBlockId, requiresReview, reviewReason }]`) with `@JsonAnySetter` strict unknown-field rejection and deep-copy semantics (`.copy()`).
   - Persisted additively inside `evaluations.grading_document` alongside `reviewedItemScores` (`Map<UUID, Map<UUID, BigDecimal>>`) and `acknowledgedReviewItems` (`Map<UUID, Set<UUID>>`). No SQL schema migration required; legacy rows without these fields deserialize cleanly to empty defaults.

2. **Validation & Bounds (`GradingFeedbackValidator`)**:
   - Enforces `schemaVersion == 1`, `<= 5` entries per summary list (`1..500` chars per entry), `evidence` `<= 1000` chars, `reason` required (`1..1000` chars), `suggestion` `<= 1000` chars, `reviewReason` `<= 500` chars, `evidenceBlockId` `<= 200` chars, and total JSON `<= 60000` chars.
   - Validates every `(rubricId, itemId)` pair against the locked rubric snapshot, rejects duplicate item entries, and requires `evidenceBlockId` (when non-null) to resolve against the submitted `DocumentModel` block IDs or rubric `partId`s.
   - Every item must provide non-blank `evidence` OR (`requiresReview == true` and non-blank `reviewReason`).
   - New AI proposals (`requireAllItems == true`) must provide one item entry per rubric item; manual saves may omit `structuredFeedback` or provide partial item entries.

3. **Absent vs. Explicit Clear & Score-Change Review Metadata (`GradingService`, `GradingCommands.Save`)**:
   - Legacy/absent `structuredFeedback` on `Save` (`structuredFeedback == null && !clearStructuredFeedback`) preserves existing `evaluation.structuredFeedback`.
   - `clearStructuredFeedback == true` explicitly clears `structuredFeedback`, `reviewedItemScores`, and `acknowledgedReviewItems`.
   - Changing an item's score after structured feedback exists leaves `reviewedItemScores` at the previously reviewed score until the teacher edits that item's explanation text or explicitly confirms it in `acknowledgedReviewItems`.
   - Silently unchecking an AI `requiresReview == true` flag without teacher acknowledgement or text edit is rejected (`UNACKNOWLEDGED_REVIEW_FLAG`).
   - Draft saves remain allowed while review flags or score mismatches are pending; server-side `finalizeOne` and official grade edits enforce that all `requiresReview` items are acknowledged (`STRUCTURED_FEEDBACK_REVIEW_REQUIRED`) and all `reviewedItemScores` match current effective item scores (`STRUCTURED_FEEDBACK_SCORE_MISMATCH`).
   - Post-finalization edits preserve `previousStructuredFeedback` in `EvaluationHistory`. Group root saves deep-copy `structuredFeedback` to non-overridden `MEMBER` evaluations, and `resetForGroupSubmission` clears stale feedback and review metadata.

4. **AI Prompt, Schema & Bounded Output Token Budget (`GradingPromptBuilder`, `GradingProposalValidator`, `GradingProposalTransactions`, `GradingProposalService`)**:
   - Extended `GradingPromptBuilder.outputSchema(Evaluation)` and `GradingPromptBuilder.SCHEMA` to require `structuredFeedback`.
   - Scaled `maxOutputTokens(Evaluation)` dynamically (`Math.min(8192, Math.max(4096, 1024 + totalItems * 220))`) up to the provider catalog ceiling (`8192`) and rejected rubrics exceeding `MAX_AI_RUBRIC_ITEMS = 30` with `RUBRIC_TOO_LARGE_FOR_AI` (400) to prevent silent JSON truncation.
   - Kept array `maxItems` compatible with `HttpProviderGateway.omitGeminiArrayMaxima` so Gemini's `responseJsonSchema` never hits state-machine complexity limits while `JsonSchemaChecker` and `GradingProposalValidator` enforce exact bounds after generation.
   - Prompted for Vietnamese evidence-based feedback, optional full-mark suggestions prefixed with `"Gợi ý thêm (không trừ điểm): "`, and `requiresReview = true` with `reviewReason` for unreadable/image-embedded evidence or alternative valid designs.
   - Malformed AI structured feedback transitions the proposal to `REJECTED` with actionable Vietnamese `safeError`: `"Phản hồi AI không khớp cấu trúc rubric hoặc thiếu minh chứng hợp lệ. Bạn vẫn có thể chấm thủ công."`

5. **Published Student Privacy (`GradebookService`, `GradingViews.StudentGradeView`)**:
   - Extended `StudentGradeView` with sanitized `structuredSummary`, `rubricItems` (`StudentRubricItemView`: `rubricId`, `partId`, `criterionTitle`, `itemId`, `itemText`, `awardedPoints`, `maxPoints`, `evidence`, `reason`, `suggestion`), `rubricScore`, and `memberOverride`.
   - Strictly strips `requiresReview`, `reviewReason`, `evidenceBlockId`, `reviewedItemScores`, `acknowledgedReviewItems`, `grading_document`, raw XML, and AI proposals from student responses, and only exposes results when `state == PUBLISHED`.

6. **Teacher & Student Frontend UI (`StructuredFeedbackSection.tsx`, `SubmissionViewer.tsx`, `AiProposalPanel.tsx`, `RubricChecklistPanel.tsx`, `GradingForm.tsx`, `useGradingWorkspace.ts`, `GradingWorkspacePage.tsx`, `BatchActionDialog.tsx`, `StudentGradePanel.tsx`)**:
   - Added `id="submission-block-<id>"` anchors in `SubmissionViewer.tsx`; `RubricItemFeedback` links known `evidenceBlockId`s and falls back to plain text when a block ID is unavailable.
   - Added `GradingFeedbackSummary` (`Làm tốt`, `Cần sửa`, `Gợi ý tiếp theo`) and expandable `RubricItemFeedback` (`Minh chứng`, `Giải thích điểm`, `Gợi ý cải thiện` / `Gợi ý thêm (không trừ điểm)`).
   - Added prominent review banners and explicit confirmation checkboxes for `requiresReview` and score-change mismatches, plus Vietnamese finalization error explanations in `BatchActionDialog.tsx`.
   - Updated `StudentGradePanel.tsx` to display published `structuredSummary`, expandable `rubricItems`, `rubricScore` vs. adjusted `score`, and `memberOverride` notice.

---

## 2. Files Changed (Structured Feedback Scope)

### Backend
- `backend/src/main/java/edu/aiplatform/grading/domain/GradingFeedback.java` *(new)*
- `backend/src/main/java/edu/aiplatform/grading/application/GradingFeedbackValidator.java` *(new)*
- `backend/src/main/java/edu/aiplatform/grading/domain/Evaluation.java`
- `backend/src/main/java/edu/aiplatform/grading/domain/EvaluationHistory.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingCommands.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingJson.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingViews.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradebookService.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingService.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingPromptBuilder.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalValidator.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalTransactions.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java`
- `backend/src/test/java/edu/aiplatform/grading/application/GradingPromptSchemaTest.java`
- `backend/src/test/java/edu/aiplatform/grading/application/GradingProposalValidatorTest.java`
- `backend/src/test/java/edu/aiplatform/grading/application/GradingCommandsTest.java`
- `backend/src/test/java/edu/aiplatform/grading/application/GradingServiceTest.java`
- `backend/src/test/java/edu/aiplatform/grading/application/GradebookServiceTest.java`
- `backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationMappingTest.java`
- `backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationVersionIntegrationTest.java`

### Frontend
- `frontend/src/features/grading/types.ts`
- `frontend/src/features/grading/StructuredFeedbackSection.tsx` *(new)*
- `frontend/src/features/grading/SubmissionViewer.tsx`
- `frontend/src/features/grading/RubricChecklistPanel.tsx`
- `frontend/src/features/grading/GradingForm.tsx`
- `frontend/src/features/grading/AiProposalPanel.tsx`
- `frontend/src/features/grading/useGradingWorkspace.ts`
- `frontend/src/features/grading/GradingWorkspacePage.tsx`
- `frontend/src/features/grading/BatchActionDialog.tsx`
- `frontend/src/features/grading/StudentGradePanel.tsx`
- `frontend/src/features/grading/structured-feedback.test.tsx` *(new)*

---

## 3. Automated Verification Commands & Results

1. **Combined Backend Unit, Schema, Architecture, and PostgreSQL Testcontainers Suite**:
   - Command:
     ```powershell
     mvn.cmd -f backend/pom.xml -Dtest="AiTaskFlowTest,JobDispatcherTest,GradingPromptSchemaTest,GradingProposalValidatorTest,GradingCommandsTest,GradingServiceTest,GradebookServiceTest,GradingProposalServiceTest,GradingProposalResultHandlerTest,EvaluationMappingTest,EvaluationVersionIntegrationTest,ArchitectureTest" test
     ```
   - Result: **57 tests run, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESS`)**.
   - Breakdown:
     - `AiTaskFlowTest`: 6 passed
     - `JobDispatcherTest`: 3 passed
     - `GradingPromptSchemaTest`: 2 passed
     - `GradingProposalValidatorTest`: 5 passed
     - `GradingCommandsTest`: 3 passed
     - `GradingServiceTest`: 15 passed
     - `GradebookServiceTest`: 3 passed
     - `GradingProposalServiceTest`: 4 passed
     - `GradingProposalResultHandlerTest`: 8 passed
     - `EvaluationMappingTest`: 3 passed
     - `EvaluationVersionIntegrationTest` (PostgreSQL Testcontainers): 3 passed
     - `ArchitectureTest`: 2 passed

2. **Frontend Vitest Grading Suite**:
   - Command:
     ```powershell
     npm --prefix frontend test -- --run src/features/grading/structured-feedback.test.tsx src/features/grading/workspace.test.tsx src/features/grading/partial-scores.test.tsx src/features/grading/ai-source-integration.test.tsx src/features/grading/manual-workflow.test.tsx
     ```
   - Result: **5 test files passed, 41 tests passed (`0 failed`)**.

3. **Frontend ESLint & TypeScript Typecheck**:
   - Commands:
     ```powershell
     npm --prefix frontend run lint
     npm --prefix frontend run typecheck
     ```
   - Result: **0 errors, 0 warnings**.

4. **Git Diff Hygiene Check**:
   - Command: `git diff --check`
   - Result: **0 errors**.

---

## 4. Constraints & Safety Compliance

- **Separate Delivery Report Preserved**: `aidlc-docs/construction/build-and-test/2026-10-26-u09-ai-result-delivery-report.md` remains intact and separate.
- **Reference PDF Privacy**: `C:\Users\Quang Hung\Downloads\Bao_cao_7A1.pdf` was never transmitted to any external API, and no student names or identifiers from it were copied into code, prompts, or test fixtures.
- **No Real Grade Overwrite / Finalize / Publish**: Live database evaluations (including `8b88dd01-6d2f-48df-9bd0-6525e6627800`) were not overwritten, finalized, or published.
- **Manual UI Cases**: Detailed in `aidlc-docs/construction/build-and-test/2026-10-26-u09-structured-feedback-manual-test-guide.md` with explicit Automated Pass vs. Live UI Pending status.

## Fresh verification before local commits - 2026-10-26
The current combined source snapshot passed 73 backend tests across the 12 focused classes, with zero failures/errors/skips. Frontend grading passed 76 tests across 10 files; lint and typecheck exited 0. These are the latest verified counts for the committed snapshot.

A follow-up live Gemini proposal reached READY with all 9 structured item explanations in 7.636 seconds after the missing-evidence schema fix. See 2026-10-26-u09-feedback-evidence-fix.md for the reproduced failure, regression evidence and live UI outcome. No draft/official grade was applied or saved. The remaining manual acceptance cases are still pending.

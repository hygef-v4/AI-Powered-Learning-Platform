# U07 AI authoring and rubric — code generation plan

**Approval**: User approved implementation of `2026-10-24-ai-assignment-swr-swd-plan.md` on 2026-10-24 (“giúp tôi triển khai kế hoạch”). Execute sequentially in this chat, without subagents. Do not push.

**Story mapping**: U07 assignment create/edit/import, AI draft request/review/apply, rubric authoring; U03 private upload policy; U13 extracted text contract; U06 task status; preserve U09 grading contract.

**Boundaries**: preserve DRAFT/version authorization, CSRF and request idempotency; keep source files private and never expose them in student views; no AI answer key in student content; do not modify grading algorithms, publish assignments, or store provider credentials/content in logs.

## Step 1 — Regression baseline and current contracts

- [ ] Record current touched files and run focused backend/frontend assignment tests before edits.
- [ ] Confirm version, permissions, FileRef ownership and extraction contract used by each call path.
- [ ] Add regression tests first for question count/target points, missing leaf rubric, title/context, decimal totals and upload policy.

## Step 2 — AI request/output contract and validator (U07)

Files: `backend/src/main/java/edu/aiplatform/assignments/service/AssignmentCommands.java`, `AssignmentRequests.java`, `AssignmentViews.java`, `AiDraftProposal.java`, `AssignmentAiDraftService.java`, new `AssignmentDraftPromptBuilder.java`, new `AssignmentDraftValidator.java`, `AssignmentMcpTools.java`; corresponding files under `backend/src/test/java/edu/aiplatform/assignments/application/`.

- [ ] Default to automatic assessment-style inference from free-text/source documents (no SWR/SWD picker in UI); preserve legacy API profiles, and support VI/EN, target top-level requirement count and total points.
- [ ] Extend the proposal schema with title and business context while accepting legacy MCP proposals without silently inventing metadata.
- [ ] Normalize without discarding validation evidence; report issues with severity and section path.
- [ ] Validate leaf rubric completeness and BigDecimal total against target; validate required profile context and question count.
- [ ] Apply title only when selected, insert business context as ungraded common content, revalidate in the apply transaction, and reject INVALID Graded proposals.
- [ ] Use the same normalization/validation path for AI task results and MCP-saved drafts; add source text as untrusted evidence in prompt.

## Step 3 — Private source upload and bounded text extraction (U03/U13)

Files: `backend/src/main/java/edu/aiplatform/files/contract/ArtifactPurpose.java`, `domain/FileType.java`, `domain/PurposePolicy.java`, `application/FilePolicyService.java`, related file policy/content inspector tests; U13 contract and implementation in `backend/src/main/java/edu/aiplatform/ai/contract/` and `backend/src/main/java/edu/aiplatform/materials/service/ReferenceMaterialService.java`; `MaterialTextExtractor.java`/`MaterialParserMain.java` only through the existing parser boundary; U07 request/controller/service/tests; frontend `features/files/purposes.ts` and its tests.

- [ ] Add a distinct assignment-source purpose, private educator-only authorization, TXT/DOCX/PDF allowlist and bounded 2 MiB per file / 3 files / 30,000 extracted characters per request.
- [ ] Verify actual content type server-side; reject unsupported, empty, oversized and unreadable sources with a safe actionable error.
- [ ] Attach signed FileRefs to the draft request with ownership/purpose checks, extract through the U13 contract, retain only private source metadata with the draft and expose no file identifiers to clients.
- [ ] Add tests for file type spoofing, wrong owner/purpose, extraction failure, aggregate limits and injection-like source text.

## Step 4 — Create and edit assignment UI

Files: `frontend/src/features/assignments/components/CreateAssignmentDialog.tsx`, `AssignmentFormPage.tsx`, `AiDraftDialog.tsx`, `RubricEditor.tsx`, `types.ts`, `api.ts`, shared `features/files/` uploader; assignment tests and new `AiDraftDialog.test.tsx`/focused upload tests.

- [ ] Provide clear Manual / Import DOCX / AI entry paths; all paths create or edit the same DRAFT assignment.
- [ ] Add free-text request, VI/EN, number of top-level requirements, total score and optional diagram constraints; let AI infer the assessment style instead of asking instructors to choose SWR/SWD.
- [ ] Upload up to three private TXT/DOCX/PDF sources with file name, size, real upload progress, retry/remove and backend policy messages; keep this separate from student-visible reference materials and DOCX structure import.
- [ ] Improve authoring hierarchy: assignment title/context/instructions, numbered questions, leaf rubric editor and fixed score/coverage summary; make panel responsive and keyboard-accessible.
- [ ] Keep incomplete manual drafts editable; do not represent missing rubric points as zero.

## Step 5 — Preview, progress and safe apply (U07/U06)

Files: `AiDraftDialog.tsx`, `AssignmentFormPage.tsx`, `RubricEditor.tsx`, assignment API/types, relevant tests.

- [ ] Show a spinner and elapsed time during submission; show actual QUEUED/RUNNING/READY/FAILED/UNKNOWN task status after receiving the task, and do not fabricate percentage or ETA.
- [ ] Preview title, business context, instructions, questions, item scores, total, rubric and validation issues.
- [ ] Make add-vs-replace instructions explicit, default replacement off when existing instructions are present, and show every content area that apply will replace.
- [ ] Lock invalid Graded apply; preserve old assignment on cancel/version conflict/failure; flush autosave before applying and refresh rubric caches afterward.
- [ ] Test polling cleanup, duplicate requestRef, UNKNOWN/no blind retry, stale version and source/error states.

## Step 6 — Verification and implementation record

- [ ] Run focused backend assignment/file/material tests, then the full backend suite as feasible; report environment-only failures separately.
- [ ] Run focused frontend authoring/file tests, lint, typecheck and production build.
- [ ] If the app and configured Gemini are reachable, perform a free-text request with source document → inferred assessment style → preview → apply → reload without publishing; otherwise report the exact blocker and do not mark a live-provider case passed.
- [ ] Verify no secrets or source content entered logs, student DTOs, test snapshots or committed artifacts; verify migration startup if a schema migration was needed.
- [ ] Update `aidlc-docs/audit.md`, `aidlc-docs/aidlc-state.md`, and U07 `code-summary.md` with evidence and remaining limitations. Leave all changes uncommitted and do not push unless asked.

## Completion rule

Mark only steps with fresh test or runtime evidence complete. Do not claim full end-to-end, live Gemini, upload retention or grading compatibility based only on unit tests. Keep unresolved items visible in the audit and final report.

## Execution snapshot — 2026-10-24

Core implementation is present: automatic style inference without a type picker, bilingual/count/score options, title/context and rubric validation, safe apply, private upload/extraction/preview, progress and editor summary. Sources can be the only input. The code compiles and packages.

Verified: 20 backend tests across 6 focused suites; frontend AI dialog 4 tests and assignment UI 13 tests; typecheck, changed-file lint, frontend production build and backend package. Mockito required preloading its Java agent with `forkCount=0`. Full lint hit host memory limits earlier; broad suite/live acceptance remain open.

The expanded plan still includes assignment-level target configuration after reload, manual per-question allocation/equal distribution and persistent source list/reuse/removal. Current AI options are retained in draft provenance; these expanded editor capabilities are not implemented yet. Checklists remain open where evidence or behavior is incomplete.

User said “bạn code xong chưa, tôi tự chạy”. Stop runtime startup and provider tests. Local images were built under the `ai-authoring-20261024` tag, but current containers were not replaced. Leave changes uncommitted/unpushed.

## Follow-up regression — empty sections / runaway prose

- [x] Reproduce empty-sections acceptance with a failing schema test.
- [x] Separate AI generation schema from legacy MCP shape; enforce nonempty graded rubrics, configured count and concise text at server validation.
- [x] Order questions/rubric before prose; split applied paragraphs without introducing graded context parts.
- [x] Preview validation/coverage first, collapsible prose and per-question rubric panels; explicit revise action preserves inputs without a hidden paid retry.
- [x] Focused backend/UI, typecheck, changed-file lint and low-worker production build.
- [ ] Live Gemini generation after the user's host backend is restarted with the new schema; leave this open until observed.

## Local commits authorized — 2026-10-24

User requested clearly separated commits and no push. The earlier leave-uncommitted preference is superseded for this work. Commit groups are file/source extraction, assignment backend, authoring UI and documentation. Existing parser regression tests were restored and all 6 passed. Do not interpret these commits as completion of unverified live-provider or expanded-plan items.

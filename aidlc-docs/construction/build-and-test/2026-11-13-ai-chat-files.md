# AI chat file attachments — delivery

Scope: [implementation plan](../plans/code-generation-plan/2026-11-13-ai-chat-attachments.md), **phase 1 only**. User explicitly postponed all link reading. No URL parsing/import endpoint, crawler, backend change, schema or dependency added in this delivery.

## Implemented behavior

- Three actions in the root + menu: Chọn đề mẫu có sẵn, Chọn học liệu của môn, Tải từ máy. Existing-source lists no longer contain file upload actions.
- Picker, image paste and scoped file drop share `useSourceUploads`. A synchronous reservation prevents multiple same-tick adds exceeding three references. File identity deduplication does not merge unrelated files with the same name. Invalid files stay visible with errors; remove/retry cancel stale callbacks.
- External files default to references; DOCX can become a template. Existing template replacement asks for confirmation; failed conversion preserves the old template/reference. Successful promotion removes the reference only after parsing; failed demotion removes its failed reference attempt and retains the template. Limits remain three references, 2 MB each and one DOCX template up to 20 MB.
- Attachment cards have visible roles, progress/error/ready states and remove/retry actions; local image thumbnail uses a revocable object URL. List height is bounded so the chat input stays accessible.
- Plain text/URLs paste normally. Image paste preserves accompanying plain text at the selection, without importing clipboard HTML. Drag events only intercept Files; nested children do not flicker the overlay, directory entries are skipped. No global clipboard/drop listener.
- Input and attachments clear only after server task acknowledgement. Before acknowledgement, errors retain the composer and identical retries retain requestRef. Immediate submission guard prevents double Enter; IME/Shift+Enter behavior preserved. After AI failure, explicit Khôi phục yêu cầu restores snapshots without an automatic model call. Restoring cannot overwrite a non-empty composer. Successful apply followed by a refresh error is correctly reported as applied. An unacknowledged apply reports an uncertain outcome and asks for refresh; it does not offer automatic recovery, discard or repeat.
- Closing the chat preserves the conversation/composer; switching assignment remounts its panel and cancels stale work. Existing apply/version/undo behavior retained.

## Checks

- Initial new hook suite failed RED because the hook did not yet exist; after implementation, role/queue tests pass. Old menu assertions failed against the intentional new three-action menu and were updated. Clipboard/drop/send failure coverage was added as regression coverage; no claim that every case was observed RED independently.
- **Full frontend: 1,038 tests / 150 files pass** (`npx vitest run --maxWorkers=2`, `.tmp/ai-chat-files-tests-final.log`). Cases include role failure/replacement, >2/20 MB, synchronous additions, late responses, remove/unmount, mixed clipboard text/image, file/text/directory drag, prepare/request failure, requestRef retry, double Enter, IME, explicit recovery, apply/refresh failure and ambiguous apply acknowledgement.
- **Full ESLint passes**, `.tmp/ai-chat-files-lint.log`.
- **TypeScript passes**, including the final regression files, using a temporary config with fresh generated Next output. Temporary config removed afterwards.
- **Production build passes** in `.next-ai-chat-files`, including TypeScript, prerender and traces; `.tmp/ai-chat-files-build.log`. Verification temporarily limits Next workers to two; original next.config.ts and tsconfig restored. As in the previous delivery, stale `.next/types` references to deleted routes are excluded only for isolated verification, not deleted or bypassed through a source/config change.
- An attempted concurrent final build/test run exhausted Windows virtual memory; reran sequentially with two workers and bounded Node memory. No product configuration change or service restart used.
- `git diff --check` passes. No backend rerun needed for this frontend-only delta; the previous backend changes remain separate WIP with their earlier verification.

## UI acceptance

Authenticated manager on `/assignments/aa221732-dc9c-41fd-be66-f46d2b1db5c4`:

- Viewed three-action menu; uploaded a non-sensitive DOCX fixture, read it as a reference, promoted it to a template and demoted it back successfully.
- Dispatched browser DragEvent/DataTransfer with a non-sensitive TXT file: reference reached Sẵn sàng. Dispatched browser ClipboardEvent with a one-pixel PNG: correct attachment and a loaded local blob image (naturalWidth=1). These are browser-event simulations, not a physical OS clipboard/key test.
- Running backend still advertises PDF/TXT/DOCX, so pasted PNG correctly remains an error and blocks send. The previous backend image-policy/worker change must be reloaded, followed by page reload, before live PNG/JPEG upload acceptance. No restart attempted.
- Inspected narrow 554x930 screenshot and desktop 1440x1000; desktop document width 1430, no horizontal overflow. Restored narrow viewport, removed all fixture attachments and closed the assistant. No model request, assignment text edit or publication. Small private file test artifacts remain under the normal file subsystem lifecycle; no destructive cleanup issued.
- The design detector's missing-src finding was a false positive for effect-assigned blob URLs. Confirmed actual loaded blob and lifecycle tests; persisted `broken-image=*` only for SourceImagePreview.tsx through the instructed ignore-value command. No global image rule suppression.

## Limits

- No link reading, model-provider end-to-end call, physical clipboard test across operating systems, every browser/device/theme combination, commit, push or deployment.
- Security/resiliency preserved: shared server file checks, private signed refs, quota/vision selection, assignment/version guards, cancellation and explicit failures. PBT remains disabled. No new external-network input boundary because URL work was excluded.

## Runtime recheck after authorized restart — 2026-11-13

User explicitly requested “Giúp tôi khởi động lại và test kại ui”. Interrupted the verified Java backend through its existing Orca terminal and confirmed its exit. Stopped only this checkout's `platform-worker-1` container (Compose working directory verified as this project's infra), then ran the existing `scripts/run-backend.bat`. Backend rebuilt and started successfully with local,worker profiles; port 8080 is owned by new Java PID 37852. Old Docker worker remains stopped because the local backend now runs the worker too. No infrastructure configuration changed.

Fresh UI checks against the restarted backend:

- Manager's draft: native file-picker PNG upload reaches Sẵn sàng. A browser ClipboardEvent carrying JPEG plus plain text also reaches Sẵn sàng and keeps the text. Both image thumbnails loaded at 480x220. The previous PNG/JPEG policy blocker is resolved.
- Browser DragEvent carrying DOCX reaches Sẵn sàng; promote to template and demote to reference both succeed without losing other attachments or composer text.
- Invalid .exe and >2 MB image fixtures show explicit Vietnamese validation errors and disable send; retry of invalid file preserves the error. A fourth file is rejected and card count stays three. Removing erroneous cards enables send again.
- Closing and reopening the assistant preserves text, all three cards and their error/ready states.
- Teacher's existing draft: file-picker JPEG reaches Sẵn sàng too. No AI submission, assignment content edits or publication on either role.
- Narrow 554x930 screenshot inspected; cards scroll while input/send remain visible. Screenshot copied to `.tmp/ai-chat-ui-restarted.png`. Paste/drop checks use browser-event simulation, not physical OS clipboard or desktop drag.
- Removed all fixture attachments from both composers, cleared test text and closed both assistants. Teacher page restored to its original class/student route. Five small private uploaded test artifacts remain under the normal file lifecycle; invalid/oversize/fourth files were rejected before upload.

No new blocking attachment issue observed. Existing console CSP inline-style warnings and backend SSE connection-aborted logs appeared during navigation (also observed before restart); not changed in this attachment UI verification. No source changes, new full-suite rerun, model-provider call, commit, push or deployment.

## Live provider verification — 2026-11-13

User explicitly requested “Gọi ai thực tế đi”. Created a separate, unpublished teacher-owned draft `[QA] Kiểm thử AI đọc ảnh và tệp 13-11` (`f0643cc5-f9a5-4947-8eea-5056564bffa8`). No existing assessment content changed. Used the normal UI file picker, chat send, polling and automatic apply flow; no stub, provider setting or credential changed.

1. Attached a generated PNG and TXT reference. The image contained `VISION-7Q4M`, Book → Loan → Reader and a relationship sentence; TXT contained `FILE-9P2K`, maximum four books and seven-day duration. Neither code was in the first user instruction. Live result reproduced both codes exactly and the loan rules, proving image and text content reached the provider. Provenance: PERSONAL / GEMINI / `gemini-3.5-flash-lite`, AI_TASK origin. Task `a3cb7291-6465-42a4-8a8f-0b680615a1e9`, draft `90dca5da-fb43-4426-bd92-ed47c4d0b017`, status APPLIED.
2. First generation omitted the requested rubric and returned literal Markdown emphasis. Stored proposal validation correctly reported NEEDS_REVIEW, zero total points and RUBRIC_RECOMMENDED. UI applied with a visible warning, so this is **not** a full content-quality pass.
3. Sent one focused follow-up through the same UI to add two five-point criteria and correct emphasis. Task `62983464-ff02-478f-b6a4-a27cb7b812bb`, draft `d0073df2-2319-42c3-96ec-a92bd7117344` applied with VALID, one question, two rubric criteria and total ten points. Markdown markers disappeared. However, normalized proposal format.runs still omitted bold on the requested phrase; persisted document runs also lack bold. Formatting acceptance remains **failed**, even though the editor supports the format contract. This check does not claim whether the raw provider omitted bold or normalization rejected a malformed value; no raw-provider payload inspected.
4. Reloaded the page: both source codes, the question and total ten-point rubric persisted. Inspected 1440x1000 screenshot `.tmp/ai-live-provider-result.png`, then restored 554x930 viewport. Left the QA draft available for user review; status remains DRAFT. Two private source uploads remain under normal file lifecycle. No publication, paid-call loop, source-code change, commit/push/deploy, or full-suite rerun.

Outcome: real provider/worker/image and TXT extraction/request/apply/persistence path passes. One-shot rubric completion is unreliable in this sample; selective bold still fails. Existing validation treats incomplete rubric as a warning for an editable draft. These findings are recorded for a follow-up fix rather than hidden by manual edits or repeated provider calls.

## Local commit verification — 2026-11-13

At the user's commit/integration request, reran 217 related frontend tests in 31 files and 131 targeted backend tests; all pass. Logs: `.tmp/ai-authoring-precommit.log`, `.tmp/ai-authoring-backend-precommit.log`. No source delta since the preceding full lint/typecheck/build/live checks, so those were not repeated. `git diff --check` passes. Fetched origin/staging; merge into the current branch returns Already up to date with no conflicts. Feature changes and documentation recorded separately; push/PR/deploy not part of this request. Provider output findings above remain open.

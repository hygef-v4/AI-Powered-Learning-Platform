# U09 grading UI follow-up — 2026-11-10

Continue the authorized grading remediation in place, without subagents. The teacher's screenshots identify presentation defects in the current workspace; keep grading, task diagnostics and persistence contracts intact.

1. [x] Add regression coverage for toolbar-relative sticky cards, expanding feedback fields, clean anchor labels and dated successful proposal history.
2. [x] Measure the existing document toolbar and reuse its inherited offset for comment cards. Reuse AutoTextarea for feedback, preserving labels, limits and callbacks. Hide technical anchor identifiers from visible copy.
3. [x] Add the existing backend createdAt field to Proposal's frontend type. Reuse the shared Vietnam date/time formatter. Exclude failed/rejected attempts from history cards, retaining the latest failure notice and task polling/diagnostics.
4. [x] Run affected grading/document tests, frontend lint/typecheck/build; inspect layout in a browser if available. Record evidence and acceptance steps.

[Verification and acceptance](../../build-and-test/2026-11-10-grading-ui-feedback-polish.md).

Scope: shared DocumentEditor/CommentRail, grading feedback/proposal components and their tests. No new dependency, backend change, data deletion, provider request, grade write or runtime restart. U09 manual grading/AI history stories apply; Security/Resiliency retained, PBT remains disabled. Prior verification reports remain historical.

# Compact grading UI, optional manual feedback and safe adjustment saves

## Result

- Remove teacher-workspace grade-history card; backend history and AI proposal history retained. History includes saved draft total changes, explaining the confusing repeated transitions and empty separators in the screenshot.
- Remove paragraph-split button/unused formatter and redundant paragraph-help text. Preserve manually entered newlines. Replace always-visible change-reason field with collapsed optional `Ghi chú thay đổi`; keep the separate required adjustment reason when points are added/subtracted.
- Optional strengths/improvements editor is available without AI, collapsed initially. Per-item comments/evidence editor is available for every editable criterion, collapsed initially, with navigation to the existing rubric section when no explicit evidence anchor exists. Opening controls does not create feedback or mark the form dirty. No default fabricated explanation.
- Manual explanation/evidence/anchor can be blank; nonempty text, rubric/item references and explicit anchors remain validated and bounded. AI proposal ingestion still requires complete rubric-item feedback and evidence or a review flag. Existing confirmation/score-review guards remain enforced.
- Remove whole-feedback-clear control to avoid discarding item evidence/review data. Remove nested borders for ordinary per-item detail sections and shorten confirmation copy. Keep AI warning treatment and read-only/student feedback behavior; suppress empty explanation labels.
- Missing adjustment reason produces accessible inline Vietnamese validation before any API call. Generic save errors pause autosave; user edits or explicit save/reload allow recovery. Fixes the1.5-second failure/retry busy loop while preserving entered values.

## Verification

- History removal initial168 frontend grading tests pass; adjustment RED4/42 followed by172 grading tests. Initial fake-timer setup timeouts corrected before recording the actual RED failures.
- Manual parity RED:1 frontend missing-editor regression and2 backend optional-field errors. Added service save/finalize scenario and validator tests retaining strict AI evidence, anchors and length limits.
- Final frontend command: `npm test -- tests/features/grading --maxWorkers=1` with512MiB Node heap:173 tests/18files,0failures. Completed2026-11-10T18:26 local. After final border/copy-only polish,45 tests/2files pass. Logs `.tmp/manual-feedback-frontend-final.log`, `.tmp/manual-feedback-final-polish.log`.
- Final `npm run typecheck` and scoped ESLint pass. Final component polish lint also passes. Logs `.tmp/manual-feedback-typecheck-final.log`, `.tmp/manual-feedback-lint-final.log`, `.tmp/manual-feedback-final-polish-lint.log`. `git diff --check` with repository CRLF handling passes.
- Backend command: Maven test with `-Dtest=edu.aiplatform.grading.**,ArchitectureTest`, UTF8,256MiB Maven/test heaps and SerialGC:204 tests,0failures/errors/skips, completed2026-11-10T18:25:40+07:00. Initial run failed before executing tests because Windows paging-file space could not commit the384MiB test heap; rerun serialized with lower heaps passes. Crash diagnostic moved under ignored `.tmp/`. Logs `.tmp/manual-feedback-backend-final.log`, `.tmp/manual-feedback-jvm-memory.log`.
- Initial frontend connected run172/173 exposed a global anchor assertion that now found both manual and AI links; scoped it to the AI preview. Initial lint caught effect-driven state and a mutable prop fallback; replaced with local state/immutable fallback. Final checks above are fresh and green.
- Refreshed local API/worker to load optional manual validation; health200. No new migration. Runtime `.tmp/manual-feedback-runtime.log`.
- Local authenticated DOM: no grade-history card, paragraph-split button or whole-feedback-delete button; optional sections and valid navigation present. With0.5 adjustment and blank reason, inline error appears, no reload action or repeated busy state. Automated tests cover positive/negative adjustments, whitespace reason, manual/auto-save blocking, recovery after correction, and autosave error pause/resume.

## Local browser-test incident and restoration

While opening a detail with native browser automation, a click changed criterion `b9d8e8c5-06bf-45fe-9d81-4ca912cc72ed` from0.50 to0.00 and autosave persisted it. This was not an intended grade edit. Evaluation `dbbf3c65-673f-49e9-8dbb-9fff6dcd1bbd` went from local DRAFT7.40/version4 to6.90/version5. Reported this to the user immediately after diagnosis.

Restored the exact criterion through the normal UI/versioned save path. Existing review metadata prevented an unacknowledged rollback; temporarily edited its explanation to confirm the restored point, saved7.40/version6, then restored the original explanation verbatim and savedversion7. No direct DB updates or audit deletion. Compared every non-audit field against the original snapshot, parsing rubricChecks JSON and canonicalizing check order: no semantic differences. Score7.40, all item scores/checks, feedback/evidence, review metadata, adjustment0 and submission hash match. State remains DRAFT. Grade-change history retains3 entries versus1 before, and version/gradedAt reflect the correction. No finalize/publish or VPS data write. Temporary test tab closed after save completed.

Evidence files: `.tmp/grading-adjustment-before.txt`, `.tmp/grading-adjustment-after.txt`, `.tmp/manual-feedback-restored.txt`. These local diagnostics are ignored and not committed.

## Limits and compliance

No real AI call, production/VPS deployment, commit/push, dependency, schema or migration change. Full backend suite/JAR/frontend production build not repeated; connected checks cover the changed paths. Browser DOM checks passed, but screenshot capture timed out on follow-up attempts; full visual/mobile/dark-mode acceptance is not claimed.

Security/Resiliency: retained application authorization, locked/versioned grade updates, rubric and anchor validation, bounds, AI strictness and review flags. Client validation does not replace backend enforcement. Error pause avoids retry loops. Existing approved controls retained; infrastructure/disaster-recovery N/A and PBT disabled. No subagents.

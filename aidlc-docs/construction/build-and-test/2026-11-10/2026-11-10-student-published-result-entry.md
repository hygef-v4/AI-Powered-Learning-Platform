# Student published result entry verification

## Diagnosis and behavior

Read-only local storage inspection found PUBLISHED8/10 for the latest submitted sports-booking assignment attempt, plus an older SUPERSEDED evaluation. The screenshot's newest attempt is an in-progress draft. Detail previously selected only latestAttempt for the result card; the history viewer also excluded all graded attempts before deadline.

Detail now selects the existing submitted grading candidate from history independently of the newest draft, with existing latestAttempt fallback if history is unavailable. The result card shows “Xem chi tiết chấm điểm” only when the existing result source returns a published score. The link preserves class, assignment, selected attempt and general-list return context. Continue-editing behavior is unchanged.

Before deadline the existing read-only viewer accepts the current submitted grading candidate, excluding drafts and older submissions. After deadline the established on-time/late history reduction remains. Practice history is unchanged. Backend ownership/enrollment and PUBLISHED-only score/feedback controls remain unchanged; the graded summary flag means candidate, not proof of publication.

## Evidence

- RED:3 failures and10 passes in2 affected page suites (`.tmp/student-result-red.log`).
- Connected frontend assignment-work:26 cases/6 files pass (`.tmp/student-result-green.log`).
- Final affected frontend pages after layout polish:13 cases/2 files pass (`.tmp/student-result-final.log`).
- Existing GradebookServiceTest:9 cases,0 failures/errors/skips (`.tmp/student-result-backend.log`), including publication suppression, sanitized feedback and access controls.
- Final TypeScript and scoped ESLint exit0 (`.tmp/student-result-typecheck-final.log`, `.tmp/student-result-lint-final.log`). Whitespace check passes.

## Scope and limits

Security compliant: no new endpoint or bypass of private grades. Resiliency compliant: existing result-loading/errors and latest-attempt fallback retained. PBT disabled under existing configuration. No dependency/config/schema changes, grade writes, student submission edits, account switches, subagents, commit/push or deploy. Browser inventory was read-only; active local browser is a teacher session, so student visual/live acceptance was not performed. Full build and unrelated test suites were not repeated. Frontend dev server can use the change without a Docker restart; VPS needs its normal release.

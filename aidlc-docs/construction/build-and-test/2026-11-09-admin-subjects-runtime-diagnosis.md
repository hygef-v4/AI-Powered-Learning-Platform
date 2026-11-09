# Admin subjects HTTP500 — runtime investigation

User screenshot showed repeated GET /api/v1/admin/subjects failures and separate session401/CSP console messages. User explicitly requested reading the Frontend terminal in Orca.

## Findings and action

- Read both existing Orca terminals. Frontend showed successful HTML page requests plus retained old notification connection-refused and file-download header errors; none established the subject API500 cause. Backend unexpected U04 exceptions were converted to SUBJECT_INTERNAL_ERROR without server diagnostics, so no original subject stack was available.
- Health and unauthenticated API probes showed backend200 and protected subject401 both directly and through Next proxy. These probes did not test the authenticated Admin request.
- Inspected only non-secret subject metadata read-only; no data changed. Existing HTTP/service regressions passed54 cases; isolated PostgreSQL repository tests passed31 cases with no skips after rerunning with Docker access. Frontend related27 tests/3files passed. Logs: .tmp/admin-subjects-diagnosis-tests.log (initial PostgreSQL31 skipped), admin-subjects-postgres-diagnosis.log (31 actually executed), admin-subjects-frontend-diagnosis.log.
- Added server-only exception logging in SubjectExceptionHandler.unexpected; preserved existing generic client error and all authorization/storage mappings. Fresh54 controller/read tests and JAR packaging passed (.tmp/admin-subjects-diagnostic-package.log). Scoped source diff whitespace passed.
- Confirmed zero RUNNING AI tasks, gracefully interrupted the old backend launcher and restarted scripts/run-backend.bat in its existing Orca terminal. New process34584 started at23:42:54+07:00 with current source; health200. The launcher clean rebuilds target/classes; pre-restart JAR packaging is historical evidence, not a promise that the JAR remains after clean.
- User then explicitly confirmed “Đã tải được môn học”. No new subject error appeared in the inspected output after restart.

## Limits

Runtime recovery is confirmed by the user, but the original500 stack was unavailable; do not claim a proven code/data/CSP root cause or a logic fix. The added server log supports diagnosis if it recurs. No CSP relaxation, authentication workaround, subject data/migration change, browser UI automation, commit or deployment.

Automatic approval review rejected the attempted Chrome tab inventory because the user retained UI testing; diagnosis continued through terminals, read-only backend/data checks and isolated tests. User supplied console text, not the API Response body; no browser-access workaround was used. Existing Security/Resiliency scope retained; PBT disabled.

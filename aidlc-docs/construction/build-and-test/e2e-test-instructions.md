# E2E instructions — role and integration acceptance

Baseline staging@794a4053. Not executed in this documentation-only change. Existing scripts and fixture requirements: [scripts/e2e/README](../../../scripts/e2e/README.md). Seeding mutates data; use only an authorized isolated/local environment and record fixture IDs without credentials.

| Actor | Priority acceptance |
|---|---|
| Student | Own classes/material modules/visible Quiz; no MCP management or bearer use; personal API remains; own attempts/results; group policy/claim behavior |
| Teacher | Current class scope, author/review/publish, Quiz edit/retire/reopen and immutable attempt snapshots; grading proposal review/finalize/publish; optional leader reasons |
| Subject Manager | Current subject modules/materials/bank/classes; OPEN class without teacher; teaching/gradebook only if assigned |
| Admin | Accounts/subjects/Settings/multiple keys; audited institutional-key reveal; own MCP reporting only; dashboard counts/activity/backup partial availability |

For each step record actor/route/expected/current result, time/commit/browser/fixture, screenshot or response evidence, and fail/skip reason. Check keyboard/focus, stale version, changed role/ownership, wrong class, provider unavailable, Mailpit delivery and no secret in logs. UI fixtures do not prove live provider integration.

Relevant scoped guides are listed in the [summary](build-and-test-summary.md); do not combine their historical pass totals into a fresh full-platform result.

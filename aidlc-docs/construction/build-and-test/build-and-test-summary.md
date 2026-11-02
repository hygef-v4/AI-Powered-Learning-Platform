# Build & Test summary — U14,2026-10-21

Scope: `announcements-and-notifications`, UC63–65. User approved code-generation step11 via “tiếp tục”; all11 code-generation steps are complete. Source subsequently split into7local commits `6cef92a` through `adfc51f` with documentation committed separately, per user request; no source changes during commit preparation. Global Build & Test is **not complete** and Operations readiness is **not established**: owner dependencies and independent acceptance remain open.

## Available results

| Check | Actual result and evidence |
|---|---|
| Final-source full backend `mvn -B verify` |BUILD SUCCESS,921total/844passed/77skipped/0failures/errors;85s, finished2026-10-21T16:35:07+07:00; `.tmp/u14-build-test-verify.log` |
| Artifact |Executable `backend/target/platform-backend-0.1.0-SNAPSHOT.jar`; JDK21 runtime, Java17 target; JDK17 execution not claimed |
| Focused backend (previous final-source run) |71passed:66U14+3U01+2architecture; separate run, not added to full totals |
| Frontend |314passed/61files; lint0warnings/errors, typecheck and production standalone build passed; retained final-source evidence, not rerun this continuation |
| Contract/security smoke |OpenAPI syntax/localrefs and production nonce/header/login/announcements/404/repeated-request checks passed; production npm audit0vulnerabilities |
| Desktop fixture |Teacher CRUD, Student read-only/preferences/read-all, no CSP/hydration console errors; fixture-only, no real provider acceptance |
| Independent integration/E2E/security/performance |Not run; no measured latency, capacity, coverage percentage or independent acceptance claimed |

Detailed logs and earlier unsuccessful environment/timeout attempts are listed in [verification handoff](u14-verification.md). Logs under `.tmp` are local ignored evidence, not committed artifacts. Skips remain skips; full counts include existing Surefire integration tests and are not unit-only counts.

## Runtime prerequisites and remaining gates

Docker CLI is installed but Engine is not running: `docker version`/`docker ps` cannot reach the `dockerDesktopLinuxEngine` named pipe. No local stack was started. U09 grading migration references real U07 `assignments` and U08 `assignment_attempts` schemas absent in this checkout. Their owner migrations must be supplied; no fake schema was created. U07/U08/U12 providers remain stubs, so assignment/group/reminder and two-instance SSE behavior needs real integration.

After owner prerequisites and Engine availability, an independent tester must execute the [full matrix](u14-verification.md): DB/CAS/rollback/audit, actual event payloads, broker retries, Mailpit failures/cap/midnight/priority, reminder changed deadlines/cardinality, retention, real sessions/CSRF, proxy headers, mobile and measured NFR. Preserve the explicitly approved IMail limitation: SMTP may accept before a crash prevents recording SENT; current interface cannot guarantee exactly-once in that window.

## Instructions and tracking

- [Build](build-instructions.md)
- [Unit/existing automated suites](unit-test-instructions.md)
- [Integration](integration-test-instructions.md)
- [Performance](performance-test-instructions.md)
- [Security and contract](security-test-instructions.md)
- [End-to-end](e2e-test-instructions.md)
- [Execution checklist](u14-build-and-test-plan.md)

Enabled Security/Resiliency controls have developer evidence in [code summary](../u14-announcements-and-notifications/code/code-summary.md); runtime effectiveness remains pending where listed. PBT stays disabled. No application source changed during Build & Test or commit preparation, no real email sent, no push/deploy or global stage advance performed. Local commits were explicitly authorized in the subsequent user request.

## MCP staff expansion — 2026-10-26

See [MCP staff expansion results](2026-10-26-mcp-staff-expansion-results.md) for exact commands, test counts, the isolated PostgreSQL migration/retry checks, limits and [manual acceptance](2026-10-26-mcp-staff-expansion-manual-test-guide.md). Bounded protocol/batch checks are verified; load/performance certification and new live-account acceptance are pending. No live stack migration is part of automated verification.

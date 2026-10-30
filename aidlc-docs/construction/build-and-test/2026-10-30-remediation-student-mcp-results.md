# Audit remediation and Student MCP — verification results

Implemented in the working tree on `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`, preserving the earlier documentation changes. This report supersedes the unresolved implementation findings in the [initial audit](2026-10-30-unit-test-business-rule-audit.md); the dated audit remains unchanged as historical evidence. Work continued directly after the user requested no further subagents.

## Delivered behavior

| Scope | Change and regression evidence |
|---|---|
| U03 jobs | Valid deliveries ACK only after completion, durable terminal handling or retry handoff. Failure of those operations requeues. Invalid/missing envelope fields and attempts outside 0–5 are discarded. `JobDispatcherTest` and `JobListenerTest` cover these branches. |
| U03 backup | Duplicate deliveries serialize through the backup row transaction. A lost success update throws for retry/rollback; it cannot become a successful ACK. Purge runs after successful commit, independently of completed transaction resources; rollback never purges archives. Error output uses a finite application-owned message allowlist. `BackupJobHandlerTest` and real-PostgreSQL `BackupConcurrentDeliveryIntegrationTest` cover CAS, retention and duplicate workers. |
| U05 | Corrected impossible HTTP fixtures to create DRAFT, clear teacher with null and use monotonic versions. Batch enrollment locks/rechecks each target's current account role/status inside its row transaction. `AcademicsApiContractTest`, `AcademicsBatchTransactionTest` and `AcademicsServiceTest` verify the behavior. |
| U06 | Student/Teacher/Subject Manager can manage/authenticate their own MCP credentials. Admin is denied, including old tokens and catalog lookups. Added negative reveal cases and real-DB institutional-key ordering, personal uniqueness, token expiry/hash and legacy ciphertext checks. Normal Admin web key/quota/usage APIs remain authorized. |
| U07/U09 | Corrected duplicate-feedback and cross-guard fixtures; negative assertions now identify the intended error. Docs describe existing partial item scores and structured-feedback review/finalize gates. No scoring algorithm was changed to fit stale binary-scoring prose. |
| U08 MCP | Restored `read_own_work` and `record_private_feedback` for own submitted PRACTICE Assignment attempts. Reuses the actual feedback service, ownership/enrollment/hash/schema/pending/daily-limit checks. No rubric or official grade is exposed. READ_ONLY cannot write. `PracticeFeedbackMcpParityTest` covers service and MCP protocol dispatch. |
| U08 concurrency | API and MCP take the same transaction-scoped advisory lock per account before pending/quota checks and insertion. Five real-PostgreSQL test invocations exercise both transport orders, a pending API reservation, independent accounts and sequential shared consumption. The race test uses a one-slot configured budget to isolate the boundary; production default remains 20 per day. Provider submission stays outside the reservation transaction. |
| U10 | OPEN editing is tested separately from RETIRED rejection. Retiring never-published Quiz preserves null publication; reopen returns DRAFT when null, otherwise OPEN. RETIRED alone cannot authorize a new version or expose an unpublished subject Quiz to class teachers/material modules. `QuizView` carries nullable publication history. Domain/service/HTTP/JDBC tests cover lifecycle, guards and projections. |
| U10 migration | Added `V20261030_1810__preserve_unpublished_quiz_retirement.sql`. Legacy equal publication/retirement timestamps require a successful `QUIZ_PUBLISHED` audit for the exact QUIZ object to keep publication. Missing/denied/wrong-object proof resets publication to null, requiring review before publication. `QuizPublicationMigrationIntegrationTest` verifies an actual Flyway upgrade. New genuine same-instant publication is preserved by domain tests. |
| U11 | Result `canRetry` uses current role/enrollment, OPEN class/Quiz, scope and current attempt limit. Own historical result access remains separate from retry eligibility. Added current-state, hidden-result and snapshot assertions. |
| U12 | Added stale/lost document CAS, resubmission snapshot and unchanged auto-close assertions; optional reason whitespace/length boundaries and group-set CAS. Group repository integration now uses disposable PostgreSQL with actual migrations and restricted application credentials. |
| U13 | Corrected cross-subject fixture permission so it reaches the intended guard. Added module rename/reorder/version/last-module/visibility and material policy/CAS/lifecycle assertions. Teacher module views exclude RETIRED subject Quiz without actual publication. |
| U14/U15 | Added important × sendEmail recipient matrix/current-group recipients; dashboard activity HTTP role/anonymous/query/no-store/no-read-on-denial and independent unavailable/null-source branches. |
| Documentation/UI | Removed Admin MCP tab and redirected its legacy route to normal AI settings. Student has Agent MCP under Kết nối AI. Updated canonical BR/UC/design/guides and material contract. BF-06/BF-08 sources, combined draw.io and PNG exports synchronized and visually inspected. |

Student MCP follows the same AI business operation as the personal API: **UC41 submitted Practice Assignment feedback**. UC52 remains configured Quiz results/answer explanations. This change adds no AI Quiz explanation service. Teacher/Manager tools retain their existing scope.

## Fresh verification

| Check | Result |
|---|---|
| Focused final backend regressions | 104 passed, 0 failures/errors/skips |
| Full backend `mvn verify` | BUILD SUCCESS; 1,611 passed, 0 failures/errors/skips; finished 2026-10-30 13:46:41 +07 |
| Required PostgreSQL guard | PASS: SubjectRepositoryIntegrationTest 31 tests; NotificationDeliveryIntegrationTest 2 tests; no skips |
| Frontend Vitest | 599 passed across 94 files, 0 failures/pending; JSON success=true |
| Frontend lint/typecheck/build | Passed; lint has 0 errors and 2 existing unused-import warnings (`ai/register.ts`, `identity/admin/AccountDetailPage.tsx`) |

[Machine-readable evidence](2026-10-30-remediation-student-mcp-evidence.json) records suite totals and selected regressions. Maven includes unit, HTTP, architecture and integration cases; these totals are not a coverage percentage or proof of all 66 UC scenarios.

RED evidence was retained before fixes for worker terminal/retry handling, backup retention/CAS, target eligibility, Quiz lifecycle/visibility, API/MCP concurrency and Student/Admin UI scope. Relevant logs are `.tmp/2026-10-30-remediation-{combined-red,retention-red,enrollment-red,final-review-red,migration-material-red,mcp-ui-red}.log`. Compilation errors and the initial migration fixture timestamp-binding error are setup failures, not regression proof. The corrected migration test passed against the final implementation.

Final logs: `.tmp/2026-10-30-remediation-backend-final.log`, `...-db-guard.log`, `...-frontend-full.json`, `...-lint.log`, `...-typecheck.log`, `...-build.log`. Disposable PostgreSQL 16/Testcontainers exercised actual migrations; U06/U12 no longer require externally configured test databases. JDK 21.0.12.1 targets Java 17; Node 25.8.1/npm 11.11.0 differ from CI Java 17/Node 22.

## Practical limits and release notes

- Backup now holds one connection and row lock through dump/upload. Other readers see the last committed state until completion. Duplicate delivery and retention were tested; a production-sized dump/restore/load test was not run.
- The new Quiz migration intentionally sends ambiguous legacy publication back through review when proof is absent. It was exercised only in disposable test databases and has not been applied to an application database.
- Real PostgreSQL races cover backup and shared feedback reservation. Enrollment account changes are tested at service/transaction boundaries; no new multi-instance enrollment/archive stress test was added. U02 DB dedup rollback and U13 populated legacy module-backfill/cascade scenarios still need dedicated integration acceptance beyond existing service tests.
- Broker delivery was tested at the listener/dispatcher boundary with mocks; real broker disconnect/confirm fault injection, real AI/Drive/SMTP providers, browser end-to-end acceptance, performance/security certification and backup restore remain separate acceptance work. Applicable security/resiliency scope is preserved; property-based testing remains disabled.
- No commit, push or deployment was performed. No live application data or external provider was used.

Approved code/test/document remediation is complete at the stated automated-verification scope. Broader platform acceptance remains pending and is not inferred from passing tests.


## Subsequent local commit request

The user subsequently authorized local commits with detailed descriptions. Pre-commit verification finished at 14:00:27 +07 on 2026-10-30: backend 1,611 passed/0 skipped, frontend 599 passed/94 files/0 pending, required PostgreSQL guard passed. Earlier no-commit statements above describe the report snapshot before this request. The commit series separates MCP access/feedback, core correctness fixes, additional business-rule tests and documentation. Git history records the resulting commit IDs; no push, deployment or application database migration is authorized by this request.

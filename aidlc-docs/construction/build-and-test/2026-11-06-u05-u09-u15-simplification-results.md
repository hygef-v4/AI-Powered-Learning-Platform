# U05/U09/U15 behavior-preserving simplification — 2026-11-06

Status: all three refactor tasks, independent review and full backend verification complete. 1,822 tests passed, zero failures/errors/skips; executable JAR built. [Approved plan](../plans/code-generation-plan/2026-11-06-u05-u09-u15-simplification.md).

## Scope and behavior

Compared against `.tmp/simplification-20261106/before/`, not HEAD, because the worktree already contained 71 formatted Java files. Snapshot/hash manifest covers 1,581 source/test files. Exactly seven production and three test files changed; frontend unchanged. No contract, endpoint/JSON/DTO/schema/migration, dependency or application configuration change. Prior formatting retained. Production line count is seven lines smaller; this is readability/duplication cleanup, not a measured performance improvement.

- U05: inline the sole `mutateClass` caller into `updateClassInfo`; retain executeCommand, lock/version, clock/read/update, audit and mapping order.
- U09: package-private static `GradingCommands.validateBatchItems` owns the identical 1–200 guard; three existing callers retain the same validation position, errors, transaction/dispatch loops and DTO annotations.
- U15: move the identical source-to-section helper into existing `DashboardSourceReader.readSection`. Keep await outside the mapper catch; input null/unavailable remains unavailable, successful mapping to null remains AVAILABLE(null). Two service helpers removed, all submit/deadline/cancel behavior preserved. Reader-to-Section dependency remains inside U15 and passes ArchitectureTest.

## Fresh verification evidence

All logs below are under ignored `.tmp/simplification-20261106/`.

| Check | Result | Log |
|---|---|---|
| Initial scoped baseline before changes | 246 passed, zero failures/errors/skips | `baseline.log` |
| U05 characterization before production edit | 175 passed, including three new cases | `u05-characterization.log` |
| U05 after inline | 175 passed, zero failures/errors/skips | `u05-after.log` |
| U09 characterization before production edit | 30 passed, including 15 new parameterized cases | `u09-characterization.log` |
| U09 after shared guard | 30 passed, zero failures/errors/skips | `u09-after.log` |
| U15 + ArchitectureTest before/after | 59 passed in each run; reuse existing null/mapping/empty-backup/deadline tests | `u15-before.log`, `u15-after.log` |
| Independent read-only review | No actionable findings; compared only refactor changes with snapshot | Session reviewer |
| Formatter on touched production files | Exact tokens/comments/literals preserved; idempotence passed | Session formatter output |
| Source scope and whitespace | Seven production/three test files only; `git diff --check` passed | Snapshot manifest and session output |
| Full backend verify with Docker | 1,822 passed, zero failures/errors/skips; executable JAR packaged; BUILD SUCCESS at 2026-11-06T14:17:44+07:00 | `full-verify-docker.log` |
| Required real PostgreSQL suites | SubjectRepositoryIntegrationTest 31 + NotificationDeliveryIntegrationTest 2 passed, no skips | `scripts/assert-required-backend-tests.ps1` |

Characterization was run green before and after refactor: no behavior change or bug fix was claimed. U05 protects stale version ahead of invalid fields, expected version update, success audit inside transaction and suppression of writes/success audit on invalid input. U09 covers null/0/1/200/201 input boundaries, independent finalize/publish transactions, no work for invalid batches, proposal/estimate results, and no AI dispatch for estimates. Existing U15 coverage distinguishes input null from AVAILABLE(null), failure isolation and deadlines.

## Environment and limits

JDK 17; existing Maven cache/settings and preloaded Byte Buddy agent. Verification-only JVM options cap Maven at 192 MB heap and fork at 256 MB with SerialGC, limited metaspace/code cache and two processors. No project dependency/configuration edits. Initial launcher attempt stopped on a native code-cache warning; corrected scratch PowerShell error handling before collecting baseline.

First full verify had zero assertion failures, two environment errors and 111 skipped because Docker Desktop was not running (`full-verify.log`); it is not a passing result. Started Docker Desktop with its CLI and confirmed Engine 29.5.3, then reran without source changes: full verify and required PostgreSQL guard passed. Database tests use disposable containers, not the live platform database.

No frontend runtime/build rerun, manual UI/provider acceptance, live migration, app restart, commit/push/PR or deployment. Docker Desktop remains running after integration verification. Existing security/resiliency scope retained; authorization/audit/error/deadline regression checks included. PBT remains disabled.

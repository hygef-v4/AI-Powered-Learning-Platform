# U03/U15 backup completion — execution evidence, 2026-10-22

## Scope and approval

User approved D1–D7 and B1–B5, including U03 provider work: “thôi thắng nhờ tôi sửa luôn r, giúp tôi duyệt plan và sửa u03 và u15”. Planning commit `32abe4bc` is local; implementation continues on `u15-backup-completion-plan` (renamed from `codex/u15-backup-completion-plan` in the desktop app). No implementation commit, push, merge or Jira transition is implied by approval.

U03 adds IBackupDashboardStatus, BackupDashboardRunView and BackupDashboardSummary. Existing IBackupStatus, BackupRunView and job lifecycle remain unchanged. BackupStatusService implements both ports, maps actual started_at, whitelists public failure messages, and exposes transactional reads with timeout1s. Repository summary is one parameterized SQL statement for a consistent snapshot: terminal FAILED rows in [from,to), historical latest SUCCESS before to, and terminal timestamp integrity.

U15 submits eight sources to its existing bounded executor. A single summary result supplies latestSuccessfulAt and failuresLast7Days. Invalid/null/failed summary degrades only those sections. HTTP paths, paging, guards, privacy and no-store stay unchanged; AVAILABLE latestSuccessfulAt.data becomes nullable as approved. UI distinguishes never-success from outage and renders zero failures and Vietnam timestamps.

## Test-first evidence

- B1 RED: contract/fallback tests could not compile without the new capability. GREEN: eight focused contract/stub tests passed.
- B2 RED: service tests could not compile without richer reads and summarize/SummaryRow. GREEN: provider tests passed; legacy contract remains unchanged.
- B3 RED: consumer fixture could not compile against the old injected port. GREEN: actual dashboard/service/API/authorization/architecture focused run passed24 tests. Further cases cover available-null JSON and healthy sections when summary fails.
- B4 RED: never-success assertion failed because old UI showed “Chưa có dữ liệu”. GREEN: dashboard22 tests passed; final suite includes additional Vietnam time/safe-error coverage.
- Ruling: retain the existing fixed fixture Clock (2026-10-20) instead of replacing every test with2026-10-22; assertions still verify the exact168h window and one summary call.
- Ruling: plan package wildcards did not select all intended tests under this local Maven invocation; use explicit class lists and the unfiltered full verify instead. No claim that the initial four-test baseline covered the whole backup/dashboard suite.

## PostgreSQL16 evidence

Testcontainers creates a dedicated database, applies only the production backup migration, and instantiates the real repository plus Spring transactional BackupStatusService. Application DB contents are untouched.

Integration coverage includes inclusive from/exclusive to, final-state counting, older/purged success, actual start versus snapshot/completion, latest failure ordering and sanitization including null error, empty history/zero/never-success, malformed terminal metadata, and concurrent updates without mixing snapshots. The first six-test run passed with no skips; the final test parametrizes table-lock timeout and connection return for summary/latest/history.

The lock assertion permits less than3s for a nominal1s JDBC timeout, verifies only the blocking connection remains active, then verifies the next read succeeds and all connections return to the pool. This is local resource-release evidence, not an end-to-end dashboard latency certification.

## Verification and environment

- Frontend first full run:447 tests/78 files passed. Lint, typecheck, production build passed; build used `.next-u15-backup-20261022` and tsconfig was restored and typechecked again.
- OpenAPI YAML parses; all local references resolve. Legacy port/record and frontend tsconfig have no diff.
- Initial full backend run:1281 tests,1 failure,22 errors,35 skipped. BackupJobHandlerTest relied on default encoding when decoding Vietnamese UTF-8. HttpProviderGatewayTest and Redis integration tests failed creating Java Windows loopback sockets. These existing files were not changed.
- UTF-8 resolved the encoding failure. Reading the installed JDK17 sources showed UnixDomainSocketsUtil uses jdk.net.unixdomain.tmpdir/TEMP before java.io.tmpdir; setting only java.io.tmpdir did not fix sockets. Explicit short jdk.net.unixdomain.tmpdir resolved all22 loopback/Redis tests without source edits.
- Final full backend verify:1285 tests,0 failures,0 errors,35 skipped;1250 executed and passed. Jar repackaging completed. Skips require separate U04/U06/U12 test JDBC configuration, not Docker availability for the new backup tests. Final backup integration8/8 passed, all three new reads release connections after lock timeout.
- Final frontend448 tests/78 files passed. Production files are unchanged since the successful lint/typecheck/build; only extra test coverage and documentation were added afterwards. Fresh lint/typecheck were rerun before handoff.

Reproduce backend with JDK17 and existing cached Maven:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
$u15Maven = 'C:/Users/Quang Hung/.m2/wrapper/dists/apache-maven-3.9.12/59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798/bin/mvn.cmd'
& $u15Maven -o -s scripts/maven-local-cache-settings.xml -f backend/pom.xml '-DargLine=-Dfile.encoding=UTF-8 -Djdk.net.unixdomain.tmpdir=F:/code/git/AI-Powered-Learning-Platform/.tmp/java -javaagent:F:/code/git/AI-Powered-Learning-Platform/.tmp/byte-buddy-agent.jar' verify
```

Ensure `.tmp/java` exists. Docker access is required for Testcontainers. These are local verification options, not application configuration changes.

## Review and remaining acceptance

One independent read-only whole-change review found no concrete correctness/contract/security issue. Reviewer did not independently rerun tests. After feedback, timeout coverage was extended from summary to all three new reads.

C01/C02 implementation no longer depends on a missing provider. Full UC66 remains subject to independent real-session/audit/browser acceptance, full-app stubs-disabled startup and representative dashboard p95/fault-load benchmarks. Isolated DB and MockMvc tests do not prove these gates, backup upload/encryption or restore readiness. No new migration, index, dependency, endpoint or backup action was added.

Security03/05/08/12/15 and Resiliency10 receive local boundary/query/privacy/timeout coverage. Security04/09 and Resiliency06 retain existing platform configuration and full-runtime gates. Resiliency04 rollback remains consumer rollback to the legacy port; no data rollback is required. Other rules remain N/A under the project's reduced scope; PBT stays disabled.

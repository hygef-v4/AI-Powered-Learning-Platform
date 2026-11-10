# Full test/build after U09 structure cleanup — 2026-11-06

Requested: “chạy lại toàn bộ test/build”. Final verification passed without application/test/dependency/configuration edits beyond the previously approved U09 move/rename. No commit, push, deployment or application restart.

## Final results

| Check | Fresh result |
|---|---|
| Backend full Maven verify | 1,830 tests; zero failures/errors/skips; BUILD SUCCESS at 20:33:16 +07:00 |
| Backend packaging | Spring Boot executable JAR generated |
| Required PostgreSQL guard | SubjectRepositoryIntegrationTest 31, NotificationDeliveryIntegrationTest 2, EvaluationVersionIntegrationTest 10, GradeDistributionVisibilityRepositoryTest 1; all passed without skips |
| Frontend full tests | 799 tests across 116 files passed; two workers; original timeouts retained |
| Frontend full lint | Exit 0, no warnings |
| Frontend production build | Exit 0; BUILD_ID and standalone/server.js present |
| Frontend typecheck after build | Exit 0 |

All final Surefire XML reports were independently summed: 1,830 tests, zero failures/errors/skips. Backend artifact: `.tmp/u09-full-verification-20261106/target/platform-backend-0.1.0-SNAPSHOT.jar`. Frontend artifacts: `frontend/.next/`.

## Execution environment and earlier failed attempts

Logs are under ignored `.tmp/u09-full-verification-20261106/`. The final backend log is `backend-verify-corrected-path.log`; frontend logs are `frontend-tests-two-workers.log`, `frontend-lint.log`, `frontend-build.log`, `frontend-typecheck.log`.

- Initial offline clean could not resolve maven-clean-plugin 3.4.1. Authorized online retry downloaded it into the existing local scratch cache; project dependencies/POM unchanged.
- First full backend run used shared `backend/target`: 1,828 tests with 14 failures/217 errors. Missing classes/resources and output timestamps changing after test startup, together with a running Java language server, indicated interference with shared output. This run is not passing evidence.
- Used a scratch POM in the backend directory with only `build.directory` changed. Exact XML comparison confirmed no other POM differences. Isolated output was compiled from scratch. Initial 256 MB test heap exhausted during ArchUnit after 1,827 passing tests; raised only the scratch test JVM heap to 768 MB.
- The first isolated directory name `backend-target` caused ArchUnit to include test classes: its built-in Maven exclusion specifically matches `target/test-classes`. Corrected the temporary output name to `target`; kept all test rules unchanged. The final full verify passed, including all three architecture rules, and packaged the JAR.
- Default-worker frontend run: 795 passed/four five-second timeouts. Full rerun with `npm test -- --maxWorkers=2`: 799 passed. No timeout increases or source changes. jsdom scroll/navigation diagnostics were non-failing.

Final backend used JDK 17, existing Maven settings/cache and Byte Buddy agent; Maven heap 192 MB, test fork heap 768 MB, SerialGC/two processors. Original POM, manifests and lockfile unchanged. Scratch POM was archived under `.tmp` and removed from `backend/` after verification. Failed-run reports/logs retained separately.

## Limits

Tests used disposable PostgreSQL/Redis containers. Docker Desktop and Java language server were left running. No manual browser/provider acceptance, performance/restore assessment, deployment or live data migration. Existing security/resiliency scope retained; PBT remains disabled. Passing automated checks do not establish complete operational acceptance.

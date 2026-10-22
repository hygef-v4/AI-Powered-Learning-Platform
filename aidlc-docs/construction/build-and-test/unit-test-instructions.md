# Unit and existing automated test execution — U14

From `backend/`:

```powershell
mvn -B verify
mvn -B '-Dtest=edu.aiplatform.announcements.**,edu.aiplatform.notifications.**,NotificationAccountLookupTest,ArchitectureTest' package
```

The second command is a focused diagnostic; do not add its counts to the full suite. Existing repository tests include integration tests under Surefire, so the full count is not a unit-only count. Docker-dependent tests can skip when Engine is unavailable. There is no separate Maven Failsafe profile in this checkout.

From `frontend/`:

```powershell
npm test -- --maxWorkers=2
npm run lint
npm run typecheck
```

Read Maven XML/text reports under `backend/target/surefire-reports` and Vitest output. Final-source baseline: backend921 total,844 passed,77 skipped,0 failures/errors; frontend314 passed across61files. Coverage percentage was not measured; test counts are not coverage.

U14 focused checks cover scoped announcements/CAS, event schema/recipient resolution, cap/preference/retry/reminders, API authorization and frontend UI/nonce behavior. Mocked provider tests cannot certify real database locks, SMTP delivery or two-instance SSE. See the [independent tester matrix](u14-verification.md).

When a check fails, retain failure output, distinguish application failure from environment failure, fix the relevant cause and rerun affected checks. Do not count a skipped case as passed or increase timeouts solely to hide failures. Record test author and execution environment separately for independent acceptance.

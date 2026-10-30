# Automated test instructions — all units

Current structure: parent HEAD `5a76e428` + working-tree refactor; [fresh verification results](2026-10-30-package-moves-only-results.md). Maven verify includes existing integration suites, so do not call its entire count unit-only.

```powershell
Push-Location backend
mvn -B verify
Pop-Location
./scripts/assert-required-backend-tests.ps1
Push-Location frontend
npm test
Pop-Location
```

Record commit/dirty source, JDK/Node versions, exact command, suite/test/pass/fail/error/skip counts and log path. Check Surefire XML and Vitest output; skipped/disabled/environment-gated cases remain distinct. Baseline existence and per-unit test paths are in the [source manifest](../../inception/reverse-engineering/current-code-baseline.md). The changed contracts require role/owner, revoked/expired/no ciphertext, OPEN null teacher, optional reason >1,000, module stale/version/delete/visibility and Quiz reopen boundary cases; inspect existing assertions before claiming coverage.

Only source/test package, import and path references change. Existing test bodies and assertions are retained; ArchitectureTest layer selectors follow the relocated packages. See [build instructions](build-instructions.md) for CI command order and [evidence](build-and-test-summary.md) for historical runs.

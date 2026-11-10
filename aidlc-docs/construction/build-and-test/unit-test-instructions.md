# Automated test instructions — all units

Current source snapshot: `f68a6ec5`, 2026-11-07. [Dated evidence and post-merge limits](build-and-test-summary.md). Maven verify includes existing integration suites, so do not call its entire count unit-only.

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

This documentation sync does not modify tests. Run the full suite for the source/config actually under review; the earlier move-only change does not describe all later feature changes. See [build instructions](build-instructions.md) for CI command order, four required PostgreSQL gates and constrained-machine guidance.

## Current frontend test layout — 2026-11-07

Test files live under `frontend/tests/`, mirroring paths below `src/`; test-only fixtures remain there. Vitest setup is `tests/setup.ts`, include is `tests/**/*.test.{ts,tsx}` and `@/` points to production `src/`. `npm test` is the full-suite command; `npm test -- --maxWorkers=2` limits concurrency on constrained machines without raising test timeouts. The dated 06/11 run had 116 files/799 tests; do not treat that count as a fixed gate. Do not run old `src/**/*.test` paths from dated reports.

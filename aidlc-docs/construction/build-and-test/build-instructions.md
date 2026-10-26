# Build instructions — U14 checkout

Scope: UC63–65 on `announcements-and-notifications`,2026-10-21. These instructions cover the current repository build; they do not certify completion of other units. See [summary](build-and-test-summary.md).

## Prerequisites

- Maven3.9+, JDK17 target; local verification used JDK21. CI uses JDK17 and must retain its own result.
- Node22 as CI baseline (package minimum20.9), npm and committed lockfile. Use the repository's existing versions, no automatic upgrades.
- Dependency registry access/cache and writable build/temp directories. Docker Engine is required for Testcontainers and full local infrastructure.
- Runtime configuration must follow `infra/docker-compose.yml` and local overrides. U14 uses existing `APP_PUBLIC_URL` and `U14_EMAIL_DAILY_CAP` (default300). Do not copy credentials into reports.

## Build and checks

From repository root, run sequentially in PowerShell:

```powershell
Set-Location backend
mvn -B verify
Set-Location ../frontend
npm ci --no-audit --no-fund
npm run lint
npm run typecheck
npm test -- --maxWorkers=2
npm run build
```

For a clean backend build use `mvn -B clean verify`. Review `backend/target/surefire-reports` including skipped tests, Maven BUILD SUCCESS and executable `backend/target/platform-backend-0.1.0-SNAPSHOT.jar`. Frontend output is under `frontend/.next`, with standalone output enabled by existing configuration. A successful build does not validate database startup or real provider integration.

Do not run full frontend tests concurrently with production compilation on a constrained machine. After a branch switch, stale generated Next route types may need regeneration; verify the exact generated directory before removal and preserve source files. For dependency/cache permission failures, repair access or use approved execution; never suppress compilation/test failures.

Local integration startup prerequisites and the safe Mailpit configuration are in [integration instructions](integration-test-instructions.md). No secret file needs to be changed for these build commands.

## MCP staff expansion — 2026-10-26

See [MCP staff expansion results](2026-10-26-mcp-staff-expansion-results.md) for exact commands, test counts, the isolated PostgreSQL migration/retry checks, limits and [manual acceptance](2026-10-26-mcp-staff-expansion-manual-test-guide.md). Bounded protocol/batch checks are verified; load/performance certification and new live-account acceptance are pending. No live stack migration is part of automated verification.

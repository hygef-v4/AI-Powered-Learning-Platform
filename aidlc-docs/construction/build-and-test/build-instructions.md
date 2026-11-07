# Build instructions — current repository

Source snapshot `f68a6ec5`, 2026-11-07. Commands match CI; not executed in this documentation-only synchronization. JDK17/Maven, Node22/npm (package minimum Node20.9), dependency cache/access and Docker for Testcontainers are prerequisites.

From repository root, sequential PowerShell:

```powershell
Push-Location backend
mvn -B verify
Pop-Location
./scripts/assert-required-backend-tests.ps1
Push-Location frontend
npm ci --no-audit --no-fund
npm run lint
npm run typecheck
npm test
npm run build
Pop-Location
```

Check each exit code before continuing. Required PostgreSQL suites cannot skip: SubjectRepositoryIntegrationTest ≥31, NotificationDeliveryIntegrationTest ≥2, EvaluationVersionIntegrationTest ≥10 and GradeDistributionVisibilityRepositoryTest ≥1. The [guard](../../../scripts/assert-required-backend-tests.ps1) is the source of truth and rejects failures/errors/skips. It reads backend/target/surefire-reports; missing/stale reports do not prove a new run. JAR under backend/target, Next output under frontend/.next. Run tests/build sequentially on constrained machines. On Windows, avoid sharing Maven output with an IDE auto-build; isolated output must retain the `target/test-classes` suffix so ArchUnit excludes tests. Size the test JVM heap for the full suite; do not weaken test rules/timeouts to obtain green.

[CI source](../../../.github/workflows/ci.yml), [evidence summary](build-and-test-summary.md). Images are pushed on main after backend/frontend checks; CI also runs shellcheck and fake-command deploy-script tests. CI does not SSH into the VPS. The VPS timer polls the latest successful main workflow and pulls/deploys its SHA, health-gating and rolling back on failure; see [deploy runbook](../../../docs/runbooks/deploy-vps.md). A successful main push can therefore trigger deployment through the VPS pull process. This doc sync does not run Compose, contact the VPS or verify live timer status.

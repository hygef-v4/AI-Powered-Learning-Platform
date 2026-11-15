# Build instructions — current repository

Commands checked against CI on `staging@7e0e8375`. Prerequisites include JDK 17, Maven, Node 22 and npm (package minimum Node 20.9), dependency cache or access, and Docker for Testcontainers.

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

Check each exit code before continuing. The [guard script](../../../scripts/assert-required-backend-tests.ps1) defines the five required PostgreSQL suites and their minimum counts, including PlatformWorkflowIntegrationTest (at least 24). It rejects missing suites, failures, errors and skips. It reads backend/target/surefire-reports; missing/stale reports do not prove a new run. JAR under backend/target, Next output under frontend/.next. Run tests/build sequentially on constrained machines. On Windows, avoid sharing Maven output with an IDE auto-build; isolated output must retain the `target/test-classes` suffix so ArchUnit excludes tests. Size the test JVM heap for the full suite; do not weaken test rules/timeouts to obtain green.

[CI source](../../../.github/workflows/ci.yml), [evidence summary](build-and-test-summary.md). Images are pushed on main after backend/frontend checks; CI also runs shellcheck and fake-command deploy-script tests. CI does not SSH into the VPS. The VPS timer polls the latest successful main workflow and pulls/deploys its SHA, health-gating and rolling back on failure; see [deploy runbook](../../../docs/runbooks/deploy-vps.md). A successful main push can therefore trigger deployment through the VPS pull process. This doc sync does not run Compose, contact the VPS or verify live timer status.

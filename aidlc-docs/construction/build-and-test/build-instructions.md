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

Check each exit code before continuing. The [guard script](../../../scripts/assert-required-backend-tests.ps1) requires seven named PostgreSQL suites, including at least 9 tests in `QuizAttemptRepositoryIntegrationTest` and at least 24 in `PlatformWorkflowIntegrationTest`. It also checks the traced test methods in the [integration manifest](2026-11-08-integration-test-cases.json). The guard reads `backend/target/surefire-reports` and rejects missing reports, missing required cases, failures, errors and skips. Verify the reports came from the current run.

The JAR is under `backend/target` and the Next output is under `frontend/.next`. Run tests and builds sequentially on constrained machines. On Windows, avoid sharing Maven output with an IDE auto-build. An isolated output directory needs the `target/test-classes` suffix so ArchUnit excludes tests. Size the test JVM heap for the full suite.

[CI source](../../../.github/workflows/ci.yml), [evidence summary](build-and-test-summary.md). Images are pushed on main after backend/frontend checks; CI also runs shellcheck and fake-command deploy-script tests. CI does not SSH into the VPS. The VPS timer polls the latest successful main workflow and pulls/deploys its SHA, health-gating and rolling back on failure; see [deploy runbook](../../../docs/runbooks/deploy-vps.md). A successful main push can therefore trigger deployment through the VPS pull process. This doc sync does not run Compose, contact the VPS or verify live timer status.

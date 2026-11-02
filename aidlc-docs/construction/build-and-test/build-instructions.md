# Build instructions — current repository

Baseline staging@794a4053, 2026-10-30. Commands match CI; not executed in this documentation-only synchronization. JDK17/Maven, Node22/npm (package minimum Node20.9), dependency cache/access and Docker for Testcontainers are prerequisites.

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

Check each exit code before continuing. Required PostgreSQL suites cannot skip: SubjectRepositoryIntegrationTest ≥31 and NotificationDeliveryIntegrationTest ≥2. The guard reads backend/target/surefire-reports; missing/stale reports do not prove a new run. JAR under backend/target, Next output under frontend/.next. Run tests/build sequentially on constrained machines.

[CI source](../../../.github/workflows/ci.yml), [guard](../../../scripts/assert-required-backend-tests.ps1), [evidence summary](build-and-test-summary.md). GitHub images are pushed only on main after checks; deploy job is explicitly disabled. Compose build/start is separate from tests, requires approved local configuration, and is not performed by this doc sync.

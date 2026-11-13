# Local restart and fresh verification — 2026-11-06

User request: “giúp tôi chạy lại backend frontend , chạy test”. Source unchanged since the approved U05/U09/U15 simplification. Logs and generated-config snapshots are in ignored `.tmp/restart-test-20261106/`.

Completed: backend http://localhost:8080 and frontend http://localhost:3000 are running; fresh backend/frontend verification and HTTP smoke checks passed.

## Execution checklist

- [x] Inspect listeners/processes: no backend/frontend listening on 8080/3000; local PostgreSQL/Redis/RabbitMQ healthy, old Docker worker stopped.
- [x] Start the existing Mailpit container; preserve all volumes and credentials.
- [x] Capture source hashes and original frontend generated-config files before checks.
- [x] Fresh frontend tests: 799 passed / 116 files, zero failures; production build passed in a separate Next output directory.
- [x] Frontend full ESLint and TypeScript checks passed.
- [x] Fresh backend verify: 1,822 passed, no failures/errors/skips, JAR packaged; required PostgreSQL guard passed 31 + 2 cases.
- [x] Launch latest backend JAR with local,worker profiles and frontend dev with Webpack; hidden background processes, log/PID files kept in scratch.
- [x] Verify backend health and frontend health return UP/200, frontend login 200 and protected API through frontend returns 401 without credentials.
- [x] Confirm source/config hashes unchanged; restore only Next-generated tsconfig/next-env/AGENTS changes from pre-run snapshots. Final typecheck and whitespace checks passed.

## Evidence

| Check | Result | Log |
|---|---|---|
| Frontend test, one thread worker | 799 passed / 116 files; 428.99 seconds, preserving per-file isolation | `frontend-test.log` |
| Frontend lint | Passed | `frontend-lint.log` |
| Frontend typecheck | Passed before build and again after restoring generated configuration | `frontend-typecheck.log`, `frontend-typecheck-final.log` |
| Frontend production build | Passed using `.next-restart-check-20261106`, including TypeScript/static generation/standalone tracing | `frontend-build.log` |
| Backend verify | 1,822 passed, zero failures/errors/skips; JAR packaged, BUILD SUCCESS | `backend-verify.log` |
| Required PostgreSQL suites | 31 + 2 passed, no skips | `backend-required-postgres.log` |
| Backend runtime :8080 | Started with local,worker; `/health` returns UP | `backend.out.log`, `backend.err.log` |
| Frontend runtime :3000 | Ready with Webpack; login and health return HTTP 200 | `frontend.out.log`, `frontend.err.log` |
| HTTP smoke | Four checks passed: backend health 200/UP; frontend health 200/UP; login 200; protected dashboard API through frontend 401 | `http-smoke.json` |

Uses existing local environment file without printing secrets. Backend runs the verified executable JAR, with its background worker; the old Docker worker remains stopped to avoid duplicate consumers. No source edit, dependency upgrade, volume deletion, commit/push or deployment requested. Runtime startup uses existing Flyway behavior; no new migration is introduced.

Backend startup validated 32 migrations and reported schema up to date, no migration necessary. Runtime launcher PIDs are stored in `backend.pid` and `frontend.pid` under the scratch directory. Mailpit was restarted for local SMTP. Next build changed generated tsconfig/next-env paths and dev regenerated the AGENTS heading; restored those exact pre-run snapshots, preserving earlier user work. Source/config hash comparison found no remaining changes from this task.

Frontend tests emitted JSDOM navigation/scrollTo warnings but all 799 tests passed. Kept per-file isolation rather than changing test semantics to shorten the 428.99-second run. No manual authenticated UI actions, provider calls or load test were performed; smoke verification is HTTP-only. Local background processes and Docker dependencies are intentionally left running for the user.

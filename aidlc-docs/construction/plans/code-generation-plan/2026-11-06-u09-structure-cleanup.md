# U09 structure cleanup — 2026-11-06

User approved the two concrete recommendations from the structure review with “ok giúp tôi sửa”. Scope: internal relocation/renaming only; retain behavior, public contracts, SQL, transactions, validation and error messages. Existing U09 design and interfaces remain applicable; no new feature/design stage or tests needed for unchanged logic.

- [x] Review repository callers, constructors, method references, Spring bean-name references, formatter consumers, tests and current documentation.
- [x] Move `formatErrorMessage` unchanged from `hooks/useResource.ts` to `model/format.ts`; update all consumers and relocate the existing formatter test to `tests/features/grading/model/format.test.ts`.
- [x] Rename concrete repositories to `EvaluationRepository` and `GradingProposalRepository`; update integration test imports and current persistence documentation.
- [x] Run grading frontend tests, typecheck and scoped lint; backend repository/integration and architecture tests; verify exact rename/move equivalence and diff whitespace.
- [x] Record verification results and limits in this plan and audit log.

No U05/U15 implementation edits, dependency/schema changes, application restart, commit, push or deployment. Security/resiliency behavior unchanged; PBT remains disabled. Historical reports retain historical names.

## Fresh verification

- Frontend `npm test -- tests/features/grading`: 134 tests passed across 16 files. Existing jsdom navigation diagnostic did not fail tests.
- Frontend `npm run typecheck` and ESLint on all nine changed/new frontend source/test files: exit 0.
- Backend compiled all 718 main and 246 test sources. Initial sandbox run: six tests passed, thirteen Docker-dependent tests skipped; this was incomplete integration evidence.
- Started Docker Desktop after confirming its daemon was stopped. Reran `EvaluationMappingTest,EvaluationVersionIntegrationTest,GradingPersistenceTest,ArchitectureTest` outside the sandbox: 19 passed, zero failures/errors/skips, BUILD SUCCESS at 2026-11-06T19:13:17+07:00. Log: `.tmp/repository-names-20261106/u09-structure-cleanup-docker-tests.log`.
- Exact snapshot comparisons confirmed unchanged repository bodies except class names/constructor/method references, unchanged formatter/hook bodies, six consumer files changed only their imports, and existing tests changed only their imports/wiring. No stale RepositoryAdapter build classes; no old source/config references; `git diff --check` passed.

No full backend/frontend suite, production build or manual browser/runtime acceptance rerun. PostgreSQL tests used disposable containers, not live application data. Docker Desktop remains running. Existing review audit entry was preserved.

## Full verification requested afterward

User request: “chạy lại toàn bộ test/build”. Existing scoped evidence above remains historical to that run.

- [x] Backend clean compilation followed by full verify in isolated output: 1,830 tests, zero failures/errors/skips and executable JAR. All four required PostgreSQL suites passed the guard.
- [x] Frontend full tests, lint, production build and typecheck: 799 tests/116 files; lint, production build and typecheck exit 0. Full tests reran with two workers after four default-worker timeouts; no timeout or source edits.
- [x] Review fresh reports/artifacts, record results and limits, update audit/state. No commit, deployment or application restart.

[Full verification results and earlier environment failures](../../build-and-test/2026-11-06-u09-structure-full-verification.md). Final backend output uses the Maven `target/test-classes` convention for ArchUnit; scratch test JVM heap is 768 MB. No project configuration edits; temporary backend POM removed.

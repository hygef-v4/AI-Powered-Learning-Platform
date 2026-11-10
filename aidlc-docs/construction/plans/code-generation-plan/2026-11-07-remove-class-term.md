# Remove Class Term Implementation Plan

> For agentic workers: use superpowers:executing-plans for this small cross-unit slice. Track each completed step immediately. User approved A and explicitly requested branch fix/remove-term from staging.

**Goal:** Remove the class semester field `term` from current APIs, persisted class data and callers while preserving the existing UI without semester.

**Architecture:** U05 owns `CourseClass` and Spring JDBC persistence; U04 consumes `SubjectClassSummary` through the existing contract. Remove the field through these existing layers, with one new Flyway migration; no new abstraction, dependency or API version.

**Tech Stack:** Spring Boot/Java, Spring JDBC, PostgreSQL/Flyway, Next.js/TypeScript, JUnit/Mockito/Testcontainers and Vitest.

**Spec:** [D-U04-04 and short design](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). FR003/005; UC18 and UC20–30; U04/U05 class DTOs and functional design. This decision supersedes semester UI/field clauses only, not unrelated class name/status or permissions requirements.

## Constraints and consequences

- Preserve current class code/name/description, IDs, status, teacher, distribution flag, counts, timestamps, version/CAS, enrollments, audit/event behavior and actor/scope checks.
- Only class-semester `term` is removed. Do not replace arbitrary strings named term, search terms, operating-system TERM signals or other concepts.
- Remove the property from both manager input DTOs, every `ClassView` response and U04 `SubjectClassSummary`. Old clients sending it receive the existing strict-input 400 INVALID_REQUEST; update frontend/scripts in the same change.
- Prepare a new classes Flyway migration dropping `course_classes.term`, without CASCADE. Do not edit the applied create-table migration/checksum. Applying the new migration later discards old semester values and requires a deployment backup/rollout decision outside this task.
- Do not connect to, migrate or restart the live application database. Migration verification uses an isolated PostgreSQL fixture/container, including an upgrade from a seeded pre-removal schema.
- Preserve historical audit entries, old backups, dated reports/plans and applied migration SQL. Their term references document past data and are not current contract leaks.
- Preserve current worktree changes and staged report drafts. Do not commit, push, merge, deploy or broaden into other U04/U13/U14 repairs.
- Use shared code/persistence conventions; Security/Resiliency scope unchanged, PBT disabled. No code generation libraries or dependencies.

## Review focus

- No `term` property in any class response, including U04 nested class summaries and all dashboard/detail list consumers.
- Create/edit requests without term succeed; stale version and unauthorized requests retain current outcomes. Former term input is specifically rejected, not silently accepted.
- Repository insert/update/read paths operate against the new schema without selecting or binding the removed column.
- An upgrade removes only the semester column; existing class metadata/version and enrollment rows remain unchanged. Fresh installation and migration replay also work.
- Fixtures and E2E seeds stop sending legacy fields. Existing module/material/notification/class-scope consumers continue compiling and testing.

## Step 1 — Record scope and inspect dependencies

- [x] Read the user's clarification, current DTO/domain/JDBC/frontend flow, strict-input policy and applied classes migrations.
- [x] Record question4=B and explicit API/data removal decision D-U04-04. Prepare concrete plan and approval question5.
- [x] Record explicit approval of this plan before implementation: user selected A and requested fix/remove-term.
- [x] Establish implementation branch fix/remove-term from staging c9b7ce96, preserving the user's dirty/staged files and decision artifacts. Ruling: user requested a branch in this workspace, not a separate worktree; use the requested branch in place and preserve the index.

Production backend files: `academics/controller/AcademicsController.java`, `academics/dto/AcademicsDtos.java`, `academics/model/CourseClass.java`, `academics/service/AcademicsService.java`, `academics/service/AcademicsContractAdapter.java`, `academics/repository/CourseClassRepository.java`, `subjects/contract/SubjectClassSummary.java`, under `backend/src/main/java/edu/aiplatform/`.

Frontend files: `frontend/src/features/academics/model/types.ts`, `components/ClassFormDialog.tsx`, `components/ClassInfoForm.tsx`, and `frontend/src/features/subjects/model/types.ts`. Follow all constructors/callers, not only literal term matches.

## Step 2 — Lock required outcomes with existing test infrastructure

- [x] Add focused controller assertions that class create/edit/detail/list JSON lacks `term`, valid requests omit it, and legacy term in create/edit bodies returns400/INVALID_REQUEST. Run the complete affected API test file and observe expected RED before production edits.
- [x] Change frontend payload expectations to omit term for create and edit. Verify existing UI-no-semester assertions remain; run affected files and record RED due to the still-emitted property.
- [x] Prepare PostgreSQL upgrade regression: migrate a schema through the previous classes version, seed a class with term and enrollment, apply all migrations, assert term absent and other values/enrollment/version retained. Before implementation/new migration, observe the specific expected failure.

Primary tests: `backend/src/test/java/edu/aiplatform/academics/controller/AcademicsApiContractTest.java`, `academics/integration/AcademicsPersistenceTest.java`, `frontend/tests/features/academics/components/academics.test.tsx`, `frontend/tests/features/academics/pages/routes.test.tsx`. Add a small dedicated migration test only if the existing fixture cannot represent pre-upgrade state cleanly.

## Step 3 — Remove the field through the existing layers

- [x] Remove term from `CreateInput`, `InfoInput`, `ClassView`, `CourseClass`, `SubjectClassSummary`, service signatures/projections and all actual callers.
- [x] Remove JDBC term insert/update bindings and row mapping; preserve SQL parameterization, affected-row CAS/locks and transaction boundaries.
- [x] Add one uniquely versioned `backend/src/main/resources/db/migration/classes/*__remove_class_term.sql` with the column drop; preserve historical migration files.
- [x] Remove frontend types/property preservation/empty-string payloads and stale comments. Keep the current visible UI and other request fields unchanged.
- [x] Update affected backend constructor fixtures/model/controller/service/repository/contract tests and frontend fixture types/expected payloads. Do not delete coverage or weaken assertions.
- [x] Remove semester from `scripts/e2e/seed.mjs` and `scripts/e2e/u04-u13-u14.mjs`; inspect every class-create request fixture for the old property.
- [x] Run RED tests again and record GREEN; investigate new failures before expanding scope.

Additional constructor/caller tests include academics model/service/access/batch/MCP/contract suites, U04 `SubjectRepositoryIntegrationTest`, and frontend academics route/characterization/pagination/integration and subjects class-table fixtures. Search by constructor/type references after removing the record component so positional empty strings do not mask incorrect argument shifts.

## Step 4 — Synchronize current aidlc-docs

- [x] Update U04 `functional-design/frontend-components.md` to remove semester column/filter and `domain-entities.md` to remove term from the shared class summary contract.
- [x] Update U05 functional-design BR13/domain field/UI/flow clauses to remove semester input/display/filter and the current field contract, preserving unrelated pending decisions and class-status/name clauses.
- [x] Update current API/source contract inventory if it enumerates term. Search current aidlc-docs for semester requirements; distinguish dated evidence/applied SQL from current specifications.
- [x] Record the actual change and validation limits in state/audit and complete these plan checkboxes. Keep unit-test report drafting paused.

## Step 5 — Verification and handoff

- [x] Compile/test backend with all updated constructor consumers; run targeted U05/U04 suites and real PostgreSQL migration/persistence checks without skips. Run required PostgreSQL guard/full verify if the environment supports the repository gates; record blockers honestly.
- [x] Run frontend academics/subjects tests, TypeScript and scoped lint. Run production build if the implementation changes client request/types; no framework/config upgrades.
- [x] Validate E2E script syntax and all current payload examples; no live E2E fixture writes without separate scoped authorization/environment.
- [x] Search active source/API contracts for semester-field remnants; explicitly retain historical migrations, historical audit/report text and unrelated search-term/signal names.
- [x] Verify final diff preserves user files/staging and contains no unrelated UI changes. Report RED/GREEN evidence, executed/skipped checks and migration/rollout limitations without claiming deployment.

Status: implemented and verified on fix/remove-term; no live migration, commit, push or deployment.

Execution evidence so far: baseline API35/35 and frontend36/36 passed; RED frontend3 failures/32 cases and backend12 failures/39 cases (including both real PostgreSQL migration cases, zero errors/skips). Frontend after removal149/149, TypeScript and scoped lint exit0. First backend scope406 cases had only one obsolete semester-validation assertion failure; removed that obsolete clause after reviewing its now-valid inputs. Full backend verify and production build were pending at that checkpoint; final results follow. No live migration or report drafting.

Final verification 2026-11-07: Maven offline verify BUILD SUCCESS, 1,846 tests with zero failures/errors/skips. Required PostgreSQL guard passed31+2+10+1; these are included in1,846, not extra cases. ClassTermMigrationIntegrationTest2/2 passed, verifying seeded upgrade, fresh install, preserved metadata/enrollment, JDBC find/update/insert and migration replay. Frontend academics/subjects149/149, TypeScript, scoped ESLint and production build31/31 pages passed. Initial sandbox build was interrupted after no progress; identical build completed outside sandbox without source/config/dependency changes. E2E scripts passed node --check; no live E2E/database/provider calls. Fresh independent reviewer confirmed the obsolete term-only validation assertion was removed and no remaining actionable findings. New relative links and git diff whitespace checked; applied migration unchanged and pre-existing staged report drafts preserved.

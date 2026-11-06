# U05/U09/U15 convention refactor execution

Baseline: `3bfc2fb8`, existing `fix/u05-u09-u15` checkout, clean at start. Spec: [code conventions](../code-conventions.md). Plan: [implementation steps](../plans/code-generation-plan/2026-11-05-u05-u09-u15-conventions.md).

Status: implemented, independently reviewed and verified on the final source. No commit or push.

**Subsequent import-only follow-up:** the user requested restoring wildcard imports. The full-suite results and import-expansion statements below record the preceding refactor state. Current import policy, source-only checks and fresh compile evidence are recorded in [the follow-up](#wildcard-import-follow-up--2026-11-05); runtime suites were not repeated after that import-only change.

## Boundary and exceptions

Preserve HTTP/JSON/database/event/MCP identifiers, cross-unit public contracts, framework overrides, bean/qualifier identities and business behavior. Framework file names, records/accessors and contract implementations are deliberate exceptions. No rename outside U05/U09/U15 except necessary callers/imports. No commit/push, migration, live grade write or restart. Existing extension scope retained; PBT disabled.

## Ledger

- Task 1: baseline passed: backend **416**, zero failures/errors/skips, including real PostgreSQL and architecture; frontend **227 / 30 files**. Logs: `.tmp/conventions-be-baseline.log`, `.tmp/conventions-fe-baseline.log`. Java compiler symbol analysis prepared 529 declaration renames (including matching constructor parameters); all references/method references are resolved, not globally replaced. Checking frontend mocks and wildcard imports before applying.
- Task 2: symbol-bound field/constructor/action renames applied; Java strings, annotation values and wire names preserved by literal comparison. Expanded all scoped wildcard imports, including DTO/stub-test imports without renaming their contracts. Initial renamed suite passed **416**, zero failures/errors/skips. Final semantic refinements preserve role-qualified `lineTransactionTemplate` separately from `transactionTemplate`; a duplicate-name compile check caught and prevented accidental role collapse before final verification. Logs/maps: `.tmp/conventions-be-renamed.log`, `.tmp/convention-java-names.txt`, `.tmp/convention-java-refinements-names.txt`.
- Task 3: TypeScript symbol renames update API consumers, mock definitions, spies, helpers/constants and callbacks; moved four component modules to descriptive PascalCase names. Hook return keys stay compatible through aliases. The first affected suite had two failures from dynamically computed mock keys; updated those to the renamed API keys while preserving `finalize`/`publish` action values and all assertions. Both batch suites now pass **18** tests. Final helper sweep covers navigation resolution, AI request construction, checklist bindings, diagram sanitization and gradebook loading; compiler-bound references updated without changing hook/data property shapes. Logs/maps: `.tmp/convention-ts-names.json`, `.tmp/convention-ts-callback-names.json`, `.tmp/conventions-fe-focused.log`, `.tmp/conventions-fe-batch-green.log`, `.tmp/conventions-fe-helper-refactor.log`.
- Task 4: final backend verify passed **1,783**, zero failures/errors/skips; required PostgreSQL guard passed (31 + 2) and production JAR built. Final frontend **776 / 115 files passed**, full lint/typecheck and two-worker production build passed. Generated TypeScript configuration restored, followed by another successful typecheck. Independent source review complete; its minor predicate naming correction is applied. Current as-built/frontend notes, plan, state and audit synchronized.

## Compatibility decisions and regression

- Accessors/record fields, repository/interface overrides, MCP `tools`/`call` and required framework names remain unchanged; convention governs internal actions, not serialization keys or published contracts.
- A new JSON regression exposed that an intermediate `isOfficial` rename would add an `official` JSON property. Keep the predicate as `hasOfficialGrade` and guard as `requireEditableGrade`; no annotation workaround or new response field. `EvaluationJsonContractTest` failed before correction and passed in the 1,783-case suite; expanded to cover PENDING/DRAFT/FINALIZED/PUBLISHED in the final run. Logs: `.tmp/conventions-json-red.log`, `.tmp/conventions-be-full-final.log`.
- Use `requireNonNegativeCount`/`validateBackupSummary` for throwing value validators and `measureJsonBytes` for byte counts; boolean prefixes are reserved for actual predicates. Feedback frequency helpers describe feedback rather than student scores.
- Keep existing formatter/layout in unrelated code; this change adds no formatter, naming-lint framework, dependencies, folder split or behavior change. Tests keep descriptive sentence names.

## Independent review

Read-only reviewer compared all 110 changed tracked Java files: identifier/import changes only, with literals/operators/control flow retained. Reviewed four moved frontend modules, API bindings, dynamic mock keys, return-key aliases, transaction roles and the new JSON regression. No Critical/Important source defect. One minor: a boolean helper named normalizeBlank implied transformation; renamed to isBlankText and updated callers. No deferred naming finding.

The final fourteen frontend helper renames followed that review and were self-checked with TypeScript symbol resolution, the full 776-case frontend suite, full lint/typecheck and production build. No additional logic was introduced.

Excluded from review scope: unrelated existing formatting/behavior, live browser/provider/deployment acceptance. Those are preserved or remain unclaimed, not newly verified by this naming refactor.

## Verification environment

Running the default 23-worker Next build alongside Maven exhausted Windows commit memory (V8 zone allocation and JVM paging-file/native-allocation failures). A later Mockito attach subprocess also ran out of memory; preload the existing Byte Buddy agent in the verification JVM rather than creating another JVM. A full run then had three document-extraction errors: child JVM crash logs confirm native-memory exhaustion, not changed extraction code. The final full run passes with SerialGC, 16 MB initial heap, two processors and 64 MB code cache also inherited by child JVMs through JAVA_TOOL_OPTIONS. Maven itself uses JDK 17, UTF-8, normalized cache path, heap 768 MB, SerialGC, metaspace 256 MB, code cache 96 MB and four active processors. Heavy build/verification jobs run sequentially. Next uses CIRCLE_NODE_TOTAL=3 (two workers in the installed Next config) and a 1 GB Node heap. These are per-command environment settings, not application/config changes. Own JVM crash/replay evidence is retained under ignored .tmp; no unrelated app was stopped.

## Verification

| Check on final source | Result | Local evidence |
|---|---|---|
| Backend Maven verify, including architecture and integration suites | 1,783 passed; zero failures/errors/skips; executable JAR packaged | `.tmp/conventions-be-verified.log`, `backend/target/surefire-reports` |
| Required real PostgreSQL test guard | Notification delivery 2; subject repository 31; no skips | `scripts/assert-required-backend-tests.ps1` output after successful verify |
| Evaluation JSON compatibility | All four grading states retain state and add no official/editable bean fields | `EvaluationJsonContractTest` in the successful full verify |
| Full frontend tests after final helper sweep | 776 passed / 115 files | `.tmp/conventions-fe-tests-final.log` |
| Full frontend lint and TypeScript | Passed | `.tmp/conventions-fe-lint-verified.log`, `.tmp/conventions-fe-types-verified.log` |
| Production Next build after final helper sweep | Passed with two workers | `.tmp/conventions-fe-build-verified.log` |
| TypeScript after restoring generated configuration | Passed; tsconfig has no source diff | `.tmp/conventions-fe-restored-types.log` |
| Compatibility/diff checks | Literals retained in all 110 changed tracked Java files, normalizing line endings; no schema/HTTP-contract/dependency-file changes; no scoped wildcard imports or stale module imports; added relative doc links/fences and whitespace checked | Final diff inspection and `git diff --check` |

The prior full 1,782/backend and 776/frontend reports describe the pre-refactor implementation; the fresh results above apply to this naming change. Final backend verify completed 2026-11-05T23:21:32+07:00. Earlier failed environment attempts are retained rather than reported as passing.

Enabled SECURITY-03/04/05/08/09/12/15 and RESILIENCY-04/06/10 scope is unchanged: no new trust boundary, secret storage, transport, filesystem access, retry or transaction behavior. Existing authorization, validation, persistence and failure-handling regressions pass in the full suites; no new penetration/load/deployment assessment claimed. PBT remains disabled. Manual browser/provider acceptance was not repeated for this identifier/import refactor. No migration, live data/grade write, application restart, commit, push or deployment.

## Wildcard import follow-up — 2026-11-05

- Request: permit compact wildcard imports and restore the previous style; retain the naming refactor.
- Updated the canonical convention: wildcard/static wildcard imports are allowed, including contract types; explicit imports remain appropriate for collisions or clarity.
- Restored baseline import blocks in 71 changed Java files, including DTO/contract-related files. All current imports were checked as covered by the baseline's explicit/wildcard imports before replacement. Self-check verifies every byte outside the replaced import regions is unchanged; 883 fewer import statements. No source-body, test assertion or naming change.
- Fresh `mvn -o -DskipTests test-compile`, with the existing local cache and bounded JDK 17 JVM, passed at 2026-11-05T23:43:51+07:00. Both production source and all 244 test source files compile; no import ambiguity/unresolved symbol. Evidence: `.tmp/conventions-wildcard-compile.log`.
- Added documentation links and whitespace diff checked. No new runtime test or frontend build was necessary for import-only edits; the earlier 1,783 backend / 776 frontend counts are not a fresh rerun of this follow-up. No commit/push, DB/dependency/frontend change, live write or restart. Existing enabled security/resiliency scope retained; no runtime control changed, PBT disabled.

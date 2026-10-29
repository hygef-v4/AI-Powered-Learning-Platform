# U05 behavior-preserving refactor

Authorization: user approved implementing the prior U05 assessment: "Giúp tôi refactor mà k làm thay đổi hành vi, contract, clean code". Continue existing Construction inline. The review document is the scope/design; no new feature or architecture is introduced. Existing review/state/audit edits are the baseline and must be preserved. No commit or push requested.

Constraints: preserve all public routes, signatures, DTOs, strings, validation/error precedence, authorization calls, lock order, transactions, audit/events, pagination quirks and five extension slots. Do not remove duplicate manager checks or change archived-class access. Keep fetching and mutation ordering sequential. No dependencies or shared API changes.

## Execution

- [x] 1. Resume repository workflow, read skills and review source/tests; confirm the approved assessment bounds the work.
- [x] 2. Retain route characterization tests for superseded requests, refresh cancellation, post-write GET failure, reopen conflict details and list pagination. Retain UI pagination tests for last/empty pages and filter shrinkage. Run on unchanged source before editing.
- [x] 3. Expand `frontend/src/features/academics/routes.tsx` in place: explicit resource types/control flow, typed generic mutation result, named actions and manager view branches. Remove only the obsolete Frame facade. Keep the resource hook local and retain request/cancellation/error semantics.
- [x] 4. Name local pagination calculations in `ClassListPage.tsx` and `EnrollmentPanel.tsx`; keep stored page versus displayed page semantics. Run focused frontend tests.
- [x] 5. Clarify local names/operation outlines in `AcademicsService.java`, especially DTO mapping, status flow and command/audit handling. Keep checks and cross-unit calls exactly ordered; run focused backend tests.
- [x] 6. Run full backend/frontend tests, lint/typecheck and production builds as feasible. Inspect diff and public contract/strings; obtain one independent final review and address regressions. Record evidence and limitations in plan/state/audit/handoff.

## Protection and review focus

Existing backend access, batch transaction, domain, API and adapter tests protect U05 behavior. Existing frontend route, workflow, candidates, API and slot tests protect public UI seams. New tests must observe real components through mocked HTTP only; no private-hook or source-text tests.

Review focus: aborted old results cannot replace new route data; refresh errors retain existing behavior without repeat writes; conflict payload conversion must survive; pagination keeps filter-induced stored-page quirks; backend authorization/lock/audit/event sequence remains unchanged.

Ruling: characterization tests pass before production edits rather than inventing missing behavior to make tests fail. This preserves the explicit no-behavior-change constraint and follows Clean Code's characterization workflow. Existing adequate backend tests are reused.

Ruling: reuse this named feature checkout and current working tree; preserve earlier documentation edits. No new Git metadata operations or worktree are necessary for this bounded, already-reviewed refactor.

Extensions: retain reduced Security 03/04/05/08/09/12/15 and Resiliency 04/06/10 scope; PBT disabled. Internal edits must preserve enabled validation, errors and authorization boundaries. Deployment/encryption/health configuration checks are N/A to unchanged configuration; real integration/NFR gates remain open.

## Evidence

- Unchanged-source characterization: 42 frontend tests / seven files passed (33 existing + nine retained cases). The five route cases observe superseded responses, refresh cancellation, failed GET after successful PATCH with read-only retry, both reopen-conflict payload forms and the existing full-page refetch. Four pagination cases preserve last/empty pages, search reset and stored-page clamping in both lists.
- Sensitivity probe: temporarily removing aborted-result suppression made the superseded-response test fail as intended. Restored the original route file byte-for-byte (SHA256 matched) before the real refactor. Focused suite passed again after refactoring.
- Backend baseline full suite: 724 total, 662 passed, 62 skipped, zero failures/errors. Focused post-refactor U05 + architecture: 213 passed, zero failures/errors/skips.
- Frontend focused post-refactor: 42 passed / seven files. An intermediate run caught missing ClassListPage pagination locals caused by an unmatched edit anchor; inserted the declarations and reran all U05 cases successfully. No assertion or behavior was changed to hide the defect.
- Lint and typecheck passed after the source edits. Full backend verify passed: 724 total, 662 passed, 62 skipped, zero failures/errors; executable Spring Boot JAR packaged.
- Independent reviewer compared all four production files with originals and both characterization files; no findings. Confirmed React child identity, conditional rendering, cancellation/key/error flow, request sequencing, stored-page quirks and backend effects/order remained equivalent. Reviewer did not rerun tests.
- Source comparison: backend string literals and public declarations unchanged; complete lock/access and exception-mapping blocks remain byte-for-byte unchanged. No API/DTO/controller/domain/repository/schema/dependency edits.
- Raw logs and four pre-edit source snapshots remain under ignored `.tmp/u05-*`; they are not deliverable artifacts.

- Final full frontend suite: **232 passed / 47 files**. Lint passed. Production `npm --prefix frontend run build` with `NEXT_DIST_DIR=.next-verify-u05-refactor` passed, including compilation, TypeScript, all 18 static pages, build tracing and standalone packaging. Restored Next's generated tsconfig edits from the pre-build snapshot; a fresh typecheck passed afterwards. No build configuration or dependency changes retained.
- Final diff whitespace check passed. TypeScript AST comparison confirmed visible JSX text unchanged in all three frontend production files. Exactly four production files changed; two new test files retained; no existing tests weakened or removed. Prior review documentation edits preserved.

## Repeating verification

- Backend: use the existing Maven launcher with `-o -s scripts/maven-local-cache-settings.xml -f backend/pom.xml -DargLine=-javaagent:<workspace>/.tmp/byte-buddy-agent.jar verify`. The local settings/agent already existed; normal CI may use its regular Maven setup.
- Frontend tests: process-local TEMP/TMP pointed at `.tmp/u05-clean-code-temp`, `npm --prefix frontend test -- --maxWorkers=1`.
- Frontend: `npm --prefix frontend run lint`, `npm --prefix frontend run typecheck`; build with the existing supported `npm --prefix frontend run build` and optional isolated NEXT_DIST_DIR.

## Limits and extension compliance

The 62 skipped backend cases match the baseline: GradingPersistenceTest (3), AccountRepositoryIntegrationTest (14), RequestThrottleIntegrationTest (6), SessionStoreIntegrationTest (8) and SubjectRepositoryIntegrationTest (31). U05 JDBC tests use mocks; this refactor does not prove live PostgreSQL concurrency, real cross-unit providers, browser behavior or NFR targets. No DB/provider/browser/NFR verification was added or claimed.

SECURITY-03/08/09/12: existing logging, error handling, validation and authorization preserved by unchanged contracts, focused/full checks and diff review; no new external trust boundary. SECURITY-04/05/15 and RESILIENCY-04/06/10 configuration/operational checks are N/A to these local edits: no encryption, permissions, deployment, health or dependency-isolation configuration changed. Existing integration/NFR compliance gates remain open; disabled PBT skipped. No commit or push performed.

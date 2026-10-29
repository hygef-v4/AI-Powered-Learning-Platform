# U05 Clean Code review

Date: 2026-10-20. Scope: review the existing U05 Class & Enrollment implementation and compare the approach with the U09 clean-code follow-up. This is an assessment, not authorization to implement all suggestions. Production code and tests remain unchanged.

## Conclusion

Recommend a small behavior-preserving cleanup, beginning with frontend route orchestration. There is no demonstrated need to rewrite the module, split every service responsibility into a new class, introduce generic repositories or alter public contracts. U09's useful precedent is cohesive private helpers and clear names, not copying its module structure.

## Findings, in priority order

### 1. Make route orchestration readable

Location: `frontend/src/features/academics/routes.tsx:18`, `:35`, `:40`, `:46`, `:50`.

`useResource`, detail loading, mutation/refresh/conflict handling, action bindings and manager rendering are compressed into long expressions. A change to one manager mutation currently requires reading API calls, reload behavior, 409 conversion and rendering together. The nesting also makes cancellation and stale-result checks difficult to audit.

Smallest useful change: expand control flow into readable blocks; give resource state an explicit type; give mutation handling a generic return type so `onBatch` does not need its assertion; name the actions object and the manager view branches. Extract the existing resource hook into a nearby file only if it makes these three route consumers easier to navigate. Do not create a generic application-wide fetching framework.

Protection: existing route tests cover capability fallback and version conflicts. Before restructuring asynchronous code, retain characterization cases for a superseded request, an aborted refresh, successful mutation followed by failed GET, reopen conflict details, and multiple list pages. Preserve current results and request ordering; any change to error/retry UX belongs to a separate behavior change.

### 2. Clarify backend local names and operation outlines

Location: `backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java:230`, `:257`, `:286`, `:297`, `:312`.

Names such as `c`, `s`, `a`, `e`, `value`, `active` and `hide` obscure domain meaning in DTO mapping, status changes and authorization. Several methods put authorization, state changes, writes and audit on one line. The service's size alone is not a reason to split it.

Smallest useful change: use `courseClass`, `subject`, `account`, `enrollment`, `subjectActive` and a name explaining that denial is hidden as not-found; put each meaningful operation on its own line. Consider private helpers for OPEN prerequisites and activation publication only if the caller becomes a clearer outline. Preserve public signatures.

Important constraint: `create` calls `requireManager` twice before `lockActive` and once after it (`:115-116`). This is a review candidate, but removing a call changes cross-unit invocation count and potentially failure behavior. Do not delete it as incidental cleanup. Likewise, keep the read-lock-recheck sequence in `locked`, per-row REQUIRES_NEW transactions, audit placement, event calls and error precedence intact.

Protection: access/read, service, batch transaction, API and adapter tests. Existing batch cases cover partial commit, retry, audit failure, revoked authorization between rows and ambient caller rollback. Live PostgreSQL concurrency is not established by these tests.

### 3. Name repeated pagination calculations

Location: `frontend/src/features/academics/ClassListPage.tsx:25`, `:28`; `frontend/src/features/academics/EnrollmentPanel.tsx:37`, `:39`.

Both components repeat page-count, clamped-page and slice-offset arithmetic inside JSX. The visible page is clamped while button logic still uses the stored page, so a refactor must not silently change navigation after filters reduce the result count.

Smallest useful change: derive clearly named local values for page count, displayed page, offset and visible rows. A small shared pure helper is warranted only if it removes the duplicated arithmetic without hiding state semantics; a generic table abstraction is unnecessary.

Protection: retain public UI cases for more than 20 rows, last page, empty results and filtering from a later page before changing this area. Existing filtering tests alone do not establish these boundaries.

### 4. Remove the obsolete Frame facade when convenient

Location: `frontend/src/features/academics/routes.tsx:5`, `:27`.

`Frame` returns only its children and ignores `audience` and `active`; these props imply layout/navigation behavior that now belongs to the route layouts. Remove the wrapper, its unused props and the now-unnecessary `AcademicsView` type in the same small batch. Existing route and slot tests should continue to pass.

## Keep as-is

- `CourseClass`, `EnrollmentPolicy`, parser and JDBC repositories have identifiable responsibilities; no demonstrated benefit from more layers.
- The two CSV parsers serve separate client preview and server validation boundaries; do not remove server validation to eliminate apparent duplication.
- Student archived-class behavior is intentional: BR-U05-40 allows archived dashboard cards, while BR-U05-41/42 restrict detail to OPEN. The UI omits the archived card entry button, and existing backend/frontend tests protect this. It is not a defect.
- Keep the five extension slots, public APIs/DTOs, permission checks and transaction boundaries.
- Shared UI migration and external-provider integration from previous handoffs are separate work, not prerequisites for this readability cleanup.

## Verification on unchanged source

- Initial Git working tree: clean.
- Backend: Maven 3.9.12, offline local-cache settings and existing ByteBuddy agent; `-Dtest=edu.aiplatform.academics.**,ArchitectureTest test`: **213 passed**, zero failures/errors/skips. Includes 211 academics cases and two architecture cases. Log: ignored `.tmp/u05-clean-code-backend.log`.
- Frontend: `npm --prefix frontend test -- src/features/academics --maxWorkers=1`, with process-local TEMP/TMP pointed at ignored workspace scratch: **33 passed / 5 files**. Log: ignored `.tmp/u05-clean-code-frontend.log`.
- Initial frontend run with four workers and sandbox-default temp failed loading temporary transformed modules (ENOENT), before four suites collected tests; the fifth suite passed four tests. The successful rerun changed only worker count and process-local temp location. No production/config/dependency patch was applied; the precise temp failure cause is not proven.
- Full repository tests, lint, typecheck, production build, live DB/provider/browser/NFR checks: not run for this assessment; no production or test files changed.

## AI-DLC continuity and extensions

Continue existing Construction review; reuse U05 design and existing project requirements. No new feature/design/code-generation stage was advanced. Security/resiliency scope remains the configured reduced scope, PBT disabled. For produced documentation, SECURITY-03/04/05/08/09/12/15 and RESILIENCY-04/06/10 implementation checks are N/A because this assessment changes no logging, encryption, errors, validation, authorization, deployment, health checks or external calls. Existing integration/NFR gates remain open; this review does not certify compliance of the deployed system.

- [x] Load Clean Code guidance, repository workflow/state, U05 design/handoff and U09 review.
- [x] Trace U05 domain, service, persistence, contracts, controller and frontend consumers/tests.
- [x] Run focused backend/frontend checks and record environmental limitations.
- [x] Record prioritized recommendations and behavior-preservation constraints; leave implementation unchanged.

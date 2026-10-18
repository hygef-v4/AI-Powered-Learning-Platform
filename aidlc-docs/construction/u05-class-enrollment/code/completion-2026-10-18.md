# U05 - code completion and integration handoff

## Scope
Latest user request: "giúp tôi haonf thiện u05". The team intends to implement units independently and merge afterward. This request supersedes the earlier instruction to implement only dependency-free domain/UI pieces. U05 code now targets existing U01/U02/U03/U04 contracts; it does not implement those units or claim a working authenticated production system before integration.

## Implemented behavior
- `AcademicsService`: class creation, information/version updates, teacher assignment, distribution flag, lifecycle, scoped dashboards/detail, student lookup/detail, single/batch enrollment, removal/restoration and managed subjects.
- Explicit current actor role/account status plus `IAuthorization`; manager subject ownership, current teacher assignment and current student enrollment. Wrong class scope returns 404; upstream failures fail closed. Each mutation rechecks permission after the subject lock and rereads the class/version.
- Immutable code/subject, archived readonly, teacher eligibility, subject ACTIVE for opening, subject-wide enrollment exclusion, stale version 409 and reopening conflict IDs. Same transaction subject lock coordinates enrollment/removal/status changes; full PostgreSQL concurrency remains to be verified.
- Batch parser limits 200 rows/100 KB UTF-8, one account batch lookup, ordered nine-result output and a separate transaction for each valid row. An infrastructure failure can return 503 after earlier rows have committed; retry is safe through existing enrollment outcomes, but such a response does not mean all rows rolled back.
- Success audit participates in the business transaction. Denied/failure audit calls occur after rollback, relying on U02's own transaction contract. Enrollment activation uses U03's publisher, which schedules delivery after commit; no delivery guarantee is added.
- Real `AcademicsContractAdapter` implements `IClassAccess`, `IClassScope` and `ISubjectClasses`, querying current relationships without caching. Contract lists do not silently truncate at 100; row HTTP APIs accept one-based page/size with size capped at 100.
- Scoped JDBC read adapter supplies manager/teacher/student queries and counts; student lists require ACTIVE enrollment and OPEN/ARCHIVED class status. Search values are parameterized and literal wildcard characters escaped.
- REST API covers domain design section 5. JSON lists are arrays, detail DTOs match `academics/types.ts`, unknown immutable request fields are rejected, validation errors are safe ProblemDetail responses. Single enrollment returns `{result}`; batch returns `{line,email,result}[]`; enrollment-status changes return 204 only on accepted outcomes.
- Frontend `api.ts`, production routes and containers call the real backend, with loading/error/retry states, role capability inferred only from authorized endpoints, candidate debounce/abort and cap20, async mutation refresh and version conflict handling. Development preview remains isolated and production-disabled.
- All five slot contracts are hosted: StudentClassSection, TeacherClassTab (info/students/materials/evals/gradebook), StudentDetailSection, SubjectSummarySection and ClassCardSection. No upstream unit's content is fabricated.

## Routes
- `/classes` and `/classes/[id]` for student capability; `/classes` redirects detail selection to teaching routes when only server-authorized teaching capability is available.
- `/teaching/classes` and `/teaching/classes/[id]` for teaching capability, including assigned Subject Managers.
- `/manager`, `/manager/subjects`, `/manager/subjects/[id]`, `/manager/classes` and `/manager/classes/[id]` for manager operations.
- Backend always decides authorization. Neither page paths, audience props, client state nor a preview role selector grant API permission.

## Integration prerequisites and activation
1. Merge U01 account schema/session principal/lookup/authorization and U04 subjects schema/query/scope; preserve the real foreign keys in U05 SQL.
2. Validate the pending migration against those actual schemas and deploy it as `db/migration/classes/V20260925_1100__create_classes_enrollments.sql` after account/subject migrations. It remains at `db/pending/academics/` until then; no upstream placeholder tables or FK weakening were introduced.
3. Supply real U02 audit and U03 event publication contracts. U01 must recognize U05 authorization actions `CLASS_STUDENT`, `CLASS_TEACHER`, `CLASS_MANAGER` (global), `CLASS_VIEW` (class) and `CLASS_MANAGE` (subject). These strings are documented because the existing interface has no action enum.
4. Set `U05_PERSISTENCE_ENABLED=true` in the backend process/container environment only after prerequisites are ready. Default false gates repositories, contract adapter, application service and controllers together. Dev contract stubs are not proof of authorization; deployment must use real contracts and the project's no-stub configuration.
5. Validate real browser/session flows and tester-owned PostgreSQL scenarios: first enrollment into different classes concurrently, reopening vs enrollment, subject lifecycle interaction, immediate role/enrollment revocation, FK/app permissions, rollback audit and after-commit events. No production writes were made by this implementation task.

## Current verification
Backend after unit-test expansion: clean Maven run passes 304 tests/36 suites, including 210 U05 cases and three architecture checks. The separate test expansion added 140 cases without changing production source. Frontend: final combined lint/typecheck pass, 70 tests/18 files pass, production build passes. Independent review and scoped re-review are complete; the one 409 feedback issue was fixed with a failing-then-passing regression test. Full command evidence is in the completion execution plan. Docker CLI exists on this machine but its daemon is unavailable; no real PostgreSQL migration or lock/concurrency run is claimed.

## Scalability note
Frontend list containers currently fetch server pages of 100 to support existing local search/filter/count components and display table pages of 20. They do not truncate at the first page, but large classes/catalogs require replacing this accumulated dataset with server-controlled pagination/filter UI. API pagination and scoped queries are already available for that change.

## Extension compliance
- SECURITY-03/05/09: identifiers-only audit details; bound queries, field/CSV bounds, strict input DTOs and safe error responses.
- SECURITY-08/12/15: existing server Actor and cross-unit authorization contracts; no substitute login or client role authority; dependency failures reject requests. Actual session/authorization integration remains pending.
- SECURITY-04: no security-header config changed; shared Next.js/gateway configuration applies and still needs deployment verification.
- RESILIENCY-10: bounded subject lock timeout; explicit errors/retries; existing after-commit event mechanism. Real failure/concurrency behavior remains integration work.
- RESILIENCY-04/06: no infrastructure service or health endpoint changes. Other rules remain N/A per the project's selected subset; PBT remains disabled.

## Use-case implementation map
| UC | Code paths |
|---|---|
| 20-21 | AcademicsService.list/detail with student scope; ClassesRoute/ClassDetailRoute; ClassDashboardPage/ReadOnlyClassPage |
| 22-23 | Teacher-scoped list/detail/students/student methods; teaching routes and StudentDetailDialog |
| 24-25 | Manager-scoped list/detail; ClassListPage and ManagedClassPage, enrollment list API |
| 26 | create endpoint/service, ClassFormDialog, teacher eligibility and immutable subject/code |
| 27 | assignTeacher endpoint/service, remote candidates and AssignTeacherDialog |
| 28 | enrollment/candidates/batch/status endpoints, per-row transactions, EnrollmentPanel and result table |
| 29 | information/distribution/status endpoints, version/readonly/reopen rules, lifecycle confirmations |
| 30 | managedSubjects/managedSubject, ManagerDashboardPage/MySubjectPage, subject slots and teaching banner |

This maps implementation coverage. It does not certify every use case end-to-end before its upstream schema/session/audit dependencies are merged and tested.

## Unit test expansion update
Latest user request: "giúp tôi bổ sung unit test cho u05". Added 99 access/read/permission cases, 35 API mapping/validation/error cases and 6 observable batch transaction cases. All pass with the old tests in the clean full run (304 total backend, 210 U05). Scope, commands, review and limits: [unit test expansion plan](../../plans/u05-unit-test-expansion-2026-10-18.md). No coverage percentage or live integration claim is made.

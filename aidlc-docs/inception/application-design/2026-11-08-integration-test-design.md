# Automated Integration Tests and Report: U01–U15

Status: approved by the user's question2=A; implementation in progress.

Independent builder review completed: no important unanswered planning questions. User approved question2=A, including detailed planning and sequential implementation.

## Intent and confirmed scope

The user selected option B in [scope questions](../requirements/2026-11-08-integration-test-questions.md): cover the important integrations across all 15 logical units, delivered in groups. Work is on `integration-test`, created from fetched `origin/staging` at `321f88929360a4bf3c733a26a4a0f06a15ecb323`.

Produce maintainable automated integration tests and an English integration test report based on `Report-5.2_Integration Test_Template.xlsx`. Preserve the template's visual conventions and keep its source file unchanged. Cover representative requirements with traceable evidence; the number of test methods or report rows is not a coverage target.

The application is a modular Spring Boot backend and worker, not 15 deployed microservices. Unit ownership follows [unit-of-work.md](unit-of-work.md). Requirements authority is `aidlc-docs`, especially each unit's `functional-design/business-rules.md` and current approved reconciliation decisions. Code supplies integration structure and failure-path information; current code output alone does not define correctness.

## Approach and alternatives

The user adopted the previously proposed combined approach: requirement-derived expected outcomes, implementation-aware scenario selection, and automated execution.

1. Reuse existing real infrastructure tests and add focused compositions of production services/adapters. This is the selected approach. It demonstrates actual integration while limiting fixture duplication and maintenance during refactoring.
2. Create one full-application scenario suite for every flow. This would exercise broad wiring, but adds unrelated startup dependencies and makes fault localization harder. Reserve broad server/browser flows for separate E2E work.
3. Use controller tests with mocked services as the main evidence. Those can verify HTTP contracts, but cannot prove the downstream database, transaction, queue or cross-unit behavior requested here.

Do not replace the existing Maven test lifecycle merely to rename tests. Current CI already runs `mvn -B verify`; preserve that entry point and extend its required-suite verification where needed. A Failsafe migration is not required by this task.

## Source findings and reuse policy

Initial annotation search found 24 source files using Testcontainers or the composed requirements Spring context. This is a candidate inventory, not a claim that all 24 are report-ready or currently pass. Parameterized methods can produce multiple executions. No new-task execution evidence has been collected yet.

Useful existing candidates include:

- `identity/repository/AccountRepositoryIntegrationTest`, `identity/integration/SessionStoreIntegrationTest` and `RequestThrottleIntegrationTest`: PostgreSQL account constraints and Redis sessions/rate limits.
- `subjects/repository/SubjectRepositoryIntegrationTest`: real PostgreSQL lifecycle/version/transaction checks, with explicit external port mocks.
- `requirements/RequirementsCoverageIntegrationTest` and `OwnerRequirementsTestConfig`: real JWT/current account authorization, security filters, PostgreSQL owner services/repositories and audit for U04/U13/U14. Session store, artifact, queue, mail and some academic contracts are bounded by mocks. Reuse its proven scenarios; do not describe mocked ports as integrated production providers.
- `ai/model/AiRepositoriesIntegrationTest` and `ai/service/McpWriteRequestIntegrationTest`: connection/task/quota persistence and idempotent write transactions.
- `assignments/repository/RubricRepositoryTest`: PostgreSQL rubric persistence.
- `assignmentwork/service/PracticeFeedbackConcurrencyIntegrationTest`: actual PostgreSQL reservation concurrency with bounded AI/auth contracts.
- `grading/repository/EvaluationVersionIntegrationTest` and `GradeDistributionVisibilityRepositoryTest`: transactions, CAS, locks, JSON and visibility persistence.
- `quizzes/repository/JdbcQuestionRepositoryIntegrationTest`, `JdbcQuizRepositoryIntegrationTest`, `quizzes/integration/QuizPublicationMigrationIntegrationTest`, and `quizpractice/repository/JdbcQuizAttemptRepositoryIntegrationTest`.
- `groupdocs/model/GroupRepositoriesIntegrationTest` and `groupdocs/integration/RabbitSseMultiInstanceIntegrationTest`: group persistence and actual Rabbit fanout after commit.
- `materials/integration/MaterialModuleMigrationIntegrationTest` and `notifications/integration/NotificationDeliveryIntegrationTest`.
- `backup/integration/BackupDashboardStatusIntegrationTest`, `BackupConcurrentDeliveryIntegrationTest` and `files/service/DownloadGrantExpiryIntegrationTest`.
- `identity/integration/BrevoApiMailAdapterTest`: adapter requests sent to a local HTTP server; useful transport evidence, not real Brevo acceptance.

Classification must inspect collaborators, configuration and assertions. For example, `academics/integration/AcademicsPersistenceTest` mocks `NamedParameterJdbcTemplate`, so its SQL expectations are not real PostgreSQL evidence. `jobs/integration/JobListenerTest` mocks the Rabbit channel, so it cannot establish broker delivery. `admindashboard/integration/DashboardSourceReaderTest` uses callable sources rather than real owner databases. Keep such tests in their existing role; do not count them as proof of the missing real boundaries.

## Per-unit integration coverage design

Each row below defines target integration behavior, existing evidence candidates and the priority gap to verify or fill. Requirement IDs are starting anchors, not a completed clause-level coverage assertion. Read the full applicable rule and related decisions before implementing each scenario.

| Owner | Requirement anchors | Components and representative outcomes | Reuse and gap to resolve |
|---|---|---|---|
| U01 Account & Access | BR-U01-01–04, 10–11 | Account commands/repository plus audit; Redis session invalidation/refresh; current persisted role/status controls access. Denied or stale requests cause no unauthorized change. | Reuse PostgreSQL/Redis suites and composed security cases. Verify authentication/session service composition and account-command audit, not only individual adapters. |
| U02 Audit | BR-U02-01–05, 07–09 | `IAudit` production provider and PostgreSQL: SUCCESS follows caller commit/rollback; DENIED/FAILURE survive caller rollback; duplicate ID is safe; sensitive detail is rejected; admin query resolves actor through real U01 lookup. | Reuse owner rollback cases. Add focused audit transaction/query integration for outcomes not exercised by those cases. |
| U03 Job and Settings | BR-U03-02–07, 50–51, 53, 56, 59 | Actual enqueue/after-commit and Rabbit sender/listener/dispatcher path; rollback does not publish; controlled retry/duplicate delivery respects owner state. Persisted settings affect a real consumer. File grants use real Redis; backup status/storage boundaries are stated. | Reuse file expiry/backup/HTTP adapter tests. Fill actual broker job path and settings persistence/consumer gaps; mocked Channel tests remain separate. |
| U04 Subject Administration | BR-U04-01–04 | Real U01/U04/U05/U02: assign eligible manager, archive all relevant classes atomically, release manager, reopen without restoring prior assignments. Stale version/audit failure leaves no partial state. | Reuse subject and composed requirements cases. Add only uncovered owner compositions/denials. |
| U05 Class & Enrollment | BR-U05-10–16, 20–21, 26 | Actual academics command services, U01/U04 lookups, JDBC repositories and audit: class lifecycle and teacher eligibility; enrollment uniqueness/reactivation/conflict; removed enrollment loses access; events only after commit. | Existing migration and subject composition cover some persistence. Add command-level class/enrollment integration; mocked JDBC tests do not replace it. |
| U06 AI Connections | BR-U06-01–05, 10, 12–13, 16, 20 | Actual source selection, encrypted connection/task/quota persistence and owner callbacks with a local controlled provider boundary. Reservation/settlement and retry do not duplicate quota or apply stale results; foreign owner credentials remain inaccessible. | Reuse AI persistence and MCP idempotency tests. Fill service-to-persistence/quota/result-path gaps without paid API calls. |
| U07 Assignment Authoring | BR-U07-02–04, 13, 23 | Production authoring/publication plus U05 access, assignment/rubric storage, audit and event port: publish valid assignment, lock rubric, reject archived container and stale mutation without partial changes. | Reuse rubric persistence. Add publication/service integration with actual owner contracts; inspect further publication rules before asserting detailed transitions. |
| U08 Assignment Work | BR-U08-01, 10–11; applicable submission rules | U07 assignment snapshot/window, U05 enrollment, work service/repositories, U09 submission callback and audit: start/save/submit, freeze submitted content, reject foreign access/stale save, preserve atomicity when a required callback fails. | Reuse practice-feedback concurrency. Add ordinary work/submission composition; trace snapshot/resubmission expectations to full applicable rules. |
| U09 Assignment Grading | BR-U09-01–03, 10 | U08 submitted work, grading services/JDBC, U05 teaching scope, audit and notification event port: correct lifecycle, stale conflict, publish visibility, resubmission invalidation, rollback of failed compound mutation. | Reuse grading transaction suites. Add service-level U08→U09 handoff and read visibility where missing; repository tests alone do not prove service authorization. |
| U10 Quiz Authoring | BR-U10-15–17, 20–25 | Question/quiz services and PostgreSQL with real U04/U05 scope: publish pinned questions, generated-pool validation and version replacement atomically; event only after commit. | Reuse question/quiz persistence and migration tests. Add publication/service composition where only repository state was exercised. |
| U11 Quiz Practice | BR-U11-01–03, 10–11, 33, 40–41 | Real U10 quiz contract and U05 access with attempt service/repository: immutable attempt snapshot, no correct-answer leak before submission, scoring and attempt limit, authorized penalty decision plus audit; no gradebook entry. | Reuse attempt persistence. Fill start/submit/read and activity-port composition; no browser-monitoring acceptance claim. |
| U12 Group Assignment | BR-U12-01–05; applicable workspace/submission rules | Group membership and actual U05 access; workspace locks/CAS; completed section ownership; group submission→U09 composition; committed events reach real Rabbit hubs while rollback emits nothing. | Reuse group persistence/Rabbit test. Fill service-level membership/work/submission boundaries after reviewing the detailed group rules. |
| U13 Reference Materials | BR-U13-01–03, 10–12 | Real subject/class access, material/module/version repositories, audit and extraction job boundary: ownership, retained versions, failed CAS/compound reorder rollback, real supported parser fixtures. | Reuse composed owner/migration cases and verify parser tests before classifying them. Add only missing real material→file/job/extraction boundaries. |
| U14 Announcements & Notifications | BR-U14-01–06 and notification preference/delivery rules | Announcement→after-commit event→recipient resolution→persisted notice; owner read state; saved email preference; bounded retry/idempotency. Use actual queue integration where claiming job delivery and local mail transport where claiming transport. | Reuse notification/owner/Rabbit suites. Fill gaps across actual producer/consumer contracts; mock mail success is not provider acceptance. |
| U15 Admin Dashboard | BR-U15-01, 10–12, 20–22 | Actual U01/U04/U05 count providers and U03 backup provider composed with dashboard service/controller: correct counts without personal data, admin-only access, one unavailable source does not falsify other sections. | Reuse backup status integration. Add dashboard-to-owner composition; callable-only source tests do not prove owner SQL wiring. |

Coverage is reported per scenario and requirement clause. A unit appearing in this table, a green test, or a code coverage percentage does not establish that all its requirements are covered. Maintain a gap list with covered/partial/not-covered/not-applicable status and rationale; distinguish automated integration scope from deployment/provider/manual acceptance.

## Test architecture and stability

- Keep existing production APIs, database schema, Flyway migrations, dependency versions and persistence choices. New persistence remains JDBC under project conventions; this task does not migrate the remaining JPA units.
- Reuse JUnit 5, Spring Test, Mockito, Testcontainers and current drivers. Prefer focused Spring contexts with production services/adapters. Share small fixture helpers only when multiple suites need them; avoid a universal framework or a mock-heavy full application context.
- A cross-unit claim requires actual production contract providers on both sides. If the provider is stubbed, state that boundary and limit the assertion accordingly. Mock external paid/uncontrolled services; never mock a repository or broker and then claim its real integration was tested.
- Use disposable PostgreSQL/Redis/Rabbit containers and ephemeral ports. Run actual Flyway owner/full migrations as appropriate. Use the restricted application database role for permission assertions; use migrator credentials only for schema setup/intentional fixture fault injection.
- Each case owns its fixture, with cleanup or a fresh schema. Do not rely on another test's order, external shared database contents or currently running application stack. Generate test credentials; never copy live secrets.
- Control business time with a fixed/injectable Clock. Use latches/barriers and bounded asynchronous polling for race/queue cases. Real TTL tests may use bounded real time. Always close contexts, executors, connections and containers.
- Assert observable API/service results, committed database state and actual events. Querying owner tables is permitted in test assertions/fixtures; do not add cross-owner SQL to production code.
- A refactor with unchanged contracts should not force changes to expected behavior. When approved requirements change, update the requirement, case and expected result together. Record a discovered product mismatch as a defect; do not change the oracle simply to pass. Fixes that alter product behavior outside this test task need a separately reviewed proposal.
- Adding tests for existing correct behavior is characterization, not evidence that the project was originally developed with TDD. If a new defect fix is approved, preserve its failing regression before fixing it.

## Delivery groups and workflow

Execute the groups sequentially on the same branch. These are reviewable delivery groups within one task, not new product units:

1. Foundation: U01–U05, including actual audit transactions, settings/job broker behavior and class/enrollment composition.
2. Individual learning and AI: U06–U09, including source/quota, assignment publication, submission and grading handoff.
3. Quiz and collaboration: U10–U12, including published snapshot scoring and group ownership/submission.
4. Content and reporting: U13–U15, closing actual file/job/notification boundaries and dashboard owner sources, reusing existing strong cases.
5. Consolidated verification, traceability and Excel report.

Requirements/scope and this test design are reviewed first. Then prepare the detailed code-generation plan under `aidlc-docs/construction/plans/code-generation-plan/` using the writing-plans workflow. Skip new product stories, product unit generation, production infrastructure redesign and product data-model design because this task verifies existing behavior. Execute test code generation and build/test, then artifact verification. Existing security/resiliency decisions remain in force and PBT remains disabled. Test isolation, safe fixture/secret handling, bounded waits and accurate unavailable outcomes are applicable; new production encryption, topology, DR or deployment changes are outside this test-only design.

## Verification and evidence

1. Before editing tests, establish a fresh baseline for the relevant existing suites and check Docker availability. Preserve original failures/skips. Do not treat historical 2,004-pass evidence as a result for this newer staging revision.
2. For each group, run focused existing and added suites and inspect JUnit XML for counts, failures, errors and skips. Every required report case must resolve to actual test execution(s). A selected parameterized case can summarize several invocations only when all required invocations pass.
3. Keep the existing `mvn -B verify` CI entry point. Extend `scripts/assert-required-backend-tests.ps1` with the final selected required suites and meaningful minimum counts, so required integrations cannot silently skip when Docker is missing. Retain the existing guards, including their minimum counts.
4. Run the complete backend verification after the final test/config change. No browser E2E run is required for this integration deliverable. Perform additional frontend checks only if frontend code is actually changed, which is not planned.
5. Preserve evidence for the tested source revision, dirty source/test diff or content hashes where relevant, actual command, environment, timestamp, suite/method/invocation results and report mapping. Revalidate affected mappings after late edits. Keep secret-free reproducible instructions in tracked documentation; scratch logs can remain ignored.
6. Classify each case as Passed/Failed/Pending/N/A using the template's conventions. A skipped/not-run/blocked case is not Passed. Record failures honestly and list defects/gaps rather than weakening assertions. Later rounds remain Pending until independently executed and evidenced.

No commit, push, PR, merge, production deployment, live migration, paid AI request or real recipient email is part of the current authorization.

## English report design

Source: `E:\FPTUni\sem9_fa26\test_report\Report-5.2_Integration Test_Template.xlsx`. Write a new output workbook in the conversation's writable output directory; leave the template untouched.

- Retain Cover, Test Cases, Test Statistics and the provided case-table structure. Copy the supplied feature sheet styling for per-unit tabs U01–U15; use the existing security tab for genuine cross-cutting integration cases if needed. Remove/replace sample feature names and sample case values, not the template's visual design.
- Preserve fonts, colors, borders, widths, merged cells, status validation and relevant native charts/conditional formatting. Extend styled rows/summary references only as needed. Adjust copied data-row heights within the template's visual convention so text is readable; do not shrink fonts to fit long text.
- The template has existing broken sample references (`#REF!`) and inconsistent sample totals. Replace affected sample formulas/references with formulas targeting the populated report. Do not carry template errors into delivered results.
- Write concise descriptions and explicit numbered procedures: setup, action, verification. Expected results describe visible outcomes and persisted effects, including what must remain unchanged on failure. Preconditions state role, fixture and actual/mock boundaries.
- Use stable IDs such as `IT-U05-001`. Include requirement IDs and `TestClass#method` in the template's notes/reference area; link to a tracked scenario-to-execution manifest for detailed provenance. One cross-unit scenario has one reporting owner and is counted once; other units link to that ID rather than duplicate its results.
- Do not impose a case-count quota or copy all test invocations into Excel. Select representative integration scenarios for every unit and the principal success, denial, rollback, stale/concurrent and async retry paths that apply. Keep detailed execution counts in the evidence manifest.
- Populate project identity from verified project documentation; do not invent a human tester, approver or approval date. Automation can be identified as JUnit 5. Report issue/test dates are actual dates, not copied sample dates. Do not fill unexecuted rounds with success.
- Test coverage in template statistics must be labeled as execution coverage for the included report cases. Requirements coverage is the separate clause map; do not infer it from Passed/Total.
- Author/recalculate with the bundled spreadsheet runtime, verify formulas and counts, inspect every populated sheet visually, export and reopen. Compare format/native features with the source. If the authoring export cannot preserve a native feature exactly, preserve the original package element while inserting authored values/formulas, and explicitly validate the resulting workbook.

## Completion criteria

- Every unit U01–U15 has representative, executable integration evidence and an explicit requirement/scenario mapping, or any blocked gap is clearly recorded without a false complete claim.
- Added scenarios test real boundaries and improve identified coverage; pre-existing useful cases are reused rather than rewritten.
- Required suites run without skipped cases for a passing delivery. Remaining failures/blockers are visible and linked to their cases.
- The final workbook reconciles detail results to summary formulas, distinguishes actual rounds, contains no unintended broken references, and preserves template styling.
- Source template and unrelated user changes are preserved. The branch contains scoped tests, any necessary test-run verification config and AI-DLC traceability documentation.

## Builder discretion

Choose exact suite names, focused context imports, fixture helpers, report row count and scenario ownership after detailed group review. Prefer existing infrastructure and avoid duplicate claims. Do not interpret this discretion as permission to omit a whole unit silently, redefine requirements, remove required guards or mark mocks as real providers.

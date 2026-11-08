# U04/U13/U14 Requirements-Based Test Remediation Implementation Plan

> For agentic workers: use superpowers:executing-plans inline, unit by unit, and one independent whole-change review before handoff. Track completed steps immediately. AI-DLC Code Generation Part 1 approved at question12=A; execute on existing fix/remove-term.

**Goal:** Align current requirements with confirmed decisions, repair non-discriminating tests and add missing business scenarios for U04/U13/U14 without presenting unit mocks as complete runtime acceptance.

**Architecture:** Reuse current U04 subjects, U13 materials, U14 announcements/notifications and shared contracts. Change production behavior only where a requirement-derived regression demonstrates a mismatch. The approved archive-confirmation explanation is the expected visible UI change. Existing semester removal remains intact; no new schema change is planned.

**Tech Stack:** Existing Java/Spring/JUnit/Mockito/Testcontainers/PostgreSQL and Next.js/TypeScript/Vitest. Follow project code/persistence conventions; do not introduce dependencies or migrate unrelated JPA owners to JDBC.

**Spec:** [Confirmed decisions 1–11](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md), current FR/UC/stories under `aidlc-docs/inception`, and the three units' current functional-design/NFR documents. Source authority is `aidlc-docs` only. Historical reports and as-built snapshots are supporting evidence, not approval of new behavior.

## Constraints

- Work on current `fix/remove-term`; preserve semester removal, user edits and two pre-existing staged draft files. No commit/push/merge/deploy/live database writes or report drafting.
- Requirements and expected outcomes come from the confirmed baseline, not current output or production constants. A BR comment/test name alone is not coverage evidence.
- Record a case-to-requirement mapping before writing each test. Explicitly identify fixture authority, input, expected output/error and forbidden side effects. Keep permissions valid except the guard deliberately under test.
- Coverage-only tests may pass immediately when existing behavior is correct; record them as added coverage, not a fabricated RED/GREEN cycle. For actual behavior changes, observe the expected regression failure before implementing and then GREEN. Do not claim the original full suite was TDD.
- Retain useful transport/wiring/security tests and classify them appropriately. Remove only obsolete/duplicate assertions with a recorded reason; never delete a failing business requirement to get green.
- No new AI/provider calls, macro/formula execution, OCR/RAG features, permission expansion, extra confirmation step or class-semester fields.
- New persistence uses the existing owner JDBC boundary. Do not change shared transaction managers or migration history; real database checks use disposable containers.
- Security/Resiliency scope retained, PBT disabled. Do not log secrets, material text, notification bodies or recipient addresses in evidence.

## Review focus

- Negative fixtures must fail for the named requirement: validate exact status/code and no successful write/audit/event, with unrelated guards satisfied.
- Current-v2/history-v1 fixtures must contain two genuinely different versions; a future version request is a separate scenario.
- Verify actual U13 selectable-list filtering, actual XLSX parsing and actual U14 retry callback behavior; mocked success/unused callbacks do not establish those outcomes.
- Unit invocation checks do not establish SQL retention, atomic rollback, concurrent email caps, broker delivery, two-instance SSE or measured latency.
- Preserve stored email preferences and mandatory Admin backup; source defaults must not overwrite explicit opt-out.

## Phase 1 — Freeze the confirmed oracle

- [x] Record answers 1–11, including XLSX as an AI source, in the decision file.
- [x] Synchronize current requirements for cascading archive, U04 class-count/UI choices, fixed four-format MATERIAL policy, XLSX extraction, staff email default-on and archived staff access. Retain unrelated-purpose policies and historical evidence.
- [x] Record approval of this plan at question12: user selected a in file and confirmed in chat.
- [x] At execution start, record baseline Git SHA and hashes of applicable requirement files; create the per-case mapping within this plan/working ledger. Do not start a unit-test report.
- [x] Reuse existing relevant passing cases instead of duplicating them. Mark each requirement covered/partial/missing/conflicting/N/A with a precise assertion reference and layer; do not invent a numerical coverage percentage.

## Requirement-derived case groups

These are scenario specifications to refine into individual test IDs before editing tests. A group does not mean that one test proves every listed requirement.

| Group | Requirement source | Expected outcomes to add or strengthen |
|---|---|---|
| U04-C01 | BR01/02/03/06/50–54 | Preserve existing validation/auth/version/search/audit coverage; exact denial and safe error assertions where incomplete. List has no class-count column; detail tab shows zero and hides unavailable count. |
| U04-C02 | BR04/11, D-U04-01/03 | Archive removes manager once, advances version once and emits both named audits with correct before/after; popup explains class cascade/manager removal/history retention, with one confirmation. |
| U04-C03 | BR12/F7 | Multi-subject release propagates second-item repository/audit failure, skips stale reassignment and does not report success after failure. Sorted locking and all-or-nothing account/subject changes require appropriate repository/integration evidence. |
| U04-C04 | BR07, U13 BR03 | Archived subject blocks U07 subject-resource writes and U13 material/module writes. Existing U10/U05 cases are credited. Read/history behavior remains allowed only under current scope. |
| U04-C05 | D-U04-02/04/06 | Preserve current class list/filter UI without semester, no separate create-cancel button, X/Escape no mutation, no counters added to status card. Do not change other unapproved UI details. |
| U13-C01 | BR01/02/03/40/42 | Role/scope/current assignment, archived subject versus archived class/material and U01/U04/U05 failure cases; exact denial/error and no sensitive data or write. |
| U13-C02 | BR11/12/40 | Title200/201, description2000/2001, order bounds; metadata versus new content version; current-v2/old-v1 Student denial and authorized staff history access. |
| U13-C03 | BR13/23/31/32, D-U13-10 | Real listSelectable implementation returns active, authorized, correct subject/class union and text status. Archived sources excluded from new AI selection; existing staff/MCP reference read allowed; recheck every requested material. Attachment filtering tested at owner query boundary, not only mocked delegation. |
| U13-C04 | BR10/14/20/30, D-U13-07/08/11 | Four fixed file formats, bounded configured size, real XLSX workbook with multiple sheets and recognizable cell values through parser; no automatic AI call on upload. Existing preview/parser tests are retained. |
| U13-C05 | BR30/42, NFR10/11 |49/50 text-density boundary and1,000,000-character cap, exact-version/idempotent job updates and failure fallback: text FAILED does not block authorized view/download. Workbook fixtures must contain enough readable text for the documented extracted-text status. |
| U13-C06 | BR41 | Exact MATERIAL_CREATED/UPDATED/VERSION_CREATED/ARCHIVED action, actor/object and success/no-success-on-failure assertions; no success audit for rejected CAS; no per-view/download audit. |
| U13-C07 | M01–M06 | Module name/order bounds; manager/teacher/Student permissions; default module, archived-subject no-create, repeated open; last empty module deletion; wrong-scope attach/detach; reorder empty/duplicate/foreign/stale/missing fields/500 versus501 and valid subset. Real unique/cascade/backfill are integration concerns. |
| U14-C01 | BR01–06/40/41 | Exact create/edit/delete access errors and audits, returned version usable for second edit, author/editor/time preservation and stale no-write. Deleted feed exclusion and chronological scoped feed require real query/repository evidence. |
| U14-C02 | BR03/10–13/20/21, D-U14-09 | Retain recipient/important×email matrix. Literal role-specific expected defaults, saved opt-out preserved, own-only updates and mandatory Admin backup. Email opt-out keeps the in-app notification. |
| U14-C03 | BR23/42 | Execute RabbitTemplate retry callback; inspect actual published payload, incremented attempt/routing/confirm-before-ACK and exhausted/failing publication outcomes. Credit existing U03 dispatcher/backoff tests; do not claim they prove U14 event retries. |
| U14-C04 | BR30–32, US-RPT-001 S2 | Test real group non-submitter recipient calculation instead of a preset mock list; enrolled members of unsubmitted groups only, subject expansion, reschedule once, closed/retired/late-open and timing boundaries with Clock fixtures. |
| U14-C05 | BR14/22/24 | Unit cutoff/budget/priority/expiry scenarios plus real SQL deletion around180-day cutoff, retained newer data/cascade integrity and concurrent reservation cap. Cover complete priority ordering if not already asserted. |
| U14-C06 | BR10/12/13/41, NFR11/31 | Source transaction/event isolation, persistent owner-only read/read-all, correct audit snapshots and log-redaction sentinels. Classify shared component coverage separately from the complete business journey. |

## Phase 2 — Implement U04 first

Primary files: `frontend/tests/features/subjects/components/SubjectStatusActions.test.tsx`, `frontend/src/features/subjects/components/SubjectStatusActions.tsx`, backend `subjects/service/SubjectStatusTest.java`, `SubjectScopeServiceTest.java`, `SubjectAuditTest.java`; dependent guard tests under `assignments/service/AssignmentAuthoringServiceTest.java` and `materials/service/MaterialAccessPolicyTest.java`/`MaterialModuleServiceTest.java`.

- [x] Translate U04 groups into fixture/expected-result cases, retaining existing adequate cases.
- [x] Change the popup expectation first, run the full affected file and observe its expected RED; add the short explanation in the existing dialog and rerun GREEN. No extra confirmation/input field.
- [x] Add missing lifecycle/release/archived-subject cases, run complete touched suites; fix only demonstrated violations of approved rules.
- [x] Identify which archive/enrollment/account-disable clauses still need real composed U01/U04/U05 verification. Do not label mock rollback assertions as closure of them.

## Phase 3 — Implement U13

Primary files under `backend/src/test/java/edu/aiplatform/materials/`: `service/MaterialAccessPolicyTest.java`, `MaterialQueryAndReferenceTest.java`, `MaterialServiceTest.java`, `MaterialModuleServiceTest.java`, `MaterialTextExtractJobHandlerTest.java`, `integration/MaterialTextExtractorTest.java`. Shared policy tests are `files/model/PurposePolicyTest.java` and upload/content tests. Frontend targets: `materials/model/xlsx.test.ts`, `components/MaterialDialogs.test.tsx`, `MaterialPreview.test.tsx`, `ModuleBoard.test.tsx` and API tests.

- [x] Add case mapping and repair the historical-version fixture; authorize unrelated guards so a negative test cannot pass for a different reason.
- [x] Add real selectable-list, policy/failure, audit and boundary cases; separate consumer protocol mocks from actual U13 service behavior.
- [x] Add XLSX parser regression using a generated real workbook; verify cell text/provenance/current scope where applicable, not just whitelist acceptance. Keep content/type and size validation intact.
- [x] Strengthen Quiz popup close/navigation test to match its claim, or rename it to its genuinely tested behavior if navigation belongs another existing case.
- [x] Add module scenarios; run scoped suites. For a genuine implementation defect, record RED before minimal fix and GREEN after; for already-correct behavior record coverage-only PASS.
- [x] Update inaccurate current source comments, such as the three-format PurposePolicy comment, without adding dependencies or altering other purpose policies.

## Phase 4 — Implement U14

Primary files under `backend/src/test/java/edu/aiplatform/`: `announcements/service/AnnouncementServiceTest.java`, `notifications/service/NotificationServiceTest.java`, `RecipientResolverTest.java`, `EmailDeliveryServiceTest.java`, `DeadlineReminderScannerTest.java`, `integration/NotificationEventListenerTest.java`, `NotificationDeliveryIntegrationTest.java`. Group provider tests belong to `assignmentwork/service/StudentAssignmentServiceTest.java` and `groupdocs/service/GroupWorkQueryServiceTest.java`. Frontend announcement/notification tests are retained/strengthened as applicable.

- [x] Add case mapping, tighten specific metadata/error/audit assertions and exercise retry callbacks rather than only invocation.
- [x] Add group/deadline, saved preferences/opt-out persistence, source/privacy and ordering cases. Do not reset user preferences or expand email recipients.
- [x] Use the existing disposable PostgreSQL infrastructure for deletion/feed/retention and budget invariants which mocks cannot establish; add a focused repository integration test if the existing fixture cannot represent the actual owner query cleanly.
- [x] Record exact180-day cutoff semantics before writing the boundary case; prefer no premature deletion and derive interpretation from the documented retention contract. No clock changes or manual live DB edits.
- [x] Apply RED/GREEN fixes only for demonstrated current requirement failures; retain existing retry limits, transaction boundaries and safe errors.

## Phase 5 — Verify and keep remaining gates honest

- [x] Run affected complete backend and frontend files after each unit; then full backend verify with required real PostgreSQL guard and any newly required owner integration suite. Record failures/errors/skips separately.
- [x] Run frontend typecheck, lint and production build; no timeout increases, skipped tests, dependency upgrades or changed build policy to hide failures.
- [x] Execute one fresh whole-change review, verify findings, fix real issues and rerun relevant checks.
- [x] Validate Markdown links/changed diagrams and the final diff; preserve staged drafts and all user changes. Update plan checkboxes, state and append-only audit with actual evidence; do not produce a unit-test report yet.
- [x] Final requirement mapping must distinguish complete unit assertions, partial scenarios and pending integration/manual/performance gates. Do not certify full NFR closure or a coverage percentage from green unit tests alone.

### Gates explicitly not certified by this test-remediation slice

Full real-security four-role matrix on every endpoint, browser PDF/YouTube/Excel behavior, real SMTP/provider crash-window delivery, two-instance SSE, representative50MB/200-recipient/p95 benchmarks and complete cross-unit account-disable/archive transactions need their own measured/composed evidence. Existing valid integration/E2E evidence is credited with its date and exact scope; it is not silently replaced with mocks. No requirement is deleted merely because this phase cannot demonstrate it.

Status: approved remediation slice implemented and verified locally on fix/remove-term. The assertion map explicitly retains partial/missing contract, integration and NFR gates; this is not complete requirement acceptance. Report drafting remains paused; no Git publication authorized.

## Execution ledger

Approval: question12=a. Baseline c9b7ce96 on fix/remove-term, preserving all earlier dirty/staged artifacts. SHA256 snapshot of8 requirement/decision inputs: `.tmp/2026-11-08-test-requirements-baseline.json`. This is a frozen expected-outcome baseline, not a test report.

### U04 cases specified before test edits

- U04-C02-a / BR04,11,D-U04-03: active subject confirmation explains DRAFT/OPEN class archive, manager removal and retained academic history, with one mutation only after confirmation. Reopen must not describe a new cascade. Expected RED on current popup before adding explanation.
- U04-C02-b / BR11,52: subject with manager, version1 archives to version2 with manager null; exactly SUBJECT_ARCHIVED and SUBJECT_MANAGER_CHANGED audit actions, actor/object/time and before/after manager correct.
- U04-C03-a / BR12/F7: multiple managed subjects supplied out of order are locked in ID order; repository failure on second release propagates and no second successful audit appears. Unit interaction evidence only, not database rollback proof.
- U04-C03-b / BR12: audit failure during second release propagates and no successful list is returned; keep existing stale-reassignment/lost-update cases.
- U04-C04-a / BR07: otherwise-authorized manager cannot create/edit subject Assignment when subject archived, with no success write/audit. Actual AssignmentAccess used, not a pre-thrown authorization mock.
- U04-C05: retain existing no-list-class-count, detail zero/unknown, X/Escape and no-semester cases; do not duplicate their existing assertions.

### U13 cases specified before test edits

- C01: archived subject still permits scoped staff reads but rejects material/module writes with409; dependency failures return503 without private exception content. Keep existing role/private-class/current-assignment cases.
- C02: current version2/file-current and historical version1/file-old: Student gets only2, staff may download1. Metadata accepts title200/description2000/order0, rejects201/2001/negative without writes.
- C03: actual ReferenceMaterialService and MaterialQueryService over owner-repository fixture: subject plus own-class union, ACTIVE-only selection, EXTRACTED flag; attachment requested IDs filter archived for Student. Existing second material denied by access must prevent every text read.
- C04: generate a real two-sheet XLSX with recognizable text; actual bounded parser returns both sheets' values; keep existing content-whitelist and no-AI upload checks.
- C05:49/50 characters per page and1,000,000 cap; FAILED extraction still permits authorized detail/download.
- C06: named update/version/archive audits include actor/object/time; failed metadata CAS emits no success.
- C07: retain existing foreign attach, move, stale/CAS, archived move, Student and Quiz visibility cases; add module name/order bounds, manager default/repeated open, archived no-create, final empty deletion and reorder subset/limits.

U04 backend coverage-only run:51/51 passing; popup RED observed1/9 failing on absent cascade explanation before production edit. No claim of database rollback from release mocks.

U04 popup GREEN and full feature suite:87/87 passing. No further U04 production behavior changed.

### U14 cases specified before test edits

- C01: two consecutive announcement edits use returned version, preserve author/createdAt, track editor/time, exact named audit snapshots; stale/CAS no success. Deleted announcement excluded and feed class/order verified at repository layer.
- C02: each role's literal default whitelist retained; saved false stays false; opt-out must keep in-app row while email becomes SKIPPED.
- C03: execute RabbitOperations callback, verify unchanged payload, persistent delivery, incremented attempt, routing, confirm before ACK; publish/confirm failure NACK-requeue, exhausted reject.
- C04: actual group non-submission provider selects enrolled unsubmitted group members; existing scanner subject expansion/once tests retained; add reschedule/status/time boundaries.
- C05: retention strictly older than now-180days; exact cutoff/newer remain including delivery child integrity. Use real repository and disposable PostgreSQL, plus concurrent budget reservations and priority order/expired handling.
- C06: retain source matching/transaction isolation and metadata privacy cases; persist owner-only single/read-all and saved preferences in owner integration tests. Shared/mock SSE is not multi-instance evidence.

### Working assertion map (verified assertions and open gates)

This is the implementation ledger, not the paused unit-test report. “Unit assertions” identifies precisely tested outcomes; it does not certify every integration/API/NFR clause. New cases were specified above before editing tests. Historical tests are credited for their assertions, without claiming historical TDD or code-independent authorship.

| Requirement | Assertion reference and layer | Boundary still open |
|---|---|---|
| U04 BR01,51,54 | `SubjectAdminCommandTest.nonAdminCannotExecuteAnyCommand`, `SubjectAdminReadTest.authorizationErrorOrNullFailsClosedBeforeRepository`, `SubjectAuditTest.authorizationFailureIsAuditedAsRejectedWithSafeReason`: roles, no write/read and safe denied audit — unit assertions | Full real JWT/CSRF four-role endpoint matrix |
| U04 BR02,06,53 | `SubjectAdminCommandTest.invalidInputAndMissingVersionNeverWrite`, `invalidManagerIsRejectedAtSubmit`, `onlyNamedUniqueConstraintIsTranslatedForConcurrentInsert`; repository integration — unit/SQL assertions | API journey remains separate |
| U04 BR03,10 | `SubjectScopeServiceTest.readsCurrentAssignmentEveryTimeAndDoesNotCacheAnAllowDecision`, `SubjectAdminCommandTest.managerChangeReloadsLockedSubjectBeforeValidationAndWrite`, `assigningManagerToArchivedSubjectIsRejected` — unit assertions | Real subsequent request across U01/U04 not inferred from stubs |
| U04 BR04,11 | `SubjectStatusTest.archiveAsksU05ToArchiveEveryClassOnCallerThreadBeforeSavingSubject`, `archiveWithManagerEmitsBothNamedAuditsForOneVersionChange`, `reopenDoesNotReadOrReopenClasses`; `SubjectStatusActions` warning/confirm tests — partial composed requirement | Actual U04+U05 archive rollback/enrollment preservation with shared transaction still pending; term-migration retention is a different scenario |
| U04 BR05,50 | `SubjectClassesTable.test.tsx` read-only three-column/filter cases; `SubjectListPage.test.tsx` no-count-column assertion; `SubjectDetailPage.test.tsx` unknown/zero tab assertions; `SubjectAdminReadTest.paginationSearchCountsAndManagerDedupFollowCurrentPage` — unit/component assertions | No browser certification |
| U04 BR07 | `AssignmentAuthoringServiceTest.archivedSubjectRejectsCreateAndEditEvenWhenManagerAuthorizationAllows`; `MaterialAccessPolicyTest.archivedSubjectBlocksWritesEvenWhenAuthorizationAllowsAndRetainsTeacherRead`; existing U05/U10 guards retained — partial cross-unit coverage | Complete composed archive flow |
| U04 BR12 | `SubjectScopeServiceTest.releaseUnassignsEveryManagedSubjectBumpsVersionAndAuditsEach`, `releasesLockInIdOrderAndSecondRepositoryFailurePropagates`, `secondReleaseAuditFailureIsNotConvertedIntoSuccessfulResult` — partial | Real U01 disable+multiple-subject+audit transaction rollback/reopen still pending |
| U04 BR52 | `SubjectAuditTest.successPayloadsAreAllowlistedAndKeepExplicitNullForUnassignment`, command metadata changed-field assertions, named status audit assertions — unit assertions | Persistent cross-unit audit atomicity above |
| U13 BR01–03,40,42 (access) | `MaterialAccessPolicyTest` current-role/private-class/archived-subject/class tests and `unavailableSubjectAndClassFailClosedWithoutPrivateDetails`, existing U01 failure test — unit assertions | Full actual U01 scope dependency composition |
| U13 BR10,14 | `PurposePolicyTest`, `MaterialServiceTest.createAttachesTrustedFileAndEnqueuesVersionedJob`, `youtubeCreateDoesNotCallFilesOrJobs`, `YoutubeVideoTest` — policy/source/job assertions | No live storage/YouTube/AI calls |
| U13 BR11,12 | `MaterialServiceTest.metadataAcceptsDocumentedBoundsAndRejectsOverflowWithoutWrites`, metadata/content version cases; repaired `MaterialQueryAndReferenceTest.studentsCannotDownloadOldVersions` asserts current2/history1/token/history visibility — unit assertions | HTTP bean validation independently retained |
| U13 BR13,32 | `MaterialQueryAndReferenceTest.staffMcpCanReadArchivedReferenceButNewAiSelectionCannotUseIt`, actual policy archived inherited teacher/manager read cases — unit assertions | MCP service and authorization tested at respective boundaries, not real identity end-to-end |
| U13 BR20,21 | real parser formats; existing frontend XLSX sheet/viewer and preview tests; `tokenIsIssuedOnlyAfterAuthorization` — partial | Real browser PDF/XLSX/YouTube behavior and actual token TTL |
| U13 BR22 | Retired requirement: Learning Material embed extension removed by approved Nov1 baseline | N/A; Quiz-in-module covered by M03/M05 |
| U13 BR23 | `MaterialQueryAndReferenceTest.selectableAndAttachmentListsFilterActualQueryResults` requested-ID/Student ACTIVE filter — partial | Real owner SQL scope filtering still not established by mocked repository rows |
| U13 BR30,42 (extraction) | `MaterialTextExtractorTest.extractsActualXlsxAcrossSheets` (actual parser; asserts both sheets and readable text density); PDF/DOCX/PPTX cases; `MaterialTextExtractJobHandlerTest.textDensityBoundaryRequiresFiftyCharactersPerPage`, `extractedTextIsCappedAtOneMillionCharacters`; query `failedExtractionDoesNotBlockAuthorizedViewOrDownload` — unit/parser assertions | Formula non-execution, macro/OCR runtime certification and large-file performance not established by this fixture |
| U13 BR31 | actual service `selectableAndAttachmentListsFilterActualQueryResults`, `existingButForbiddenSecondIdPreventsAllTextReads`, budget/ten-ID cases — partial | Current `MaterialText` contract lacks versionNo although BR31 requests provenance version. No assertion can certify a field absent from contract; resolving this shared U06 contract remains follow-up, not silently documented away |
| U13 BR41 | `MaterialServiceTest.mutationsAuditExactActionActorObjectTimeAndVersion`; existing create audit and CAS-no-success tests — unit assertions | Persistent audit transaction durability separate |
| U13 M01,M06 | `MaterialModuleServiceTest.moduleNamesAndOrdersRespectDocumentedBounds`, request-validation 500/501/missing fields, existing duplicate/stale/foreign/CAS and subset test — model/validator/service assertions | Full HTTP validation matrix separate |
| U13 M02 | manager default/repeated-open, existing teacher default and Student no-create, archived subject no-create — unit assertions; `MaterialModuleMigrationIntegrationTest` real seeded backfill | Concurrent first-open default uniqueness not specified/certified |
| U13 M03,M04 | existing attach foreign/move/delete-active/archived-last/move cases, last empty deletion; real migration unique quiz link and delete-link cascade — unit/SQL constraints | Complete repository attachment SQL query above remains partial |
| U13 M05 | existing subject/class/published-retired/Student module Quiz visibility tests — unit assertions | Real Quiz authorization provider composition separate |
| U14 BR01,02,40 | `AnnouncementServiceTest.cannotManage`, `anotherTeacherCannotManage`, enrolled Student cases, sanitization and validation tests — unit assertions with exact404 strengthened | Real four-role endpoint/security matrix separate |
| U14 BR03,10–12 | `RecipientResolverTest.announcementEmailFlagMatrixKeepsInAppRecipients`, subject expansion/dedupe/source tests; `NotificationServiceTest.sseOnlyAfterCommitAndOneNoticePerRecipient`; real `NotificationDeliveryIntegrationTest` rollback/replay tests — unit/local-SQL partial | External producer/broker commit and SMTP crash-window delivery not certified |
| U14 BR04,05,41 | `AnnouncementServiceTest.consecutiveEditsUseReturnedVersionAndAuditExactMetadataWithoutContent`, delete actor/time/status and CAS-no-success — unit assertions | Repository-backed feed test also checks deleted exclusion |
| U14 BR06 | `NotificationDeliveryIntegrationTest.announcementRepositoryExcludesDeletedAndForeignClassAndOrdersNewestFirst` — actual owner query | Class-list API-to-browser journey separate |
| U14 BR13 | real `savedOptOutPersistsWhileInAppNoticeAndOwnerOnlyReadStateRemain`: foreign read denied; own read and read-all persisted without changing foreign row; existing principal-only service/channel cases — local-SQL/unit assertions | Two-instance SSE and real session isolation separate |
| U14 BR14 | real `retentionDeletesOnlyOlderThan180DaysAndCascadesOnlyTheirDeliveries`: cutoff−1s deleted; cutoff/newer retained — SQL assertions | Production scheduling/operational retention not measured |
| U14 BR20,21 | literal role whitelist/default cases; `storedOptOutIsPreservedWhileOtherDefaultsStayEnabled`; locked Admin backup; actual preference/in-app/SKIPPED integration — unit/local-SQL assertions | Existing historical opt-out rows are preserved, no data reset |
| U14 BR22,24 | actual `concurrentSchedulesReserveOnlyAvailableDailySlot`, `deferredRepositoryOrdersEveryPriorityAndExcludesFutureReservations`; existing expiry/Vietnam-midnight/cap tests — SQL/unit assertions | Representative 300-email workload/latency not measured |
| U14 BR23,42 | `NotificationEventListenerTest.temporaryFailureSchedulesBoundedRetry` now executes callback and checks payload/header/confirm-beforeACK; failed confirmation/exhausted rejection; shared U03 dispatcher tests credited separately — protocol/unit assertions | No actual broker-confirm or provider exactly-once proof |
| U14 BR30–32 | `GroupWorkQueryServiceTest.reminderProviderSelectsOnlyEnrolledMembersWhoseActualGroupHasNotSubmitted` composes actual U08+U12 services over repository fixtures; scanner reschedule/late/closed/retired/once/time-window cases — unit/service-composition assertions | Real group/enrollment SQL and distributed scanner race separate |
| U14 privacy/NFR11,31 | scanner safe-log sentinel, existing grade payload/mail body redaction, metadata-only announcement audit — unit assertions | Whole-system log audit and measured NFRs remain partial |

Ruling: keep absent provenance-version contract and actual material owner-query filtering explicitly partial rather than claim complete coverage from passing DTO/mocked-list tests — the shared U06 contract change is outside this minimal test slice, and actual material owner-query composition remains an unexecuted integration gate. No persistence migration was performed or required as evidence. Existing performance/security/manual gates remain requirements, with no invented pass or coverage percentage.

Ruling during final assertion review: the existing SubjectDetailPage null-count test is discriminating, but a zero-valued fixture elsewhere is not a zero-badge assertion. Add direct component cases for API counts0 and7, assert the value beside the Lớp học tab, and keep unknown-count/no-list-column cases. This corrects the earlier “retain existing zero assertion” assumption; it is coverage-only, with no UI behavior change.

Final fixture review: the selectable subject-list assertion now uses a SUBJECT_MANAGER actor; the class/attachment assertions use Student. An otherwise-allowing policy mock must not make an invalid Student subject-list scenario look authorized. Application behavior unchanged; rerun the complete MaterialQueryAndReferenceTest file after this fixture-only correction.

Independent whole-change review: corrected the proper-subset module reorder fixture (two modules exist, only one requested, omitted module not updated) and renamed the XLSX parser test to its proven multi-sheet extraction behavior. Reviewer verified both, the owned-unread read-all fixture, and the final0/7 count assertions; no remaining actionable production/test finding at that review boundary. Exact verification evidence follows after processes complete.

### Final verification — 2026-11-08

- Full backend offline Maven verify: BUILD SUCCESS,1,879 tests, zero failures/errors/skips, executable JAR produced. Required PostgreSQL guard passed31 subject repository +7 notification delivery +10 evaluation version +1 grade-distribution suite. These are included in1,879, not additional tests. New material-module migration1/1 and earlier semester migration2/2 passed without skips.
- After the final fixture-only Manager actor correction, the complete MaterialQueryAndReferenceTest file passed15/15. Production sources stayed unchanged from full verify. Earlier broad U13/U14 run had247 passing and one Mockito fixture error; the repaired final scoped run passed98/98 before full verify. Compilation/setup errors are not TDD RED evidence.
- Frontend full affected feature run:231/231 passing. The final complete SubjectDetailPage file passed13/13, including two newly added0/7 cases not included in the earlier231-case collection. Do not add overlapping reruns to claim a larger suite count. The first combined run had3 existing5s timeouts; complete affected files passed21/21 unchanged, then the complete231-case rerun passed. No timeout/config/assertion was relaxed.
- Frontend TypeScript, scoped ESLint and production build31/31 pages passed; final count-test lint and TypeScript rerun also passed. E2E seed/runner syntax checked; no live E2E/provider/database application.
- Popup TDD evidence: expected missing-explanation failure in1/9 cases before UI edit;9/9 after and87/87 complete U04 feature suite. All added tests for existing correct behavior are coverage-only PASS. No historical TDD/authorship claim.
- Independent whole-change reviewer verified proper-subset fixture, honest XLSX name, read-all owned unread fixture,0/7 badge assertions and final Manager subject-scope actor; no remaining actionable delta findings.
- Relative Markdown links checked, changed Mermaid transition/text inspected, whitespace checked, staged draft files preserved163/16 lines. Source hash evidence in ignored .tmp; final fixture-only additions recorded separately. No report, commit, push, merge, deploy, live migration or real provider call.

Evidence: .tmp/requirements-full-backend.log; requirements-final-scoped-backend.log; requirements-material-scope-final.log; requirements-all-frontend-final.json; requirements-class-count-final.json; requirements-typecheck-final.log; requirements-lint.log; requirements-count-lint.log; requirements-build.log. All relative to repository scratch directory.

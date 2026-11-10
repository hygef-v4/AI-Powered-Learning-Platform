# U04/U13/U14 Atomic Requirement Coverage Implementation Plan

> Execute inline with superpowers:executing-plans; independent read-only clause audits may run in parallel. Continue on the user-authorized fix/remove-term branch. User requested this expanded closure directly after reviewing the prior partial map.

**Goal:** Audit every applicable clause in current aidlc-docs, add missing discriminating tests and fix demonstrated violations. Retain exact evidence by layer instead of equating passing unit tests with complete acceptance.

**Spec:** Current U04/U13/U14 functional design/NFRs, relevant inception FR/UC/stories, and [confirmed decisions](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Prior [minimal slice](2026-11-08-u04-u13-u14-requirements-tests.md) remains dated evidence; its partial gates are now in scope for investigation and local verification.

**Architecture:** Keep existing owner services/contracts and production migrations. Test existing JPA owner adapters through their existing test infrastructure; add no new production ORM persistence or unrelated migration. Use JDBC fixtures and independent SQL observations against disposable PostgreSQL. Use existing Rabbit/SSE and security infrastructure for real protocol/security checks where available. No dependency upgrades.

**Constraints:** No report drafting, commit/push/merge/deploy, live DB changes or real external-provider calls. Preserve dirty/index state and staged drafts. No altered requirements/expectations, skipped cases or increased timeouts to manufacture success. Record missing prerequisites or failed requirements explicitly. Existing business choices remain settled; routine implementation continues without another approval cycle.

## Steps

- [x] Record current requirement sources and extract clauses across BR, flows, frontend and NFR; inventory applicable groups with assertion references and layer. The 35-file SHA256 capture was taken after verification, not before tests; see evidence below.
- [x] Close U13 returned-version provenance: regression must fail on absent versionNo in serialized extractText result, then add actual current-version metadata and adapt constructor consumers. Non-material uploaded sources have no material version; do not invent one.
- [x] Verify real U13 owner scope queries, CAS/history/extraction races, module detach/default/archive and token/access boundaries; add missing tests before changing demonstrated failures.
- [x] Verify composed U04 archive+class/enrollment/history+audit rollback and manager release/account-disable transactions; distinguish owner repository tests from complete source transaction.
- [x] Verify U14 source/recipient/email/read/SSE/retry/security clauses with appropriate actual repositories/protocols; retain and extend existing strong cases rather than duplicate.
- [x] Execute feasible isolated API/protocol/local performance acceptance with explicit fixtures and measured outputs; retain browser/deployment/provider acceptance as open gates instead of substituting mocks.
- [x] Run relevant suites after production changes, then full backend verify/PostgreSQL guards, affected frontend suites/typecheck/lint/build; final test-only refinements verified separately. Independent review and source/doc validation completed.
- [x] Update clause inventory, state and append-only audit with actual pass/fail/unverified outcomes. Do not call unverified clauses covered.

## Pre-test oracle

- U13 BR31: extractText returns material id, current version number, title, bounded current text. Fixture current2/history1 must yield versionNo2 and current text. Missing JSON version metadata is the expected RED; a compiler failure is not sufficient evidence.
- U13 BR12/13/23/40/42: actual owner query must exclude other classes/subjects; Student gets only ACTIVE and current version, current staff may retain historical archived reads, stale CAS cannot append history/audit/jobs. Dependency absence/failure cannot expose data.
- U04 BR04/11/12: a failure after child-class/manager changes must roll back all source, class, enrollment/history and audit effects together; successful reopen does not restore previous child state or manager.
- NFR clauses remain separately identified as measurable criteria; local microbenchmarks are not representative service p95 or multi-instance acceptance unless the tested composition actually matches the criterion.

## Execution ledger

Starting baseline: existing completed minimal slice1879 backend tests,231 frontend cases plus final detail-page13/13 and material-query15/15. Current source remains uncommitted. Read-only unit auditors collect independent clause inventories; implementation and Maven builds stay serialized here.

Pre-test additional regressions: U04 BR04/54 requires a non-null verified archive result; absent bean, null result and exception all return SUBJECT_CLASSES_UNAVAILABLE without subject update/audit, while an empty list is valid. U14 BR31/42 requires unknown group-submission status to propagate as a dependency failure; it must not identify a student as a non-submitter or consume a SENT reminder marker. Existing unit fixtures may mock repositories, but composed rollback needs actual PostgreSQL.

## In-progress evidence and decisions

- Observed U13 version RED: serialized versionNo missing (-1), expected2; added nullable versionNo to MaterialText and actual current version from U13, adapting all constructor consumers. Null denotes unversioned uploaded prompt sources, not a fabricated version1.
- Observed U04/U14 RED: null archive result and GROUP lookup failure both incorrectly returned success instead of raising the required failure. Added null fail-closed guard and propagated group dependency failure; subsequent scoped tests passed.
- Actual owner fixture uses production migrations and existing U04/U13/U14 adapters, real U01 JWT/session authority/current-account authorization, U05 class/enrollment repositories, U02 AuditStore and a real security filter chain. Remote session presence/storage/mail/job sender remain explicit boundaries. Test-only Spring configuration was corrected for EntityManager selection, interface proxy injection, standard ProblemDetail Jackson mapping and Duration conversion; these setup errors are not business RED evidence.
- Observed endpoint-matrix RED: six outside-role module attach/detach requests returned404 without ACCESS_DENIED audit. Added safe denial audit without changing the existing error code.
- Observed real blocked-class-query RED: provider waited beyond the bounded fixture deadline. Added Spring transaction timeout1s for the normal controller-owned lifecycle transaction and retained monotonic lifecycle checks. REQUIRED joining a longer outer transaction and exact cumulative subsecond SQL timing are not certified by this narrower fix; keep these residual clauses explicit.
- Added actual Redis grant redemption/TTL, cached-formula-value XLSX extraction, image-only PDF no-OCR, real broker/two-hub commit/rollback/account isolation, owner SQL/history/CAS/extraction completion, persisted multi-subject account-disable rollback, source commit isolation and actual U03 email dispatcher/repository retry/idempotence scenarios.
- Additional oracle: all eligible assignment/class candidates in the reminder window must be processed; no documented requirement authorizes silently dropping candidate501. Added501-assignment and501-class regression fixtures. A bounded query result already supplied by U07 is distinct from dropping its tail in U14.
- U03 retry interpretation follows its documented dispatcher: initial attempt plus five retries (attempt0…5), then FAILED; six provider attempts are not described as five total attempts. The new composed pipeline fixture asserts this exact routing/persisted state and preserved source notification.
- Local NFR-U14-03 acceptance fixture creates200 actual ACTIVE/enrolled recipients, resolves through real owner contracts and persists notices through U14; asserts≤5000ms and exact200 rows/replay deduplication. This is a declared local dataset, not a production capacity certification.
- Review strengthened rollback oracle: a nontransactional PostgreSQL sequence marks the injected audit trigger, proving it was reached after business writes even though the transaction rolls back. Early authorization/setup/deadline errors cannot satisfy this oracle.
- Additional NFR02 parser oracle: a real65-page raster/text PDF with deterministic high-entropy images must be between50,000,000 and52,428,800 bytes, preserve readable text/pages and finish extraction within120s under unchanged parser limits. No padding-only file, increased heap/deadline or OCR is used.
- Local p95 experiment uses declared single-user owner fixtures (10 material sources, 200 notifications, existing scoped subject data), five warmups and50 samples through actual services/DB; nearest-rank p95 is the48th sorted sample. It does not represent sustained production/proxy load.

## Atomic inventory carried into implementation

| Unit / clause | Existing or newly added assertion | Remaining distinction |
|---|---|---|
| U04 BR01/51/54, server permission | Existing seven-route role/current-account/failure matrices plus real-JWT current DB role/disabled account and actual filter tests | Session-store backend mocked in owner HTTP fixture; external session infrastructure separately tested in U01 |
| U04 BR02/06/53 | Existing code/name/description/version/unique/strict JSON cases, real PostgreSQL uniqueness/CAS | Preserve existing passing cases |
| U04 BR03/10 | Real authorization re-reads manager assignment and account status; old scope grants revoked on next call | Role changes and manager command fixtures combined with existing tests |
| U04 BR04/11 | Real SubjectStatusService→AcademicsContractAdapter→U02 AuditStore: DRAFT/OPEN archive, existing ARCHIVED unchanged, foreign subject untouched, versions/audits, manager cleared, reopen does not restore | Actual enrollment/material snapshots compared; more types of historical learning data remain separate |
| U04 BR12 | Real AccountAdminService→SubjectScopeService with two managed subjects; second-audit failure rolls account/subjects/audits back; success/reopen retains cleared assignments | Redis revocation is a boundary, no real user session is touched |
| U04 BR05/50/UI decisions | Existing read-only/no-list-count/filters plus direct0/7/unknown tab assertions and semester migration evidence | Browser/manual accessibility distinct |
| U04 NFR01/02 | Actual pooled-DB local50-sample p95 for scope/list/detail plus existing paging/current-assignment cases | Representative sustained/concurrent production measurements remain open |
| U04 NFR03/P3 | SubjectClassesReaderTest.saturationRejectsWithoutCallerContextAndTimedOutQueueRecovers:4 blocked workers,16 queued calls, rejection, timed-out queue purge/recovery, no caller security/transaction inheritance | Real provider cancellation under deployment load remains separate |
| U04 lifecycle resource budget | Actual blocked U05 JDBC update regression plus Spring timeout | Longer caller-owned transaction / combined provider budget residual above |
| U13 BR01–03/40/42 | Real four-role route matrix for subject/class/student/material/module controllers with JWT, U01/U04/U05 current DB scope and deny audit | Real file storage remains a boundary |
| U13 BR10/14 | Existing fixed4-format/size/content/YouTube/no-auto-AI cases retained | Provider/browser availability not inferred |
| U13 BR11/12 | Metadata bounds, append2/history1, actual persisted versions/CAS and Student versus staff history | Additive versionNo now closes returned contract gap |
| U13 BR13/23 | Real repository foreign-subject/other-class filtering, requested attachment IDs/ACTIVE selection, archived staff history and immutable archived writes | Source storage access is authorized before boundary token call |
| U13 BR20/21 | Real Redis300s TTL, wrong owner/expired redemption; existing frontend PDF/XLSX/YouTube tests | Live browser/private video behavior still manual |
| U13 BR30/NFR11 | Actual format parser/cached formula/no-OCR fixtures; density49/50, cap1m; real pending-only exact-version completion and stale/history protection; real51,197,757-byte65-page PDF within120s | Adversarial memory/process-limit acceptance remains distinct |
| U13 BR31/32 | Actual returned current version/text; all IDs reauthorized before text reads; existing MCP untrusted/version/paging tests | U07/U10 persistent downstream provenance is not certified by the U13 return contract; no historical TDD claim |
| U13 BR41 | Actual U02 audit transactions plus existing exact successful action/actor/object/version cases; new denial audit regression | Read/download must not invent successful mutation audits |
| U13 M01/M02/M06 | Bounds, manager/teacher/default-once/Student/archive, proper subset and invalid500/501 request validation | Concurrent first-open behavior not newly specified |
| U13 M03/M04/M05 | Detach success/missing/denied/foreign; same-module attach idempotence; real unique/cascade migration; actual HTTP delete relocates archived sources with history unchanged; injected audit failure proves delete/move rollback | Quiz visibility retained in existing tests; no concurrent-client race claim |
| U13 M01/M06 transaction integrity | Actual HTTP two-row reorder, fault-injected second CAS failure, nontransactional sequence witness and independent SQL rollback snapshots, identical successful retry with both versions/order checked | Fault-injected CAS loss is distinct from two simultaneous clients |
| U13 NFR01–03/30/31 | Local actual-DB p95 list/extract10, real51MB parser case, content-based renamed XLSX and macro-enabled XLSM rejection, cached formula/no-OCR and privacy cases | Representative concurrent load and complete adversarial process/memory/macro isolation remain acceptance gates |
| U14 BR01–06/40/41 | Real12-route four-role JWT/DB/CSRF/owner/current membership tests, persisted edit/delete/feed/author and audit; existing sanitizer/UI cases | UI browser journey separate |
| U14 BR10/11/12 | Actual announcement commit versus rollback versus failing sender, real recipient fanout/replay; existing producer routing/privacy matrices | Real external source/broker crash windows not claimed |
| U14 BR13/14 | Actual owner read/read-all/prefs SQL, foreign unread unchanged, real Redis/broker fixtures, retention cutoff and10000/10001 batches | Server emitters are not a full browser/proxy deployment |
| U14 BR20–24 | Role preferences/mandatory Admin, actual queue-handler-dispatcher/repository bounded retries, terminal SENT and concurrent sends; existing cap/priority/Vietnam-day expiry | Provider-accepted then DB-crash exactly-once cannot be inferred from SMTP mocks |
| U14 BR30–32 | Real U08 GROUP failure→U14 marker rollback/recovery; existing individual/group recipient and reschedule/window cases; new501 regressions | Scheduler/backlog wall-clock cadence separate |
| U14 BR42/NFR10/11 | Real source after-commit isolation, owner unique keys and dispatcher retry paths | Actual broker retry/DLQ and provider acceptance remain distinct |
| U14 NFR01/03/04/40 | Real Rabbit subscriptions/two hubs and transaction commit receipt, local200-recipient fixture, existing timing boundaries, full route matrix | Browser receipt/proxy/load and representative sustained backlog cannot be certified by unit fixtures |

## Verified execution evidence — 2026-11-08

- Full backend `mvn -o -B ... verify`: **2,010 tests, zero failures/errors/skips**, BUILD SUCCESS at15:52:14+07. Required PostgreSQL guard:31/7/10/1, all without skips. This full run precedes the final test-only refinements listed below; do not present it as a later rerun.
- Final test-only ContentInspector plus archive snapshot refinement: **6/6 pass**, zero skips,15:55:01+07. ContentInspector detects a genuine XLSX renamed.pdf and rejects a macro-enabled XLSM renamed.xlsx without any production detector change.
- Final module-delete transaction and archive/reopen retention refinements: **3/3 pass**, zero failures/errors/skips,16:03:03+07. `RequirementsCoverageIntegrationTest.moduleDeleteMovesArchivedSourcesWithoutChangingTheirHistory` and `moduleDeleteAuditFailureRollsBackDeleteAndArchivedSourceMove` verify real HTTP/security/U13/U02 transactions. `composedArchiveAndReopenPreserveEnrollmentsAndLearningReferences` compares ACTIVE/REMOVED enrollments, materials and versions after both archive and reopen. An initial test-oracle failure incorrectly expected the HTTP advice to throw instead of returning500; fixed the oracle, not production behavior.
- Final `moduleReorderRollsBackEarlierRowsWhenLaterCasLosesAndCanRetry`: **1/1 pass**, zero failures/errors/skips,16:06:51+07; second SQL update demonstrably reached, specific409 response, full rollback and identical successful retry. Independent review found no actionable finding; this is fault-injected CAS loss, not a simultaneous-client race test.
- Fresh affected frontend **233/233**, zero pending/failures; TypeScript/scoped ESLint and production build **31 pages** exited0. E2E seed/test script syntax checks passed; this is not an executed browser E2E journey.
- Full-run local measurements: scope p95 **16.6127ms** (≤20); U04 list/detail **86.821/62.7901ms** (≤300); U13 list **201.1888ms** (≤300), extract10 **145.8991ms** (≤1000); U14 feed/announcements **16.0673/170.1248ms** (≤300); actual200-recipient fanout **3010ms** (≤5000); genuine51,197,757-byte PDF **6534ms** (≤120s). These are local single-user/test datasets, not production capacity certification.
- Independent read-only review found a rollback-oracle weakness; corrected it with a nontransactional trigger witness and reverified. Follow-up review found no further actionable issue in the reviewed change. Later module transaction tests are coverage additions and do not imply an observed production defect.
- Requirement source capture: `.tmp/clause-requirement-source-hashes.json`,35 current functional/NFR/inception requirement/story files, HEAD c9b7ce96. Capture timestamp is after execution, explicitly retained as such. Scratch logs are local ignored evidence, not a report artifact.
- Final validation:230 relative links in29 changed/untracked AI-DLC Markdown files resolve; `git diff --check` passed. Required PostgreSQL guard repeated successfully;35 requirement hashes unchanged.67 changed source/test/script files captured in `.tmp/clause-final-verified-source-hashes.json`. Two staged draft artifacts remain unchanged163/16 added lines. Build generated no tracked config changes. Existing security/resiliency requirements retained; PBT disabled, no new extension scope.
- Meaningful expected REDs: missing current-version return field; null archive result; swallowed group lookup error; missing module denial audit; blocked JDBC lifecycle update; two501-candidate reminder cases. Compiler/configuration/JSON-fixture errors are setup repairs, not TDD evidence. Existing-behavior coverage tests do not establish original TDD authorship or wholly code-independent historical design.

## Remaining acceptance gates

- [ ] Representative sustained/concurrent API and reminder backlog load; local single-user timings alone do not close these clauses.
- [ ] Full browser journeys for PDF/XLSX/private or unavailable YouTube and SSE receipt across deployed processes/proxy, including browser accessibility behavior.
- [ ] External broker/provider crash-window acceptance, including provider-accepted email followed by DB failure; SMTP mock idempotence is narrower evidence.
- [ ] Broader adversarial parser memory/process-isolation corpus. The51MB/cached-formula/no-OCR/XLSM-content cases close their tested behaviors only.
- [ ] Universal lifecycle deadline when joining a longer caller-owned transaction, including cumulative provider/lock time; current timeout fixes the normal controller-owned path.

These gates and the distinctions above are deliberately not counted as completed requirements. Downstream U07/U10 provenance persistence and broader historical learning-data fixtures are also outside the specific returned-contract/retained-row assertions. No100% claim is made.

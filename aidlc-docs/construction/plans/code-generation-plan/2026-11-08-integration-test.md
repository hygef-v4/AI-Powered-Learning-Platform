# U01–U15 Integration Test Implementation Plan

> Execute inline with superpowers:executing-plans. Track each completed step here; use one independent whole-change review before delivery.

**Goal:** Add missing representative integration evidence across all15 units and produce a traceable English Excel report using the supplied template.

**Architecture:** Reuse the existing PostgreSQL/Redis/Rabbit suites and supplement missing service compositions with a production Spring context using disposable services. No browser/E2E flow or remote provider is required. The report records actual execution results and explicit boundaries.

**Tech Stack:** Java17, Spring Boot3.5.6, JUnit5, existing Testcontainers1.21.4, PostgreSQL16, Redis7.4, RabbitMQ4.1, Maven3.9.9; bundled artifact-tool for XLSX.

**Spec:** [approved design](../../../inception/application-design/2026-11-08-integration-test-design.md).

## Authorization and constraints

User selected question2=A, explicitly authorizing the detailed plan and sequential implementation. This is not a new approval request. Keep the requested `integration-test` branch based on staging321f8892. Do not commit/push/merge, contact live providers, migrate live data or modify product requirements. Keep production code unchanged unless a separately approved defect fix is necessary. Existing behavior tests may pass on first execution; that is characterization, not a retroactive TDD claim. Keep security/resiliency decisions and PBT disabled.

Ruling: supplement focused existing suites with a single shared production-application composition for the missing owner-service paths. Using actual beans avoids copying and maintaining a second set of manual production wiring across15 units. This extends real wiring evidence without requiring a deployed server/browser or replacing the focused suites. Test-only infrastructure properties disable worker schedules/listeners and use disposable service endpoints; no production profile changes.

## Review focus

- False integration claims when repositories/ports are mocked: record exact real and simulated boundaries for each report case.
- Partial writes on audit/compound mutation failure: assert committed state after the service returns/throws.
- Stale ownership/enrollment/session access: use real persisted authorization and Redis rather than constructing an always-authorized mock.
- Queue/event duplication or publication before commit: inspect the actual disposable broker and preserve owner idempotency checks.
- Stale/skip/failure evidence presented as success: require actual JUnit testcase results and ensure statistics reconcile to unique report IDs.

## Task1: Baseline and mapping

- [x] Read persisted design approval A and confirm branch/user changes.
- [x] Start fresh full baseline `mvn -B -f backend/pom.xml verify`, log `.tmp/integration-test/baseline.log`.
- [x] Inspect baseline result: invalidated by overlapping compilation into the same target; original log retained, no acceptance claim. Final complete run is sequential with a cleared report directory.
- [x] Read applicable requirement clauses and map49 selected scenarios to actual suites/methods with explicit boundaries.

## Task2: Foundation U01–U05

Create `backend/src/test/java/edu/aiplatform/requirements/PlatformWorkflowIntegrationTest.java` with Spring Boot test context, no worker profile, disposable PostgreSQL/Redis/Rabbit, generated secrets, isolated local files, production beans and per-test fixture reset. No mocked owner repository or authorization provider.

- [x] Compose real account/session/JWT/security behavior and verify persisted account disablement revokes access.
- [x] Add audit SUCCESS commit/rollback, independent DENIED persistence and duplicate/sensitive-details assertions using actual PostgreSQL.
- [x] Add settings update/read/version/audit behavior, real after-commit Rabbit publication/rollback and real listener/dispatcher acknowledgement of missing terminal work.
- [x] Add real academics create/remove/reactivate/stale-version paths. Reuse existing subject cascade/CAS/rollback suites for U04; broad class-open permutations remain outside selected report coverage.
- [x] Run added foundation cases: all passed in the16-case composed run; initial failures were test context/fixture issues, not product changes.

## Task3: Individual learning and AI U06–U09

Extend the new composed suite; use existing service command types, not test copies of business logic.

- [x] Add encrypted personal connection persistence/ownership and disabled-state tests without external provider calls; map existing quota/MCP persistence/idempotency cases.
- [x] Add valid assignment publication and incomplete-draft rejection; assert real rubric/assignment storage and audit.
- [x] Add student start/save/submit via actual U07/U05/U08 providers and assert the U09 evaluation handoff; verify invalid access and stale saves preserve committed state.
- [x] Map existing U09 real CAS/rollback/interoperability and visibility cases; further grading lifecycle/AI provider boundaries remain explicit gaps.
- [x] Final complete verification ran affected added/existing suites successfully; actual JUnit evidence retained.

## Task4: Quiz and group U10–U12

- [x] Add a real quiz authoring/publication→attempt→scoring scenario with answer masking, no gradebook entry and separate invalid-publication path.
- [x] Add real group-set save/membership/version behavior using enrolled accounts; map existing group repository/Rabbit cases for locks, submission persistence and fanout.
- [x] Final complete verification ran affected added/existing suites successfully; all results inspected.

## Task5: Materials, notifications and dashboard U13–U15

- [x] Map actual U13/U14 owner composition, delivery, retention and Rabbit/Redis cases with precise actual/mock limits. Parser/provider and deployed browser transport acceptance remain outside the selected report.
- [x] Add actual dashboard→U01/U04/U05 counts and non-admin denial; map existing U03 backup status/consistency evidence. Missing-source fault injection remains focused existing evidence, not a new full owner-outage claim.
- [x] Final complete verification ran affected suites successfully; exact method/requirement/boundary mappings recorded.

## Task6: Required checks, final run and review

Modify `scripts/assert-required-backend-tests.ps1` to retain prior guard checks and require the final new composed suite plus selected real integration suites. Keep `mvn -B verify` and existing CI entry points.

- [x] Guard passes actual evidence and a valid fixture set, and rejects missing suite, skipped case, renamed/missing method and hidden failure child.
- [x] Fresh final backend verify2,020/2,020 passed with zero failures/errors/skips; required guard passed. Earlier invalid/setup attempts retained without acceptance claims.
- [x] Tracked secret-free scenario/evidence manifests and per-unit gap summary record requirement IDs, methods/invocations, boundaries, command, base revision/source hashes and execution time.
- [x] Independent whole-change review found no actionable findings in the composed suite, role fixture, guard or49-case manifest. Final verification/workbook checks remain pending.

## Task7: English Excel report

Source: `E:\FPTUni\sem9_fa26\test_report\Report-5.2_Integration Test_Template.xlsx`. Keep the source untouched. Output a separate workbook under the conversation output directory. Reuse one builder in ignored scratch and the bundled artifact-tool runtime.

- [x] Inspected template cells/styles/merges/native drawings/status validations and rendered its original sample case structure.
- [x] Selected49 evidenced cases across15 units, with unique reporting ownership and no pure mock/unit or browser E2E cases.
- [x] Authored English descriptions/procedures/outcomes/preconditions and requirement/test references; Round1Passed49, Rounds2/3Pending49 each.
- [x] Original style definitions, native drawings/media/theme, widths/hidden groups and status validation conventions preserved. Sample references/statistics repaired. Compact sheet labels and summary wording prevent clipping without changing widths/fonts.
- [x] Recalculated/exported/reopened; verified49 unique cases/54 actual executions, all summary totals, original template SHA, original style definitions, native parts and49 internal navigation links. Visually reviewed all19 sheets plus metadata/trace columns. In a disposable reopened model, changed a Round1 result and Round2 status, verified dependent totals recalculate and restored both without changing the output file.
- [x] Plan/state/audit/results updated; workbook ready for delivery, branch retained uncommitted/unpushed.

## Execution ledger

Baseline session started with Docker29.7.2 after sandbox named-pipe access required escalation; initial result was invalidated as explained below. The user-approved branch is isolated from staging by its explicit requested branch; no extra worktree was created.

Ruling: never run Maven compilation concurrently into the same target. Early overlap caused missing/replaced anonymous test classes and invalidated the initial baseline; preserve that log without classifying those errors as product defects. Final verification uses one Maven process and fresh reports.

Composed suite progression: initial missing JPA scan, accidental inclusion of other test configuration, and an overbroad exclusion of production ConnectionTestService were fixed only in the new test context. Runtime fixtures were aligned with the actual heading schema, nullable/default setting value, score field and mandatory rubric for both grading modes. The sixth focused run passed16/16, no skips. Generated test secrets and context shutdown refinements are included in the ongoing final full verification. No production code changed.

Ruling: artifact-tool copyFrom rejects imported summary hyperlinks carrying colorSource metadata. Author content/formulas with the required bundled tool, then preserve original OOXML style IDs/columns/native parts when inserting that authored content. No dependency internals are modified. Exactly one artifact-operation marker succeeded before the first edit; do not repeat it.

Final workbook: `C:\Users\ADMIN\.codex\visualizations\2026\11\07\01a1b635-2514-78e0-acde-e96ed7b0a899\outputs\integration-test-report-20261108\Report-5.2_Integration Test.xlsx`.19 sheets;49 representative cases across U01–U15. Source template SHA256 remains `04bbda174116da1fd0894f58314d530cd936348332d18e9fc3a53edd89290008`. No product fix was required. Remaining per-unit integration/provider/deployment boundaries are explicit in the results document; no exhaustive requirements or original TDD claim.

## Task8: Complete the prioritized integration boundaries

Authorized by the user's follow-up "giúp tôi hoàn thiện đi" after reviewing the specific remaining scenarios. Continue the approved real-component approach on the existing integration-test branch; do not restart scope/design approvals or populate reports5.3–5.5. Preserve the existing49 report cases and add eight focused scenarios. No production feature, provider acceptance, commit or push is requested.

Requirements: BR-U09-10/20/22/40/41/42; BR-U03-64; BR-U12-40/41/42; BR-U03-50/51/53/56/59; BR-U14-23; BR-U06-13/14. Existing approved design covers these partial boundaries; this addendum specifies the completion work.

Files: extend `backend/src/test/java/edu/aiplatform/requirements/PlatformWorkflowIntegrationTest.java`; raise its minimum required count in `scripts/assert-required-backend-tests.ps1`; extend the scenario/evidence/results documents and the existing ignored spreadsheet builder/catalog. Keep all production code, migrations and dependencies unchanged unless a demonstrated regression requires separately justified action.

- [x] Verify the previous2,020-test source snapshot and preserve historical evidence before adding cases. Run Maven sequentially only.
- [x] Add `individualGradingPublicationAndResubmissionInvalidatePriorStudentScore`: submit through U08, grade/finalize/publish through actual U09, verify Student grade visibility, resubmit and require old SUPERSEDED/new PENDING with old score hidden.
- [x] Add `individualEvaluationFailureRollsBackSubmissionAndRetriesOnce`: inject an isolated database evaluation-insert failure; require attempt/receipt/evaluation/audit atomic rollback, then remove fault and retry successfully.
- [x] Add `groupSubmissionCreatesMemberGradesAndResubmissionResetsThem`: actual group membership/section/workspace/submission→U09 root/member evaluations; grade/finalize/publish, change group content and resubmit; require retained root/member identities reset PENDING and stale score hidden.
- [x] Add `groupEvaluationFailureRollsBackSnapshotAndRetriesOnce`: fail a member evaluation insert after the root write; require no partial root/member evaluations or submitted snapshot/audit, then verify clean recovery.
- [x] Add `queuedNotificationSendsOnceAndDuplicateDeliveryIsAcknowledged`: actual U14 event/recipient/notice producer→Rabbit→automatic test consumer running production listener/dispatcher/handler→actual database; only external IMail is a controlled primary test boundary. Require SENT and one accepted mail despite duplicate jobs.
- [x] Add `queuedNotificationRetriesThroughRealRabbitDelayThenSends`: reject the first controlled mail attempt, inspect actual retry queue/PENDING state, then let the configured30-second Rabbit TTL redeliver; require one successful acceptance and terminal SENT without weakening production retry timing.
- [x] Add `sameAccountQuotaRaceAllowsOneReservationThenSettles`: real quota policy/service plus PostgreSQL transactions, synchronized requests against one account cap; one winner, no overspend, correct actual-token settlement and subsequent reservation/release.
- [x] Add `globalQuotaRaceAcrossAccountsHonorsCapAndRollbackReleases`: different eligible accounts contend for the real global cap; one winner, rejected caller changes neither counter; explicit rollback/release restores capacity.
- [x] Run focused suite, diagnose failures against requirements and preserve attempts; update required-suite minimum and traceability.
- [x] Run fresh complete backend verify with clean JUnit report directory, required-case guard and negative guard checks. Independent whole-change review; fix actionable findings and repeat affected checks only.
- [x] Append evidenced report cases in English, preserve prior case IDs/content and template styling, recalculate/reopen/visually verify affected sheets and statistics. Update evidence/source hashes and explicit remaining boundaries.

Ruling: the mail provider is the only substituted production boundary for the new queue scenarios; it is clearly labeled in report evidence. A SimpleMessageListenerContainer consumes the actual disposable Rabbit queue and calls the unmodified production JobListener. This verifies automatic queue consumption without enabling unrelated scheduled worker jobs or sending to real recipients. Tests characterize current behavior; any product fix requires a failing regression first.

Task8 progress: preserved the previous source/evidence/workbook snapshot and verified all1,031 baseline source hashes before edits. Added eight methods. Focused run1 passed22/24; both errors came from the new group fixture using email instead of the actual school_email column. Corrected the fixture; focused run2 passed24/24, zero failures/errors/skips, BUILD SUCCESS2026-11-08T23:28:18+07. The group resubmission changes actual paragraph text, and duplicate email assertions wait for two completed listener invocations.

Task8 independent review: two P2 test findings, no declined judgments or deferred minors. Fixed the quota midnight risk by freezing the Spring business Clock at the suite's start instant; actual PostgreSQL transactions/locks and Rabbit's real30-second TTL remain unchanged. Added real save attempts after both FINALIZED and PUBLISHED with409/GRADE_LOCKED and unchanged version/status/score/feedback assertions, making BR-U09-42 traceability supported. Added the direct U08-21 and U09-50/51 anchors. These are fixture/assertion refinements to characterization tests, not production fixes or a claim of original TDD. A fresh full Maven verify is running sequentially with both refinements; no completion claim yet.

Task8 continuation2026-11-09: the first extended full run finished2026-11-08T23:42:45+07 with2,028 executions, one failure and eight errors. The blanket fixed clock preceded live fixture timestamps and could compare before PostgreSQL-rounded opensAt; this is a test-fixture defect, not an application deadline defect. The pre-existing local U04_SCOPE p95 test measured25.1172ms against an unchanged20ms limit. Saved the complete failed report snapshot/log separately; do not accept it as green evidence.

Ruling: freeze business time only while exercising the quota service scenarios, resetting to live time before every fixture. This prevents a race spanning two quota days without imposing frozen time on existing live-timestamp fixtures. Cost if wrong: quota-day instability would reappear; the account/global race assertions and full suite verify the final fixture. The mail boundary and real Rabbit retry timing are unchanged. Preserve all49 original report case cells/styles/execution dates; add eight cases with their newly verified date and update summary totals only as required. No thresholds, production code or requirements are weakened.

Task8 complete: final sequential full verify finished2026-11-09T11:18:00+07,2,028/2,028 passed with zero failures/errors/skips; executable JAR packaged. All seven unchanged local latency checks passed. Current required guard passed57 mapped cases/62 executions; valid/negative validator QA used the actual complete current report snapshot. Source hashing verified1,031 backend files, with only the composed test source changed from the historical baseline. Final workbook has57 Passed cases, preserves49 original case cells/styles/dates and the Cover revision, and retains template/native styles, widths/hidden groups and drawings. Five affected unit tabs and summary/trace/header views visually reviewed; eleven unchanged unit/security tabs compared. Reopened Round1/2 formula changes recalculated and were restored without saving. No deferred review findings, production changes, commits or pushes; provider/deployment boundaries remain explicit.

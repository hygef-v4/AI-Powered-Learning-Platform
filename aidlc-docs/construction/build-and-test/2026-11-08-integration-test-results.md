# U01–U15 Automated Integration Test Results

Branch: `integration-test`, based on staging `321f88929360a4bf3c733a26a4a0f06a15ecb323`. Changes are uncommitted. No production source, schema, migration, dependency version or frontend behavior was changed for this task.

## Verified result

Fresh sequential `mvn -B -f backend/pom.xml verify` completed successfully at **2026-11-09T11:18:00+07:00**: **2,028 tests, zero failures, errors or skips**. The24-case composed suite passed, and the executable JAR was packaged successfully.

The [scenario manifest](2026-11-08-integration-test-cases.json) maps57 representative report cases to requirements and production test methods across all15 units. Those57 cases resolve to62 passing JUnit executions, because some selected methods are parameterized. This is not57 new test methods:24 composed methods were added and appropriate existing methods were reused.

The [execution evidence](2026-11-08-integration-test-evidence.json) contains source hashes, environment, actual suite totals, per-case results and boundaries. Local runtime used Java21.0.11, release target17, Maven3.9.9, Spring Boot3.5.6 and Docker29.7.2 with disposable PostgreSQL16, Redis7.4 and RabbitMQ4.1.

`scripts/assert-required-backend-tests.ps1` retains the previous required-suite checks and additionally requires the24-case production composition and every mapped scenario. The current actual guard result is recorded separately in the execution evidence: Passed. Negative checks verified rejection of a missing suite, skipped case, renamed/missing mapped method and failure child hidden behind zero result attributes. A valid report set was accepted. The existing CI `mvn -B verify` entry point remains unchanged.

Independent read-only review identified two test issues: a quota-day race across midnight and a grading-lock mapping without a rejection assertion. Both were addressed with a quota-only frozen business Clock and409/GRADE_LOCKED/unchanged-state assertions. The initial blanket-clock refinement was corrected after the saved failed full run. No deferred review findings remain; the separate local latency result is explicitly retained.

## What the new suite adds

- Actual production component composition with isolated PostgreSQL, Redis, Rabbit and current Flyway migrations. Other suites' manual test configurations are excluded without excluding production `ConnectionTestService`.
- Real session/JWT/current-account security, audit commit/rollback/idempotency and independent denial records.
- Persisted settings/cache invalidation, after-commit queue publication and a real Rabbit channel→listener→dispatcher→email-handler acknowledgement for work whose delivery row no longer exists. Additional cases run an automatic Rabbit consumer with the unmodified listener/dispatcher/handler, persisted U14 deliveries, duplicate acknowledgement and the actual 30-second retry TTL. Only IMail is controlled; scheduled worker startup and real provider acceptance remain excluded.
- Class/enrollment owner operations, stale updates, enrollment reactivation and immediate access removal.
- Personal AI-key storage/ownership/disabled-state checks with no provider call.
- Actual assignment publication/rubric lock→student work/submission→grading evaluation handoff, plus invalid publication and stale/removed-enrollment rejection.
- Actual quiz publication→student attempt→automatic scoring and answer masking, without a gradebook entry; invalid quiz publication is rejected.
- Actual group-set membership/version protection, two-member submission→grading publication/resubmission, member-write failure rollback and dashboard composition with real count providers.
- Individual grade draft/finalize/publish visibility, supersession after resubmission and failed evaluation-write atomicity.
- Same-account and cross-account quota races through the actual services and separate PostgreSQL transactions, settlement, release and transaction rollback.

## Coverage and remaining boundaries

This is representative integration coverage, not proof that every clause in `aidlc-docs` is fully covered. The table distinguishes selected evidence from work that still requires additional scenarios or a different test level. A gap below means the selected report does not establish that behavior; it does not assert that the entire repository lacks any relevant test.

| Unit | Selected report IDs | Established boundary | Partial or excluded requirement areas |
|---|---|---|---|
| U01 | IT-U01-001–003 | JWT/current status, Redis refresh race, account lock persistence | Complete activation/reset/OTP-to-mail path, all role-change/session-revocation permutations and live transport acceptance |
| U02 | IT-U02-001–003 | Commit/rollback/duplicate audit, independent DENIED, sensitive-detail rejection and audit-failure atomicity | Full query/filter/actor-resolution matrix; every prohibited detail key and FAILURE-result variation |
| U03 | IT-U03-001–005 | Settings cache/version/audit, after-commit Rabbit publication, real listener acknowledgement, Redis grant expiry and backup duplicate control | All other retry TTL/terminal/error branches through real broker; the first real delay is established by cross-unit IT-U14-006, Drive upload/cleanup, paid/live providers and production restore |
| U04 | IT-U04-001–003 | Actual lifecycle cascade/retention, immediate manager scope and unique-code race | Every endpoint/role/search/timeout permutation and deployed concurrent-load acceptance |
| U05 | IT-U05-001–003 | Class creation, enrollment removal/reactivation, stale/unauthorized writes and semester-removal migration | Full import/bulk/partial-result matrix, all class-open/reopen conflicts and teacher/account races |
| U06 | IT-U06-001–006 | Key ownership/encryption/disablement, MCP transactional retries, quota/task/usage persistence and service-level account/global quota races with settlement/release/rollback | Actual provider responses/fallback, every quota outcome/window variation and server/agent callbacks |
| U07 | IT-U07-001–003 | Publication/rubric lock, incomplete-draft rejection, rubric round trip and handoff to U08/U09 | All copy/version/schedule/retire/AI/DOCX/diagram branches |
| U08 | IT-U08-001–004; cross-unit IT-U07-001 | Real stale save and enrollment revocation; shared API/MCP reservation concurrency; submission handoff and evaluation-write failure rollback/recovery | Every deadline/auto-submit/snapshot permutation, real AI transport and browser activity capture |
| U09 | IT-U09-001–004; cross-unit IT-U07-001 | Evaluation creation, concurrent CAS, application transaction interoperability, actual finalize/publish visibility and individual resubmission supersession | Every grading change/AI/export flow and gradebook-filter variation and every distribution privacy threshold |
| U10 | IT-U10-001–003; cross-unit IT-U11-001 | Question lineage, publication history, invalid publication and actual published-quiz handoff | Full generated-pool/matching/version-replacement/import/AI matrix |
| U11 | IT-U11-001–003 | Actual scoring/masking/no-gradebook, single active attempt constraint and penalty-decision persistence | Complete auto-submit/time-window/penalty authorization/activity signal and result-mask combinations |
| U12 | IT-U12-001–005 | Group-set ownership/version, workspace submission persistence and real two-hub Rabbit fanout, group→member grade publication/reset and member-insert failure rollback | All section conflict/leader-change permutations and browser/proxy SSE transport |
| U13 | IT-U13-001–003 | Real scope filtering, source version history/CAS and failed module-delete atomicity | Actual file/Drive/extraction-worker delivery composition, all parser/resource/adversarial inputs and browser preview |
| U14 | IT-U14-001–006 | Real announcement HTTP/security/version/delete; saved preferences, delivery concurrency/retention and producer→automatic Rabbit consumer→delivery processing with duplicate acknowledgement and a real retry delay | Real email provider acceptance, all terminal retries/caps/sweeper combinations, scheduled worker-profile startup and browser/proxy behavior |
| U15 | IT-U15-001–003 | Actual owner counts/admin-only access plus real U03 backup availability/snapshot consistency | Complete source-outage/timeout combinations in the fully composed dashboard and browser presentation |

Cross-unit scenarios have one reporting owner and are counted once. `SecurityTest` is a reference index to already counted cases. Spreadsheet execution coverage is the executed fraction of included report cases; it is not a requirements-coverage percentage.

## Evidence integrity and earlier attempts

The initial baseline was invalidated by an accidental overlapping Maven compile into the same target, which replaced test bytecode during execution. Its failure log is retained under ignored scratch and is not accepted as a product-defect baseline. Subsequent Maven runs were sequential. The final run started with a cleared report directory and preserved a separate complete report snapshot.

New-suite setup failures were corrected in test code only: production scanning, test-configuration exclusion, heading schema, nullable setting defaults, score-column selection and mandatory publication rubric. No requirement, production guard or threshold was weakened. The historical focused suite passed16/16 and full run passed2,020/2,020. For the eight-case extension, the first focused run passed22/24; two group fixtures used email instead of the schema column school_email. This was corrected in test code only. The final extended suite and fresh complete run establish the current evidence. All earlier logs and the49-case workbook/source snapshot are preserved under ignored scratch.

These tests characterize and protect existing behavior. They do not establish that the original project was developed with TDD.

## Report

The English report is based on `Report-5.2_Integration Test_Template.xlsx`. Its source file remains unchanged. Round1 uses the verified execution above; Rounds2/3 remain Pending. The report uses the template's layout, styling, column widths/hidden groups and native drawings, with copied unit tabs and corrected sample references/statistics. The original49-case workbook had all19 sheets visually reviewed. For this update, the five changed unit tabs, Cover, Test Cases, Test Statistics and changed trace/header ranges were reopened/rendered and visually reviewed. All49 original case cells/styles/dates and the original Cover revision were preserved; eleven unchanged unit/security tabs were compared with the baseline. Source/template/native style preservation,49 navigation links and57-case totals were verified. Temporary Round1/2 changes in a disposable reopened model correctly updated dependent totals and were restored without changing the output file. The SecurityTest index keeps its own template vocabulary (`Pass`), while the unit sheets use `Passed`; no case is counted twice.

Output: `C:\Users\ADMIN\.codex\visualizations\2026\11\07\01a1b635-2514-78e0-acde-e96ed7b0a899\outputs\integration-test-report-20261108\Report-5.2_Integration Test.xlsx`.

Reproduce the test evidence with Docker available:

```powershell
mvn -B -f backend/pom.xml verify
./scripts/assert-required-backend-tests.ps1
```

No live data, remote recipient, paid AI provider, production deployment, commit, push, PR or merge was performed.

## Extension execution and preservation

Eight cases were appended to the existing49, preserving all original case cells, styles and execution dates (2026-11-08). New case dates and report metadata use2026-11-09. The report counts included integration scenarios only; its pass/execution percentages are neither exhaustive requirements coverage nor the overall backend build result. Reports5.3–5.5 and live provider acceptance are outside this update.

Focused run3 verified all24 composed methods with the final clock and lock assertions, while the unchanged local p95 case failed24.4466ms/20ms. The first full extension attempt (2,028 executions; one failure/eight fixture errors) and every focused log are preserved separately. The final full-run result above is the acceptance record; earlier or QA-only reports are not substituted for it. No performance threshold, requirement or production behavior was weakened.

The final full run passed all seven unchanged local latency criteria, including U04_SCOPE8.9966ms/20ms. The earlier25.1172ms and24.4466ms failures remain historical evidence; no threshold or production behavior was changed. Workbook SHA256: `40f035fa2f63e57abd718b64870ba87bb94a2e969f53c43539eab02e9c39e125`.

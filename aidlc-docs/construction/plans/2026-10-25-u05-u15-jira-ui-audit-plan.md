# U05 / U15 Jira-based functional and UI audit plan

**Authorization:** "check u05 với u15 xem có lỗi j ko, ui ổn chưa, lập plan check theo jira và đưa cho gemini check"

**Owner:** Existing Gemini 4 Argon in Orca, terminal `term_95be006a-9105-415a-a82c-f9ff67f7d188`. Sequential audit on current checkout; no additional agents. This task is inspection/testing and reporting. Propose fixes with priorities; do not implement broad redesigns or bug fixes without a follow-up instruction.

**Goal:** Determine actual defects and UI usability for U05 Class & Enrollment (UC20–30) and U15 Admin Dashboard (UC66), with Jira-to-code-to-test traceability and real browser evidence.

**Baseline:** Planning inspection found HEAD `41d6cd19`, branch `mcp-test`, clean working tree. Gemini's prior U09 implementation is locally committed in 522dee10, 299558d3, dc5494bf, 41d6cd19. Recheck actual status before work; never reset, stash, switch branch, commit, push, merge or deploy for this audit. Do not revert these changes.

## Sources and priority

Read [live Jira snapshot](2026-10-25-u05-u15-jira-snapshot.md). U05 epic KAN-21 is Done, all eleven child UCs Done. U15 epic KAN-32 is To Do; child KAN-80 is In Progress. This administrative status mismatch is a tracking observation, not a runtime defect. Do not change Jira statuses, comments or ownership.

Current mapping: U05 = academics/class enrollment, U15 = admindashboard. Older application-design files incorrectly call grading U15 or materials U05. Use current unit-of-work/usecase catalog and Jira; record conflicting specifications rather than silently guessing.

Read AGENTS.md, CLAUDE.md, scoped frontend guidance, current aidlc-state extension settings, U05/U15 functional designs, stories US-CAT-001/002/003, US-LRN-001, US-RPT-002, US-OPS-001, current OpenAPI. Read U15 backup-completion-verification-2026-10-22.md: provider C01/C02 were implemented; older pending notes are historical. Existing test counts are historical, rerun relevant checks.

Primary code: `frontend/src/features/academics/`, `frontend/src/features/admindashboard/`, `backend/src/main/java/edu/aiplatform/academics/`, `backend/src/main/java/edu/aiplatform/admindashboard/`; cross-unit sources only through public contracts. Inspect `frontend/src/features/grading/ClassProgressSection.tsx`, ClassGradebookTab and groups slot registration for recent teacher overview regressions. U04 subject editing is outside U05, but its managed-subject entry integration is in scope.

## 1. Startup and safe test data

- [x] Read current git status, staged/unstaged diff, HEAD and previous U09 implementation report. Stop conflicting source mutations by choosing read-only audit; never discard other work.
- [x] Identify listeners 3000/8080 and matching repo processes. Check PostgreSQL/Redis/RabbitMQ and current worker ownership. Reuse healthy host terminals; restart only verified repo processes when needed. Backend `.\scripts\run-backend.bat`; frontend `npm --prefix frontend run dev`. Never start a second worker alongside an old worker.
- [x] Load Orca CLI/browser documentation before browser use. Preserve teacher session in its existing tab/profile. Discover existing manager/student/admin authenticated sessions in separate profiles if available. No password guessing, credential dump, new AI connection or key/model change. If required login unavailable, record exact role/URL and ask user only for login assistance.
- [x] Record environment, HEAD, role, viewport, data IDs and timestamp in report. Use dedicated disposable QA subject/classes/accounts for mutations; confirm they are test-only before changing enrollment/teacher/status. Do not archive/change live classes, remove real students, change real teacher assignments, publish grades or run backup/restore/upload.
- [x] Preserve existing class `4d18e255-1789-45d6-8fbb-351e2188f672` as read-only audit reference; teacher `u05.teacher@school.test`, student `u05.student@school.test`. Protected evaluation `7ca08c41-e9d4-4738-946d-24c049e3d1e5` untouched; QA evaluation `826f5c29-b5d1-4a32-9999-9313c242d8d3` already PUBLISHED 10/version7 at earlier observation, do not reset. Missing safe test data means Blocked, not Pass.
- [x] Baseline focused frontend academics + admindashboard tests and backend owned-package tests. Capture exact commands/counts/skips and duration; do not run repetitive full suites before findings justify them.

## 2. U05 traceable checks

Each row requires a case ID, role, setup, steps, expected/actual, API observation and evidence. Every row starts Not run. Validate UI and backend authorization separately; hidden buttons alone do not prove authorization.

| Case | Jira / UC | Required checks |
|---|---|---|
| U05-01 | KAN-75 / UC20 | Student lists own ACTIVE enrollments: OPEN under current, ARCHIVED information-only, DRAFT absent. Search code/name, empty query/results, pagination, refresh/back navigation. |
| U05-02 | KAN-126 / UC21 | OPEN enrolled class opens correct teacher/info/tabs. Non-enrolled/REMOVED/non-OPEN detail returns not-found without class data. Materials/evals/groups/announcements integrate without broken links or duplicate sections. |
| U05-03 | KAN-51 / UC22 | Teacher sees only current assigned classes in correct DRAFT/OPEN/ARCHIVED groups. Manager who teaches can access teacher entry; other teachers' classes absent. |
| U05-04 | KAN-135 / UC23 | Assigned teacher detail and ACTIVE student list; student popup read-only includes only permitted fields and group. Teacher cannot edit class/enrollment through UI or direct API. Recent overview metrics match owned sources, zero differs from unavailable, no duplicate provider requests. |
| U05-05 | KAN-137 / UC24 | Manager class list is scoped to managed subjects; search/status/page filters stable. Forged other-subject URL/request does not reveal data. Admin does not gain U05 class API access. |
| U05-06 | KAN-138 / UC25 | Manager class detail shows correct teacher/roster/status and permitted actions; teacher/student roles cannot enter manager flow. Stale/deleted ID and missing resource show useful recovery. |
| U05-07 | KAN-50 / UC26 | QA create in ACTIVE managed subject begins DRAFT; required/blank/trim/length/duplicate-code validation. Code <=30, immutable code/subject, duplicate submit guard and reload persistence. Archived/unmanaged subject rejected. |
| U05-08 | KAN-139 / UC27 | QA assign/change ACTIVE TEACHER or SUBJECT_MANAGER, invalid role/disabled rejected. OPEN cannot lose teacher. Old teacher access revoked at next request, new teacher gains scoped access; stale version conflict preserves form with useful message. |
| U05-09 | KAN-52 / UC28 | QA add/remove/restore, candidate search <=20; STUDENT ACTIVE/PENDING only. CSV/email list <=200 lines/100KB, duplicate/invalid/missing/disabled/other-class rows and partial success mapped correctly. Removal confirmation/cancel, history retained, next-request student access revoked. Never mutate reference class. |
| U05-10 | KAN-140 / UC29 | QA name/description/term/showGradeDistribution edits persist. DRAFT->OPEN/ARCHIVED, OPEN->ARCHIVED, ARCHIVED->OPEN with active-subject/teacher and enrollment-conflict gates. ARCHIVED read-only. Default distribution off, only manager changes flag; student privacy remains U09 policy. |
| U05-11 | KAN-141 / UC30 | Manager sees owned subjects and real status counts; correct subject detail/context and scoped downstream links; teacher dashboard switch only when applicable. Missing provider fails clearly, never fabricated totals. |
| U05-12 | Cross-cutting | Existing audit evidence for create/update/status/teacher/enroll/remove/denied; after-commit enrollment event where safe QA mutation exists. Concurrent/version requests cannot overwrite silently; U01/U04 unavailable fails closed. Use isolated tests for unsafe fault cases, label those as automated only. |

## 3. U15 traceable checks

| Case | Jira / UC | Required checks |
|---|---|---|
| U15-01 | KAN-80 / UC66 | ADMIN login lands /admin; refresh fetches current counts/calculatedAt. Accounts by role/status, subjects ACTIVE/ARCHIVED, classes DRAFT/OPEN/ARCHIVED, active enrollment totals reconcile with authorized provider/aggregate evidence, not guessed visible list lengths. |
| U15-02 | KAN-80 / UC66 | Teacher/manager/student/anonymous and revoked/inactive roles rejected by API + route. Invalid paging for unauthorized role still denied before provider access; no stale metrics/dialog after 401/403; persisted deny audit where observable. |
| U15-03 | KAN-80 / UC66 | Partial count source failures affect only that section; unavailable differs from zero. Loading/retry/error UX, rapid refresh/out-of-order replies, leave/re-enter freshness. Fault injection through tests/browser fixtures is not proof of live provider failure. |
| U15-04 | KAN-80 / UC66 | Backup disabled/no runs/CREATED/RUNNING/FAILED/SUCCESS/purged render correctly using actual returned state. Latest run differs from latest successful. Start/end/duration/size/null fields, sanitized errors, last7days failures and configured schedule/next run Vietnam timezone. Do not trigger job or change schedule/retention for audit. |
| U15-05 | KAN-80 / UC66 | History open/close/reopen, page boundaries/size validation/error/retry, correct rows/totals, no stale page mixing, keyboard focus restore. Existing no-data case is valid; missing state fixtures remain Not tested live. |
| U15-06 | KAN-80 / UC66 | API/UI expose counts and backup metadata only: no PII/secret/connection credentials, backup archive download or restore controls. Navigation to accounts/subjects/settings/audit only as permitted, no academic content bypass. |
| U15-07 | KAN-80 / UC66 | Review existing bounded timeout/pool/queue tests and authorization guard ordering. Record local request timings as observations only; historical p95<=1s/deadline2s NFR requires representative load evidence, otherwise Not verified. Do not stress shared runtime or interrupt DB/network. |

## 4. UI audit with screenshots

- [x] Capture teacher assigned class list (Open, Draft, Archived) and detail tabs (overview, distribution, students, dialog, materials, evals, gradebook). Inventory inaccessible roles (Admin, Manager, Student) explicitly as Blocked due to credential protection.
- [ ] Dedicated physical 390 CSS px device viewport and browser zoom 200% testing (observed responsive container behavior in desktop viewport; physical 390px/zoom200 not independently executed).
- [x] Check hierarchy, spacing, title wrapping, long names/emails/class codes, card alignment, readable labels/statuses, contrast, overflow, meaningful empty/error/loading states and pagination visibility on teacher views.
- [x] Keyboard Tab/ShiftTab/Enter/Escape, visible focus, dialog focus trapping/return, accessible names on StudentDetailDialog.
- [x] New teacher overview: student/group/open/pending counts correct, metadata and progress order coherent, show-more works, lazy distributions don't fetch collapsed data; gradebook modes verified.
- [x] For each concern classify confirmed functional defect, usability/accessibility finding, visual suggestion, or requirements ambiguity. Assign P0/P1/P2/P3 with actual user impact and screenshot.

## 5. Verification and deliverables

- [x] Run `npm --prefix frontend test -- src/features/academics src/features/admindashboard --maxWorkers=1`; add grading/groups suites for teacher overview integration (15 files / 82 tests pass). Backend unit suite with `-Dtest=edu.aiplatform.academics.**,edu.aiplatform.admindashboard.**` (18 classes / 249 tests pass). Backup integration suite with Testcontainers PostgreSQL 16 (3 classes / 12 tests pass).
- [x] Inspect relevant tests/contracts for denied access, version/state, partial failures and stale responses. Corroborated Findings 1–4 with reproduction steps.
- [ ] Authorized minimal fixes implementation (addressed in follow-up plan).
- [x] Write `aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-audit-report.md`: scope/HEAD/env, requirement matrix for all 12 UCs, cases Pass/Fail/Blocked/Not run, reproducible defects, UI verdict per role/screen, exact checks/counts/skips, known limitations and next actions.
- [x] Write `aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md` in Vietnamese with complete setup/roles/URLs/data/steps/expected/cleanup. User-result column initialized to Chưa test.
- [x] Update plan checkboxes, append audit/state without overwriting concurrent entries.
- [x] Report concise findings to user, distinguishing executed vs blocked.

## Planning progress

- [x] Read workflow, inspect actual clean Git baseline and existing Gemini terminal.
- [x] Read live Jira scope/status and current U05/U15 rules; save source snapshot.
- [x] Create sequential functional/UI audit and handoff requirements.
- [x] Gemini executes audit and produces results; audit baseline corrected. Follow-up fixes authorized.

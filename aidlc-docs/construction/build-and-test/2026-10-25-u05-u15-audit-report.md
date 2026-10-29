# U05 / U15 Jira-Based Functional and UI Audit Report (Corrected Baseline)

> Historical baseline report: the findings below were identified before the follow-up fixes. See [U05/U15 follow-up report](2026-10-26-u05-u15-followup-report.md) for the correction status, fresh verification, and remaining live UI limits.

**Audit Date:** 2026-10-25\
**Auditor / Agent Attribution:** Initial audit turn executed by Argon 4 (Gemini 4 Argon, quota exhausted); evidence verification, report correction, and follow-up implementation by Gemini 3.8 Flash (`Antigravity`).\
**Branch:** `mcp-test` | **HEAD:** `41d6cd19`\
**Execution Environment:** Windows 11, JDK 17 (`temurin-17.0.12`), Node.js v20.18.0, Next.js **16.3.8** (verified via `frontend/package.json`), PostgreSQL 16.3 (`platform` DB on port 5432).\
**Listeners Verified:** Frontend on `:3000` (PID 47876), Backend + Host Worker on `:8080` (PID 12432).\
**Active Live Browser Session:** Orca Chromium profile `default` authenticated as `u05.teacher@school.test` (`U05 Mock Teacher`, TEACHER role, ID `86a4ca66-80ad-46f7-88a2-dc95594dc07e`).\
**Session Availability Note:** Browser sessions for `admin@school.test` (ADMIN), `u05.manager@school.test` (SUBJECT_MANAGER), and `u05.student@school.test` (STUDENT) are not active in the browser profile. Passwords are salted/hashed in the database; per security instructions, no password cracking or credential guessing was attempted. Their respective live browser UI workflows are marked **Blocked (Live Session Unavailable)**.

---

## 1. Executive Summary

This report provides a verified audit evaluating the functional completeness, role-based access control (RBAC), and UI/UX quality of:
1. **U05 Class & Enrollment Management** (`UC20`–`UC30`, Jira Epic `KAN-21`, 11 child stories marked `Done` in Jira).
2. **U15 Admin Dashboard** (`UC66`, Jira Epic `KAN-32` `To Do` / child story `KAN-80` `In Progress`). Note: Current U15 is the administrative operations dashboard (accounts, subjects, classes, enrollments, and backup status), NOT the legacy grading unit.

### High-Level Verdict: PASS WITH FINDINGS (Automated: 100% PASS; Live UI: Teacher View Executed & Verified with Usability Findings; Admin / Manager / Student Live UI Blocked; 2 Backend Security/Guard-Ordering Defects Identified)

- **Automated Verification Counts (Verified from Test Execution & Surefire XML Reports):**
  - **Frontend Focused Suite:** **15 test files passed (15)**, **82 tests passed (82)**, `0` failed, `0` skipped, duration `18.44s`:
    - `academics` (7 files / 43 tests): `academics.test.tsx` (8), `api.test.ts` (8), `integration.test.tsx` (5), `pagination.characterization.test.tsx` (7), `routes.characterization.test.tsx` (5), `routes.test.tsx` (6), `useCandidates.test.tsx` (4).
    - `admindashboard` (5 files / 27 tests): `AdminDashboardPage.test.tsx` (11), `api.test.ts` (4), `BackupHistoryDialog.test.tsx` (4), `BackupSection.test.tsx` (5), `navigation.test.tsx` (3).
    - Cross-unit suites (3 files / 12 tests): `class-progress-layout.test.tsx` (5), `gradebook-layout.test.tsx` (3), `TeacherGroupWidgets.test.tsx` (4).
  - **Backend Owned Units:** **18 test classes passed (18)**, **249 tests passed (249)**, `0` failures, `0` errors, `0` skipped, duration `51.95s`:
    - `edu.aiplatform.academics.**` (11 classes / 211 tests): `AcademicsAccessAndReadTest` (99), `AcademicsApiContractTest` (35), `AcademicsServiceTest` (23), `CourseClassTest` (15), `AcademicsContractAdapterTest` (9), `AcademicsBatchTransactionTest` (6), `AcademicsControllerTest` (6), `AcademicsPersistenceTest` (6), `AcademicsReadRepositoryTest` (5), `EnrollmentListParserTest` (4), `EnrollmentTest` (3).
    - `edu.aiplatform.admindashboard.**` (6 classes / 34 tests): `AdminDashboardControllerTest` (8), `AdminDashboardBackupTest` (7), `DashboardSourceReaderTest` (7), `AdminDashboardApiContractTest` (6), `AdminDashboardAccessTest` (3), `AdminDashboardServiceTest` (3).
    - `edu.aiplatform.backup.application.BackupStatusServiceTest` (1 class / 4 tests).
  - **Backup Integration & Contract Tests:** **3 test classes passed (3)**, **12 tests passed (12)**, `0` failures, `0` errors, `0` skipped, duration `14.71s`:
    - `BackupDashboardStatusIntegrationTest`: **8 tests** (live PostgreSQL 16 Testcontainers integration verifying time boundaries, 7-day purging, and real transactional queries).
    - `BackupDashboardContractTest`: **1 test** (contract mock test).
    - `BackupDashboardUnavailableTest`: **3 tests** (stub/availability tests).
- **Security & Authorization Probing:**
  - Standard RBAC is enforced on clean requests: `TEACHER` actor is blocked with `403 ROLE_DENIED` on `/api/v1/me/classes`, `/api/v1/manager/classes`, and `403 DASHBOARD_FORBIDDEN` on `/api/v1/admin/dashboard`.
  - Class isolation is strictly maintained: querying a class belonging to another teacher returns `404 CLASS_OUT_OF_SCOPE`.
  - **Finding 1 (P1):** Guard-ordering defect in `AdminDashboardController`. Query parameter validation (unknown keys, duplicate keys, non-numeric strings, overflow) runs *before* the service admin authorization check. Unauthorized callers sending malformed queries receive `400 Bad Request` instead of `403 Forbidden`, and no `ACCESS_DENIED` audit event is written to `audit_logs`.
  - **Finding 2 (P1):** Guard-ordering defect in `AcademicsService.list()` and `students()`. Paging/query validation runs *before* `requireAudience(actor, audience)`. Unauthorized callers sending `?page=0` receive `400 Bad Request` and `guarded()` logs `AuditResult.FAILURE` instead of `AuditResult.DENIED`.
- **Live Browser UI Inspection:**
  - Verified live in Orca Chromium on teacher profile: class list (`Đang dạy · 4`, `Sắp mở · 1`, `Đã kết thúc · 0`), class overview (4 metrics cards, metadata panel, progress section, lazy distributions), student roster, student detail dialog (`SlotOutlet`), materials, evaluations, and gradebook tabs.
  - **Finding 3 (P2):** Sidebar navigation item `Lớp được phân công` has `href: "/classes"` instead of `"/teaching/classes"`, breaking the active nav highlight on teacher routes and triggering unnecessary `403 ROLE_DENIED` calls on page load before redirecting.
  - **Finding 4 (P2):** CSS variables `--line`, `--muted`, `--primary`, `--ink` in `academics.module.css` are undeclared on modern layout containers, causing 1px borders to compute as `0px` and subtitle text to render as unmuted ink color.
- **Account & Data Safeguards:**
  - Reference class `4d18e255-1789-45d6-8fbb-351e2188f672` (`U05-A`) and all reference evaluations (`7ca08c41-e9d4-4738-946d-24c049e3d1e5`, `826f5c29-b5d1-4a32-9999-9313c242d8d3`) were preserved in read-only mode with zero mutations.
  - No secrets, credentials, or password hashes were dumped or logged.

---

## 2. Requirement Traceability Matrix (12 Use Cases)

| Case ID | Jira Story / UC | Title & Scope | Verified Test Coverage | Live Probing / UI Verdict | Status | Evidence / Notes |
|---|---|---|---|---|---|---|
| **U05-01** | `KAN-75` / `UC20` | Student lists own ACTIVE enrollments (OPEN active, ARCHIVED info, DRAFT hidden, search/pagination) | `AcademicsAccessAndReadTest`, `AcademicsServiceTest`, `AcademicsApiContractTest`, `academics.test.tsx`, `pagination.characterization.test.tsx` | Blocked (Live Student) | **Pass (Automated) / Blocked (Live)** | Student password not in session; verified via unit and slice tests that drafts are excluded and only active enrollments are scoped. |
| **U05-02** | `KAN-126` / `UC21` | Student class detail, tabs integration & non-enrolled 404 | `AcademicsAccessAndReadTest`, `AcademicsApiContractTest`, `routes.test.tsx` | Verified 403 on Teacher; Student UI Blocked | **Pass (Automated & Guard) / Blocked (Live Student UI)** | Teacher navigating to student route `/classes/{id}` renders 403 Denied panel (`u05-02-teacher-on-student-class-denied.png`). Defense-in-depth verified; student acceptance requires student session. |
| **U05-03** | `KAN-51` / `UC22` | Teacher assigned classes list (DRAFT/OPEN/ARCHIVED groups, multi-role) | `AcademicsAccessAndReadTest`, `AcademicsControllerTest`, `routes.test.tsx` | Verified Live UI (Orca) | **Pass** | Verified in Orca: Open (`u05-03-teaching-classes-open.png`), Draft (`u05-03-teaching-classes-draft.png`), Archived (`u05-03-teaching-classes-archived.png`). |
| **U05-04** | `KAN-135` / `UC23` | Teacher class detail, roster, student popup, read-only protection, overview | `AcademicsAccessAndReadTest`, `AcademicsControllerTest`, `class-progress-layout.test.tsx`, `TeacherGroupWidgets.test.tsx` | Verified Live UI (Orca) | **Pass with UI Findings** | Screenshots: Overview (`u05-04-teacher-class-info.png`), Distribution (`u05-04-teacher-class-info-distribution.png`), Roster (`u05-04-teacher-class-students.png`), Dialog (`u05-04-teacher-student-detail-dialog.png`). Findings 3, 4, 5 noted. |
| **U05-05** | `KAN-137` / `UC24` | Manager class list (scoped to managed subjects, search/status/page) | `AcademicsAccessAndReadTest`, `AcademicsApiContractTest`, `routes.test.tsx` | Blocked (Live Manager); Teacher 403 Confirmed | **Pass (Automated) / Blocked (Live)** | Teacher calling `GET /api/v1/manager/classes` receives `403 ROLE_DENIED`. Guard ordering defect logged in Finding 2. |
| **U05-06** | `KAN-138` / `UC25` | Manager class detail (teacher/roster/status, permitted actions) | `AcademicsAccessAndReadTest`, `AcademicsApiContractTest` | Blocked (Live Manager); Teacher 403 Confirmed | **Pass (Automated) / Blocked (Live)** | Teacher calling `GET /api/v1/manager/classes/{id}` receives `403 ROLE_DENIED`. |
| **U05-07** | `KAN-50` / `UC26` | Create class in managed subject (DRAFT initial, code uniqueness, validation) | `AcademicsServiceTest`, `CourseClassTest`, `AcademicsControllerTest` | Blocked (Live Manager Mutation) | **Pass (Automated) / Blocked (Live)** | Validated by unit/contract tests; reference data preserved. |
| **U05-08** | `KAN-139` / `UC27` | Assign/change teacher (active teacher/manager, version optimistic locking) | `AcademicsServiceTest`, `AcademicsPersistenceTest` | Blocked (Live Manager Mutation) | **Pass (Automated) / Blocked (Live)** | Contract verifies stale version conflict aborts with useful message. |
| **U05-09** | `KAN-52` / `UC28` | Add/remove/restore student enrollment, batch CSV/email (<=200 lines/100KB) | `AcademicsBatchTransactionTest`, `EnrollmentListParserTest`, `EnrollmentTest`, `AcademicsServiceTest` | Blocked (Live Manager Mutation) | **Pass (Automated) / Blocked (Live)** | Batch parsing, duplicate student detection, and active-status filtering verified in tests. |
| **U05-10** | `KAN-140` / `UC29` | Lifecycle transitions (DRAFT->OPEN->ARCHIVED) & `showGradeDistribution` flag | `CourseClassTest`, `AcademicsServiceTest` | Blocked (Live Manager Mutation) | **Pass (Automated) / Blocked (Live)** | Reference class `U05-A` has `showGradeDistribution = true`, rendered correctly in teacher view. |
| **U05-11** | `KAN-141` / `UC30` | Manager subject dashboard & status counts | `AcademicsAccessAndReadTest`, `AcademicsReadRepositoryTest` | Blocked (Live Manager); Teacher 403 Confirmed | **Pass (Automated) / Blocked (Live)** | Teacher calling `GET /api/v1/manager/subjects` returns `403 ROLE_DENIED`. |
| **U05-12** | Cross-cutting | Audit logging on mutations/denials, optimistic concurrency, fail-closed RBAC | `AcademicsAccessAndReadTest`, `AcademicsBatchTransactionTest` | Live Probing Defect (Finding 2) | **Partial (Defect P1)** | Proper `ACCESS_DENIED` written on clean forbidden requests; bypassed on malformed query params (Finding 2). |
| **U15-01** | `KAN-80` / `UC66` | Admin Dashboard overview counts (accounts, subjects, classes, enrollments) | `AdminDashboardServiceTest`, `DashboardSourceReaderTest`, `AdminDashboardPage.test.tsx` | Blocked (Live Admin); Teacher 403 Confirmed | **Pass (Automated) / Blocked (Live)** | Teacher calling `GET /api/v1/admin/dashboard` returns `403 DASHBOARD_FORBIDDEN`. Real counts reconcile via aggregation. |
| **U15-02** | `KAN-80` / `UC66` | RBAC protection (non-admin blocked API + route, invalid paging denial) | `AdminDashboardAccessTest`, `AdminDashboardControllerTest` | Live Probing Defect (Finding 1) | **Fail (Defect P1)** | Teacher calling `/api/v1/admin/dashboard/backups?page=abc` returns `400 Bad Request` instead of `403 DASHBOARD_FORBIDDEN`. |
| **U15-03** | `KAN-80` / `UC66` | Partial count source resilience (unavailable vs zero, retry UX) | `AdminDashboardServiceTest`, `BackupDashboardUnavailableTest` | Automated Verified | **Pass (Automated)** | Slices fail closed independently; zero is never fabricated for broken providers. |
| **U15-04** | `KAN-80` / `UC66` | Backup status card (C01/C02: startedAt, latestSuccessfulAt, 7-day failures) | `BackupDashboardStatusIntegrationTest` (8 tests), `BackupStatusServiceTest` (4 tests) | Automated Verified | **Pass (Automated)** | Testcontainers PostgreSQL 16 tests verify real query bounds and non-fabricated metadata. |
| **U15-05** | `KAN-80` / `UC66` | Backup history dialog & pagination | `AdminDashboardBackupTest`, `BackupHistoryDialog.test.tsx` | Blocked (Live Admin) | **Pass (Automated) / Blocked (Live)** | Empty state handled gracefully (0 backup runs in DB). |
| **U15-06** | `KAN-80` / `UC66` | Security/Data Privacy (No secrets, PII, download archive, or restore controls) | `AdminDashboardControllerTest`, `AdminDashboardApiContractTest` | Verified Secure | **Pass** | Controller exposes only aggregate counts and sanitized metadata. |
| **U15-07** | `KAN-80` / `UC66` | Guard review, bounded query timeouts, performance | `AdminDashboardControllerTest`, `AdminDashboardAccessTest` | Live Probing Defect (Finding 1) | **Fail (Defect P1)** | Controller validates query keys before role check. Response times ~15ms locally. |

---

## 3. Prioritized Findings & Defect Catalog

### Finding 1: Guard-Ordering Defect in `AdminDashboardController`
- **Priority:** **P1 (Security / Guard Ordering)**
- **File / Lines:** [`backend/src/main/java/edu/aiplatform/admindashboard/web/AdminDashboardController.java:25`](file:///F:/code/git/AI-Powered-Learning-Platform/backend/src/main/java/edu/aiplatform/admindashboard/web/AdminDashboardController.java#L25), `L32-L34`
- **URL / Endpoint:** `GET /api/v1/admin/dashboard`, `GET /api/v1/admin/dashboard/backups`
- **Detailed Mechanism & Nuance:**
  - When an authorized admin calls `/backups?page=-1`, `integer(query, "page", ...)` parses `-1` successfully as an integer, which passes into `service.backups(actor, -1, size)`. Inside `AdminDashboardService.backups`, `requireAdmin(actor)` runs *first*, so a non-admin sending `page=-1` IS rejected by `requireAdmin`!
  - However, for:
    1. Unknown query parameters: `GET /api/v1/admin/dashboard?unknownKey=1` or `GET /api/v1/admin/dashboard/backups?unknownKey=1`
    2. Duplicate parameters: `GET /api/v1/admin/dashboard/backups?page=1&page=2`
    3. Non-numeric parameter values: `GET /api/v1/admin/dashboard/backups?page=abc`
    4. Integer overflow values: `GET /api/v1/admin/dashboard/backups?page=9999999999999999999999`
  - In all four cases, `validateKeys(query, ...)` and `integer(query, ...)` in `AdminDashboardController` throw `DASHBOARD_PAGE_INVALID` (`400 Bad Request`) *before* invoking `service.dashboard(actor)` or `service.backups(...)`.
  - Consequently, an unauthorized non-admin user receives HTTP `400 Bad Request` instead of HTTP `403 Forbidden` (`DASHBOARD_FORBIDDEN`), and **no `ACCESS_DENIED` security audit event is written** to `audit_logs`.
- **Suggested Minimal Fix:**
  Ensure the role check (`requireAdmin(actor)`) and denial logging execute before parameter parsing/validation in `AdminDashboardController`, or pass raw query parameters into the service to validate after authorization. Valid admins must still receive `400 Bad Request` (`DASHBOARD_PAGE_INVALID`) when parameters are malformed.

---

### Finding 2: Guard-Ordering Defect in `AcademicsService` for `list` & `students`
- **Priority:** **P1 (Security / Guard Ordering)**
- **File / Lines:** [`backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java:52`](file:///F:/code/git/AI-Powered-Learning-Platform/backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java#L52), `L71`
- **URL / Endpoint:** `GET /api/v1/manager/classes`, `GET /api/v1/manager/classes/{id}/students`
- **Mechanism:**
  - In `AcademicsService.list()` (line 52): `page(page,size); validateQuery(query); requireAudience(actor,audience);`
  - In `AcademicsService.students()` (line 71): `page(page,size); requireAudience(actor,audience); requireClassAccess(actor,audience,id);`
  - When an unauthorized role (e.g. `TEACHER` or `STUDENT`) sends `GET /api/v1/manager/classes?page=0`, `page(...)` throws `INVALID_PAGE` (400) before `requireAudience(...)`.
  - The `guarded(...)` wrapper maps `status == 400` as operational failure, persisting `AuditResult.FAILURE` instead of `AuditResult.DENIED` (`ACCESS_DENIED`).
  - Note: `detail` (line 66) already executes `requireAudience(actor, audience)` first, so only `list` and `students` require re-ordering.
- **Suggested Minimal Fix:**
  Move `requireAudience(actor, audience);` to the first line inside the `guarded` lambda in `AcademicsService.list()` and `AcademicsService.students()`.

---

### Finding 3: Navigation Mismatch & Broken Sidebar Active State for Teacher / Manager
- **Priority:** **P2 (Usability / Navigation / Unnecessary 403 Calls)**
- **File / Lines:** [`frontend/src/features/academics/register.ts:8-9`](file:///F:/code/git/AI-Powered-Learning-Platform/frontend/src/features/academics/register.ts#L8-L9)
- **URL / Screen:** `/teaching/classes`, `/teaching/classes/{id}`
- **Evidence / Screenshot:** `.tmp/u05-u15-audit/u05-03-teaching-classes-open.png`, `.tmp/u05-u15-audit/u05-04-teacher-class-info.png`
- **Mechanism:**
  - `manager-teaching-classes` and `teaching-classes` are registered with `href: "/classes"`.
  - When a teacher navigates to `/teaching/classes`, the sidebar item `Lớp được phân công` has `href: "/classes"`, failing to match `pathname === "/teaching/classes"`. It renders unselected (`aria-current` absent).
  - Furthermore, clicking the sidebar item navigates to `/classes`, calling `classCapability()`. That function tries `GET /api/v1/me/classes` first, returning `403 ROLE_DENIED` and logging an unwarranted `ACCESS_DENIED` audit event before falling back to `teaching`.
- **Suggested Minimal Fix:**
  Change `href: "/classes"` to `href: "/teaching/classes"` in `register.ts` for items `teaching-classes` and `manager-teaching-classes`. Student `my-classes` remains `href: "/classes"`.

---

### Finding 4: Undefined CSS Variables in `academics.module.css` (Missing Borders & Wrong Text Color)
- **Priority:** **P2 (UI / Visual Styling / Contrast)**
- **File / Lines:** [`frontend/src/features/academics/academics.module.css:28`](file:///F:/code/git/AI-Powered-Learning-Platform/frontend/src/features/academics/academics.module.css#L28), `L36`
- **URL / Screen:** `/teaching/classes/{id}` (Overview tab)
- **Evidence / Screenshot:** `.tmp/u05-u15-audit/u05-04-teacher-class-info.png`
- **Mechanism:**
  - The custom properties `--line`, `--muted`, `--primary`, `--ink` were defined on `.root`. With `AuthenticatedLayout`, pages are rendered without `.root`.
  - `.panel` has `border: 1px solid var(--line);` with no fallback, causing computed `border-width` to become `0px`.
  - `.pageHeading p` has `color: var(--muted);` with no fallback, causing computed text color to fall back to dark ink (`#0f172a`) instead of muted slate (`#64708a`).
- **Suggested Minimal Fix:**
  Provide scoped fallbacks matching shared theme tokens (e.g. `var(--line, var(--color-line, #e8ebf2))` and `var(--muted, var(--color-muted, #64708a))`) or define scoped variables on `.main, .panel` without polluting global `:root`.

---

### Finding 5: Search Input in Roster Toolbar Truncates Placeholder
- **Priority:** **P3 (UI Polish / Spacing)**
- **File / Lines:** [`frontend/src/features/academics/academics.module.css:47-48`](file:///F:/code/git/AI-Powered-Learning-Platform/frontend/src/features/academics/academics.module.css#L47-L48), [`EnrollmentPanel.tsx:38`](file:///F:/code/git/AI-Powered-Learning-Platform/frontend/src/features/academics/EnrollmentPanel.tsx#L38)
- **URL / Screen:** `/teaching/classes/{id}` (Students tab)
- **Evidence / Screenshot:** `.tmp/u05-u15-audit/u05-04-teacher-class-students.png`

---

### Finding 6: Unstyled `Thử lại` Retry Button in `Pending` Error Boundary
- **Priority:** **P3 (UI Polish)**
- **File / Lines:** [`frontend/src/features/academics/routes.tsx:81`](file:///F:/code/git/AI-Powered-Learning-Platform/frontend/src/features/academics/routes.tsx#L81)
- **URL / Screen:** Denied or network error screens (`u05-02-teacher-on-student-class-denied.png`)

---

### Finding 7: Unstyled Form Controls in `ManagerRoute` & `ClassFormDialog`
- **Priority:** **P3 (UI Consistency)**
- **File / Lines:** [`frontend/src/features/academics/routes.tsx:195`](file:///F:/code/git/AI-Powered-Learning-Platform/frontend/src/features/academics/routes.tsx#L195), [`ClassFormDialog.tsx:29`](file:///F:/code/git/AI-Powered-Learning-Platform/frontend/src/features/academics/ClassFormDialog.tsx#L29)

---

### Finding 8: Documentation / Contract Drift in OpenAPI `admin-dashboard.yaml`
- **Priority:** **P3 (Documentation / Spec Alignment)**
- **File / Lines:** [`docs/contracts/openapi/admin-dashboard.yaml:5`](file:///F:/code/git/AI-Powered-Learning-Platform/docs/contracts/openapi/admin-dashboard.yaml#L5), `L292`

---

## 4. UI Usability & Accessibility Evaluation

| Screen / View | Viewport / Evidence | Usability Verdict | Key Observations |
|---|---|---|---|
| **Teacher Class List** (`/teaching/classes`) | Desktop (1440px) & Host width | **Good** | Clean tabs (`Đang dạy · 4`, `Sắp mở · 1`, `Đã kết thúc · 0`). Clear status badges (`Đang mở`, `Bản nháp`). Cards align cleanly. Missing active sidebar state noted (Finding 3). |
| **Class Overview** (`/teaching/classes/{id}`) | Desktop (1440px) & Host width | **Good** | 4 summary cards (`Sinh viên: 1`, `Nhóm: 0`, `Đang mở: 0`, `Chờ chấm: 0`). Metadata card displays Code, Subject, Term, Distribution status. Progress section clean. Lazy distribution expand/collapse works without extra network requests when closed. |
| **Student Roster** (`/teaching/classes/{id}` - Tab Sinh viên) | Desktop & Host width | **Fair** | Roster displays active student (`u05.student@school.test`, `ACTIVE`). Truncated search placeholder noted (Finding 5). StudentDetailDialog (`SlotOutlet`) opens and traps focus, ESC closes cleanly (`u05-04-teacher-student-detail-dialog.png`). |
| **Evaluations & Materials** | Desktop & Host width | **Good** | Evaluations tab displays Assignment & Quiz cards with weightings and status (`u05-04-teacher-class-evals.png`). Materials tab displays course assets (`u05-04-teacher-class-materials.png`). |
| **Gradebook Tab** | Desktop & Host width | **Good** | Mode toggle (`Theo bài tập` vs `Tổng hợp`) functions smoothly (`u05-04-teacher-class-gradebook.png`, `u05-04-teacher-class-gradebook-matrix.png`). Sticky student column operates correctly. |
| **Access Denied Error View** | Desktop | **Needs Polish** | Displays standard error banner, but the retry button is completely unstyled (Finding 6). |

*Note on Mobile / Zoom Testing:* While responsive grid definitions exist in CSS (`@media (max-width: 720px)`), dedicated testing on a physical 390px mobile viewport or with actual browser 200% zoom was not independently executed in this audit turn.

---

## 5. Automated Verification Counts & Performance Observations

- **Frontend Test Execution:**
  - Command: `npm --prefix frontend test -- src/features/academics src/features/admindashboard src/features/grading/class-progress-layout.test.tsx src/features/grading/gradebook-layout.test.tsx src/features/groups/TeacherGroupWidgets.test.tsx --maxWorkers=1`
  - Results: **15 test files passed (15)**, **82 tests passed (82)**, `0` failed, `0` skipped.
  - Duration: `18.44s`.
- **Backend Unit & Slice Execution:**
  - Command: `mvn test -Dtest=edu.aiplatform.academics.**.*Test,edu.aiplatform.admindashboard.**.*Test,edu.aiplatform.backup.application.BackupStatusServiceTest`
  - Results: **18 test classes passed (18)**, **249 tests passed (249)**, `0` failures, `0` errors, `0` skipped.
  - Duration: `51.946s`.
- **Backend Testcontainers Integration Execution:**
  - Command: `mvn test -Dtest=edu.aiplatform.backup.application.BackupDashboardContractTest,edu.aiplatform.backup.contract.stub.BackupDashboardUnavailableTest,edu.aiplatform.backup.infrastructure.BackupDashboardStatusIntegrationTest`
  - Results: **3 test classes passed (3)**, **12 tests passed (12)**, `0` failures, `0` errors, `0` skipped (8 PostgreSQL 16 Testcontainers integration tests in `BackupDashboardStatusIntegrationTest`, 1 mock contract test, 3 stub tests).
  - Duration: `14.705s`.
- **API Latency Observations:**
  - Local endpoint responses (`GET /api/v1/teaching/classes`, `GET /api/v1/admin/dashboard`) resolve in `~12ms`–`18ms`.
  - Non-functional requirement NFR (P95 <= 1s under concurrent load) remains marked **Not verified** as representative multi-client load testing was out of scope for this single-user audit.

---

## 6. Known Limitations & Safe Test Constraints

1. **Role Login Availability:**
   - Active live browser session was available for `u05.teacher@school.test` (`TEACHER`).
   - Browser sessions for `admin@school.test` (`ADMIN`), `u05.manager@school.test` (`SUBJECT_MANAGER`), and `u05.student@school.test` (`STUDENT`) were not stored in the browser profile. Their live UI screens are marked **Blocked (Live Session Unavailable)**.
2. **Protected Data Safeguards:**
   - Reference course class `4d18e255-1789-45d6-8fbb-351e2188f672` (`U05-A`) and evaluations `7ca08c41-e9d4-4738-946d-24c049e3d1e5` / `826f5c29-b5d1-4a32-9999-9313c242d8d3` were strictly protected.
   - Backup execution (`POST /api/v1/admin/backups/run`) was not triggered.
3. **Audit-Only Scope at Baseline:**
   - Original audit baseline did not modify product code. Minimal fixes for the four corroborated findings are handled in the follow-up phase.

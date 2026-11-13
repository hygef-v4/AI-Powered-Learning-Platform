# U05 / U15 Corroborated Fixes and Audit Correction Plan

**Date:** 2026-10-25 to 2026-10-26\
**Owner:** Gemini 3.8 Flash began implementation; Codex took over after the user requested transfer.\
**Authorization:** User instructions to hand off U05/U15 checks, "giao nhiệm vụ cho gemini trước sau đợi báo cáo lại thì lập kế hoạch tiếp", "giups tooi bảo gemini tự động làm", and "chuyển job hiện tại cho codex đi"\
**Branch:** `mcp-test` | **HEAD:** `41d6cd19`\
**Scope:** Bounded correction of audit evidence and implementation of the FOUR corroborated findings only. No broad future planning, no cosmetic backlog.

---

## Task Checklist

### 1. Correct Audit Evidence in `2026-10-25-u05-u15-audit-report.md`
- [x] Correct authorship attribution: initial audit turn by Argon 4 (exhausted quota), continuation by Gemini 3.8 Flash.
- [x] Correct Next.js version to `16.3.8` (was misreported as `16.0.7`).
- [x] Remove all nonexistent test class names (e.g. `StudentClassListTest`, `TeachingClassDetailTest`, `ManagerClassListTest`, `AssignTeacherTest`, `AdminDashboardSecurityTest`, etc.).
- [x] Replace with verified test file paths, test counts, and surefire-backed numbers:
  - Frontend: 15 files / 82 tests (`academics`: 7 files / 43 tests; `admindashboard`: 5 files / 27 tests; cross-unit: 3 files / 12 tests).
  - Backend owned units: 18 classes / 249 tests (Academics: 11 classes / 211 tests; AdminDashboard: 6 classes / 34 tests; Backup: 1 class / 4 tests).
  - Integration/contract: 3 classes / 12 tests (`BackupDashboardStatusIntegrationTest`: 8 PostgreSQL integration tests; `BackupDashboardContractTest`: 1 contract test; `BackupDashboardUnavailableTest`: 3 stub tests).
- [x] Remove fictitious per-UC numeric allocations in traceability table; map each UC accurately to actual test classes/methods.
- [x] Correct status: Teacher denial on `/classes/{id}` is defense-in-depth verification, not student UC21 acceptance.
- [x] Mark Admin, Manager, and Student live UI as `Blocked` (credentials not active in session).
- [x] Correct overclaimed checkboxes in `2026-10-25-u05-u15-jira-ui-audit-plan.md` (mobile 390px, 200% zoom, unperformed roles).

### 2. Implement Minimal Fixes for Four Corroborated Findings
- [x] **Fix (a) AdminDashboardController guard ordering:**
  - Write failing regression tests covering non-admin sending unknown query keys, duplicate keys, non-numeric strings, overflow, and range negative.
  - Ensure `requireAdmin(actor)` and `ACCESS_DENIED` audit logging happen BEFORE query validation errors return `400 Bad Request`.
  - Preserve valid admin `400 Bad Request` (`DASHBOARD_PAGE_INVALID`) and defense-in-depth in service.
- [x] **Fix (b) AcademicsService.list and students guard ordering:**
  - Add regression coverage for unauthorized users sending invalid paging to both operations; assert `403 ROLE_DENIED` semantics and `ACCESS_DENIED` / `DENIED` audit events.
  - In `AcademicsService.list()` and `students()`, move `requireAudience(actor, audience)` before `page(page, size)` and `validateQuery(query)`.
  - Authorized-invalid response behavior remains covered by existing validation paths; a dedicated assertion for its audit classification was not added in this follow-up.
- [x] **Fix (c) academics/register.ts navigation hrefs:**
  - Update `teaching-classes` and `manager-teaching-classes` to `href: "/teaching/classes"`.
  - Keep `my-classes` at `href: "/classes"`.
  - Add regression test for nav entry registration.
- [x] **Fix (d) academics.module.css scoped theme fallbacks:**
  - Add theme token fallbacks to `.panel`, `.pageHeading p`, and `.footer` so borders and muted text remain visible outside the app's scoped theme.
  - Do NOT pollute global `:root` or restore obsolete `AcademicsShell`.

### 3. Verification & Live Retesting
- [x] Run backend unit & contract tests for academics and admindashboard. Fresh targeted run: 43 tests passed (24 AcademicsServiceTest, 8 AdminDashboardControllerTest, 11 AdminDashboardApiContractTest), 0 failures/errors/skips.
- [x] Run frontend unit and component tests. Gemini run: 15 files / 83 tests passed, 0 failures (includes academics, admindashboard, grading overview/gradebook and groups). The original audit baseline was 15/82; this is a fresh follow-up run.
- [x] Run frontend lint, typecheck, and isolated build. All exit 0; isolated build generated 28 routes and tsconfig was restored after Next added generated type paths.
- [ ] Retest live teacher session: blocked after system restart because Orca browser now redirects to `/login`; screenshot `.tmp/u05-u15-audit/followup-login-required.png`. No session credential available. Need user to sign in as teacher in Orca, then retest sidebar highlight, no `/me/classes` request, and computed card border/subtitle.
- [x] Capture screenshot evidence for the current login-required blocker in `.tmp/u05-u15-audit/followup-login-required.png`.

### 4. Deliverables & State Recording
- [x] Deliver `aidlc-docs/construction/build-and-test/2026-10-26-u05-u15-followup-report.md`.
- [x] Update `aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md` to verify the fixed menu and card styles; user result column remains `Chưa test`.
- [x] Append entries to `aidlc-docs/aidlc-state.md` and `aidlc-docs/audit.md`.
- [x] Run `git diff --check`.
- [x] Stop and report to user after documenting the live UI blocker; leave the host app available for user testing.

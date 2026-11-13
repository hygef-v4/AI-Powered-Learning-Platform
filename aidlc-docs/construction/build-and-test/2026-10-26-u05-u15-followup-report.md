# U05 / U15 Follow-up Fix and Verification Report

**Date:** 2026-10-26 (Asia/Saigon)\
**Branch / baseline HEAD:** `mcp-test` / `41d6cd19`\
**Execution:** Codex took ownership after the user transferred the active Gemini job. Changes remain uncommitted.

## Outcome

Four corroborated findings from the Jira-based audit were addressed in the current working tree. The security changes preserve the existing role checks and make them run before query validation, so malformed requests from unauthorized actors receive the denial response and denial audit event. Frontend automated checks pass. The live browser UI could not be rechecked because the restarted Orca browser no longer has the teacher session and redirects to `/login`; no credential was available, and none was guessed or changed.

## Changes

1. **Admin dashboard authorization ordering:** `AdminDashboardController` calls the admin guard before validating query keys and pagination for dashboard and backup endpoints. Service-level access checks remain in place as defense in depth. Regression coverage includes non-admin requests with unknown, non-numeric, duplicate, overflow, and out-of-range parameters, plus existing admin validation behavior.
2. **Academics authorization ordering:** `AcademicsService.list()` and `students()` check the requested audience before pagination/query validation. Regression tests verify invalid paging from a student against the manager audience returns 403 and records `ACCESS_DENIED` / `DENIED`.
3. **Class navigation:** teacher and subject-manager sidebar entries now route to `/teaching/classes`; the student entry remains `/classes`. A frontend registration test covers all three destinations.
4. **Class theme fallbacks:** `.panel` border, `.pageHeading p` text, and `.footer` border/text now fall back to defined global tokens and literal colors when scoped theme variables are absent. The live computed style was not observed in this follow-up because of the login barrier.

## Verification

- Backend targeted Maven run: `AcademicsServiceTest`, `AdminDashboardControllerTest`, and `AdminDashboardApiContractTest` — **43 tests passed**, 0 failures/errors/skips. Command used offline Maven with the repository's local-cache settings and an explicit Byte Buddy java agent.
- Frontend focused run: **15 files / 83 tests passed**, 0 failures. Covered academics, admin dashboard, grading overview/gradebook, and groups. This is a fresh follow-up run; the earlier audit's 15/82 count is historical.
- `npm --prefix frontend run lint` — passed.
- `npm --prefix frontend run typecheck` — passed.
- Isolated production build with `NEXT_DIST_DIR=.next-build-u05-u15-followup` — passed; Next generated 28 routes. The build's generated `frontend/tsconfig.json` changes were restored after confirming they were build-generated only.
- `git diff --check` — passed.

No dedicated new test was added for the authorized-invalid Academics audit classification; the existing validation flow is retained, but that specific audit assertion is not claimed as verified by this follow-up.

## Live runtime and UI status

- Host API is running on port 8080 and Next dev server on port 3000. PostgreSQL, Redis, and RabbitMQ containers were healthy at startup. The old Docker worker was stopped before the host `local,worker` backend was launched, avoiding two workers competing for jobs.
- Orca browser navigation to the teacher class page redirects to `/login`. Evidence screenshot: `.tmp/u05-u15-audit/followup-login-required.png`.
- Consequently no authenticated U05 teacher UI regression was completed in this follow-up. Admin, subject-manager, and student live UI were also not re-authenticated. Manual guide result cells remain `Chưa test`; automated results do not substitute for these UI checks.
- To complete the blocked check, sign in to the running `http://localhost:3000` as the existing teacher in an Orca-controlled browser, then verify `/teaching/classes` is active in the sidebar, no `/api/v1/me/classes` request is made for that view, and the class overview card border/subtitle colors are visible. Use role-specific existing sessions for Admin, Manager, and Student cases in the guide.

## Artifacts and repository state

- Plan/checklist: `aidlc-docs/construction/plans/2026-10-25-u05-u15-corroborated-fixes-plan.md`
- Original Jira audit report, marked as historical baseline: `aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-audit-report.md`
- Manual test guide: `aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md` (40 manual cases remain unrecorded by user)
- Jira snapshot and initial audit plan remain read-only reference artifacts.
- No credentials, Jira data, grades, commits, pushes, merges, or deployments were changed by this follow-up.

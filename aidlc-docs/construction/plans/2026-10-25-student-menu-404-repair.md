# Student menu 404 — investigation and runtime repair

Date: 2026-10-25. Branch mcp-test; existing U09 fixes are committed through 067cd207. This work does not change grading behavior or application route definitions.

## Request and evidence

User reports 404 on Student Bài tập, Luyện tập Quiz, Thông báo lớp, then adds Kết nối AI. Read extracted contents from attached image(8).png and image(9).png. Used the existing authenticated IAB session, U05 Mock Student; no sign-in or credential changes.

| Menu | Observed destination | Live result |
|---|---|---|
| Bài tập | /my-assignments | Next 404 on click and full reload |
| Luyện tập Quiz | /quiz-practice | Next 404 on direct navigation |
| Thông báo lớp | /announcements | Next 404 on direct navigation |
| Kết nối AI | /ai/connection | Next 404 on direct navigation |
| Lớp của tôi | /classes | Authenticated Student class page works |

All four destinations have page.tsx and authenticated layouts in current source; Student is allowed by all four layouts. RoleGuard redirects unauthorized roles rather than generating the observed bare Next 404. The generated .next/server/app-paths-manifest.json and standalone app contain the four routes. Source navigation registrations point to the same URLs observed in the browser. Repository Nginx locations.conf forwards these paths unchanged to frontend:3000.

Conclusion: the current source/build is inconsistent with responses served at localhost:8088. A stale/incomplete frontend runtime or outdated Nginx upstream is a hypothesis, not yet a confirmed container-level root cause. Need to inspect the running frontend manifest and direct HTTP responses, then compare gateway responses. Do not add duplicate routes, bypass guards or catch all 404s.

## Execution plan

- [x] Reproduce all four live failures; distinguish full-page route 404 from backend API errors.
- [x] Verify source routes, links, layouts, build manifest, Dockerfile and proxy configuration.
- [x] Prepare scripts/repair-student-routes.ps1 with optional -CheckOnly; resolve the existing nginx publishing 8088 and its Compose project/configuration, inspect frontend manifest/direct HTTP, rebuild only frontend, replace with --no-deps --wait, verify direct routes and restart nginx, then check all four gateway URLs.
- [x] Validate script syntax and isolated mock behavior: CheckOnly does not mutate; a failed direct probe is rejected; repair mutation order/scope is build frontend, up frontend, restart nginx. These mock results are not real runtime acceptance.
- [ ] Inspect/repair actual Docker runtime. Execution currently blocked by Docker socket permission denied; outbound localhost HTTP from shell also denied. Browser navigation itself works and reproduces the issue.
- [ ] After repair, click all four menu items on the authenticated Student session; verify full reload and actual feature content/API authorization. HTTP 200 from the script is only a route-availability check.

## Files and handoff

Run from a PowerShell terminal with normal Docker Desktop access:

    & 'F:\code\git\AI-Powered-Learning-Platform\scripts\repair-student-routes.ps1'

Diagnostics only: add -CheckOnly. Script stops on missing/ambiguous gateway, unsupported Compose provenance, failed build or failed direct-route probe. It reuses existing project configuration paths inside this checkout and infra/.env.local, without printing secrets. It does not run compose down, delete volumes, reset data, alter user permissions or call AI. Backend/worker/datastores are not recreated. Nginx/frontend updates briefly affect local availability.

No application source patch is justified by current evidence; runtime repair remains incomplete until execution succeeds. No new full frontend test/build result claimed for this script-only change. Prior 501-test result belongs to the U09 implementation. Security access boundaries preserved; infra health/deployment configuration is unchanged. No commit/push/merge.

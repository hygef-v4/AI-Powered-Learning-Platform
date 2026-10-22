# U14 — Verification and independent tester handoff

Update after handoff: user subsequently authorized local commits only; source split into7commits `6cef92a`–`adfc51f` and documentation separately on `announcements-and-notifications`. No push/deploy; no source changed during commit preparation. Results and unresolved gates below remain applicable.

Ngày2026-10-21; workspace `E:/GitHub/AI-Powered-Learning-Platform`, branch `announcements-and-notifications`. Approval Answer a covers C01–C08; user approved code-review step11 with “tiếp tục” on2026-10-21. No commit/push/real SMTP/deployment. See [Build & Test summary](build-and-test-summary.md) for current execution gates.

## Reproduce developer checks

Backend from `backend/`:

```powershell
mvn -B clean verify
mvn -B '-Dtest=edu.aiplatform.announcements.**,edu.aiplatform.notifications.**,NotificationAccountLookupTest,ArchitectureTest' package
```

Frontend from `frontend/`:

```powershell
npm test -- --maxWorkers=2
npm run lint
npm run typecheck
npm run build
npm audit --omit=dev --json
```

Do not run full UI tests concurrently with production compilation on a constrained Windows machine: observed5existing-test timeout5000ms failures under load, final maxWorkers2 passed without changing tests/timeouts. Windows sandbox may require approved execution for Maven cache/SWC and loopback; failures are not passes. Branch switch left U13-generated Next route types: only `frontend/.next/types` and `.next/dev/types` were verified inside the workspace and removed/regenerated, no source/typecheck suppression.

| Evidence | Result | Local ignored log |
|---|---|---|
| Full backend clean verify |919total,842pass,77skip,0failure/error; before final review changes | `.tmp/u14-backend-verify.log` |
| Full backend verify after user code approval |921total,844pass,77skip,0failure/error; executable JAR,85s; finished2026-10-21T16:35:07+07:00 | `.tmp/u14-build-test-verify.log` |
| Final backend focused + package |71pass,0failure/error/skip (66U14,3U01,2Arch); final JAR | `.tmp/u14-backend-handoff.log` |
| Full frontend final |314pass,61files;22new U14/security tests | `.tmp/u14-frontend-final.log` |
| Typecheck/lint |Pass, lint0warning/error | `.tmp/u14-typecheck-final.log`, `u14-lint-final.log` |
| Production standalone build |Pass final source, all pages request-rendered for CSP nonce | `.tmp/u14-build-final.log` |
| OpenAPI/HTTP smoke |Localrefs and syntax valid; announcements/login/404/repeat nonce checks | `.tmp/u14-smoke.log` |
| Production dependency audit |0vulnerabilities, omit dev | `.tmp/u14-audit-production.json` |

Full backend and focused runs are separate evidence; do not add their counts. Executable JAR re-packaged by latest full verify. Java target17 checked using JDK21 runtime; no claim of execution on JDK17. Docker CLI exists, but Docker Engine is unavailable (dockerDesktopLinuxEngine named pipe missing). All77 skipped tests remain skips. Real startup additionally requires U07/U08 migrations referenced by grading and real U07/U08/U12 providers; no substitute schema was created.

## Browser QA performed

Production Next standalone3415 behind disposable fixture3414 (`.tmp/u14-ui-fixture.cjs`). Fake Teacher/Student only, no backend/provider/SMTP credentials. Checked:

- Feed/class selector/Markdown rendering against local t08/s06 reference; Teacher creates important+email announcement, edits preserving flags and version, confirms delete; form resets after success.
- Student full reload has no composer/edit/delete; bell opens popup with5preference switches. Teacher popup has no learning preferences. Student disables important-announcement email, marks all read, unread count changes2->0.
- Browser console no CSP/hydration warnings/errors. HTTP tests verify script nonce/header matches, nonce changes between requests, caller CSP/x-nonce overwritten, DENY, HSTS/nosniff/referrer, private/no-store and404 headers.
- Desktop screenshot visually inspected. Mobile viewport/native provider behaviors are not certified by this fixture; responsive CSS alone is not browser acceptance. U05 StudentClassSection registration has component source/query test but full U05 page integration still needs real API.

## Independent tester matrix

| Area | Required runtime cases | Expected |
|---|---|---|
| Migration/startup |All owner migrations on PostgreSQL, Hibernate validate, app grants |5tables/indexes/FK/checks; no fake owner schema; DELETE only notification parent with child cascade |
| Class access |4roles; assigned/nonassigned teacher, subject manager not teaching, ACTIVE/removed student, DRAFT/ARCHIVED class |Only assigned staff mutate OPEN class; members read; Admin denied; outside scope404+denied audit |
| CAS/audit |Concurrent PATCH/delete expectedVersion; audit failure and repository failure |Single winner409loser; original author/flags/time remain; success audit atomic and denied audit survives |
| Creation event |Create vs update/delete, transaction rollback, publisher outage |Only create after commit emits class.announcement-posted; no rollback of source for transport errors |
| Event schema/recipients |Actual U05/U07/U09/U10/U12/U03 payloads; class vs subject; enrollment accountId, grade studentIds, all group events;125ACTIVE Admins and disabled accounts |Correct authorized/current recipients, no truncation20, no scores/submissions/secrets; unknown schema no fanout |
| Dedup/fault |Same event concurrently/replayed; notification insert/email failure |Unique event/recipient/type and unique email/notification; atomic rollback; SSE after commit only |
| Rabbit |jobs.notification bindings, schema mismatch/malformed, TTL30s/1m/2m/4m/8m, publish failure, exhausted attempts |Confirmed retry before ack, bounded retry, parking queue, no raw payload logs; domain events not routed as JobMessages |
| Email |Default/on/off5types, backup always enabled, real address ACTIVE/currentrole, SMTP down/timeout/accept-crash, terminal SENT |Correct SKIPPED/DEFERRED/PENDING/SENT/FAILED; finite retry; no duplicate known-SENT; document IMail crash ambiguity |
| Cap/priority |Concurrent API+workers,300reservations, VN midnight, retry across day, OTP traffic, deferred mixed priorities |No budget race; OTP excluded; backup>deadline>important>assignment>grade>enrollment; expired deadline SKIPPED |
| Reminder |Individual/group, submitted vs not, subject across OPEN classes, late open, changed close/retire, repeated scans,500+cardinality |Once per assignment/class/deadline, correct current non-submitters, no late-open/retired notification; marker+fanout atomic; measure batch coverage |
| Preference/retention |Student own-only, all staff denied, invalid types/null, email created before vs after update;180day cutoff/batch10000 |In-app unaffected, no backup toggle, no premature cleanup/orphan, no extra app DELETE rights |
| SSE/session |Real U12 hub2instances, cookie expiry/disable/role change, guessed foreign channel, read/read-all from second tab, Nginx buffering |Own server-selected channel; notifications refresh≤5s; reconnect bounded and cleanup on unmount/logout |
| UI |Teacher/Student/Subject Manager/Admin header, U05 card link, query route changes/filter/pagination, server409/503, keyboard/Escape/mobile |Correct actions, no stale rows across filter/account, sanitized Markdown, fallback reload; no mock production data |
| NFR/fault |200student fanout, p95list300ms, SSE5s/reminder5min, DB lock/provider cancel and worker stress |Record measured latency/resource-release evidence; unit timeouts/batch limits are not proof |
| Deployment/security |APP_PUBLIC_URL, cap env, real HTTPS/Nginx, caching, header composition |Email links use public origin; no duplicate/conflicting CSP; no public cache of nonce HTML; existing health/rollback checks |

Provider gates U07/U08/U12 and SMTP exactly-once limit were approved explicitly in C04/C08 and remain open. This handoff does not mark UC63–65 accepted, independent tests passed, or Operations ready.

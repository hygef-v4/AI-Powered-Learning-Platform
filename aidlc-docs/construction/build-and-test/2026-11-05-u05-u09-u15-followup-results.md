# U05/U09/U15 follow-up execution

Plan: [2026-11-05-u05-u09-u15-followup](../plans/code-generation-plan/2026-11-05-u05-u09-u15-followup.md).
Requested by: Quang Hung.

Status: implementation and automated verification complete; manual browser acceptance deferred. Baseline `630f59c5`; only the approved plan/state/audit were dirty at start.

Pre-flight: Tasks 2/4 share GradebookService; retain its indexed selection when moving scoring calls. Task 3 changes only document types/adaptation; Task 4 changes only arithmetic ownership. Other tasks have no shared interfaces.

Ruling: execute in the existing `fix/u05-u09-u15` checkout with the approved uncommitted plan — the user requested this scoped continuation; no branch switching or incidental commit. Cost: changes share the existing checkout and must be reviewed together.

## Execution ledger

- Task 1: complete. Original implementation failed three hanging-source cases and four-signal cancellation assertion (4 failures/8 passes). Independent requests, bounded cancellation and pending states now pass 26 dashboard/usage tests. Includes ordering, late response, primary timeout, denial/poll-stop and timer cleanup. Logs: `.tmp/followup-u15-red.log`, `.tmp/followup-u15-all.log`.
- Task 2: complete. The 501-learner controller regression first failed on repeated source reads. Lookup/delegation now pass 20 focused tests including denied scope, absent/removed students, stale receipts, contact fallback and existing export/privacy behavior. Logs: `.tmp/followup-u09-red.log`, `.tmp/followup-u09-green.log`.
- Task 3: complete. Submission DTO derives from the shared block union; workspace adaptation no longer uses the unchecked double cast. Grading frontend: 138 tests pass; TypeScript passes. Tests preserve every block kind, image flags and unchanged input. Fixtures match actual origin/content fields. The new adapter test initially failed because the planned module did not yet exist, rather than a behavioral assertion failure; do not count that as a reproduced runtime defect. Logs: `.tmp/followup-grading-fe.log`, `.tmp/followup-types.log`.
- Task 4: complete. Five scoring characterization cases plus 17 existing grading-service cases passed before moving code. Model methods are textually identical to the old bodies apart from public visibility; all callers updated, JSON operations remain in the service. Scoped backend run includes architecture tests. Log: `.tmp/followup-scoring-before.log`.
- Task 5: complete. Date/error characterization passed before the move; helper bodies preserved, all callers updated and unused repository import removed. Combined scoped frontend: 226/226 passed. Scoped backend: 359 cases, zero failures/errors, seven Docker-dependent skips under sandbox; these skips are not accepted as final verification. Logs: `.tmp/followup-format-before.log`, `.tmp/followup-fe-focused.log`, `.tmp/followup-be-focused.log`.
- Task 6: complete with explicit manual-browser limitation. Full verification reruns passed after the review fix; docs synchronized without replacing historical evidence.

## Independent final review

One P2 found: replacing a refresh while optional sources were pending, followed by primary failure, could retain stale loading flags indefinitely. Regression failed first with `usage: true`; primary failure now settles retained loading flags. Review fix plus grading tests: 145 passed. Logs: `.tmp/followup-review-red.log`, `.tmp/followup-review-green.log`. Restored existing document runtime guards and retained legacy text/array-cell rendering coverage. No other confirmed introduced finding; no deferred minor finding.

Reviewer exclusions resolved:
- Database-level per-student filtering remains deferred per plan: metadata loads once; large-class memory/DB work is still possible.
- Runtime document schema validation is not added; existing defensive rendering guards remain. Compile-time typing is not validation of arbitrary server JSON.
- Shared authentication refresh is unchanged; the dashboard bounds its own request promises and signals but does not redesign the session transport.
- Verification and generated-config restoration remain execution gates, not assumed passes.

## Environment notes

The first full backend run under sandbox failed on Docker pipe permissions and child-JVM document extraction, with skipped integration suites. Retried with approved elevated test access; Testcontainers creates temporary PostgreSQL containers. The first elevated run executed 1,782 cases with zero skips but had four unrelated environment failures: default Windows `Cp1252` decoded UTF-8 backup text incorrectly; the unnormalized relative Maven cache path expanded the parser JVM classpath to 33,869 characters, above the Windows command-line limit. UTF-8 JVM startup and an absolute normalized cache path reduce it to 31,406; both affected suites then passed (17/17), without source/test changes. Final full verify uses that configuration. An attempted offline clean step could not resolve the uncached clean plugin; used the planned `verify` command and removed only stale generated artifacts of the test moved to the model package. No application database change.

Manual Orca browser acceptance could not start: `orca tab list --json` returned “The term 'orca' is not recognized as a name of a cmdlet, function, script file, or executable program.” No browser acceptance claimed; no live user/grade changes.

## Verification and limits

- Frontend final full suite: **776 passed / 115 files**, no failures. Log: `.tmp/followup-fe-full-final.log`.
- Frontend full lint, TypeScript and production webpack build passed. Build used `.next-followup-20261105` to preserve the running dev output. Generated TypeScript config restored; typecheck after restoration passed. Logs: `.tmp/followup-fe-lint-final.log`, `.tmp/followup-fe-build-final.log`, `.tmp/followup-fe-types-restored.log`.
- Moved scoring/helper bodies compared programmatically to baseline: identical apart from scoring method public visibility. Changed documentation local links resolve; `git diff --check` passed.
- Backend final Maven offline `verify`, JDK 17, `-DforkCount=0`, UTF-8 JVM startup and normalized absolute repository cache path: **1,782 passed, zero failures/errors/skips**; production JAR packaged. Log: `.tmp/followup-be-verify.log`.
- Required PostgreSQL guard passed: `SubjectRepositoryIntegrationTest` **31** and `NotificationDeliveryIntegrationTest` **2**, zero skips. Tests use temporary Testcontainers databases; application data unchanged.

## Extension compliance

Retained scope: SECURITY-03/04/05/08/09/12/15 compliant for this delta (audit/error behavior, headers, input validation, authorization/authentication and public-contract gates preserved; fresh authorization/privacy/architecture regressions pass). RESILIENCY-04/06/10 compliant for this delta (bounded request deadlines/cancellation, source isolation and existing operational health/rollback boundaries preserved). No new infrastructure, credential flow, deployment or recovery strategy; broader runtime certification is N/A for this follow-up. PBT remains disabled by the existing decision.

No DB migration, live grade mutation, commit, push or deployment. Existing security/resiliency scope retained; PBT disabled. Manual browser and load/AI-quality calibration not claimed. Student lookup still loads class metadata once.

## Delivery request — 2026-11-05

After the implementation-only verification above, Quang Hung requested a clear English commit, feature-branch push, pull request into `staging` and CI verification. Those delivery actions are now authorized; merge/deployment remain outside this request. The PR is the source of truth for its exact head and remote CI checks. Local verification evidence above applies to unchanged source in this delivery.

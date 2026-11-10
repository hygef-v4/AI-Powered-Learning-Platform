# U05/U09/U15 cleanup execution ledger — 2026-11-04

Plan: `../plans/code-generation-plan/2026-11-04-u05-u09-u15-cleanup.md`.
Baseline: `45baad9b`, existing `fix/u05-u09-u15` branch. No commits or push requested.

Status: implementation, automated verification and independent review complete. Manual browser acceptance remains deferred because the Orca CLI is unavailable.

Pre-flight: tasks 1 and 3 share workspace tests; task 4 moves scoring imports used by task 1. Execute sequentially and run the complete grading suite after all changes.

Ruling: use the current user-selected feature branch in the shared workspace; preserve its staged plan. No extra checkout or worktree is needed for this bounded change.

Ruling: retain unfiltered Java `detail` semantics for MCP/provider compatibility. Only requests explicitly carrying navigation filters use filtered listing order.

Ruling: put gradebook request-count/characterization fixtures in `GradebookAssemblyTest` rather than enlarging `GradebookServiceTest`; both suites run. Share evaluation URL construction through the small pure `model/navigation.ts` helper. No public unit interface change.

Ruling: browser CLI is unavailable, so execute interaction/export acceptance with React/JUnit fixtures and explicitly defer manual browser acceptance. Do not use the user's live grades or restart applications to work around that environment limit.

## Evidence

- Navigation RED: workspace expected filter-preserving URL but received a bare evaluation URL; route/list tests also failed for missing filters. Backend RED: new detail overload did not exist. After implementation, backend navigation/MCP tests passed (4 tests before the additional group case).
- Gradebook RED: with 201 student contacts correctly stubbed, characterization passed but export read class evaluations three times instead of once. Focused backend run after the optimization passed 279 tests, zero failures/errors/skips, including real PostgreSQL, U05/U15 and ArchUnit. A later group-navigation case is covered by the full run.
- Conflict RED: four business-code cases plus the concurrent-lock workspace case failed on the generic 409 message; mapping implemented without automatic write retry or input reset.
- First affected frontend run: 203 passed, four failed because the moved `checklistPoints` helper was not imported back into its UI caller. Root cause corrected; full verification pending below.
- Independent review found one P2: an absent current evaluation produced index -1 and save-next selected the queue's first item. Reproduced by a failing workspace test; now only advances from a valid current index and computes next before the save request. Reviewer found no additional gradebook/group/HTTP correctness or security regression.
- Browser limitation: the selected `orca` executable returned "The term 'orca' is not recognized as a name of a cmdlet, function, script file, or executable program." No Computer Use fallback, application restart, live grade write or live DB mutation was attempted. React UI regression tests cover filtered links, save-next/last/missing-current, explicit student selection and locked-grade feedback; this is not a manual browser acceptance claim.
- Independent re-review confirmed the missing-current fix and reported no remaining actionable introduced finding.

## Final verification

- Backend full Maven offline `verify`, Java 17, repository cache settings, `-DforkCount=0`: **1,771 tests passed, zero failures/errors/skips**, production JAR repackaged. Log: `.tmp/cleanup-be-full.log`. Required real PostgreSQL suites: `SubjectRepositoryIntegrationTest` 31 passed and `NotificationDeliveryIntegrationTest` 2 passed. New navigation tests: 4 passed including groups; gradebook assembly tests: 3 passed including 201-student export. Temporary Testcontainers databases only; application data unchanged.
- Frontend first full run: 755 tests / 109 files passed (`.tmp/cleanup-fe-full.log`). Following review and additional API/model tests, complete grading suite: **135 tests / 15 files passed** (`.tmp/cleanup-fe-final-grading.log`). Final whole-suite run: **760 tests / 111 files passed**, exit 0 (`.tmp/cleanup-fe-full-final.log`).
- Final frontend production build, lint and TypeScript: all exit 0 (`.tmp/cleanup-fe-build-final.log`, `.tmp/cleanup-fe-lint-final.log`, `.tmp/cleanup-fe-types-final.log`). Production output: `frontend/.next-build-cleanup-20261104`, separate from development. Next's temporary tsconfig includes/formatting restored to the unchanged baseline after the build.
- Source self-check: all six moved scoring helper bodies match HEAD exactly. OpenAPI parsed successfully with the already installed `js-yaml`; anchors resolve and `id/status/late` parameter schemas were asserted. `git diff --check` passes.
- Existing exporter tests passed for CSV quoting/formula neutralization and well-formed XLSX text cells. React interaction tests replace manual browser acceptance for this run, as explained above; no load/latency measurement or live grade mutation was performed.

## Scope and remaining limits

U15 source is unchanged and covered by backend/frontend regression tests. Authorization/transaction boundaries, finalized-grade locks, resubmission reset policy, AI proposal/rubric validation and public unit contracts remain intact. Gradebook still loads class source metadata and export materializes its output in memory; this change removes repeated scans/queries rather than introducing streaming or database pagination. No new dependency, DB migration, deployment, commit or push.

Security baseline: existing access, immutable grade and audit guards preserved and regression-checked. Resiliency: current unit scope retained; no new provider/retry policy. PBT remains disabled. No unresolved applicable extension finding was identified in this bounded change.

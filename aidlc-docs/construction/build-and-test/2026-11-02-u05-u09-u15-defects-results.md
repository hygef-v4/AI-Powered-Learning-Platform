# U05/U09/U15 defect remediation — verification 2026-11-02

Branch: `fix/u05-u09-u15`; base `86b16d92`. [Execution plan](../plans/code-generation-plan/2026-11-02-u05-u09-u15-defects.md). User requested a local commit after verification; no push, merge or deployment.

## Changes and traceability

| Defect | Implementation | Regression evidence |
|---|---|---|
| KAN-153 | Preserve explicit partial scores and saved adjustment/reason. Checkbox clicks remain full/zero; partial values and retained adjustments are visible and included in the total. | `preserve-grading.test.tsx`, partial scores and workspace suites |
| KAN-154 | Pass structured feedback through the page callback; retain AI points, general/structured feedback and evidence; reset previous acknowledgements. Flagged/mismatched explanations expose review controls and block finalize. Legacy proposals retain old explanation score baselines for the feedback's own items only; summary-only/no-feedback cases send no invented item metadata. | Six hook cases, structured-feedback apply/save/finalize and score-change acknowledgement cases; existing backend grading review gates |
| KAN-155 | Manual enrollment and restoration check caller/class access before taking the target account share lock, then take the subject lock and recheck scope/state. Reuse the existing account contract and transaction, matching batch lock order. | Academics service lock order, stale eligibility, out-of-scope no-lock cases; real PostgreSQL concurrent disable waits until the contact-lock transaction commits |
| KAN-156 | Accept SUBJECT assignments only when the authorized teaching class has the same subject; show the actual class label. Keep CLASS matching and response-sequence isolation. | Submissions suite: same subject, wrong subject/class, dependency unavailable and late class response after route change |
| KAN-157 | Keep calendar and add operational backup metadata, disabled/never-run/unavailable distinction, settings link and inline 20-row paginated history with independent retry/abort. No archive/download/restore control. | Dashboard/backup suites for disabled, never-run, failed, unavailable, history paging/failure/retry; actual admin browser checks |

No data migration, stored-grade rewrite, new dependency, new permission, provider call or public endpoint change.

## Verification

| Check | Fresh result |
|---|---|
| Regression RED | Three grading hook assertions failed on the original form; four academics invocations failed on original reads; subject-context/backup run produced five expected failures. Re-review controls and legacy partial/summary-only metadata regressions also failed before their fixes. Initial test-signature compilation errors were corrected before claiming backend RED evidence. |
| Backend affected suites | 411 distinct tests, zero failures/errors/skips in current Surefire XML. Initial 410-case run includes real PostgreSQL/Testcontainers account, evaluation and visibility repository tests; final AcademicsServiceTest rerun adds the denied-target-lock case (28 cases passed). |
| Frontend affected suites | 24 files, 189/189 passed with `--maxWorkers=1`, covering academics, grading and admin dashboard. |
| TypeScript | `npm run typecheck` passed after final source changes. |
| Lint | Full lint: zero errors, one pre-existing unused `SlotOutlet` import warning in `materials/pages/LearningMaterialPage.tsx`. Final focused ESLint for changed grading/dashboard source/tests: clean. |
| Production | Final `NEXT_DIST_DIR=.next-build-u05-u09-u15 npm run build` passed, generated 30 static/dynamic routes. Build-generated tsconfig formatting/include changes restored to HEAD afterward. |
| Diff | `git diff --check` passed; no data/credential files included. |
| Review | Read-only fresh reviewer found one Important legacy metadata scope issue; two failing cases pinned it, fix limits metadata to retained feedback items. Reviewer rechecked and reported no remaining Critical/Important findings. |

Docker was unavailable inside the restricted sandbox but worked with an approved escalated test run. PostgreSQL containers were disposable and used the application's migrations/role grants; application databases and live grades were not modified.

## Browser evidence and limits

- Opened `http://localhost:3000/admin` with the existing local test admin. Confirmed disabled status, never-run and never-success messages, zero recorded failures, settings link and on-demand empty history. Reloaded to check the final inline empty history; no duplicate empty pagination text remained.
- Inspected a desktop screenshot and measured document scroll width 2122 against viewport width 2133 (no page horizontal overflow). Toggled dark/light and restored light. No real mobile viewport or complete visual acceptance in both themes is claimed.
- U05 concurrency was exercised against real PostgreSQL. The local application backend was not restarted or deployed; no live manager enrollment was created to test the new service.
- Grading mutations were tested with service/component fixtures; no live grade save/finalize/publish or real model-quality evaluation was performed. SUBJECT context negative/stale behavior was verified by automated API-mocked UI cases, not a newly created subject assignment.
- Existing dashboard chart inline-style CSP warnings appeared in the running development UI; this patch adds no inline styles and does not claim to fix that separate behavior. No broad role journey, load test or full-project suite rerun.

## Compliance

Existing security scope retained: authentication/role/resource checks, no widening of class/subject access, safe projected backup metadata, current account eligibility and server-authoritative grade review/version validation. Existing resilience scope retained: failed metadata stays unavailable, history failure does not suppress dashboard data, stale/aborted responses are ignored. Infrastructure/deployment controls are unchanged/N/A; property-based testing remains disabled by the recorded project decision.

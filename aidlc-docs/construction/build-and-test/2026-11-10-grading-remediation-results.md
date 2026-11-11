# Grading remediation results — 2026-11-10

Completed the reviewed Antigravity continuation sequentially, without subagents, on fix/u05-u09-u15. Restored the 37-file stash onto matching baseline d0e56561; original stash e9dd4bdd is retained. All changes are left unstaged and uncommitted.

[Continuation plan](../plans/code-generation-plan/2026-11-10-grading-remediation-continuation.md) · [Original plan](../plans/code-generation-plan/2026-11-10-antigravity-grading-remediation-original.md) · [Evidence receipt](2026-11-10-grading-remediation-evidence.json).

## Implementation against the original plan

| IDs | Implemented behavior |
|---|---|
| P0-01 | No fabricated missing AI scores/evidence, erasing unknown anchors, clamping or rounding invalid precision into valid scores. Supplied totals survive empty-rubric merging for validation; JSON decimal parsing preserves exact precision. Deterministic zeros for known empty parts remain supported. |
| P0-02 | Structured feedback editing/acknowledgement is wired. Batch publication retries only successfully finalized items; individual failures and incomplete responses remain visible. |
| P0-03, P0-04 | Untouched groups do not generate empty deadline submissions; deadline closing preserves the leader receipt. Resubmission resets drafts while retaining FINALIZED/PUBLISHED group/member scores and original grading receipts. |
| P1-01, P1-02 | Requested READY proposals fill the inline form without opening a popup or autosaving before teacher action. Partial scores, feedback, adjustment, finalize controls and history are exposed. Historical proposal links keep the existing review flow. |
| P1-03 | Group saves reload all member versions, including overrides. Receipt changes during refresh clear old marks; refresh failures remove stale member controls. |
| P1-04, P1-05 | Draft score corrections append prior score/check/feedback snapshots to history. Effective adjustments match rubric totals after clamping; legacy member overrides use the actual group rubric breakdown. Old unofficial memberships are superseded under identity locks. Optional member reasons and official-grade locks remain. |
| P1-06, P2-01 | Rubric locking removes fully blank rows/criteria, assignment detail includes the whole-assignment rubric, and legacy null-point scoring/display is guarded. |
| P1-07 | Students see published teacher feedback, structured summary and item evidence/reasons/suggestions; group members receive published results through the U08 slot in the U12 submitted tab. |
| P1-08 | Assignment-mode gradebook exposes root submission selection and the existing AI/finalize/publish batch dialog. UI selection supports up to 100 per batch and displays its limit. |
| P1-09, P1-10 | Quiz best/last scores follow each attempt's visibility snapshot; attempt number breaks submission-time ties. Starting rechecks OPEN after the identity lock. |
| P2-02, P2-03 | requestRef accepts 200 characters. Group grading checks document/class/assignment identity, receipt freshness and evidence anchors. |
| P2-04, P2-05 | Closure respects the 30-second grace period; valid individual grace submissions without a late window are not marked late. Active quiz operations require an OPEN class. |
| P2-06, P2-07 | Cross-class subject quiz attempt counts agree with start limits; score columns widen to numeric(10,2). ACTIVE question versions outrank RETIRED versions. |
| P2-08 | Activity observation detects initial/trailing gaps; event batches enforce 1–50 entries and IDs up to 64 characters. Signals do not automatically change scores or declare cheating. |
| P2-09 | Progress reuses roster, contacts and evaluation reads. MCP filters use bulk persisted proposal state without per-row task reconciliation/locks; summaries aggregate authorized listing metadata instead of loading full evaluation details per row. |
| P2-10 | MCP catches argument errors narrowly; infrastructure failures reach the existing safe handler. Frontend retains specific 409 business messages and confirms navigation once. |
| P3-01 | A new least-privilege migration grants SELECT/INSERT/UPDATE and revokes DELETE on the actual grade_distribution_visibility table. |
| P3-02, P3-03 | Zero-token estimates display; anonymous distributions render only server-approved buckets. Practice polling pauses after 60 automatic refreshes at 3 seconds and supports an explicit recheck without a new task. |
| P3-04 | Exact decimal validation, shared score/check consistency and null-point guards are applied; inherited helper extraction remains. No new dependency or HTTP route. |

The original plan named a nonexistent assignment_grade_visibility table and requested four OpenAPI endpoints already present. The new migration uses the real table; existing contract descriptions were reconciled rather than routes duplicated. U07/U08/U09/U11/U12 rules and UI descriptions, requirements and stories are synchronized. Individual resubmission semantics remain unchanged. Published migrations and historical reports/workbooks were not rewritten.

## Fresh final verification

| Check | Result |
|---|---|
| Backend Maven verify | 2,095 tests / 250 suites, zero failures/errors/skips; executable JAR packaged. Finished 2026-11-10 11:21:14 +07. |
| Required guard | Five suites passed: Subjects 31, Notifications 7, Evaluation/privileges 11, Distribution visibility 1, composed workflows 26. All 57 mapped integration cases have passing, non-skipped executions. These overlap with the full suite. |
| Full frontend | 870 tests / 127 files passed with maxWorkers=2; assertions/timeouts were not relaxed. |
| Frontend lint/typecheck/build | All passed; production build generated 32 pages. |
| Source snapshot | 1,637 source/test/config files unchanged after verification; digest in the evidence JSON. |
| Contract/docs | OpenAPI YAML parsed (18 paths); current local links and diff whitespace checked. |

The final Maven log identifies the 250 current suites; their XML reports agree. An obsolete AssignmentAiRevisionTest XML containing nine tests remains in target and is excluded from this run's totals. Logs are listed in the receipt.

Earlier failures remain in separate logs: inherited compile errors; regression RED runs; frontend contention timeouts during overlapping heavy work; and the historical composed test expecting published group scores to be erased. The same traced composed group test now verifies both draft reset and preservation of published scores, identities and original grading receipt. Separate self-review reproduced invalid-decimal rounding, saved-check inconsistency, the post-save receipt race and a legacy override adjustment of +1 instead of -2. Focused GREEN runs preceded final verification; overlapping counts are not added to full-suite totals.

## Applying and remaining limits

Two new migrations ship with the backend: grading/V20261110_1000__grant_grade_distribution_visibility_to_app.sql and quizpractice/V20261110_1010__widen_quiz_attempts_score_columns.sql. Restart the updated backend through the existing process so Flyway applies them. This task did not restart the running API/worker or migrate the user's live database.

The existing Orca browser was at login; authenticated visual/mobile acceptance remains unperformed. No real provider request or live grade write. Historical records are not backfilled. No commit, push or deployment. Original stash retained; all source changes remain available for review.

## Extension scope

SECURITY-03/04/05/08/09/12/15 are retained within the approved change scope: existing logging/header/session controls; parameterized SQL; ownership/permission checks; official-grade locks; strict input/proposal validation; safe global errors and promise handling. No credentials or authentication flow changed. Existing security suites ran; this does not certify deployment controls.

RESILIENCY-04 is N/A (no deployment/pipeline/rollback change). RESILIENCY-06 health behavior is unchanged. RESILIENCY-10 keeps dependency timeouts and adds bounded observation/safe degradation. PBT remains disabled; other extension controls remain outside approved scope.

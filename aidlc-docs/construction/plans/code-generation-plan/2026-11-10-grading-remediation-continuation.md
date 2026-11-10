# Grading remediation continuation

User authorization: “Giúp tôi làm tiếp, tự làm đừng chia subagent”. Execute sequentially in the existing feature checkout; no subagents, commit, push, deployment or live grade writes.

Source plan: [original Antigravity artifact](2026-11-10-antigravity-grading-remediation-original.md), session b11619f9-a269-4758-afc7-b5891b83299e. Baseline d0e56561; restored stash e9dd4bdd without dropping it.

## Scope and decisions

Continue all five waves in the existing Java/JDBC and React implementation, preserving current authorization, official-grade locks and public contracts. Requirements/stories are the existing U06–U12 grading/assignment/practice artifacts. Reuse existing endpoints and components. Q1/Q3 use the plan recommendations: preserve official group grades; batch actions in assignment view. No automatic acceptance of AI grades.

Ruling: official grades remain immutable, including after resubmission; preserve their original receipt/snapshot. Do not implement editing finalized grades despite stale plan wording. Record draft score changes instead.
Ruling: the four OpenAPI endpoints already exist; reconcile descriptions rather than duplicate them.
Ruling: no independent agent review, per explicit user instruction; perform a separate self-review.
Ruling: track this continuation here rather than alter the external original. Keep inherited changes until verified; fix root causes with focused regression tests.

## Execution

- [x] 1. Inspect plan, HEAD and stash; restore 37 files while preserving stash.
- [x] 2. Backend grading: official-grade lifecycle, member transfer, history/adjustments, rubric validation, requestRef and group evidence; regression tests.
- [x] 3. Proposal normalization, bulk read paths and MCP error handling; migration permissions.
- [x] 4. Verify inherited rubric/submission/quiz changes; complete initial activity gap handling and tests.
- [x] 5. Frontend member versions, student feedback/group results/distribution, batch controls and bounded polling; regression tests.
- [x] 6. Run backend/frontend checks, fix failures and self-review the complete diff.
- [x] 7. Reconcile current AI-DLC/contracts; record exact test results, remaining limits and extension compliance.

Security scope SECURITY-03/04/05/08/09/12/15 and Resiliency RESILIENCY-04/06/10 retained. PBT disabled by project configuration. Preserve trust-boundary validation, transactions and privacy.

## Implementation checkpoints

- Restored code initially failed compilation: nonexistent RubricItemView, wrong forbidden overload, nonexistent ApiException.details and List/Set mismatch in the question repository test. Corrected against current contracts.
- RED: official group receipt preservation/history/adjustments/AI-total tests failed; GREEN: 17 tests. Expanded implementation including migrations passed 751 scoped backend tests with zero skips.
- Final self-review found and reproduced precision rounding, inconsistent saved checks, and the frontend group receipt refresh race. Corrected; 65 focused backend tests and 194 frontend tests/25 files passed with lint/typecheck. Full final verification remains step 6 until fresh outputs are recorded.
- Ruling: use the actual grade_distribution_visibility table. Preserve existing migration files; add the grants migration and numeric quiz widening migration only.
- Ruling: retain optional member override reasons (BR-U09-51), official-grade locks and existing individual resubmission semantics; the original plan does not authorize reopening official grades.
- Ruling: preserve existing oversized service files instead of unrelated wholesale splitting. New components/helpers stay compact; inherited extraction is retained. This avoids expanding a bug-remediation task into a structural rewrite.
- Ruling: bounded polling pauses after 60 automatic refreshes (3 seconds each), with an explicit recheck; it does not cancel or resubmit an AI task.
- Browser check: the only existing Orca page is at login. No account/session alteration, live grading action or provider call is used for acceptance.

- Final full backend pass initially ran 2,094 tests with one failure: the historical composed group test expected published scores to be erased. Expanded that same traced test to check both draft resets and preservation of published grades, identities and original grading receipt. Historical test reports remain unchanged.
- Self-review also reproduced an old-data member override bug (expected adjustment -2, actual +1). Override now receives the actual rubric score calculated from the group snapshot, rather than deriving it from the legacy member adjustment. Optional member reasons are preserved.
- Final frontend verification: 870 tests/127 files, full lint, standalone typecheck and production build passed. JS DOM navigation/scroll warnings are environment limitations; no authenticated browser grading acceptance is claimed.

Final verification: backend 2,095/0 failures/errors/skips and JAR; five-suite/57-case guard; frontend 870/127 files plus lint/typecheck/build passed. Source snapshot unchanged. [Results and applying](../../build-and-test/2026-11-10-grading-remediation-results.md). No subagents, live restart/migration, commit, push or deployment.

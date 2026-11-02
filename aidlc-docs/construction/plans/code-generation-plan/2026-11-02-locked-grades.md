# Locked grades with existing resubmission storage

Latest user authorization: "tôi nghĩ để cái cũ đi nộp lại thì bỏ điểm cũ". This supersedes separate official grading rounds on resubmission. This plan is the construction source of truth.

- [x] Step 1: Update rules, requirements, use cases and API docs: official grades cannot be edited; resubmission invalidates the previous grade.
- [x] Step 2: Preserve domain/backend/UI locks for manual, AI-applied and member edits. Keep current FINALIZED publication allowed.
- [x] Step 3: Restore existing group lifecycle, contract, query consumers and unique indexes; remove the unexecuted migration and extra snapshot/current fields. Reset existing group root/member grades to PENDING on resubmission.
- [x] Step 4: Supersede all old individual grades on resubmission, including PUBLISHED, so the old score is not student-visible or used in current aggregates. Retain audit/history without deleting rows.
- [x] Step 5: Verify changed lifecycle, reset, grade visibility, locking and UI; remove the deleted migration from generated backend resources. Full backend 1,741 pass/zero skipped, required PostgreSQL suites pass; grading frontend 118 pass, TypeScript/scoped lint/production build pass. Independent recheck: no remaining Critical/Important findings. No live migration/restart/commit/push.

Scope: U09 grade locking and resubmission; U12 existing submit contract unchanged. No new dependency, DB schema/index change, unlock/regrade feature or parallel group grading rounds.

Existing authorization, optimistic versions, identity/row locks and transaction/fail-closed boundaries remain. Security/resiliency scope retained; PBT disabled.

[Results](../../build-and-test/2026-11-02-locked-grades-results.md).

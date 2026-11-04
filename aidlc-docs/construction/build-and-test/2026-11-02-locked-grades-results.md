# Locked grades; old grade invalidated on resubmission

Branch: fix/u05-u09-u15, starting HEAD d857fd7f. Latest user decision: "tôi nghĩ để cái cũ đi nộp lại thì bỏ điểm cũ".

## Current behavior

- Manual/API/AI/member edits to FINALIZED/PUBLISHED remain rejected before mutation (409 GRADE_LOCKED), including requests with a reason. Current FINALIZED publication still works.
- UI is read-only after finalization; no correction-reason field. Reload after another session finalizes takes the official server values.
- Individual resubmission makes all prior grades SUPERSEDED, including published ones. They are no longer returned as student-visible grades or current gradebook/distribution values.
- Group resubmission reuses the original root/member identities and resets current score, checks, feedback, adjustment, publication timestamps and AI review metadata to PENDING. Existing history/audit is retained for traceability, not as an active grade.
- Removed proposed group-round migration, extra snapshot/current flags and related contract/query changes. No schema/index change is required.

## Current verification

- Full backend: 1,741 tests across 229 suites passed, zero failures/errors/skips. Required PostgreSQL suites passed (SubjectRepositoryIntegrationTest 31; NotificationDeliveryIntegrationTest 2).
- Fresh grading frontend: 118 tests / 12 files passed; TypeScript passed. Covers edit locks plus reset of stale local grading values/AI selection when the same group evaluation ID gets a new receipt. The new receipt-reload regression failed before its fix and passed afterward. Same-receipt conflict preservation still passes.
- Independent reviewer identified the stale local form on group resubmission; corrected it and received a targeted recheck with no remaining Critical/Important findings.
- Scoped ESLint on all changed grading source/tests passed with zero warnings/errors. Production frontend build passed, exit 0; restored only Next-generated tsconfig changes.
- Tests cover both FINALIZED/PUBLISHED group reset using the same root/member IDs, clearing points/publication/feedback/adjustment, individual supersession, student result visibility and manual/AI/member edit locks.
- Maven clean could not run offline because its plugin was not cached. Removed only the generated copy of the discarded SQL migration, verified source/target copies absent, then successfully ran the full Maven test suite. No schema/index migration is part of the current diff.

The previous implementation passed 1,743 backend cases (full execution plus corrected-fixture suite rerun), 710 frontend tests, typecheck/lint/build and independent review. That evidence is historical for the superseded retain-old-grading-rounds implementation; it is not the current reset-policy acceptance.

Current ignored logs: .tmp/locked-reset-backend-final.log, .tmp/locked-reset-frontend-final.log, .tmp/locked-reset-reload-red.log, .tmp/locked-reset-build-final.log.

The initial reset-policy build worker exited with Windows native code 3221226505 while generating static pages with 23 workers. The final build passed with CIRCLE_NODE_TOTAL=2 (one build worker, confirmed in installed Next source), a bounded Node heap and isolated NEXT_DIST_DIR; these are process-local environment settings, not committed configuration changes.

## Limits

No live grade mutation, DB migration, backend restart, commit, push or deployment. Historical audit/grade-change records are not deleted. Existing submitted individual attempt records remain, with their grades invalidated after resubmission. Rechecking complete external-provider/browser/load acceptance is out of scope.

The running backend still needs updated code on restart to enforce the rule. No migration step is needed for this change. Security/resiliency scope retained, PBT disabled.

## Local commit authorization

After verification the user requested "giúp tôi commit". Include this implementation, tests, API/AI-DLC documentation and verification evidence in a local English commit on fix/u05-u09-u15. No push, live migration, backend restart or deployment authorized by that request.

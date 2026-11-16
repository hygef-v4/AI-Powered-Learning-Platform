# Locked grades; resubmission invalidates previous grades

> Superseded. The grade lock in rule 1 (`GRADE_LOCKED`) was withdrawn on 2026-11-11: published grades can be corrected. The group rule in point 5 was replaced on 2026-11-10 and grading was tied to closed assignments on 2026-11-15. Current rules: [U09 business rules](../../construction/u09-assignment-grading/functional-design/business-rules.md) (BR-U09-10, 42, 50, 51). The text below is kept as the record of the 2026-11-02 decision.

Latest user decision: "tôi nghĩ để cái cũ đi nộp lại thì bỏ điểm cũ". This supersedes the earlier choice to retain separate official group grading rounds. Keep the existing DB design; remove the unexecuted group-round migration.

1. Current PENDING/DRAFT can be edited. FINALIZED/PUBLISHED rubric, score, adjustments, feedback and member grades cannot be manually edited or changed by applying AI, even with a reason: HTTP 409 GRADE_LOCKED.
2. A current FINALIZED evaluation can be published; this changes visibility only. No unlock or correction workflow.
3. Resubmission remains allowed under existing permissions/deadlines. It invalidates the previous grade; the new submission must be graded again.
4. Individual submissions retain the existing attempt model: all previous evaluations become SUPERSEDED, including FINALIZED/PUBLISHED. They are excluded from the gradebook, distributions and student-visible grade results. A new evaluation starts PENDING.
5. Group submissions reuse existing root/member rows and reset their current scores, feedback, adjustments, publication and AI review metadata to PENDING. Prior score history/audit remains for traceability; it is not an active official grade. No parallel group snapshots or grading rounds.
6. No schema/index migration, no live DB rewrite or deletion of historical rows. The earlier proposed V20261102_1900 migration was never applied to the live DB and is removed.

Acceptance: reject manual/AI/member edits for both official states; FINALIZED publication still works; individual resubmission supersedes published scores and hides them from students; group resubmission clears both root/member grades and publication metadata, using the same IDs. Reloading a changed receipt clears stale local marks/AI selection and refreshes proposals; same-receipt conflicts preserve unsaved work. Preserve existing authorization, versions, transactions and submission-window validation.

Traceability: FR-007/008, UC40/42–45, BR-U09-10/42/50/51, BR-U12-42, US-GRD-003/004, US-GRP-006. Existing security/resiliency scope retained; PBT disabled.

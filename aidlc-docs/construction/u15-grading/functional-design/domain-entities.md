# U15 Grading & Gradebook — domain-entities

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 37, 38, 39, 40. **Primary stories:** US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001. **Requirements:** FR-008/009/020/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u15-grading` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- Evaluation: id/submissionRef/classId/studentId/groupRef/kind(PRACTICE/OFFICIAL)/rubricVersion, outcomes/score/feedback/status/version/finalizedBy/publishedBy/timestamps.
- GradingProposal: id/sourceTaskRef/submissionVersion/rubricVersion/providerProvenance/validatedOutcomes/status; acceptance expectedEvaluationVersion.
- GradeHistory immutable: before/after/reason/actor/timestamp; group contribution adjustment explicit bounded per student; no ActivityEvent tables in U15.

## Boundary

proposal validation; manual evaluation; finalize/publish/history/gradebook; reviewWorkActivity(authorizedTeacher, attempt/member) within UC 37 via U11 ActivityQueryPort

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- UC37 submissions/work detail plus activity review assigned teacher R3/R4. Admin or manager only R2 denied. Quiz Practice activity read does not create Gradebook.
- Only human teacher confirms Assignment GRADED official result, finalizes/publishes. AI proposal separate PENDING/READY/ACCEPTED/REJECTED, never automatically final/publish.
- Rubric scores deterministic by checklist/max, linked submission/rubric version; proposal stale if submission changed. Manual override requires reason/audit; batch finalize rejects/informs invalid rows, no silent grading.
- Practice Quiz scorer and Assignment feedback not official; answer reveal follows snapshot policy; Student own published official grades only.
- Group common document score + manual contribution adjustment, bounded and explained; activity/number edits cannot determine contribution or cheating score automatically.
- ActivityQueryPort U11 projection per member/work, after current class/policy check; disabled/no data/unavailable distinct. Does not mutate grading entities from telemetry.

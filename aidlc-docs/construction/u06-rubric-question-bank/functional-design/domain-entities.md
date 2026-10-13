# U06 Rubrics & Subject Quiz Bank — domain-entities

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 46, 56, 57. **Primary stories:** US-QBK-001, US-QBK-002. **Requirements:** FR-016/008.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u06-rubric-question-bank` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- QuizBankQuestion: id/subjectId/version/status/stem/options[{id,text}]/correctOptionIds/maxScore; MCQ validation single/multiple selection theo cấu hình.
- Rubric: id/assignmentPartId/version/checklist[{criterionId,text,points}]/maxScore/lockedAt; RubricSnapshot immutable.
- ScoreResult: rubricVersion/itemOutcomes/total/evidence; không do AI cộng điểm ngoài rule.

## Boundary

rubric CRUD/validate/lock/snapshot; MCQ bank version/select

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- Subject bank chỉ MCQ cho Quiz, owner Subject Manager R2. Teacher chỉ chọn câu ACTIVE môn trong scope; không bank lớp.
- Question version immutable khi được snapshot/published; sửa nháp dùng optimistic version, câu đã dùng retire không phá snapshot.
- Rubric cho từng document part, auto-create empty rubric lúc thêm phần; checklist điểm không âm, tổng tính deterministic. Publication cần rubric đủ; lock/snapshot lúc phát hành.
- Assignment có/không diagram dùng cùng rubric parts; diagram criteria chỉ nếu diagramEnabled. Không bank CODE/solution/test runner.
- Quiz scorer validate option IDs theo snapshot và deterministic keys; chỉ trả answer/feedback đúng reveal policy.

# U06 Rubrics & Subject Quiz Bank — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 36, 46, 47. **Primary stories:** US-QBK-001, US-QBK-002. **Requirements:** FR-016/008.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u06-rubric-question-bank` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- /api/v1/subjects/{id}/quiz-bank; /api/v1/assignments/{id}/rubrics; ScorePort and RubricSnapshotPort.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- Subject bank chỉ MCQ cho Quiz, owner Subject Manager R2. Teacher chỉ chọn câu ACTIVE môn trong scope; không bank lớp.
- Question version immutable khi được snapshot/published; sửa nháp dùng optimistic version, câu đã dùng retire không phá snapshot.
- Rubric cho từng document part, auto-create empty rubric lúc thêm phần; checklist điểm không âm, tổng tính deterministic. Publication cần rubric đủ; lock/snapshot lúc phát hành.
- Assignment có/không diagram dùng cùng rubric parts; diagram criteria chỉ nếu diagramEnabled. Không bank CODE/solution/test runner.
- Quiz scorer validate option IDs theo snapshot và deterministic keys; chỉ trả answer/feedback đúng reveal policy.

## UI acceptance

- No invalid correct keys/negative scores; archived question cannot edit published snapshots; rubric lock.
- Same answer snapshot same Quiz score; Student context excludes hidden keys; diagram-off disallows diagram rubric requirement.

# U06 Rubrics & Subject Quiz Bank — business-rules

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 46, 56, 57. **Primary stories:** US-QBK-001, US-QBK-002. **Requirements:** FR-016/008.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u06-rubric-question-bank` không quyết định chức năng hiện hành.

## Quy tắc nghiệp vụ

- **BR-U06-01**: Subject bank chỉ MCQ cho Quiz, owner Subject Manager R2. Teacher chỉ chọn câu ACTIVE môn trong scope; không bank lớp.
- **BR-U06-02**: Question version immutable khi được snapshot/published; sửa nháp dùng optimistic version, câu đã dùng retire không phá snapshot.
- **BR-U06-03**: Rubric cho từng document part, auto-create empty rubric lúc thêm phần; checklist điểm không âm, tổng tính deterministic. Publication cần rubric đủ; lock/snapshot lúc phát hành.
- **BR-U06-04**: Assignment có/không diagram dùng cùng rubric parts; diagram criteria chỉ nếu diagramEnabled. Không bank CODE/solution/test runner.
- **BR-U06-05**: Quiz scorer validate option IDs theo snapshot và deterministic keys; chỉ trả answer/feedback đúng reveal policy.
- **BR-U06-06**: Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- **BR-U06-07**: expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- **BR-U06-08**: Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- **BR-U06-09**: No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Negative scenarios / acceptance

- No invalid correct keys/negative scores; archived question cannot edit published snapshots; rubric lock.
- Same answer snapshot same Quiz score; Student context excludes hidden keys; diagram-off disallows diagram rubric requirement.

# U06 Rubrics & Subject Quiz Bank — business-logic-model

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 36, 46, 47. **Primary stories:** US-QBK-001, US-QBK-002. **Requirements:** FR-016/008.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u06-rubric-question-bank` không quyết định chức năng hiện hành.

## Luồng xử lý

- Manager draft MCQ → validate options/key → publish/retire; author copies version snapshot into Quiz.
- Document part add → empty rubric → educator fills checklist → validation/lock → scorer computes sum from validated item decisions.

## Public contracts

rubric CRUD/validate/lock/snapshot; MCQ bank version/select

## Invariants và failure

- Subject bank chỉ MCQ cho Quiz, owner Subject Manager R2. Teacher chỉ chọn câu ACTIVE môn trong scope; không bank lớp.
- Question version immutable khi được snapshot/published; sửa nháp dùng optimistic version, câu đã dùng retire không phá snapshot.
- Rubric cho từng document part, auto-create empty rubric lúc thêm phần; checklist điểm không âm, tổng tính deterministic. Publication cần rubric đủ; lock/snapshot lúc phát hành.
- Assignment có/không diagram dùng cùng rubric parts; diagram criteria chỉ nếu diagramEnabled. Không bank CODE/solution/test runner.
- Quiz scorer validate option IDs theo snapshot và deterministic keys; chỉ trả answer/feedback đúng reveal policy.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm nghiệm

- No invalid correct keys/negative scores; archived question cannot edit published snapshots; rubric lock.
- Same answer snapshot same Quiz score; Student context excludes hidden keys; diagram-off disallows diagram rubric requirement.

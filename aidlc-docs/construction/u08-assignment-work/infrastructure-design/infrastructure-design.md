# U08 Assignment Work — infrastructure-design

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, tool MCP), `worker` (scanner, handler phản hồi AI) |
| Bảng | `assignment_attempts`, `practice_feedback`, `activity_events` trong `postgres` |
| Migration | `db/migration/assignmentwork/V20260925_1400__create_assignment_work_tables.sql`: `assignment_attempts` (unique `(assignment_id, account_id, attempt_no)`, unique riêng phần `(assignment_id, account_id)` với `IN_PROGRESS`, index `(status, deadline_at)`, `(assignment_id, class_id, status)`; FK `assignment_id` → `assignments`, `class_id` → `course_classes`, `account_id` → `accounts`); `practice_feedback` (index `(attempt_id, created_at)`, `(account_id, created_at)`, unique `request_ref`); `activity_events` (unique `(account_id, work_kind, work_id, client_event_id)`, index `(work_kind, work_id, account_id, server_time)`, `(server_time)`); chạy sau `assignments` (1300) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE` trên ba bảng; `DELETE` chỉ trên `activity_events` (scanner dọn) |
| Settings | `activity.retentionDays` nhóm `ACTIVITY` (1–365, không có mặc định) khai báo qua `SettingDefinition` |
| Nginx | Giới hạn body cho lưu nháp 20 MB, nhập DOCX 21 MB |
| Redis | Bộ đếm giới hạn sự kiện và yêu cầu phản hồi |
| Mạng ra | Không có |

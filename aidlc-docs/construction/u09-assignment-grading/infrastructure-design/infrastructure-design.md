# U09 Assignment Grading — infrastructure-design

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, xuất, tool MCP), `worker` (handler đề xuất AI) |
| Bảng | `evaluations`, `grading_proposals` trong `postgres` |
| Migration | `db/migration/grading/V20260925_1500__create_grading_tables.sql`: `evaluations` (unique `attempt_id`, unique riêng phần `(group_document_id, account_id)` với `MEMBER`, unique riêng phần `group_document_id` với `GROUP_DOCUMENT`, index `(assignment_id, class_id, status)`, `(class_id, account_id)`; FK `assignment_id` → `assignments`, `class_id` → `course_classes`, `attempt_id` → `assignment_attempts`, `account_id` → `accounts`); `grading_proposals` (unique `request_ref`, index `(evaluation_id, version_no)`); chạy sau `assignmentwork` (1400) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE` trên hai bảng; không `DELETE` |
| Sự kiện | `grade.published` qua exchange `platform.events` của U03 |
| Tệp xuất | Ghi luồng vào response, không lưu |
| Mạng ra | Không có |

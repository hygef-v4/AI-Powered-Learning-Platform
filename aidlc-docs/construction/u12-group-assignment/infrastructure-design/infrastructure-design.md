# U12 Group Assignment — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, SSE), `worker` (job tạo tài liệu, scanner tự nộp) |
| Bảng | `class_groups`, `class_group_members`, `leader_change_requests`, `group_documents`, `group_document_sections`, `group_document_revisions` trong `postgres` |
| Migration | `db/migration/groups/V20260925_1800__create_group_tables.sql`: unique `(class_id, name)` với nhóm, `(class_id, account_id)` với thành viên, riêng phần `group_id` với yêu cầu `PENDING`, `(group_id, assignment_id)` với tài liệu, `(group_document_id, part_id)` với mục; index `(status, assignment_id)` tài liệu, `(section_id, created_at)` lịch sử; FK tới `course_classes`, `accounts`, `assignments`; chạy sau `quizpractice` (1700) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`; `DELETE` chỉ trên `class_group_members` và nhóm chưa dùng (kiểm trong code) |
| Việc nền | Job `GROUP_DOC_CREATE` trên queue `jobs.triggered` của U03 |
| RabbitMQ | Exchange fanout `platform.realtime`; mỗi instance backend một queue tạm tự xóa |
| Nginx | `location` cho `/api/v1/group-docs/*/events` và kênh thông báo: `proxy_buffering off`, `proxy_read_timeout 1h` |
| Mạng ra | Không có |

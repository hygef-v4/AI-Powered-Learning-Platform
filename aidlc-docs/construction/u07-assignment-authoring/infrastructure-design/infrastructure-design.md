# U07 Assignment Authoring — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, DOCX, MCP tool), `worker` (scanner, handler kết quả AI) |
| Bảng | `assignments`, `assignment_rubrics`, `assignment_ai_drafts` trong `postgres` |
| Migration | `db/migration/assignments/V20260925_1300__create_assignment_tables.sql`: `assignments` (index `(class_id, status)`, `(subject_id, scope, status)`, `(status, opens_at)`, `(status, closes_at)`; unique riêng phần `(source_assignment_id)` với `origin = 'NEW_VERSION'` và trạng thái `DRAFT`, `REVIEWED`; FK `class_id` → `course_classes`, `subject_id` → `subjects`, `created_by` → `accounts`); `assignment_rubrics` (unique `(assignment_id, part_id)`, FK `assignment_id`); `assignment_ai_drafts` (index `(assignment_id, status)`); chạy sau `ai` (1200) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE` trên ba bảng; `DELETE` chỉ trên `assignment_rubrics`, `assignment_ai_drafts` và dòng `assignments` còn `DRAFT` hoặc `REVIEWED` (kiểm trong code) |
| Sự kiện | `assignment.opened` qua exchange `platform.events` của U03 |
| Tệp | Ảnh trong khung qua U03 `DOCUMENT_IMAGE`; DOCX không lưu |
| Nginx, CSP | Giữ `frame-src https://embed.diagrams.net`; giới hạn body cho API nhập DOCX 21 MB |
| Mạng ra | Trình duyệt tải Draw.io từ `embed.diagrams.net`; backend không gọi dịch vụ ngoài |

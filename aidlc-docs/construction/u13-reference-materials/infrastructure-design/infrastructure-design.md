# U13 Reference Materials — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, tool MCP), `worker` (trích chữ) |
| Bảng | `materials`, `material_versions` trong `postgres` |
| Migration | `db/migration/materials/V20260925_1900__create_material_tables.sql`: `materials` (index `(subject_id, scope, status, order_no)`, `(class_id, status, order_no)`; FK `subject_id` → `subjects`, `class_id` → `course_classes`); `material_versions` (khóa `(material_id, version_no)`, index `text_status`); chạy sau `groups` (1800) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`; không `DELETE` |
| Việc nền | Job `MATERIAL_TEXT_EXTRACT` trên queue `jobs.triggered` của U03 |
| Tệp | Purpose `MATERIAL` của U03 trên Google Drive |
| CSP | Giữ `frame-src https://www.youtube-nocookie.com` |
| Mạng ra | Không có (Drive do U03 gọi) |

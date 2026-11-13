# U13 Reference Materials — infrastructure-design

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, tool MCP), `worker` (trích chữ) |
| Bảng | `materials`, `material_versions`, `material_modules`, `material_module_quizzes` trong `postgres` |
| Migration | `db/migration/materials/V20260925_1900__create_material_tables.sql`: `materials` (index `(subject_id, scope, status, order_no)`, `(class_id, status, order_no)`; FK `subject_id` → `subjects`, `class_id` → `course_classes`); `material_versions` (khóa `(material_id, version_no)`, index `text_status`); chạy sau `groups` (1800) |
| Quyền database | Base material tables retain their scoped grants; migration 29/10 grants app SELECT/INSERT/UPDATE/DELETE on material_modules and material_module_quizzes for module/link deletion |
| Việc nền | Job `MATERIAL_TEXT_EXTRACT` trên queue `jobs.triggered` của U03 |
| Tệp | Purpose `MATERIAL` của U03 trên Google Drive |
| CSP | Giữ `frame-src https://www.youtube-nocookie.com` |
| Mạng ra | Không có (Drive do U03 gọi) |

## Module migration — 2026-10-29

V20261029_1000__create_material_modules.sql adds material_modules and material_module_quizzes, backfills old materials into Module1 and sets materials.module_id NOT NULL. Quiz unique link, module-link delete cascade and same-subject service check must be documented together. No migration executed against a live stack by this documentation sync.

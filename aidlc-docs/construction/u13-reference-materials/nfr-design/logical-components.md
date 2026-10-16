# U13 Reference Materials — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `SubjectMaterialController`, `ClassMaterialController`, `MaterialController`, `MyMaterialController` | backend | API mục 4 của domain-entities |
| `MaterialService` | backend | Thêm, sửa, phiên bản, lưu trữ (F2, F4, P1) |
| `MaterialQueryService`, `MaterialAccessPolicy` | backend | Danh sách, xem, token tải (F1, F3, F5, P3) |
| `MaterialTextExtractJobHandler`, `MaterialTextExtractor` | worker | Trích chữ (F6, P2) |
| `ReferenceMaterialService` (cài `IReferenceMaterial`), `MaterialMcpTools` | backend | Nguồn cho AI, tool MCP (F7) |
| Repository `materials`, `material_versions` | backend, worker | Chỉ dùng trong package |

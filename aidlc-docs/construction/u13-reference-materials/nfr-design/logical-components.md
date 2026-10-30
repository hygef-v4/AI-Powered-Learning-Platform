# U13 Reference Materials — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `SubjectMaterialController`, `ClassMaterialController`, `MaterialController`, `MyMaterialController`, `MaterialModuleController` | backend | API mục 4 của domain-entities |
| `MaterialModuleService`, `MaterialModuleRepositoryAdapter` | backend | Module create/rename/delete/reorder, Quiz attach/detach, CAS and scoped visibility |
| `MaterialService` | backend | Thêm, sửa, phiên bản, lưu trữ (F2, F4, P1) |
| `MaterialQueryService`, `MaterialAccessPolicy` | backend | Danh sách, xem, token tải (F1, F3, F5, P3) |
| `MaterialTextExtractJobHandler`, `MaterialTextExtractor` | worker | Trích chữ (F6, P2) |
| `ReferenceMaterialService` (cài `IReferenceMaterial`), `MaterialMcpTools` | backend | Nguồn cho AI, tool MCP (F7) |
| Repository `materials`, `material_versions`, `material_modules`, `material_module_quizzes` | backend, worker | Chỉ dùng trong package |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

# U07 Assignment Authoring — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ClassAssignmentController`, `SubjectAssignmentController`, `AssignmentController`, `RubricController`, `AssignmentAiDraftController` | backend | API mục 6 của domain-entities |
| `AssignmentAuthoringService` | backend | Tạo, sửa, xóa, duyệt (F3, F4, F8, F14) |
| `AssignmentPublicationService` | backend | Phát hành, sửa lịch, ngưng giao (F9, F11) |
| `AssignmentCopyService` | backend | Nhân bản, version mới, copy giữa lớp (F12, F13) |
| `SkeletonService`, `PartCalculator`, `DocumentModelService` (cài `IDocumentModel`) | backend | Khung, phần, kiểm tài liệu (P1) |
| `RubricService` (cài `IRubric`) | backend | Rubric, khóa, nhân bản, tính điểm (F5) |
| `DiagramXmlValidator`, `SvgSanitizer` | backend | Kiểm XML, làm sạch SVG |
| `DocxExporter`, `DocxImporter` (cài `IDocx`) | backend | Xuất, nhập DOCX (F7, P4) |
| `AssignmentAiDraftService`, `AssignmentDraftResultHandler` | backend, worker | AI soạn bản nháp (F6, P5) |
| `AssignmentMcpTools` (cài `IMcpToolProvider`) | backend | Tool MCP (F15) |
| `AssignmentQueryService` (cài `IAssignmentQuery`) | backend | Interface cho unit khác (F16) |
| `AssignmentScheduleScanner` (cài `IScheduledScanner`) | worker | Mở, đóng theo lịch (F10, P3) |
| Repository `assignments`, `assignment_rubrics`, `assignment_ai_drafts` | backend, worker | Chỉ dùng trong package |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

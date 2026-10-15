# U07 Assignment Authoring — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ClassAssignmentController`, `SubjectAssignmentController`, `AssignmentController`, `RubricController`, `AssignmentAiDraftController` | backend | API mục 6 của domain-entities |
| `AssignmentAuthoringService` | backend | Tạo, sửa, xóa, duyệt (F3, F4, F8, F14) |
| `AssignmentPublicationService` | backend | Phát hành, sửa lịch, ngưng giao (F9, F11) |
| `AssignmentCopyService` | backend | Nhân bản, version mới, copy giữa lớp (F12, F13) |
| `SkeletonService`, `PartCalculator`, `DocumentModelService` (cài `DocumentModelPort`) | backend | Khung, phần, kiểm tài liệu (P1) |
| `RubricService` (cài `RubricPort`) | backend | Rubric, khóa, nhân bản, tính điểm (F5) |
| `DiagramXmlValidator`, `SvgSanitizer` | backend | Kiểm XML, làm sạch SVG |
| `DocxExporter`, `DocxImporter` (cài `DocxPort`) | backend | Xuất, nhập DOCX (F7, P4) |
| `AssignmentAiDraftService`, `AssignmentDraftResultHandler` | backend, worker | AI soạn bản nháp (F6, P5) |
| `AssignmentMcpTools` (cài `McpToolProvider`) | backend | Tool MCP (F15) |
| `AssignmentQueryService` (cài `AssignmentQueryPort`) | backend | Port cho unit khác (F16) |
| `AssignmentScheduleScanner` (cài `ScheduledScanner`) | worker | Mở, đóng theo lịch (F10, P3) |
| Repository `assignments`, `assignment_rubrics`, `assignment_ai_drafts` | backend, worker | Chỉ dùng trong package |

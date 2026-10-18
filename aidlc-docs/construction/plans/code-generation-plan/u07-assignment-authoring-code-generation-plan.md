# U07 Assignment Authoring — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Thiết kế: [business-logic-model](../../u07-assignment-authoring/functional-design/business-logic-model.md), [business-rules](../../u07-assignment-authoring/functional-design/business-rules.md), [domain-entities](../../u07-assignment-authoring/functional-design/domain-entities.md), [frontend-components](../../u07-assignment-authoring/functional-design/frontend-components.md), [NFR](../../u07-assignment-authoring/nfr-requirements/nfr-requirements.md), [NFR design](../../u07-assignment-authoring/nfr-design/nfr-design-patterns.md), [infrastructure](../../u07-assignment-authoring/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/assignments/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../assignments/contract/` | interface `IAssignmentLifecycleListener`, `IAssignmentQuery`, `IDiagramLinker`, `IDiagramResolver`, `IDocumentModel`, `IDocx`, `IGroupAssignmentSupport`, `IRubric`; kiểu dữ liệu `AssignmentContent`, `AssignmentScope`, `AssignmentStatus`, `AssignmentView`, `BlockOrigin`, `BlockType`, `CriterionKind`, `DiagramType`, `DocumentBlock`, `DocumentModel`, `DocumentPart`, `DocxImportPreview`, `GradingMode`, `ReadinessResult`, `RubricCriterion`, `RubricItem`, `RubricScore`, `RubricView`, `SubmissionMode`, `SubmissionWindow`, `ValidationIssue`, `ValidationResult` |
| `backend/.../assignments/contract/stub/AssignmentsContractStubs` | Bản tạm trả dữ liệu mẫu cho `IAssignmentQuery`, `IRubric`, `IDocumentModel`, `IDocx`, `IGroupAssignmentSupport` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/assignments/slots.ts` | Slot đã khai báo: `assignmentRowActions`, `assignmentFormSections` |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/assignments/V20260925_1300__create_assignment_tables.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `Assignment`, `AssignmentRubric`, `AssignmentAiDraft`, `DocumentModel`, `Block`, `Part` và enum; kiểm chuyển trạng thái; repository chỉ dùng trong package.
- [ ] **Bước 3 — Tài liệu:** `DocumentModelService` (cài `IDocumentModel`), `PartCalculator`, `DiagramXmlValidator`, `SvgSanitizer`; `compactDiagramXml`, `toPlainText`.
- [ ] **Bước 4 — Rubric:** `RubricService` (cài `IRubric`): tự tạo, tự xóa theo phần, sửa, kiểm hợp lệ, khóa, nhân bản, tính điểm.
- [ ] **Bước 5 — Soạn bài:** `AssignmentAuthoringService` (F3, F4, F8, F14), `SkeletonService` (P1).
- [ ] **Bước 6 — Phát hành:** `AssignmentPublicationService` (F9, F11, P2), `AssignmentScheduleScanner` (F10, P3), `IAssignmentLifecycleListener`, `IGroupAssignmentSupport`, phát `assignment.opened`.
- [ ] **Bước 7 — Nhân bản, version, copy:** `AssignmentCopyService` (F12, F13).
- [ ] **Bước 8 — DOCX:** `DocxExporter`, `DocxImporter` (cài `IDocx`) theo BR-U07-60…62 và P4; trang xem sơ đồ.
- [ ] **Bước 9 — AI và MCP:** `AssignmentAiDraftService`, `AssignmentDraftResultHandler` (cài `IAiTaskResultHandler` cho `ASSIGNMENT_DRAFT`), dùng `IReferenceMaterial` của U06, `AssignmentMcpTools` (cài `IMcpToolProvider`).
- [ ] **Bước 10 — Interface cho unit khác:** `AssignmentQueryService` (cài `IAssignmentQuery`) gồm `submissionWindow`, `listClosingBetween`.
- [ ] **Bước 11 — API:** controller theo domain-entities mục 6; `contracts/openapi/assignments.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 12 — Frontend:** `DocumentEditor`, `DiagramBlock`, `DocumentViewer`, `RubricViewer` dùng chung; `ClassAssignmentsSection` (đăng ký `TeacherClassTab`), `SubjectAssignmentListPage` (đăng ký `NavEntry`), `AssignmentSummary` (`SubjectSummarySection`), `AssignmentFormPage` và các panel, `RubricDetailDialog`, `DiagramViewPage`; slot `AssignmentRowAction`, `AssignmentFormSection`.
- [ ] **Bước 13 — Unit test (người code viết):** tính phần với các cây heading; tự tạo, xóa rubric theo phần; rubric hợp lệ và tổng điểm; tắt sơ đồ bị từ chối; chuyển trạng thái hợp lệ và không hợp lệ; phát hành khi rubric trống; lịch sai; sửa lịch khi `OPEN`; ngưng giao gọi listener; nhân bản, version, copy không sao lịch và nhóm; một version đang mở; XML có DOCTYPE bị từ chối; SVG có script bị làm sạch; nhập DOCX tìm sơ đồ theo alt text, hyperlink, `mxfile`, rồi giữ là ảnh; xuất DOCX có alt text và `mxfile`; đề xuất AI không sửa thẳng bài, áp dụng lệch `version` trả `409`; tool MCP với người học và giảng viên; component frontend. Integration test do tester viết.
- [ ] **Bước 14 — Review:** đối chiếu UC 34–36, story, BR-U07 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Phát hành và sửa rubric cùng lúc: một bên nhận `409`, rubric không đổi sau khi khóa.
- Scanner mở bài đúng giờ, gọi listener, phát `assignment.opened`; chạy lại không trùng.
- Copy giữa lớp khi không dạy lớp nguồn: "không tìm thấy".
- Xuất đề có sơ đồ, mở và lưu lại bằng Word và bằng Google Docs, nhập lại: sơ đồ được khôi phục.
- Phân quyền 4 role trên mọi endpoint, gồm giảng viên lớp sửa bài của môn và dùng ID bài của lớp khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.

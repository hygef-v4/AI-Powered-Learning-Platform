# U09 Question Type Authoring - Logical Components

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt: DocumentEditor, RubricDetailDialog --postMessage--> embed.diagrams.net
      |
      v
+---------------------------------------------------------------------------------+
| backend                                                                         |
| TypeConfigController --> TypeConfigService (TypeConfigPort cho U08)             |
|                      --> rubric từng câu Text Essay, cài đặt quiz               |
|                      --> RubricPort (U06), AiDraftPort (U13)                    |
| SkeletonController --> SkeletonService --> DocumentValidator, PartBuilder       |
|                                        --> RubricPort (U06), AiDraftPort (U13)  |
| DocxImportController --> DocxImporter --> SafeZipGuard, BlockMapper,            |
|                                          DiagramDetector --> SafeDrawioParser   |
|                                          ArtifactPort (U03 DOCUMENT_IMAGE)      |
| DocxExportService (DocxExportPort) --> JsvgRasterizer, PngChunkWriter           |
| DocumentModelService (DocumentModelPort cho U11, U13, U14, U15)                 |
| DiagramCompactor (DiagramCompactPort cho U13)                                   |
| Ghi qua AssignmentExtensionPort (U08): config                                   |
+---------------------------------------------------------------------------------+
```

**Text alternative**: Trình soạn tài liệu và popup Rubric Detail chạy trong trình duyệt; trình soạn trao đổi với iframe Draw.io qua `postMessage`. Ở backend, `TypeConfigService` quản lý cài đặt quiz, rubric từng câu Text Essay (tự tạo/xóa qua `RubricPort` của U06 khi U08 thêm/bỏ câu) và trả lời kiểm duyệt, nhân bản cho U08; `SkeletonService` lưu khung, chia phần qua `PartBuilder` (tự tạo/xóa rubric từng phần), nhận khung AI đề xuất qua `AiDraftPort` của U13; `DocxImporter` nhập DOCX an toàn và nhận sơ đồ Draw.io nhúng trong ảnh; `DocxExportService` xuất DOCX có nhúng lại XML sơ đồ; `DocumentModelService` kiểm tài liệu cho U11, U13, U14, U15; `DiagramCompactor` rút gọn XML cho U13. Mọi nội dung ghi vào `assignments.config` qua U08.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `TypeConfigService` | F1, F2, F3, F6 bước 2, F7, F8, F9; P8 |
| `SkeletonService`, `PartBuilder` | F4, F6 bước 1; P2, P8 |
| `DocumentValidator`, `DocumentModelService` | F11; P1, P2 |
| `DocxImporter`, `SafeZipGuard`, `BlockMapper`, `DiagramDetector` | F5, F10; P3 |
| `DocxExportService`, `JsvgRasterizer`, `PngChunkWriter` | F12; P4 |
| `SvgSanitizer`, `SafeDrawioParser` | P5, BR-U09-35 |
| `DiagramCompactor` | F13 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U09_DOCX_MAX_BYTES` | 20MB |
| `U09_DOCX_MAX_UNZIPPED_BYTES` | 200MB |
| `U09_DIAGRAM_MAX_XML_BYTES` | 2MB |
| `U09_EXPORT_CONCURRENCY` | 2 |
| `NEXT_PUBLIC_DRAWIO_URL` | `https://embed.diagrams.net` |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung tài liệu |
| SECURITY-04 | Compliant | P6 origin, CSP |
| SECURITY-05 | Compliant | P3, P5 |
| SECURITY-08 | Compliant | P2 khóa phía server |
| SECURITY-15 | Compliant | P3 lỗi → ảnh, P6 timeout |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

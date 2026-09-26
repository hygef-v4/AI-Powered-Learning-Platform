# U09 Question Type Authoring - Logical Components

## 1. Sơ đồ

```
 Trình duyệt: DocumentEditor --postMessage--> iframe embed.diagrams.net
      |
      v
 +------------------------------ backend -------------------------------------+
 | TypeConfigController --> TypeConfigService (TypeConfigPort cho U08)   |
 | SkeletonController --> SkeletonService --> DocumentValidator, SvgSanitizer  |
 | DocxImportController --> DocxImporter --> SafeZipGuard, BlockMapper,       |
 |                                          DiagramDetector --> SafeDrawioParser|
 |                                          ArtifactPort (U03 DOCUMENT_IMAGE)  |
 | DocxExportService (DocxExportPort) --> JsvgRasterizer, PngChunkWriter      |
 | DocumentModelService (DocumentModelPort cho U06, U11, U15)                 |
 | DiagramCompactor (DiagramCompactPort cho U13)                              |
 | Ghi qua AssignmentExtensionPort (U08): type_config, skeleton               |
 +----------------------------------------------------------------------------+
```

**Text alternative**: Trình soạn tài liệu trong trình duyệt trao đổi với iframe Draw.io qua `postMessage`. Ở backend, `TypeConfigService` quản lý cấu hình loại bài và trả lời kiểm duyệt cho U08; `SkeletonService` lưu khung sau khi kiểm và làm sạch SVG; `DocxImporter` nhập DOCX an toàn, nhận sơ đồ Draw.io trong ảnh và lưu ảnh qua U03; `DocxExportService` dựng DOCX với sơ đồ thành PNG có nhúng XML; `DocumentModelService` cho U06, U11, U15 kiểm tài liệu; `DiagramCompactor` rút gọn XML cho U13.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `TypeConfigService` | F1, F4 |
| `SkeletonService` | F2; P2 |
| `DocumentValidator`, `DocumentModelService` | F5; P1, P2 |
| `DocxImporter`, `SafeZipGuard`, `BlockMapper`, `DiagramDetector` | F3; P3 |
| `DocxExportService`, `JsvgRasterizer`, `PngChunkWriter` | F6; P4 |
| `SvgSanitizer`, `SafeDrawioParser` | P5, BR-U09-35 |
| `DiagramCompactor` | F7 |

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

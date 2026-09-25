# U09 Question Type Authoring - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U09. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-ASM-004 (soạn khung, kiểm/xuất tài liệu; phần làm bài ở U11), US-ASM-006, US-ASM-007. US-ASM-002 đã loại.
- **Use case**: UC-ASM-02, 03, 04.
- **Thiết kế nguồn**: `construction/u09-question-type-authoring/` (functional-design, nfr-requirements, nfr-design, infrastructure-design). Tham khảo code: `../demo_do_an` (`DocxOutlineImporter`, `DocxExporter`, `DiagramRasterizer`, `DiagramContentCleaner`, `EssayDocument`).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ArtifactPort` | U03 | Dùng thật (purpose `DOCUMENT_IMAGE`) |
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort` | U06 | Dùng thật (kiểm câu quiz khi duyệt) |
| `AssignmentQueryPort`, `TypeConfigSlot` | U08 | Dùng thật |
| U09 cài `TypeConfigPort` (U08), `DocumentModelPort` (U06, U11, U15) | | Thay adapter tạm của U08 và U06 |

### Dữ liệu U09 sở hữu

PostgreSQL `question_type_config`, `document_skeletons`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u09/
    api/                TypeConfigController, SkeletonController, DocxImportController, DTO
    application/        TypeConfigService, SkeletonService, DocumentModelService,
                        DocxExportService, DiagramCompactor
    document/           Block (sealed) + record, DocumentValidator, SvgSanitizer,
                        SafeDrawioParser, DiagramType
    docx/               SafeZipGuard, DocxImporter, BlockMapper, DiagramDetector,
                        PngChunkReader, PngChunkWriter, JsvgRasterizer
    infrastructure/     JPA repository
    port/               DocumentModelPort, DocxExportPort, DiagramCompactPort
/backend/src/main/resources/db/migration/u09/
/backend/src/test/resources/u09/docx/     bộ DOCX mẫu (NFR-U09-30)
/contracts/schemas/document.json
/frontend/src/shared/document/            DocumentEditor, các block, DrawioPanel, EssayEditor
/frontend/src/app/teaching/assignments/[id]/type-config/
/contracts/openapi/u09-question-type.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - `pom.xml`: `com.github.weisj:jsvg`; POI đã có từ U06. Biến cấu hình U09 theo `logical-components.md` §3; Nginx route nhập DOCX 20 MB.

### Nhóm B - Mô hình tài liệu và logic

- [ ] **Bước 2** - `contracts/schemas/document.json`; Java `Block` sealed + record; sinh kiểu TypeScript từ schema (P1).
- [ ] **Bước 3** - `SafeDrawioParser`, `SvgSanitizer` (P5, BR-U09-35).
- [ ] **Bước 4** - `DocumentValidator`: `validateSkeleton`, `validateForSave` (hash khóa block), `validateForSubmit` (block giảng viên đủ, sơ đồ không rỗng, `requiredDiagrams`, có nội dung người học); ESSAY chỉ block chữ, trần 1 000 000 ký tự (F5, P2, BR-U09-20…21, 30…38).
- [ ] **Bước 5** - `TypeConfigService` và `TypeConfigPort` (`check`, `copy`) cho QUIZ/ESSAY/DOCUMENT (F1, F4, BR-U09-01…03, 10…14).
- [ ] **Bước 6** - `SkeletonService` (lưu khung, hash, làm sạch SVG) (F2).
- [ ] **Bước 7** - Nhập DOCX: `SafeZipGuard`, `DocxImporter`, `BlockMapper`, `PngChunkReader`, `DiagramDetector` (PNG `tEXt`/`zTXt`/`iTXt`, SVG `content`, giải nén diagram nén), báo cáo nhập (F3, P3, BR-U09-40…44).
- [ ] **Bước 8** - Xuất DOCX: `DocxExportService`, `JsvgRasterizer`, `PngChunkWriter`, semaphore 2 (F6, P4, BR-U09-50…52).
- [ ] **Bước 9** - `DiagramCompactor` (F7, BR-U09-60).
- [ ] **Bước 10** - `DocumentModelService` cài `DocumentModelPort`; thay adapter tạm của U06 và U08.
- [ ] **Bước 11** - Unit test mọi `BR-U09-xx`; bộ DOCX mẫu (PNG/SVG draw.io, ảnh thường, ảnh mất XML, bảng gộp ô, textbox, zip bomb); test vòng tròn xuất → nhập.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u09-question-type-authoring/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 13** - Flyway `V20260925_1600__u09_question_type.sql` theo `infrastructure-design.md` §4.
- [ ] **Bước 14** - JPA repository.
- [ ] **Bước 15** - Integration test: sửa cấu hình khi bài không `DRAFT` bị chặn; U08 duyệt gọi `TypeConfigPort` thật.
- [ ] **Bước 16** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 17** - `/contracts/openapi/u09-question-type.yaml`.
- [ ] **Bước 18** - Controller + DTO + validation.
- [ ] **Bước 19** - Test MockMvc: chỉ giảng viên của lớp sửa cấu hình/khung; DOCX quá lớn `413`; lỗi kiểm trả theo `blockId`.
- [ ] **Bước 20** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 21** - `DocumentEditor` (3 chế độ), các block, `LockedBadge`, ảo hóa > 200 block (P7).
- [ ] **Bước 22** - `DrawioPanel` (iframe `embed.diagrams.net`, kiểm origin, timeout 10 s, lưu XML + SVG) (P6).
- [ ] **Bước 23** - `EssayEditor`; `QuizConfigForm`, `DocumentConfigForm` (`SkeletonEditor`, `DocxImportDialog`, `RequiredDiagramsForm`) gắn vào `TypeConfigSlot` của U08.
- [ ] **Bước 24** - Test frontend: chế độ `LEARNER` không sửa/xóa block khóa, bảng/sơ đồ giảng viên sửa được nhưng không xóa, message từ origin lạ bị bỏ qua.
- [ ] **Bước 25** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 26** - Cập nhật `README.md`: xuất sơ đồ từ draw.io kèm "Include a copy of my diagram", tắt "Compress pictures" trong Word để giữ sơ đồ khi nhập; cách U11/U15 dùng `DocumentModelPort`, `DocxExportPort`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-004 S1, S2 (UC-ASM-04) | 2, 3, 4, 6, 7, 21, 22 |
| US-ASM-004 S3 | 9 |
| US-ASM-006 (UC-ASM-03) | 5, 23 |
| US-ASM-007 (UC-ASM-02) | 4, 5, 23 |
| Xuất DOCX | 8 |

## 5. Ngoài phạm vi

- Làm bài và lưu bài nộp (U11), chấm và hiển thị chấm (U15), Code Lab (U13), đề chung cấp môn (đã loại).

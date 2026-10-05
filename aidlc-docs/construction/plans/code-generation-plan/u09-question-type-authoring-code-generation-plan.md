# U09 Question Type Authoring - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U09. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-ASM-004 (soạn khung, kiểm/xuất tài liệu; phần làm bài ở U11), US-ASM-005 (soạn Code Lab), US-ASM-006, US-ASM-007, US-GRP-003 (soạn bài nhóm). Đề chung cấp môn đã loại.
- **Use case**: UC 23, UC 24, UC 25, UC 26, UC 27 (cả năm loại bài, quyết định 2026-10-04); cả năm kế thừa UC 28 Manage Assignments của U08, U09 chỉ làm phần riêng của dạng bài.
- **Wave**: 3. U13 (wave 4) cung cấp `CodeLabCheckPort`, `VerifySolutionButton` và `AiDraftPort` (AI soạn khung) qua `C`: trước khi có U13 dùng adapter giả báo "chưa kiểm được lời giải" và ẩn nút AI soạn khung.
- **Thiết kế nguồn**: `construction/u09-question-type-authoring/` (functional-design, nfr-requirements, nfr-design, infrastructure-design). Tham khảo code: `../demo_do_an` (`DocxOutlineImporter`, `DocxExporter`, `DiagramRasterizer`, `DiagramContentCleaner`, `EssayDocument`).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ArtifactPort` | U03 | Dùng thật (purpose `DOCUMENT_IMAGE`) |
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort`, `RubricPort` | U06 | Dùng thật (kiểm câu quiz khi duyệt; lấy khung `DOCUMENT` từ ngân hàng; tạo, sửa, nhân bản rubric từng câu và từng phần) |
| `CodeLabCheckPort`, `AiDraftPort` | U13 (`C`) | Adapter giả báo "chưa kiểm được lời giải", ẩn nút AI soạn khung; U13 thay |
| `AssignmentQueryPort`, `AssignmentExtensionPort`, `TypeConfigSlot` | U08 | Dùng thật (ghi cấu hình loại bài và khung qua `AssignmentExtensionPort`) |
| U09 cài `TypeConfigPort` (U08), `DocumentModelPort` (U06, U11, U15) | | Thay adapter tạm của U08 và U06 |

### Dữ liệu U09 sở hữu

PostgreSQL: cột `config` của `assignments` (U08 tạo bảng, U09 ghi qua `AssignmentExtensionPort`). Không có bảng hay migration riêng.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  authoring/
    api/                TypeConfigController, SkeletonController, DocxImportController, DTO
    application/        TypeConfigService, SkeletonService, DocumentModelService,
                        DocxExportService, DiagramCompactor
    document/           Block (sealed) + record, DocumentValidator, SvgSanitizer,
                        SafeDrawioParser, DiagramType
    docx/               SafeZipGuard, DocxImporter, BlockMapper, DiagramDetector,
                        PngChunkReader, PngChunkWriter, JsvgRasterizer
    infrastructure/     JPA repository
    port/               DocumentModelPort, DocxExportPort, DiagramCompactPort
/backend/src/test/resources/u09/docx/     bộ DOCX mẫu (NFR-U09-30)
/contracts/schemas/document.json
/frontend/src/shared/document/            DocumentEditor, các block, DrawioPanel, EssayEditor
/frontend/src/app/teaching/assignments/[id]/type-config/
/contracts/openapi/authoring.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `com.github.weisj:jsvg`; POI đã có từ U06. Biến cấu hình U09 theo `logical-components.md` §3; Nginx route nhập DOCX 20 MB.

### Nhóm B - Mô hình tài liệu và logic

- [ ] **Bước 2** - `contracts/schemas/document.json`; Java `Block` sealed + record; sinh kiểu TypeScript từ schema (P1).
- [ ] **Bước 3** - `SafeDrawioParser`, `SvgSanitizer` (P5, BR-U09-35).
- [ ] **Bước 4** - `DocumentValidator`: `validateSkeleton`, `validateForSave` (hash khóa block), `validateForSubmit` (block giảng viên đủ, sơ đồ không rỗng, `requiredDiagrams`, có nội dung người học); ESSAY chỉ block chữ, trần 1 000 000 ký tự (F5, P2, BR-U09-20…21, 30…38).
- [ ] **Bước 5** - `TypeConfigService` và `TypeConfigPort` (`check` gồm rubric từng câu/từng phần, `copy` nhân bản các rubric, `repointRubric`); rubric từng câu Text Essay qua `RubricPort` và đặt điểm câu bằng tổng rubric qua `AssignmentExtensionPort` (F2b, BR-U09-23); cho QUIZ/ESSAY/DOCUMENT/CODE_LAB/GROUP_ASSIGNMENT (Code Lab: ngôn ngữ, file khởi đầu, test, lời giải mẫu; kiểm lời giải qua `CodeLabCheckPort` của U13) (F1, F4, BR-U09-01…03, 10…14).
- [ ] **Bước 6** - `SkeletonService` (lưu khung, hash, làm sạch SVG; lấy khung từ câu `DOCUMENT` của ngân hàng khớp dạng bài qua `BankQueryPort`, giữ nguyên heading); tự chia phần theo heading nhỏ nhất (`PartBuilder`) và rubric từng phần qua `RubricPort` (`config.parts[]`); AI soạn khung qua `AiDraftPort` (xem trước, xác nhận thì thay khung, gợi ý rubric điền sẵn) (F2, F2a, F2c, BR-U06-28, BR-U09-24…28).
- [ ] **Bước 7** - Nhập DOCX: `SafeZipGuard`, `DocxImporter`, `BlockMapper`, `PngChunkReader`, `DiagramDetector` (PNG `tEXt`/`zTXt`/`iTXt`, SVG `content`, giải nén diagram nén), báo cáo nhập. Hỗ trợ cả khung giảng viên và preview block `STUDENT` cho U11 (F3, F3a, P3, BR-U09-40…48).
- [ ] **Bước 8** - Xuất DOCX: `DocxExportService`, `JsvgRasterizer`, `PngChunkWriter`, semaphore 2 (F6, P4, BR-U09-50…52).
- [ ] **Bước 9** - `DiagramCompactor` (F7, BR-U09-60).
- [ ] **Bước 10** - `DocumentModelService` cài `DocumentModelPort`; thay adapter tạm của U06 và U08.
- [ ] **Bước 11** - Unit test mọi `BR-U09-xx` (gồm chia phần, rubric từng phần, nhân bản và chuyển rubric, NFR-U09-33); bộ DOCX mẫu (PNG/SVG draw.io, ảnh thường, ảnh mất XML, bảng gộp ô, textbox, zip bomb); test vòng tròn xuất → nhập.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u09-question-type-authoring/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 13** - Không có migration: kiểm schema JSON của `assignments.config` (cấu hình loại bài, khung tài liệu, `parts[]` kèm `rubricId`) khi đọc/ghi.
- [ ] **Bước 14** - JPA repository.
- [ ] **Bước 15** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: sửa cấu hình khi bài không `DRAFT` bị chặn; U08 duyệt gọi `TypeConfigPort` thật.
- [ ] **Bước 16** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 17** - `/contracts/openapi/authoring.yaml`.
- [ ] **Bước 18** - Controller + DTO + validation.
- [ ] **Bước 19** - Test MockMvc: chỉ giảng viên của lớp sửa cấu hình/khung; DOCX quá lớn `413`; lỗi kiểm trả theo `blockId`.
- [ ] **Bước 20** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 21** - `DocumentEditor` (3 chế độ), các block, `LockedBadge`, ảo hóa > 200 block (P7).
- [ ] **Bước 22** - `DrawioPanel` (iframe `embed.diagrams.net`, kiểm origin, timeout 10 s, lưu XML + SVG) (P6).
- [ ] **Bước 23** - `EssayEditor`, `QuestionRubricPanel` (Text Essay: mỗi câu một rubric); `QuizConfigForm`, `CodeLabConfigForm` (dùng `CodeEditor`, `VerifySolutionButton` của U13), `DocumentConfigForm` cho Diagram Essay và bài nhóm (`SkeletonEditor`, `PartRubricPanel`, `DocxImportDialog`, `AiSkeletonDraftDialog`, `RequiredDiagramsForm` chỉ cho Diagram Essay) gắn vào `TypeConfigSlot` của U08; `StudentDocxImportDialog` dùng trong `DocumentWorkspace` U11.
- [ ] **Bước 24** - Test frontend: chế độ `STUDENT` không sửa/xóa/kéo được bất kỳ block giảng viên nào (kể cả bảng, sơ đồ); người học chèn block của mình được; message từ origin lạ bị bỏ qua.
- [ ] **Bước 25** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 26** - Cập nhật `README.md`: xuất sơ đồ từ draw.io kèm "Include a copy of my diagram", tắt "Compress pictures" trong Word để giữ sơ đồ khi nhập; cách U11/U15 dùng `DocumentModelPort`, `DocxExportPort`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-004 S1, S2 (UC 25) | 2, 3, 4, 5, 6, 7, 21, 22, 23 |
| US-ASM-004 S3 | 9 |
| US-ASM-006 (UC 24) | 5, 23 |
| US-ASM-005 (UC 26) | 5, 23 |
| US-GRP-003 (UC 27, phần soạn bài) | 6, 23 |
| US-ASM-007 (UC 23) | 4, 5, 23 |
| Xuất DOCX | 8 |

## 5. Ngoài phạm vi

- Làm bài và lưu bài nộp (U11), chấm và hiển thị chấm (U15), Code Lab (U13), đề chung cấp môn (đã loại).

# U09 Question Type Authoring - Code Generation Plan

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U09. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-ASM-004 (soạn khung, kiểm/xuất tài liệu; phần làm bài ở U11), US-ASM-005 (soạn Code Lab), US-ASM-006 (cài đặt quiz luyện tập), US-ASM-007 (Text Essay), US-GRP-003 (soạn bài nhóm).
- **Primary UC hiện hành**: phần nội dung theo dạng bài của UC 35, 42–45 và popup Rubric Detail của UC 46 theo bản 73 UC. Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-09**: câu của quiz, Text Essay, Code Lab nằm ở `assignment_questions` của U08 (ngân hàng của môn hoặc câu riêng); khung có thể sao từ câu `DOCUMENT` của ngân hàng; rubric tự tạo/xóa theo câu hoặc phần, sửa trong popup Rubric Detail khi bài nháp; có bài của môn (Text Essay, Diagram Essay, Code Lab); quiz luyện tập chỉ có cài đặt (không còn `AFTER_CLOSE`); kết quả kiểm lời giải Code Lab do U13 lưu theo `contentHash` của phiên bản câu `CODE`.
- **Wave**: 3. U13 (wave 4) cung cấp `CodeLabCheckPort`, `VerifySolutionButton` và `AiDraftPort` qua `C`: trước khi có U13 dùng adapter giả báo "chưa kiểm được lời giải" và ẩn nút AI.
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
| `ClassScopePort`, `SubjectScopePort` | U04 | Dùng thật |
| `RubricPort`, `RubricEditor`, `BankQueryPort`, `QuestionPicker` | U06 | Dùng thật (tự tạo, sửa, tự xóa, nhân bản rubric từng câu và từng phần; lấy khung câu `DOCUMENT`) |
| `AssignmentQueryPort`, `AssignmentExtensionPort`, `TypeConfigSlot`, `QuizSettingsSlot` | U08 | Dùng thật (ghi `config` qua `AssignmentExtensionPort`) |
| `CodeLabCheckPort`, `AiDraftPort` | U13 (`C`) | Adapter giả báo "chưa kiểm được lời giải", ẩn nút AI; U13 thay |
| U09 cài `TypeConfigPort` (U08), `DocumentModelPort` (U11, U13, U14, U15) | | Thay adapter tạm của U08 |

### Dữ liệu U09 sở hữu

PostgreSQL: cột `config` của `assignments` (U08 tạo bảng, U09 ghi qua `AssignmentExtensionPort`). Không có bảng hay migration riêng.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  authoring/
    api/                TypeConfigController, SkeletonController, RubricDetailController,
                        DocxImportController, DTO
    application/        TypeConfigService, SkeletonService, PartBuilder, RubricSync,
                        DocumentModelService,
                        DocxExportService, DiagramCompactor
    document/           Block (sealed) + record, DocumentValidator, SvgSanitizer,
                        SafeDrawioParser, DiagramType
    docx/               SafeZipGuard, DocxImporter, BlockMapper, DiagramDetector,
                        PngChunkReader, PngChunkWriter, JsvgRasterizer
    port/               TypeConfigPort (cài), DocumentModelPort, DocxExportPort,
                        DocxStudentImportPort, DiagramCompactPort
/backend/src/test/resources/u09/docx/     bộ DOCX mẫu (NFR-U09-30)
/contracts/schemas/document.json
/frontend/src/shared/document/            DocumentEditor, các block, DrawioPanel, EssayEditor
/frontend/src/shared/authoring/           QuestionRubricList, CodeLabCheckPanel, DocumentConfigForm,
                                          RubricDetailDialog, QuizSettingsForm
/contracts/openapi/authoring.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `com.github.weisj:jsvg`; POI đã có từ U06. Biến cấu hình U09 theo `logical-components.md` §3; Nginx route nhập DOCX 20 MB.

### Nhóm B - Mô hình tài liệu và logic

- [ ] **Bước 2** - `contracts/schemas/document.json`; Java `Block` sealed + record; sinh kiểu TypeScript từ schema (P1).
- [ ] **Bước 3** - `SafeDrawioParser`, `SvgSanitizer` (P5, BR-U09-35).
- [ ] **Bước 4** - `DocumentValidator`: `validateSkeleton`, `validateForSave` (hash khóa block), `validateForSubmit` (block khung đủ, sơ đồ không rỗng, `requiredDiagrams`, có nội dung người học); Text Essay chỉ block chữ, trần 1 000 000 ký tự (F11, P2, BR-U09-20…21, 30…38).
- [ ] **Bước 5** - `RubricSync` (P8): so danh sách câu Text Essay (khi U08 gọi `syncQuestionRubrics`) hoặc phần (khi lưu khung) cũ và mới, tự `RubricPort.create` rubric trống cho câu/phần mới, `RubricPort.delete` cho câu/phần không còn, cùng transaction với ghi `config` (BR-U09-23, 26, 27).
- [ ] **Bước 6** - `TypeConfigService` (cài đặt quiz, BR-U09-11…14; rubric từng câu Text Essay, BR-U09-23); cài `TypeConfigPort` (`check` gồm rubric đã điền, `syncQuestionRubrics`, `copy` nhân bản rubric) (F1, F2, F8, F9).
- [ ] **Bước 7** - `SkeletonService`, `PartBuilder` (lưu khung, hash, làm sạch SVG; sao khung từ câu `DOCUMENT` của ngân hàng qua `BankQueryPort`; tự chia phần theo heading nhỏ nhất, đồng bộ rubric phần qua `RubricSync`); AI soạn khung qua `AiDraftPort` (xem trước, xác nhận thì thay khung, gợi ý rubric điền vào rubric của phần) (F4, F6, BR-U09-24…30).
- [ ] **Bước 8** - `CodeLabCheckPanel` đọc trạng thái kiểm lời giải từng câu `CODE` qua `CodeLabCheckPort` (U13) (F3, BR-U09-16, 18).
- [ ] **Bước 9** - Rubric Detail: đọc/lưu rubric của câu hoặc phần qua `RubricPort.getRubric`/`update` sau khi kiểm quyền và bài `DRAFT`; bài đã phát hành chỉ đọc (F7).
- [ ] **Bước 10** - Nhập DOCX: `SafeZipGuard`, `DocxImporter`, `BlockMapper`, `PngChunkReader`, `DiagramDetector` (PNG `tEXt`/`zTXt`/`iTXt`, SVG `content`, giải nén diagram nén), báo cáo nhập; hỗ trợ khung và preview block `STUDENT` cho U11 (F5, F10, P3, BR-U09-40…48).
- [ ] **Bước 11** - Xuất DOCX: `DocxExportService`, `JsvgRasterizer`, `PngChunkWriter`, semaphore 2 (F12, P4, BR-U09-50…52).
- [ ] **Bước 12** - `DiagramCompactor` (F13, BR-U09-60).
- [ ] **Bước 13** - `DocumentModelService` cài `DocumentModelPort`; thay adapter tạm của U08.
- [ ] **Bước 14** - Unit test mọi `BR-U09-xx` (gồm đồng bộ rubric câu/phần, chặn duyệt khi rubric trống, sao khung từ ngân hàng, NFR-U09-33); bộ DOCX mẫu (PNG/SVG draw.io, ảnh thường, ảnh mất XML, bảng gộp ô, textbox, zip bomb); test vòng tròn xuất → nhập.
- [ ] **Bước 15** - Tóm tắt: `aidlc-docs/construction/u09-question-type-authoring/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 16** - Không có migration: kiểm schema JSON của `assignments.config` (cài đặt quiz, `questionRubrics[]`, khung, `parts[]`) khi đọc/ghi.
- [ ] **Bước 17** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: sửa nội dung khi bài không `DRAFT` bị chặn; U08 duyệt gọi `TypeConfigPort` thật; lưu khung đồng bộ rubric đúng với U06.
- [ ] **Bước 18** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 19** - `/contracts/openapi/authoring.yaml`; cần sửa theo mục 7.
- [ ] **Bước 20** - Controller + DTO + validation.
- [ ] **Bước 21** - Test MockMvc: chỉ người có quyền với bài sửa nội dung/khung/rubric; giảng viên lớp chỉ đọc bài của môn; Admin bị từ chối; DOCX quá lớn `413`; lỗi kiểm trả theo `blockId`, câu, đề hoặc phần.
- [ ] **Bước 22** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 23** - `DocumentEditor` (3 chế độ), các block, `LockedBadge`, ảo hóa > 200 block (P7); `DrawioPanel` (iframe `embed.diagrams.net`, kiểm origin, timeout 10 s, lưu XML + SVG) (P6).
- [ ] **Bước 24** - `QuestionRubricList`, `CodeLabCheckPanel` (dùng `VerifySolutionButton` của U13), `DocumentConfigForm` (`SkeletonEditor`, `SkeletonFromBankButton`, `PartRubricList`, `DocxImportDialog`, `AiSkeletonDraftDialog`, `RequiredDiagramsForm` chỉ cho Diagram Essay), `RubricDetailDialog` gắn vào `TypeConfigSlot` của U08; `QuizSettingsForm` gắn vào `QuizSettingsSlot`; `StudentDocxImportDialog` dùng trong Diagram Essay Workspace của U11.
- [ ] **Bước 25** - Test frontend: chế độ `STUDENT` không sửa/xóa/kéo được bất kỳ block khung nào (kể cả bảng, sơ đồ); người học chèn block của mình được; message từ origin lạ bị bỏ qua; cảnh báo rubric trống; form chỉ đọc khi bài đã phát hành hoặc là bài của môn với giảng viên lớp.
- [ ] **Bước 26** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 27** - Cập nhật `README.md`: xuất sơ đồ từ draw.io kèm "Include a copy of my diagram", tắt "Compress pictures" trong Word để giữ sơ đồ khi nhập; cách U11/U13/U15 dùng `TypeConfigPort`, `DocumentModelPort`, `DocxExportPort`.
- [ ] **Bước 28** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-004 S1, S2 (UC 44) | 2, 3, 4, 5, 7, 10, 23, 24 |
| US-ASM-004 S3 | 12 |
| US-ASM-006 (UC 35, cài đặt quiz) | 6, 24 |
| US-ASM-005 (UC 43) | 8, 24 |
| US-GRP-003 (UC 45, phần soạn bài) | 5, 7, 24 |
| US-ASM-007 (UC 42) | 4, 5, 6, 24 |
| UC 46 (Rubric Detail) | 5, 9, 24 |
| Xuất DOCX | 11 |

## 5. Ngoài phạm vi

- Làm bài và lưu bài nộp (U11), chấm và hiển thị chấm (U15), chạy code và lưu kết quả kiểm lời giải (U13), câu của quiz, Text Essay, Code Lab (U08, U06).

## 6. Revision implementation scope - 2026-10-08
- [ ] Actor là tài khoản có quyền với bài theo U08 (R3/R4 bài của lớp, R2 bài của môn), không kiểm literal TEACHER.

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] Câu Text Essay, Code Lab giữ ở `assignment_questions` của U08 (ngân hàng dùng cho mọi dạng bài, người dùng chốt lại 2026-10-09); khung vẫn sao được từ câu `DOCUMENT` của ngân hàng.
- [ ] Rubric tự tạo/xóa theo câu và phần (`RubricSync`, `TypeConfigPort.syncQuestionRubrics` cho U08); API rubric chỉ còn `GET`, `PUT` (thêm `GET`); bỏ `repointRubric`, `RubricPort.revise`.
- [ ] Cài đặt quiz: `showCorrectAnswers` chỉ `NEVER`, `AFTER_SUBMIT`; giới hạn thời gian không gắn hạn đóng; `PUT /type-config` chỉ cho quiz.
- [ ] Code Lab: kết quả kiểm lời giải do U13 lưu theo `contentHash` của phiên bản câu `CODE`; U09 chỉ hiện trạng thái.
- [ ] Quyền theo U08: R3/R4 bài của lớp, R2 bài của môn (Text Essay, Diagram Essay, Code Lab), giảng viên lớp chỉ đọc bài của môn; bỏ "Template Editor"; route frontend gắn vào `AssignmentFormPage`, `QuizDetailPage` của U08.

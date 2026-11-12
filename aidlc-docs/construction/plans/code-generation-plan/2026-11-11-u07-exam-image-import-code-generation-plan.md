# Code Generation Plan — U07 nhập đề từ ảnh/PDF (kèm U06, U03)

Kế hoạch này là nguồn duy nhất cho bước Code Generation. Làm từng bước theo thứ tự, đánh dấu `[x]` ngay khi xong. Không viết mã vào `aidlc-docs/`.

## Bối cảnh

- **Yêu cầu**: [FR-IMP-01…08](../../../inception/requirements/2026-11-11-exam-image-import.md). Truy vết FR-017, US-ASM-004 (UC 35).
- **Thiết kế**: BR-U07-87…101 ([U07](../../u07-assignment-authoring/functional-design/business-rules.md)), BR-U06-54…60 ([U06](../../u06-ai-connections/functional-design/business-rules.md)), BR-U03-99…100 ([U03](../../u03-job-and-settings/functional-design/business-rules.md)).
- **Quy ước**: [code-conventions](../../code-conventions.md) và [persistence-conventions](../../persistence-conventions.md). Dùng Spring JDBC; interface có tiền tố `I`; method bắt đầu bằng động từ.
- **Phụ thuộc giữa unit**:
  - U07 dùng `IAiTask` và `IAiImageSource` của U06, `IArtifact` (lưu ảnh `DOCUMENT_IMAGE`) và `IScheduledScanner` của U03.
  - U06 không phụ thuộc ngược U07: U07 đăng ký nguồn ảnh qua interface.
- **Dữ liệu U07 sở hữu**: `assignment_ai_drafts` (đã có, thêm `mode = IMPORT` trong provenance), `assignment_import_pages` (bảng mới).
- **Thư viện mới**: `org.apache.pdfbox:pdfbox` 3.0.5, `com.twelvemonkeys.imageio:imageio-webp` 3.12.0 (Java thuần, không đổi Docker).

## Các bước

### Bước 0 — Nhánh
- [x] Nhánh `feat/exam-image-import` tạo trong worktree `.claude/worktrees/exam-image-import` (`--no-track` từ `origin/staging@e78ac7b9`) theo lựa chọn của người dùng, vì thư mục chính có thay đổi chưa commit của phiên khác.

### U03 — tệp
- [x] **Bước 1**: `FileType.WEBP` (`image/webp`, `webp`); `ContentInspector` nhận diện `RIFF....WEBP`. `ArtifactPurpose.ASSIGNMENT_IMPORT`. `PurposePolicy`: loại tệp PNG/JPEG/WEBP/PDF, trần 10MB, `files.assignmentImport.maxSizeMb`, người tải lên Teacher/Subject Manager. `FileSettingDefinitions` thêm setting (1–10). `ArtifactService.store` từ chối mục đích `ASSIGNMENT_IMPORT` (BR-U03-99, 100).
- [x] **Bước 2**: test `ContentInspectorTest` (WEBP), `PurposePolicyTest` (mục đích mới, kẹp trần), `UploadServiceTest` (kiểm không lưu, từ chối lưu mục đích nhập đề). Thêm `IArtifact.inspect` để U07 kiểm tệp qua contract. 25/25 test qua.

### U06 — ảnh trong prompt
- [x] **Bước 3**: contract `AiImageRef`, `AiImage`, `IAiImageSource`. `AiPrompt(system, user, images)` kèm constructor cũ. `IProviderGateway.Request` thêm `images` kèm constructor cũ; `toString` chỉ ghi số ảnh. `AiImageSourceRegistry` (giống `AiTaskResultHandlerRegistry`).
- [x] **Bước 4**: `AiProperties.Provider.visionModels`, `ProviderCatalog.supportsImages`; `application.yml` thêm `U06_*_VISION_MODELS` (mặc định rỗng = mọi model).
- [x] **Bước 5**: `AiTaskService`:
  - kiểm ảnh (≤ 10, mime JPEG/PNG, `source` đã đăng ký);
  - lưu tham chiếu ảnh vào `input_payload`, đưa vào fingerprint;
  - ước tính quota +1.600 token/ảnh;
  - `sourceOrder` chỉ lấy kết nối đọc được ảnh khi prompt có ảnh.

  (BR-U06-54, 56, 58.)
- [x] **Bước 6**: `AiTaskJobHandler`: đọc tham chiếu ảnh, nạp bytes qua registry. Thiếu ảnh thì tác vụ FAILED, không gọi nhà cung cấp, hoàn quota. Bước thử kết nối kế tiếp bỏ qua model không đọc ảnh (BR-U06-55, 58).
- [x] **Bước 7**: `HttpProviderGateway`: gửi ảnh theo định dạng Gemini, OpenAI, Claude. Không có ảnh thì body không đổi. Nhà cung cấp trả 400 về ảnh thì `REJECTED` với thông báo an toàn (BR-U06-59, 60).
- [x] **Bước 8**: test `HttpProviderGatewayTest` (body có ảnh của ba nhà cung cấp; body không ảnh giữ nguyên), `AiTaskFlowTest` (payload, fingerprint, quota, thiếu ảnh, bỏ qua model không đọc ảnh), `KeyCipherAndCatalogTest` (`supportsImages`), `AiImageSourceRegistryTest`.

### U07 — backend
- [x] **Bước 9**: `backend/pom.xml` thêm PDFBox và `imageio-webp`.
- [x] **Bước 10**: migration `backend/src/main/resources/db/migration/assignments/V20261111_1200__create_assignment_import_pages.sql`: bảng `assignment_import_pages`, FK `ON DELETE CASCADE`, check `page_no` 1–10, index `created_at`.
- [x] **Bước 11**: `assignments.repository.ImportPageRepository` (JDBC): `insertPages`, `loadPage`, `listPages`, `deletePages`, `deletePagesOlderThan`.
- [x] **Bước 12**: `assignments.service.ImportFileReader`: kiểm số tệp, loại theo nội dung, dung lượng theo policy, trộn loại, PDF trang/mật khẩu, megapixel từ header (BR-U07-88). `ImportPageNormalizer`: xoay theo EXIF, dựng trang PDF 150 DPI, thu về 2.000px, JPEG 0,9, chữ gợi ý PDF, cảnh báo `LOW_RESOLUTION` (BR-U07-89, 90).
- [x] **Bước 13**:
  - `ExamImportPrompt`: system, user, JSON schema, 16.000 token ra (BR-U07-93, 94).
  - `ExamImportMapper`: JSON sang khối, lọc màu, làm đều bảng, đếm `[không đọc rõ]`, cảnh báo (BR-U07-95, 97).
  - `FigureCropper`: kẹp vùng, cắt, PNG hoặc JPEG, `IArtifact.store(DOCUMENT_IMAGE)`; lỗi thì thành đoạn giữ chỗ (BR-U07-96).
- [x] **Bước 14**:
  - `AssignmentImportService`: `startImport` (BR-U07-87, 91, 92, 100, 101), `acceptResult` / `rejectResult`, `applyImport` (BR-U07-98), `discardPages` (BR-U07-99).
  - `AssignmentImportResultHandler` (`taskType = ASSIGNMENT_IMPORT`).
  - `AssignmentImportImageSource` (`IAiImageSource`).
  - `ImportPageCleanupScanner` (`IScheduledScanner`, xóa trang quá 24 giờ).
- [x] **Bước 15**: nối vào luồng có sẵn:
  - `AssignmentAiDraftService.apply` chuyển bản nháp `IMPORT` sang `applyImport`; `discard` xóa ảnh trang.
  - `AssignmentCommands.ApplyDraft` thêm `placement`; `AssignmentViews.AiDraft` thêm `mode`.
  - `AssignmentAuthoringService` tách phần lưu khung dùng chung cho nhập DOCX và nhập ảnh. Hành vi `saveSkeleton` giữ nguyên.
- [x] **Bước 16**: `AssignmentImportController` `POST /api/v1/assignments/{id}/imports` (multipart `files`, `requestRef`, `source`, `provider`). Mã lỗi mới qua `AssignmentExceptionHandler` hiện có.
- [x] **Bước 17**: test `ImportFileReaderTest`, `ImportPageNormalizerTest` (ảnh và PDF nhỏ sinh trong test), `ExamImportMapperTest`, `FigureCropperTest`, `AssignmentImportServiceTest`, `ImportPageCleanupScannerTest`, và bổ sung `AssignmentAiDraftServiceTest` (apply REPLACE/APPEND, discard). Dùng nhà cung cấp AI giả; không gọi AI thật.

### Frontend
- [x] **Bước 18**: `features/files/model/purposes.ts` thêm `ASSIGNMENT_IMPORT`. `features/assignments/model/types.ts`: `AiDraft.mode`, kiểu đề xuất `IMPORT`. `api/index.ts`: `startImport`, `applyAiDraft` có `placement`.
- [x] **Bước 19**: `components/ExamImportDialog.tsx` (PICK, RUNNING, PREVIEW, ERROR; `data-testid` dạng `exam-import-*`) và `model/pendingImports.ts`.
- [x] **Bước 20**:
  - `AssignmentFormPage`: mục menu "Nhập đề từ tệp", ghi vào lịch sử để Ctrl+Z, tự mở khi có `pendingImports`.
  - `CreateAssignmentDialog` và `TemplateFileField`: nhận ảnh/PDF, tối đa 5 ảnh.
  - Chọn DOCX trong dialog thì đi đường `importDocx` cũ.
- [x] **Bước 21**: test `frontend/tests/features/assignments/components/ExamImportDialog.test.tsx` (kiểm tệp, sắp thứ tự, hủy, xem trước, Thay toàn bộ / Chèn vào cuối, lỗi), bổ sung `assignments.test.tsx` (popup tạo bài nhận ảnh/PDF, mục menu).

### Tài liệu
- [x] **Bước 22**: `aidlc-docs/construction/u07-assignment-authoring/code/` (tóm tắt thay đổi), ghi chú as-built U06/U03, `inception/reverse-engineering/api-inventory.md` (endpoint mới, `placement`), `current-code-baseline.md` (file mới).

## Ngoài kế hoạch

Build và chạy toàn bộ test thuộc giai đoạn Build and Test. Không commit hay push nếu bạn chưa yêu cầu.

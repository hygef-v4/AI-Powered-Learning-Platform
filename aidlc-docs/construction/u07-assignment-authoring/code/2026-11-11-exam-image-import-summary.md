# Nhập đề từ ảnh/PDF — tóm tắt mã, 2026-11-11

Nhánh `feat/exam-image-import`, tạo `--no-track` từ `origin/staging@e78ac7b9`, nằm trong worktree `.claude/worktrees/exam-image-import`. Viết theo [kế hoạch](../../plans/code-generation-plan/2026-11-11-u07-exam-image-import-code-generation-plan.md). Thiết kế gốc: BR-U07-87…101, BR-U06-54…60, BR-U03-99…100. Chưa commit.

## U03 — tệp

| Thay đổi | Tệp |
|---|---|
| `FileType.WEBP` (Tika nhận diện `RIFF....WEBP`) | `files/model/FileType.java` |
| `ArtifactPurpose.ASSIGNMENT_IMPORT` có `storable() = false`; `PurposePolicy` nhận PNG/JPEG/WEBP/PDF, trần 10MB, Teacher/Subject Manager; setting `files.assignmentImport.maxSizeMb` | `ArtifactPurpose.java`, `PurposePolicy.java`, `FileSettingDefinitions.java` |
| `IArtifact.inspect(...)` kiểm quyền, dung lượng và loại tệp mà không lưu; `UploadService.upload` từ chối mục đích không lưu được (`FILE_PURPOSE_NOT_STORABLE`) | `IArtifact.java`, `ArtifactService.java`, `UploadService.java`, `FilesContractStubs.java` |

## U06 — ảnh trong prompt

| Thay đổi | Tệp |
|---|---|
| `AiPrompt.images`, `AiImageRef`, `AiImage`, `IAiImageSource`, `AiImageSourceRegistry` | `ai/contract/*`, `ai/service/AiImageSourceRegistry.java` |
| `AiTaskService`: kiểm ảnh (≤ 10, JPEG/PNG, nguồn đã đăng ký); lưu tham chiếu vào `input_payload`; đưa ảnh vào fingerprint; quota +3.200 ký tự (≈ 1.600 token) mỗi ảnh; khi có ảnh chỉ chọn kết nối có model đọc được ảnh | `AiTaskService.java`, `AiTokenEstimate.java` |
| `AiTaskJobHandler`: nạp ảnh ngay trước khi gọi nhà cung cấp; thiếu ảnh thì FAILED ("Ảnh gửi AI không còn"), không gọi, hoàn quota; bước thử kết nối kế tiếp bỏ qua model không đọc ảnh | `AiTaskJobHandler.java` |
| `vision-models` theo provider (để trống là mọi model); `ProviderCatalog.supportsImages` | `AiProperties.java`, `ProviderCatalog.java`, `application.yml` |
| Gateway gửi ảnh trước phần chữ: Gemini `inline_data`, OpenAI `image_url` (data URL, `detail: high`), Claude khối `image` base64. Nhà cung cấp trả 400 nhắc tới "image" thì báo "Model đang dùng không đọc được ảnh" | `HttpProviderGateway.java`, `IProviderGateway.java` |

## U07 — nhập đề

| Thay đổi | Tệp |
|---|---|
| Bảng tạm `assignment_import_pages` (FK cascade theo bản nháp, check trang 1–10, index `created_at`, grant cho role `app` nếu có) | `db/migration/assignments/V20261111_1300__create_assignment_import_pages.sql` |
| `ImportPage`, `ImportPageRepository` (JDBC) | `assignments/model`, `assignments/repository` |
| `ImportFileReader` kiểm tệp qua `IArtifact.inspect`: tối đa 5 ảnh hoặc 1 PDF, không trộn loại | `assignments/service/ImportFileReader.java` |
| `ImportPageNormalizer`: xoay theo EXIF, dựng PDF ở 150 DPI (PDFBox 3.0.7), chặn ảnh trên 40MP từ header, thu cạnh dài về 2.000px, JPEG 0,9, cảnh báo `LOW_RESOLUTION`, lấy chữ của PDF có lớp chữ làm gợi ý | `ImportPageNormalizer.java` |
| `ExamImportPrompt` (system, user, schema), `ExamImportMapper`, `FigureCropper` | `assignments/service/*` |
| `AssignmentImportService` (`startImport`, `acceptResult`, `rejectResult`, `applyImport`, `discardPages`, `deleteExpiredPages`), `AssignmentImportResultHandler`, `AssignmentImportImageSource`, `ImportPageCleanupScanner` | `assignments/service/*` |
| `POST /api/v1/assignments/{id}/imports` (multipart `files`, `requestRef`, `source`, `provider`; trả 202 kèm `AiDraft`) | `AssignmentImportController.java` |
| `:apply` nhận `placement`; bản nháp `IMPORT` đi nhánh `applyImport`; `:discard` xóa ảnh trang. Tách `storeSkeleton` dùng chung cho `saveSkeleton` và `saveImportedSkeleton` | `AssignmentAiDraftService.java`, `AssignmentCommands.java`, `AssignmentAuthoringService.java` |
| Phụ thuộc `pdfbox` 3.0.7 (cùng bản Tika đang kéo theo), `imageio-webp` 3.12.0 | `backend/pom.xml` |

## Frontend

| Thay đổi | Tệp |
|---|---|
| `ExamImportDialog` (chọn tệp, sắp thứ tự bằng nút hoặc kéo thả, chờ có Hủy, xem trước kèm số đếm và cảnh báo, "Dùng … làm tên bài", Ghi vào đề / Thay toàn bộ / Chèn vào cuối). DOCX đi đường `importDocx` cũ, không gọi AI | `components/ExamImportDialog.tsx`, `model/importFiles.ts` |
| Trang soạn bài: mục "Nhập đề từ tệp" (bài `DRAFT`/`REVIEWED`, có quyền sửa); ghi xong đưa vào lịch sử để Ctrl+Z; tự mở popup khi có tệp chờ từ popup tạo bài | `pages/AssignmentFormPage.tsx`, `model/pendingImports.ts` |
| Popup tạo bài, Đề mới: ô Mẫu đề nhận DOCX, ảnh, PDF (chọn được nhiều ảnh) | `CreateAssignmentDialog.tsx`, `TemplateFileField.tsx` |
| API: `startImport`, `importDraft`, `applyAiDraft(..., placement)`; kiểu `ImportDraft`, `ImportProposal`; mục đích `ASSIGNMENT_IMPORT` | `api/index.ts`, `model/types.ts`, `features/files/model/purposes.ts` |

## Điều chỉnh so với thiết kế (đã ghi vào business-rules)

1. **Tiêu đề trong đề thành đoạn in đậm, không thành khối `HEADING`.** Trong khung đề, `HEADING` là mục câu hỏi có đánh số và tạo phần chấm. Nhập không tự tách câu hỏi (FR-IMP-03.4), nên `HEADING` của AI được ghi thành `PARAGRAPH` in đậm; cấp 1 thì căn giữa. Số "tiêu đề" trong màn xem trước là số đoạn như vậy.
2. **`AiDraft` view không thêm trường `mode`.** Frontend nhận ra bản nhập qua `proposal.kind = "IMPORT"` và `provenance.mode`.
3. **Mã lỗi tệp của U03 được đổi thành mã nhập đề, đều trả HTTP 400.** `FILE_TOO_LARGE` thành `ASSIGNMENT_IMPORT_TOO_LARGE`; `FILE_TYPE_NOT_ALLOWED` thành `ASSIGNMENT_IMPORT_FILE_INVALID`.
4. **Hình đã cắt quá hạn thì không ghi được.** Hình được lưu bằng `FileRef` có hạn 1 giờ (BR-U03-30). Ghi đề sau hạn đó thì `:apply` trả `409 AI_DRAFT_EXPIRED` ("Hình trong bản nhập đã quá hạn; hãy nhập lại tệp đề").
5. **Số token ra thực tế bị giới hạn ở 8.192.** Thiết kế ghi `maxOutputTokens` 16.000, nhưng `ProviderCatalog` kẹp theo `max-output-tokens` của provider, hiện là 8.192.
6. **Schema kết quả không dùng `$ref` hay kiểu hợp.** Như vậy cả ba nhà cung cấp đều đọc được. `title` là chuỗi rỗng khi đề không có tên. Mọi trường chứa nội dung đều bắt buộc, vì Gemini bỏ mảng và object con không bắt buộc: khối luôn có `runs`, `items`, `rows`. Vị trí hình nằm ở mảng `figures` cấp trên cùng (`page`, `box`, `caption`), khối FIGURE thứ n ứng với `figures[n]` (phát hiện khi thử trên trình duyệt).
7. **`title` cắt theo `Assignment.TITLE_MAX` (200 ký tự).** Cắt ngay khi dựng đề xuất để "Dùng làm tên bài" luôn hợp lệ.

## Kiểm tra đã chạy

Backend chạy bằng Maven trong Docker (`maven:3.9-eclipse-temurin-17`, `-o`), frontend chạy trong worktree sau `npm ci`.

- **U03**: 4 lớp test, 25/25 qua.
- **U06**: `HttpProviderGatewayTest`, `AiTaskFlowTest`, `KeyCipherAndCatalogTest`, `AiTaskResultHandlerRegistryTest`, `AiSourceServiceTest`, 62/62 qua; `AiImageSourceRegistryTest` 2/2 qua.
- **U07**: 9 lớp test (5 lớp mới cùng các test bản nháp AI, soạn bài và DOCX), 79/79 qua.
- **`ArchitectureTest`**: 3/3 qua.
- **Frontend**: `tsc --noEmit` và `eslint` sạch. Vitest `tests/features/assignments` 61/61 qua, gồm 9 test của `ExamImportDialog` và test popup tạo bài nhận ảnh.

Chưa chạy: toàn bộ backend (`mvn verify`), toàn bộ Vitest, production build, trình duyệt, nhà cung cấp AI thật. Các việc này thuộc giai đoạn Build and Test.

**Cập nhật Build and Test**: `mvn verify` phát hiện `AiProperties.Provider` có constructor phụ làm Spring không bind được danh sách model; đã bỏ constructor phụ. Sau đó backend 2197/2197, frontend 968/968. Xem [kết quả](../../build-and-test/2026-11-11-exam-image-import-verification.md).

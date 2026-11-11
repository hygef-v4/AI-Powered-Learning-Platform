# Functional Design Plan — U07 nhập đề từ ảnh/PDF (kèm delta U06, U03)

Dựa trên [yêu cầu](../../../inception/requirements/2026-11-11-exam-image-import.md) và [execution plan](../../../inception/plans/2026-11-11-exam-image-import-execution-plan.md). Chưa sửa mã nguồn.

## Hiện trạng đã đọc

- **AI soạn đề (U07)**: `AssignmentAiDraftService.request` tạo bản ghi `assignment_ai_drafts` (PENDING), gửi `IAiTask.submit` với `taskType = ASSIGNMENT_DRAFT`, rồi `AssignmentDraftResultHandler.onReady` ghi đề xuất (READY). `:apply` ghi khung đề, `:discard` hủy task. Frontend ghi thẳng lên trang, giữ bản trước để Ctrl+Z.
- **Tác vụ AI (U06)**:
  - `AiTaskService.submit` lưu prompt dạng JSON `{system, user}` vào `ai_tasks.input_payload`, giữ chỗ quota theo số ký tự.
  - Worker (`AiTaskJobHandler`, có thể là process riêng) đọc lại prompt và gọi `IProviderGateway`.
  - Gặp lỗi tạm thì thử kết nối kế tiếp.
- **Gateway**: `Request(model, apiKey, system, user, outputSchema, maxOutputTokens, timeout)`; cả ba nhà cung cấp chỉ nhận một phần text.
- **Ảnh trong đề**: `DocxImporter` lưu ảnh bằng `IArtifact.store(DOCUMENT_IMAGE)`, khối `IMAGE` giữ `fileRef`. `saveSkeleton` gắn tệp; tệp không được gắn thì dọn theo cơ chế mồ côi.
- **Tệp (U03)**:
  - `PurposePolicy` có trần cứng và setting theo mục đích.
  - `ContentInspector.detect` nhận diện loại tệp theo nội dung.
  - `FileSettingDefinitions` đăng ký setting.
  - Multipart tối đa 51MB.
- **Khối đề**: `PARAGRAPH runs[]`, `LIST ordered, items[] (runs, level 0–4)`, `TABLE rows[][] (cell: runs), headerRow`. Run có `bold`, `italic`, `underline`, `color`, `highlight`.

## Hướng thiết kế đề xuất

Dùng lại hạ tầng bản nháp AI của U07, thêm chế độ `IMPORT`:

1. `POST /api/v1/assignments/{id}/imports` nhận multipart (ảnh hoặc PDF), kiểm tệp, chuẩn hóa thành ảnh trang (tối đa 10), tạo bản nháp `IMPORT` (PENDING), gửi tác vụ AI `ASSIGNMENT_IMPORT`. Trả bản nháp như AI soạn đề.
2. U06 thêm phần ảnh vào prompt dưới dạng tham chiếu. Worker lấy bytes qua `IAiImageSource`: U06 khai báo, U07 cài, giống mẫu `IAiTaskResultHandler`. Gateway gửi ảnh theo định dạng của từng nhà cung cấp.
3. `onReady`: kiểm JSON, cắt hình theo vùng AI trả, lưu `DOCUMENT_IMAGE`, dựng `DocumentModel` qua bộ kiểm khối hiện có, ghi đề xuất READY kèm cảnh báo, rồi xóa ảnh trang.
4. `:apply` với `placement = REPLACE | APPEND`. Frontend dùng cơ chế "ghi lên trang, Ctrl+Z" sẵn có.

## Các bước

- [x] Domain entities: chế độ `IMPORT` của `AssignmentAiDraft`, ảnh trang tạm, đề xuất nhập (khối, vùng hình, cảnh báo), `AiImageRef`, `ArtifactPurpose.ASSIGNMENT_IMPORT`, `FileType.WEBP`.
- [x] Business rules BR-U07-87…: tệp, chuẩn hóa ảnh, PDF, prompt và schema, ánh xạ khối, cắt hình, xem trước và ghi, hủy và dọn, lỗi, audit.
- [x] Business rules U06: phần ảnh trong prompt, ước tính quota cho ảnh, model đọc ảnh, gửi ảnh cho ba nhà cung cấp, bỏ qua kết nối không đọc ảnh khi thử kết nối kế tiếp, không ghi bytes vào log.
- [x] Business rules U03: mục đích `ASSIGNMENT_IMPORT` (PNG/JPEG/WEBP/PDF, trần 10MB, setting), nhận diện WebP.
- [x] Business logic model: luồng tạo, chạy, nhận kết quả, ghi, hủy; sơ đồ trạng thái.
- [x] Frontend components: ô "Mẫu đề" trong `CreateAssignmentDialog`, mục "Nhập đề từ tệp" trong menu trang soạn bài, `ExamImportDialog` (chọn và sắp tệp, chờ và hủy, xem trước, Thay toàn bộ / Chèn vào cuối).
- [x] API: endpoint mới và mã lỗi (api-inventory cập nhật khi viết mã).

## Câu hỏi thiết kế

Phương án A là đề xuất của AI.

## Question 1
Giữ ảnh trang ở đâu trong lúc chờ AI trả kết quả? Worker có thể chạy ở process riêng, nên cần chỗ dùng chung.

A) Bảng tạm `assignment_import_pages` trong PostgreSQL (bytea, ảnh đã thu nhỏ khoảng 0,3–1MB/trang), xóa ngay khi bản nháp xong, lỗi hay bị bỏ; thêm job dọn bản ghi quá 24 giờ (đề xuất)

B) Lưu tạm qua dịch vụ tệp (Google Drive / thư mục local) rồi xóa sau

C) Nhét ảnh base64 thẳng vào `ai_tasks.input_payload`

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Giữ định dạng chữ đến đâu?

A) Giữ in đậm, nghiêng, gạch chân và màu chữ rõ ràng (ví dụ đoạn đỏ của đề SWR302 giữ màu đỏ và in đậm); bỏ màu đen/xám mặc định (đề xuất)

B) Chỉ giữ in đậm/nghiêng/gạch chân, chữ màu chuyển thành in đậm, đúng như yêu cầu ban đầu

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Ảnh chụp bằng điện thoại bị xoay hoặc nghiêng thì sao?

A) Server tự xoay theo EXIF; AI được dặn đọc cả trang xoay 90°/180°; không nắn thẳng phối cảnh (đề xuất)

B) Như A, thêm nút xoay 90° cho từng ảnh trong danh sách trước khi chuyển đổi

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Chọn nguồn AI thế nào?

A) Tự chọn như AI soạn đề: API chung của trường trước nếu còn hạn mức, sau đó API cá nhân; chỉ dùng model đọc được ảnh (đề xuất)

B) Thêm ô chọn nguồn/model trong popup nhập

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Tiêu đề trong đề (ví dụ "SWR302_SU25_PE1_407173") xử lý thế nào?

A) Đưa vào khung đề như một heading; không đổi tên bài. Màn xem trước có ô chọn "Dùng làm tên bài", mặc định tắt (đề xuất)

B) Luôn đổi tên bài theo tiêu đề trong đề

C) Không đụng đến tên bài, không có ô chọn

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
PDF có sẵn lớp chữ (không phải bản scan) xử lý thế nào?

A) Vẫn dựng trang thành ảnh cho AI đọc, gửi kèm chữ trích được của từng trang làm gợi ý chính tả; ảnh là căn cứ chính (đề xuất)

B) PDF có lớp chữ thì bỏ qua AI, trích chữ trực tiếp không tốn quota (mất bảng, tiêu đề, định dạng)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

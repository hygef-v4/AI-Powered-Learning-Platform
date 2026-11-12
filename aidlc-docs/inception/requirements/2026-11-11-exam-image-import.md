# Nhập đề từ ảnh/PDF thành khung đề chỉnh sửa được — yêu cầu

Chưa sửa mã nguồn. Quyết định: [câu hỏi xác nhận](2026-11-11-exam-image-import-questions.md), người dùng chọn phương án A cho cả 10 câu.

## Phân tích ý định

- **Yêu cầu của người dùng**: "kiểm tra xem đã có tính năng tải đề cũ lên, biến thành đề đang chỉnh sửa chưa cho tôi, chẳng hạn như tôi có đề trong ảnh", sau đó "Có, làm tính năng này".
- **Loại yêu cầu**: Enhancement. Mở rộng FR-017 và US-ASM-004 (nhập tài liệu vào workspace Assignment) từ DOCX sang ảnh và PDF.
- **Phạm vi**: nhiều thành phần. U07 (nhập khung đề, giao diện soạn bài), U06 (gửi ảnh cho nhà cung cấp AI, tác vụ AI, quota), U03 (chính sách tệp).
- **Độ phức tạp**: vừa đến phức tạp.
- **Độ sâu**: Standard.

## Hiện trạng

- Nhập DOCX: `POST /api/v1/assignments/{id}/skeleton:import-docx` trả bản xem trước gồm khối, cảnh báo, số heading/ảnh/sơ đồ. Frontend lưu bằng `saveSkeleton(..., { fromDocx: true })`. Chỉ chủ bài nhập được, và chỉ khi bài chưa phát hành.
- `HttpProviderGateway` chỉ gửi text cho Gemini, OpenAI, Claude.
- Không có OCR hay chuyển trang PDF thành ảnh ở backend.

## Yêu cầu chức năng

### FR-IMP-01 — Tệp đầu vào

1. Nhận ảnh PNG, JPEG, WebP hoặc PDF (PDF scan và PDF có lớp chữ).
2. Mỗi lần nhập: tối đa 5 ảnh, hoặc đúng 1 PDF tối đa 10 trang. Không trộn ảnh với PDF trong cùng một lần.
3. Mỗi tệp tối đa 10MB. Mức này là trần hệ thống; Administrator chỉnh thấp hơn được trên Settings như các mục đích tệp khác (FR-033).
4. Nhiều ảnh được ghép theo thứ tự đã chọn. Người dùng kéo thả hoặc dùng nút lên/xuống để đổi thứ tự trước khi chuyển đổi.
5. Server kiểm loại tệp bằng nội dung thật (chữ ký tệp), không tin phần mở rộng hay `Content-Type`. Tệp sai loại, quá dung lượng, PDF quá 10 trang, PDF có mật khẩu, hay ảnh vượt giới hạn điểm ảnh đều bị từ chối với thông báo tiếng Việt rõ ràng.

### FR-IMP-02 — Chỗ đặt trên giao diện

1. Popup tạo bài, lựa chọn "Đề mới": ô "Mẫu đề" hiện nhận DOCX, nay nhận thêm ảnh và PDF. Gợi ý của ô ghi rõ các loại tệp nhận.
2. Trang soạn bài: nút "Nhập đề từ tệp" (DOCX, ảnh, PDF). Chỉ hiện cho người có quyền sửa bài, và chỉ khi bài chưa phát hành.
3. DOCX vẫn đi đường nhập hiện có: không gọi AI, không tính quota.

### FR-IMP-03 — Chuyển đổi nguyên văn

1. AI chép đúng nội dung trong đề: không thêm, bớt, dịch hay viết lại. Giữ nguyên ngôn ngữ gốc của đề.
2. Dựng lại cấu trúc thành các khối hiện có: `HEADING`, `PARAGRAPH`, `LIST` (có đánh số / không đánh số), `TABLE`.
3. Chữ in đậm hoặc in màu nổi bật trong đề (ví dụ đoạn chữ đỏ của đề SWR302) chuyển thành chữ in đậm, nếu mô hình tài liệu hỗ trợ.
4. Không tự tách câu hỏi, không đặt điểm, không soạn rubric. Giảng viên làm các việc này sau trên trang soạn bài, hoặc dùng "Tạo nháp bằng AI" có sẵn.
5. Chỗ AI không đọc chắc được đánh dấu `[không đọc rõ]` ngay trong nội dung, và được liệt kê trong phần cảnh báo.
6. Đầu trang/chân trang lặp lại (ví dụ "Page 1 | 1") bị bỏ, và có ghi trong cảnh báo.

### FR-IMP-04 — Hình, sơ đồ và bảng

1. Bảng chuyển thành khối `TABLE`.
2. Hình và sơ đồ: AI trả vùng chứa hình trên trang. Server cắt đúng vùng đó từ ảnh gốc (với PDF thì từ ảnh dựng lại của trang), lưu thành ảnh tài liệu của bài, rồi chèn khối `IMAGE` vào đúng vị trí, kèm chú thích hoặc mô tả ngắn làm alt text.
3. Sơ đồ chỉ được chèn dưới dạng ảnh, không chuyển thành sơ đồ Draw.io (`DIAGRAM`) có thể chỉnh sửa.
4. Vùng cắt không hợp lệ hoặc cắt lỗi: thay bằng đoạn giữ chỗ `[Hình: mô tả]` và thêm một cảnh báo; phần còn lại của đề vẫn được nhập.

### FR-IMP-05 — Xem trước và ghi vào đề

1. Chuyển đổi xong thì hiện bản xem trước: nội dung đã dựng, danh sách cảnh báo, số heading/bảng/hình.
2. Khung đề đang trống: nút "Ghi vào đề".
3. Khung đề đã có nội dung: chọn "Thay toàn bộ" hoặc "Chèn vào cuối".
4. Ghi xong vẫn hoàn tác được bằng Ctrl+Z như nhập DOCX và AI soạn đề hiện nay.
5. Bỏ bản xem trước thì đề không đổi. Ảnh hình đã cắt cho bản xem trước được dọn theo cơ chế dọn tệp mồ côi hiện có.
6. Trong popup tạo bài: chuyển đổi lỗi thì vẫn giữ bài đã tạo và báo "Đã tạo bài, chưa nhập tệp: …", giống cách xử lý DOCX lỗi hiện nay.

### FR-IMP-06 — Đọc ảnh bằng AI

1. Đọc ảnh qua kết nối AI đã cấu hình (Gemini, OpenAI, Claude), như một tác vụ AI chạy nền qua `AiTaskService`, tính vào quota của giảng viên.
2. Trong lúc chạy: popup chờ có vòng xoay và nút Hủy. Hủy thì không ghi gì.
3. Model đang dùng không đọc được ảnh: báo lỗi riêng, rõ ràng, và gợi ý chọn model khác. Hết quota hay lỗi từ nhà cung cấp AI dùng thông báo có sẵn.
4. Kết quả AI phải đúng JSON schema. Sai schema thì lượt nhập thất bại, không ghi gì vào đề.

### FR-IMP-07 — Phạm vi

1. Chỉ áp dụng cho Assignment (cả bài cấp lớp và bài cấp môn).
2. Nhập câu hỏi trắc nghiệm cho Quiz/ngân hàng câu hỏi ngoài phạm vi; làm ở đợt sau.

### FR-IMP-08 — Không giữ tệp gốc

1. Ảnh/PDF gốc chỉ dùng để chuyển đổi và bị xóa khi tác vụ kết thúc (xong, lỗi hay hủy).
2. Chỉ giữ ảnh hình đã cắt và được ghi vào đề (ảnh tài liệu của bài).

## Yêu cầu phi chức năng

- **Bảo mật (Security Baseline, trong phạm vi đã duyệt)**:
  - Kiểm quyền chủ bài và trạng thái chưa phát hành ở mọi endpoint.
  - Kiểm loại tệp thật, dung lượng, số trang, số điểm ảnh (chặn ảnh nén "bom").
  - Chữ trong ảnh chỉ là dữ liệu: câu lệnh nằm trong đề không đổi cách hệ thống xử lý.
  - Không nhận HTML/SVG/script từ AI; nội dung đi qua bộ kiểm khối tài liệu hiện có.
  - Không ghi nội dung đề hay khóa API vào log.
- **Khả năng chịu lỗi (Resiliency Baseline, trong phạm vi đã duyệt)**:
  - Có timeout khi gọi nhà cung cấp AI và ánh xạ lỗi theo loại.
  - Lỗi nửa chừng không ghi một phần vào đề.
  - Tệp tạm được dọn cả khi lỗi.
  - Có thể thử lại từ popup.
- **Hiệu năng**: mục tiêu dưới 60 giây cho 5 trang với model mặc định. Chuyển trang PDF thành ảnh và cắt hình dùng bộ nhớ có giới hạn.
- **Khả dụng**: chữ trên giao diện bằng tiếng Việt; thao tác được bằng bàn phím; thông báo trạng thái có `aria-live`; dùng được ở chiều rộng 375px.
- **Kiểm thử**:
  - Unit test cho kiểm tệp, ánh xạ JSON sang khối, cắt hình, ghép nhiều trang.
  - Test gateway cho phần ảnh của cả ba nhà cung cấp (giả lập HTTP).
  - Test frontend cho popup, xem trước, Thay toàn bộ / Chèn vào cuối.
  - Không gọi nhà cung cấp AI thật trong test tự động.
  - Property-Based Testing tắt.

## Tiêu chí chấp nhận

1. Giảng viên tải ảnh đề SWR302_SU25_PE1_407173. Bản xem trước có:
   - tiêu đề "SWR302_SU25_PE1_407173";
   - các đoạn mô tả hệ thống đúng nguyên văn tiếng Anh;
   - đoạn yêu cầu màu đỏ thành chữ in đậm;
   - "Question 1 (0.5 point): …" là một đoạn riêng;
   - không có "Page 1 | 1".
2. PDF scan 3 trang ra một đề liền mạch đúng thứ tự trang. 5 ảnh đổi thứ tự trước khi chuyển đổi thì ra đúng thứ tự đã chọn.
3. Đề có hình sơ đồ: hình được cắt và chèn thành khối ảnh tại đúng vị trí. Cắt lỗi thì có dòng giữ chỗ và cảnh báo.
4. Bài đã có nội dung: "Chèn vào cuối" giữ nội dung cũ, "Thay toàn bộ" thay hết; Ctrl+Z hoàn tác được.
5. Các trường hợp bị từ chối, không gọi AI và không tốn quota:
   - tệp `.png` mà thật ra là tệp khác;
   - tệp 11MB;
   - PDF 11 trang;
   - 6 ảnh;
   - bài đã phát hành;
   - người không phải chủ bài.
6. Model không hỗ trợ ảnh, hết quota, hay nhà cung cấp AI lỗi: báo lỗi rõ, đề không đổi, tệp gốc đã bị xóa.
7. Nhập DOCX hoạt động như trước, không gọi AI.

## Ngoài phạm vi

- Nhập câu hỏi cho Quiz và ngân hàng câu hỏi.
- Tự tách câu hỏi, đặt điểm, soạn rubric khi nhập.
- Chuyển sơ đồ trong ảnh thành Draw.io chỉnh sửa được.
- OCR chạy trên server không cần AI.
- Bảo đảm chất lượng với chữ viết tay.

## Ghi chú kỹ thuật cho bước thiết kế

- `IProviderGateway.Request` cần thêm danh sách phần ảnh (mime + bytes). Mỗi nhà cung cấp có cách gửi ảnh riêng: `inline_data` (Gemini), `image_url` dạng data URL (OpenAI), khối `image` base64 (Claude).
- Cần thư viện dựng ảnh từ trang PDF (ví dụ Apache PDFBox) và thông tin model nào hỗ trợ ảnh trong `ProviderCatalog`.
- Cần một mục đích tệp mới cho tệp nhập (tạm thời, không phải học liệu), hoặc nhận tệp multipart trực tiếp như endpoint DOCX. Chốt ở Functional Design.
- Kết quả AI dùng lại schema khối của bản nháp AI U07 nếu khớp, cộng thêm vùng hình (trang, x, y, rộng, cao theo tỉ lệ 0–1).

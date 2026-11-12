# Nhập đề từ ảnh/PDF thành khung đề chỉnh sửa được — yêu cầu và câu hỏi xác nhận

Chưa sửa mã nguồn.

## Yêu cầu đã ghi nhận

Giảng viên có đề cũ dạng ảnh chụp hoặc bản scan (ví dụ đề SWR302_SU25_PE1_407173) và muốn tải lên để hệ thống chuyển thành đề đang chỉnh sửa, giống cách nhập DOCX hiện nay.

- **Loại yêu cầu**: Enhancement, mở rộng nhập DOCX của U07 (BR-U07 nhập khung đề) sang ảnh và PDF.
- **Phạm vi**: backend U07 (dịch vụ nhập mới bên cạnh `DocxService`), U06 (`IProviderGateway`/`HttpProviderGateway` gửi được ảnh cho Gemini, OpenAI, Claude), U03 (`PurposePolicy` thêm mục đích hoặc loại tệp); frontend `features/assignments` (`CreateAssignmentDialog`, trang soạn bài); tài liệu U07, U06, U03.
- **Độ phức tạp**: vừa đến phức tạp. Phần khó là gửi ảnh cho ba nhà cung cấp AI và ép kết quả về đúng mô hình khối của đề (`HEADING`, `PARAGRAPH`, `LIST`, `TABLE`, `IMAGE`, `DIAGRAM`).
- **Độ sâu**: Standard.

## Hiện trạng liên quan

- `POST /api/v1/assignments/{id}/skeleton:import-docx` trả bản xem trước (khối, cảnh báo, số heading/ảnh/sơ đồ); frontend lưu bằng `saveSkeleton(..., { fromDocx: true })`. Chỉ nhập được khi bài chưa phát hành.
- `HttpProviderGateway` chỉ gửi một phần text cho cả ba nhà cung cấp, chưa gửi ảnh hay PDF.
- Mục đích tệp `ASSIGNMENT_SOURCE` nhận TXT/DOCX/PDF tối đa 2MB; `DOCUMENT_IMAGE` nhận PNG/JPEG tối đa 5MB; chưa nhận WebP.
- Mọi lượt gọi AI đi qua `AiTaskService`, tính quota của giảng viên và dùng kết nối AI đã cấu hình.

## Question 1
Nhận những loại tệp nào?

A) Ảnh PNG, JPEG, WebP và PDF (cả PDF scan lẫn PDF có chữ) (đề xuất)

B) Chỉ ảnh PNG, JPEG, WebP

C) Ảnh, PDF và gộp luôn DOCX vào cùng một nút "Nhập đề từ tệp"

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Đặt chức năng ở đâu trên giao diện?

A) Cả hai chỗ: ô "Mẫu đề" của "Đề mới" trong popup tạo bài nhận thêm ảnh/PDF, và trang soạn bài có nút "Nhập đề từ tệp" cho bài chưa phát hành (đề xuất)

B) Chỉ trong popup tạo bài

C) Chỉ trên trang soạn bài

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Hệ thống chuyển đổi đến mức nào?

A) Chép đúng nguyên văn, chỉ dựng lại cấu trúc (tiêu đề, đoạn, danh sách, bảng, chữ in đậm/màu đỏ thành nhấn mạnh); không thêm, bớt hay viết lại nội dung (đề xuất)

B) Như A, thêm tách câu hỏi và đặt điểm theo số điểm ghi trong đề (ví dụ "Question 1 (0.5 point)")

C) Như B, thêm AI soạn rubric chấm cho từng câu

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Đề có hình vẽ, sơ đồ hoặc bảng dạng ảnh thì xử lý thế nào?

A) Bảng chuyển thành bảng; hình và sơ đồ cắt đúng vùng từ ảnh gốc và chèn thành khối ảnh tại vị trí đó (đề xuất)

B) Bảng chuyển thành bảng; hình và sơ đồ để một dòng giữ chỗ "[Hình: mô tả ngắn]" cho giảng viên tự chèn lại

C) Đính kèm toàn bộ ảnh gốc ở cuối đề để đối chiếu

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Giới hạn số tệp và dung lượng mỗi lần nhập?

A) Tối đa 5 ảnh (ghép theo thứ tự chọn, kéo đổi thứ tự được) hoặc 1 PDF tối đa 10 trang; mỗi tệp tối đa 10MB (đề xuất)

B) Chỉ 1 tệp mỗi lần (1 ảnh hoặc 1 PDF tối đa 10 trang)

C) Không giới hạn số trang, chỉ giới hạn dung lượng

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
Có bước xem trước trước khi ghi vào đề không?

A) Có: hiện bản xem trước kèm cảnh báo (chữ mờ, đoạn không đọc được); bài đã có nội dung thì hỏi "Thay toàn bộ" hay "Chèn vào cuối"; ghi xong vẫn Ctrl+Z được (đề xuất)

B) Không: ghi thẳng lên trang như AI soạn đề hiện nay, hoàn tác bằng Ctrl+Z

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
Đọc ảnh bằng cách nào?

A) Dùng AI đọc ảnh qua kết nối AI hiện có (Gemini/OpenAI/Claude), tính vào quota của giảng viên; model không đọc được ảnh thì báo lỗi rõ và gợi ý đổi model (đề xuất)

B) Dùng OCR chạy trên server (ví dụ Tesseract), không tốn quota nhưng đọc tiếng Việt và bố cục kém hơn, phải thêm thư viện vào image Docker

C) Thử AI trước, AI lỗi hoặc hết quota thì chuyển sang OCR

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 8
Áp dụng cho loại đề nào?

A) Chỉ bài tập (assignment) trong đợt này; nhập câu hỏi trắc nghiệm cho ngân hàng câu hỏi quiz làm sau (đề xuất)

B) Cả bài tập và ngân hàng câu hỏi quiz (tách câu hỏi, đáp án, đáp án đúng nếu có)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
Có lưu tệp ảnh/PDF gốc sau khi nhập không?

A) Không lưu: tệp chỉ dùng để chuyển đổi rồi xóa, như cách nhập DOCX hiện nay (đề xuất)

B) Lưu kèm bài làm tệp nguồn để giảng viên xem lại sau

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 10
Extension cho tính năng này?

A) Giữ như dự án: Security Baseline và Resiliency Baseline bật trong phạm vi đã duyệt (kiểm loại tệp thật, giới hạn kích thước, phân quyền chủ bài, timeout và lỗi nhà cung cấp AI), Property-Based Testing tắt (đề xuất)

B) Bật thêm Property-Based Testing cho phần chuyển kết quả AI thành khối đề

C) Other (please describe after [Answer]: tag below)

[Answer]: A

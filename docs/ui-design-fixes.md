# Sửa UI (lần 2): các chỗ buộc phải sửa DB nếu làm theo

Đối chiếu bản thiết kế "Nền tảng học tập – 45 màn theo screen flow G21 (sửa theo ui-design-fixes 17/10)" (123 artboard) với thiết kế dữ liệu của 15 unit.

File chỉ ghi những chỗ UI hiện dữ liệu hoặc hành vi mà DB hiện tại không có. Nếu dev làm đúng theo UI thì phải thêm bảng hoặc cột, hoặc đổi ràng buộc. Cách xử lý đề xuất là **sửa UI**: DB đã chốt theo tài liệu thiết kế. Nếu nhóm muốn giữ chức năng trên UI thì phải báo để sửa tài liệu và DB trước khi code.

Các lỗi khác chỉ là hiển thị hoặc câu chữ (quy đổi điểm thang 10, câu mô tả, dữ liệu mẫu lệch nhau…) và không đụng DB, nên không ghi ở đây.

---

## 1. Ngày kích hoạt tài khoản

- **Màn**: #13 Account Detail, khối "Trạng thái".
- **UI hiện tại**: có dòng "Kích hoạt · 19/09/2026 20:14" bên dưới "Ngày tạo".
- **DB hiện có**: bảng `accounts` (U01) chỉ có `created_at`, `updated_at`, `version`. Phần mật khẩu có `password_changed_at`, nhưng giá trị này đổi mỗi khi người dùng đổi hoặc đặt lại mật khẩu, nên không dùng làm ngày kích hoạt được.
- **Nếu giữ trên UI**: thêm cột `activated_at` vào `accounts`, ghi lúc tài khoản chuyển từ Chờ kích hoạt sang Hoạt động. Phải tính cả trường hợp mở lại một tài khoản chưa từng kích hoạt.
- **Sửa UI**: bỏ dòng "Kích hoạt". Giữ "Trạng thái" và "Ngày tạo".

## 2. Ngày mở lớp

- **Màn**: #29 Class Detail, tab Thông tin (cả popup Phân công giảng viên và popup Mở lại bị từ chối, vì dùng chung nền này).
- **UI hiện tại**: có dòng "Mở lớp · 05/09/2026" bên dưới "Ngày tạo".
- **DB hiện có**: bảng `course_classes` (U05) có `status` (Nháp / Đang mở / Lưu trữ), `created_at`, `updated_at`, `version`. Không có thời điểm mở lớp.
- **Nếu giữ trên UI**: thêm cột `opened_at` vào `course_classes`, ghi lúc lớp chuyển từ Nháp sang Đang mở. Phải quyết định có ghi đè khi lớp được mở lại sau lưu trữ hay không.
- **Sửa UI**: bỏ dòng "Mở lớp". Giữ "Trạng thái" và "Ngày tạo".

## 3. Ghi chú cho mức hạn mức riêng của giảng viên

- **Màn**: #10 Setting Detail — API AI của trường và hạn mức, bảng "Mức riêng từng giảng viên".
- **UI hiện tại**: bảng có cột "Ghi chú" (ví dụ "Chủ nhiệm 2 môn", "Tạm dừng theo yêu cầu khoa").
- **DB hiện có**: bảng `ai_teacher_quotas` (U06) chỉ có `account_id`, `day_tokens`, `version`, `updated_by`, `updated_at`.
- **Nếu giữ trên UI**: thêm cột ghi chú (ví dụ `note`, tối đa 200 ký tự) vào `ai_teacher_quotas`, thêm ô nhập ở popup Sửa mức riêng, và đưa ghi chú vào audit khi sửa.
- **Sửa UI**: bỏ cột "Ghi chú". Bảng chỉ còn Giảng viên, Token / ngày, nút Sửa. Lý do đổi mức riêng (nếu cần) xem ở Nhật ký audit.

## 4. Nhớ nguồn AI đã chọn

- **Màn**: popup "Chọn nguồn AI" (dùng chung cho mọi nút gọi AI).
- **UI hiện tại**: chân popup có câu "Lựa chọn được nhớ cho lần sau".
- **DB hiện có**: U06 không lưu nguồn AI ưa dùng của từng người. Theo quy tắc, mỗi yêu cầu AI phải chọn rõ một nguồn, và khi lỗi hoặc hết hạn mức thì không tự chuyển sang nguồn khác.
- **Nếu giữ trên UI**: thêm bảng hoặc cột lưu nguồn mặc định theo người dùng, có thể theo từng loại tác vụ. Phải sửa quy tắc "không có nguồn mặc định".
- **Sửa UI**: bỏ câu "Lựa chọn được nhớ cho lần sau". Mỗi lần mở popup không chọn sẵn nguồn nào; nút "Gửi yêu cầu" chỉ bật khi người dùng đã chọn.

## 5. Agent MCP làm nguồn để nền tảng gửi việc AI

- **Màn**:
  - popup "Chọn nguồn AI" (dùng chung);
  - #34 Submission Detail — popup "AI đề xuất hàng loạt";
  - #41 Submission History — popup "Nhận góp ý AI".
- **UI hiện tại**: ngoài "API của trường" và "API cá nhân" còn có lựa chọn "Agent MCP · Claude Desktop" ("Gửi tác vụ cho agent của bạn; kết quả quay lại màn này").
- **DB hiện có**: bảng `ai_tasks` (U06) có cột `source` chỉ nhận `SHARED` (API của trường) và `PERSONAL` (API cá nhân). Agent MCP chạy trong ứng dụng của người dùng và tự gọi công cụ của nền tảng; nền tảng không có kênh để đẩy việc sang agent. Kết quả agent gửi về được lưu riêng với nguồn gốc "MCP", ví dụ đề xuất chấm hoặc góp ý riêng của sinh viên.
- **Nếu giữ trên UI**: thêm giá trị `MCP` cho `ai_tasks.source`, thêm hàng đợi việc chờ agent lấy, và đổi cách tính hạn mức và trạng thái. Đây là thay đổi lớn về thiết kế.
- **Sửa UI**:
  - Bỏ lựa chọn "Agent MCP" khỏi cả ba popup.
  - Thay bằng một dòng hướng dẫn, ví dụ "Muốn dùng agent MCP: mở agent của bạn và yêu cầu đề xuất chấm cho bài này; kết quả sẽ hiện ở đây với nhãn Agent".
  - Popup "Nhận góp ý AI" của sinh viên chỉ còn lựa chọn API cá nhân, cộng dòng hướng dẫn trên.

## 6. Bài tập cho nộp không giới hạn lượt

- **Màn**: #39 Assignment Detail — bài Practice.
- **UI hiện tại**: "Số lượt · Đã dùng 2 / không giới hạn".
- **DB hiện có**: bảng `assignments` (U07) có cột `max_attempts`, bắt buộc khi phát hành, giá trị từ 1 đến 10, cho cả bài Graded và Practice. (Quiz thì được để trống là không giới hạn; bài tập thì không.)
- **Nếu giữ trên UI**: cho `assignments.max_attempts` được để trống, sửa ràng buộc khi phát hành, và sửa cách đếm lượt còn lại khi bắt đầu lượt mới.
- **Sửa UI**: hiện số lượt cụ thể, ví dụ "Đã dùng 2 / 3". Ở #26 Assignment Form bước 3 "Lịch & lượt nộp", ô "Số lượt nộp tối đa" chỉ nhận 1–10, không có lựa chọn "không giới hạn".

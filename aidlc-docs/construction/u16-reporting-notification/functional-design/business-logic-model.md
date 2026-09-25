# U16 Reporting & Notification - Business Logic Model

## F1 - Nhận event
1. Listener cho mỗi event ở `domain-entities.md` §6 → xác định người nhận (U04/U12/U14) → INSERT `Notification` (ON CONFLICT bỏ qua) (BR-U16-01…03).
2. Loại có email → tạo `EmailOutbox` (`SKIPPED` nếu người dùng tắt) (BR-U16-10, 11).
3. Đẩy SSE tới người nhận đang trực tuyến (BR-U16-04).

## F2 - Gửi email
1. Job U02 `U16_EMAIL_DISPATCH` mỗi phút lấy `QUEUED`/`DEFERRED` đến hạn theo ưu tiên (BR-U16-12).
2. Còn trần ngày → tạo job `U16_EMAIL_SEND {outboxId}`; hết trần → `DEFERRED` sang 00:05 ngày sau.
3. Job gửi: tra email (U01), render mẫu tiếng Việt, gửi SMTP (Gmail/Mailpit), `SENT`/retry/`FAILED` (BR-U16-14).

## F3 - Nhắc hạn nộp
1. `u08.assignment.opened` → tạo `DeadlineReminder` tại `closesAt − 24h` (bỏ qua nếu đã qua) + job U02 tại thời điểm đó.
2. Job: kiểm publication còn `OPEN` và hạn không đổi → lấy người chưa nộp (U11/U14) → tạo thông báo `DEADLINE_REMINDER` (BR-U16-20…22).
3. Đổi hạn (U08 sửa lịch) → cập nhật `remindAt` (job cũ tự bỏ qua vì so thời điểm).

## F4 - Người dùng
1. Chuông thông báo: số chưa đọc, danh sách, đánh dấu đã đọc/tất cả đã đọc.
2. Cài đặt email theo loại.

## F5 - Báo cáo tiến độ
1. Giảng viên chọn lượt phát hành → tổng hợp theo BR-U16-30.

## F6 - Dashboard cá nhân
1. Lấy lớp đang ghi danh qua U04, bài đang mở/sắp hạn qua U08, trạng thái/lượt của chính người học qua U11/U14, điểm `PUBLISHED` qua U15 (BR-U16-40, 41).
2. Nếu lớp bật phân bố, U15 tổng hợp điểm công bố theo bài; U16 chỉ trả các khoảng đáp ứng BR-U16-42. Không trả điểm của người khác.

## F7 - Xuất bảng điểm
1. Kiểm quyền lớp/bài và bộ lọc trước khi đọc dữ liệu; lấy bảng điểm từ U15, trạng thái nộp từ U11/U14 (BR-U16-43, 44).
2. Tạo CSV hoặc XLSX dạng stream, escape giá trị không tin cậy, audit yêu cầu xuất; không lưu tệp xuất vào U03 (BR-U16-45).

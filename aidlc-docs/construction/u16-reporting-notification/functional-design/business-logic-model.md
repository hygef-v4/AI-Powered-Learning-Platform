# U16 Reporting & Notification - Business Logic Model

## F1 - Nhận event
1. Listener cho mỗi event ở `domain-entities.md` §7 → xác định người nhận (từ payload như `grade.published` kèm danh sách người học, hoặc tra U04/U12/U14) → INSERT `notifications` (ON CONFLICT bỏ qua) (BR-U16-01…03).
2. Loại có email → `email_status = QUEUED` (`SKIPPED` nếu `accounts.email_preferences` tắt loại đó), ngược lại `NONE`; sau commit gửi việc `EMAIL_SEND {notificationId}` lên `jobs.email` với độ ưu tiên theo loại (BR-U16-10…12).
3. Đẩy SSE tới người nhận đang trực tuyến (BR-U16-04).

## F2 - Gửi email
1. Handler `EMAIL_SEND`: chỉ gửi khi `email_status IN ('QUEUED','DEFERRED')`; `INCR email:daily-count:{yyyyMMdd}`; hết trần → `DEFERRED` (BR-U16-12).
2. Còn trần: tra email (U01), render mẫu tiếng Việt, gửi SMTP (Gmail/Mailpit) → `SENT`; lỗi tạm → retry qua queue `jobs.retry.*` của U03, quá 5 lần → `FAILED` (BR-U16-14).
3. `EmailDeferredScanner` lúc 00:05 chuyển `DEFERRED` → `QUEUED` theo ưu tiên, thời điểm tạo và gửi lại việc; `PendingSweeper` gửi lại việc cho dòng `QUEUED` quá 5 phút.

## F3 - Nhắc hạn nộp
1. `DeadlineReminderScanner` mỗi phút chọn bài đến mốc nhắc theo `domain-entities.md` §5.
2. Lấy người chưa nộp (U11/U14) → tạo thông báo `DEADLINE_REMINDER` → ghi `assignments.reminder_sent_at` trong cùng transaction (BR-U16-20…22).
3. Gia hạn (U08 chỉ cho kéo dài khi `OPEN`) làm mốc nhắc mới muộn hơn `reminder_sent_at` nên scanner nhắc lại một lần theo hạn mới; bài ngưng giao/đóng không còn được chọn.

## F4 - Người dùng
1. Chuông thông báo: số chưa đọc, danh sách, đánh dấu đã đọc/tất cả đã đọc.
2. Cài đặt email theo loại: ghi `accounts.email_preferences`.

## F5 - Báo cáo tiến độ
1. Giảng viên chọn bài → tổng hợp theo BR-U16-30.

## F6 - Thống kê quản trị và phân bố điểm
1. Thống kê: kiểm actor là `ADMIN`; lấy số tài khoản theo vai trò × trạng thái qua `AccountLookupPort` (U01), số môn/lớp theo trạng thái và số ghi danh `ACTIVE` qua `ClassAccessPort` (U04); ghép thành `AdminStatistics` và trả về, không lưu (BR-U16-40, 41).
2. Phân bố điểm: Assignment List của Student (U11) nhúng `GradeDistributionBadge`; badge gọi API của U16 với `classId`; U16 kiểm Student đang ghi danh lớp và cờ `showGradeDistribution` của U04, lấy điểm `PUBLISHED` theo bài qua `GradeQueryPort` (U15) và chỉ trả các khoảng đáp ứng BR-U16-42. Không trả điểm của người khác.

## F7 - Xuất bảng điểm
1. Kiểm quyền lớp/bài và bộ lọc trước khi đọc dữ liệu; lấy bảng điểm từ U15, trạng thái nộp từ U11/U14 (BR-U16-43, 44).
2. Tạo CSV hoặc XLSX dạng stream, escape giá trị không tin cậy, audit yêu cầu xuất; không lưu tệp xuất vào U03 (BR-U16-45).

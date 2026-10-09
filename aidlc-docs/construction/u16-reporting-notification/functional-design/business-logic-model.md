# U16 Reporting & Notification - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 12, 58 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); luồng phụ: phân bố điểm của UC 22, tiến độ nộp của UC 37, phần xuất tệp của UC 40; primary stories: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Vai trò theo quyết định 2026-10-09: Subject Manager kế thừa Teacher; Teacher, Student kế thừa User; Admin chỉ dùng chức năng User và quản trị (không vào lớp, không R2/R3/R4, không có ví). Màn của U16 trên screen flow: Admin Dashboard (UC 58). UC 12 không có màn riêng: chuông và popup thông báo nằm trên thanh điều hướng của mọi vai trò.

## F1 - Nhận event
1. `NotificationListener` nhận event ở `domain-entities.md` §7 và xác định người nhận (BR-U16-01…03, 06, 09):
   - Theo lớp: người học ghi danh `ACTIVE` qua `ClassAccessPort.listActiveStudents(classId)` (U04).
   - `assignment.opened` có `subjectId` (bài hoặc quiz của môn): lấy mọi lớp `OPEN` của môn qua U04 rồi gộp người học `ACTIVE` của từng lớp, mỗi người một thông báo.
   - `assignment.opened` có `type = MULTIPLE_CHOICE_QUIZ`: loại `QUIZ_PUBLISHED`, chỉ trong app; dạng khác: `ASSIGNMENT_OPENED`, app và email.
   - `grade.published`: danh sách người học trong payload; `group.*`: thành viên/giảng viên lớp theo payload hoặc `GroupMembershipPort` (U12); `group.submitted`: thành viên nhóm (U12); `payment.paid`: người mua; `class.announcement-posted`: người học `ACTIVE` của lớp trừ actor.
2. INSERT `notifications` theo lô (ON CONFLICT bỏ qua). Loại có email → `email_status = QUEUED` (`SKIPPED` nếu `accounts.email_preferences` tắt loại đó), ngược lại `NONE`; sau commit gửi việc `EMAIL_SEND {notificationId}` lên `jobs.email` với độ ưu tiên theo loại (BR-U16-10…12).
3. Sau commit phát `notification.created {recipientIds}` lên `platform.realtime`; `SseHub` đẩy tới người nhận đang trực tuyến (BR-U16-04).

## F2 - Gửi email
1. Handler `EMAIL_SEND`: chỉ gửi khi `email_status IN ('QUEUED','DEFERRED')`; `INCR email:daily-count:{yyyyMMdd}`; hết trần → `DEFERRED` (BR-U16-12).
2. Còn trần: tra email (U01), render mẫu tiếng Việt, gửi SMTP (Brevo/Mailpit) → `SENT`; lỗi tạm → retry qua queue `jobs.retry.*` của U03, quá 5 lần → `FAILED` (BR-U16-14).
3. `EmailDeferredScanner` lúc 00:05 chuyển `DEFERRED` → `QUEUED` theo ưu tiên, thời điểm tạo và gửi lại việc; `PendingSweeper` gửi lại việc cho dòng `QUEUED` quá 5 phút.

## F3 - Nhắc hạn nộp
1. `DeadlineReminderScanner` mỗi phút chọn bài tập (không phải quiz) có `closes_at` tới mốc nhắc theo `domain-entities.md` §5.
2. Lấy người chưa nộp (U11/U14): bài của lớp theo lớp đó; bài của môn theo mọi lớp `OPEN` của môn. Tạo thông báo `DEADLINE_REMINDER` và ghi `assignments.reminder_sent_at` trong cùng transaction (BR-U16-20…22).
3. Gia hạn (U08 chỉ cho kéo dài khi `OPEN`) làm mốc nhắc mới muộn hơn `reminder_sent_at` nên scanner nhắc lại một lần theo hạn mới; bài ngưng giao/đóng không còn được chọn.

## F4 - Xem thông báo (UC 12)
1. Thanh điều hướng của mọi vai trò có `NotificationBell` với số chưa đọc (SSE, mất kết nối thì tải lại số).
2. Bấm chuông mở popup thông báo: mới nhất trước, 20 mỗi trang, "Xem thêm"; "Đánh dấu tất cả đã đọc".
3. Bấm một thông báo: đánh dấu đã đọc rồi mở `link` tới màn liên quan (`domain-entities.md` §2); màn đích tự kiểm quyền hiện hành (BR-U16-07).
4. Student có phần "Email" trong popup để bật/tắt email từng loại; ghi `accounts.email_preferences` (BR-U16-11, 15).

## F5 - Tiến độ nộp bài (luồng phụ của UC 37)
1. Giảng viên lớp (R3/R4) mở Submission Detail (U15) của một bài; `SubmissionProgressPanel` gọi API tiến độ kèm `classId`.
2. Kiểm R3/R4 của lớp và bài thuộc lớp hoặc thuộc môn chứa lớp; ngoài quyền `404` (BR-U16-31).
3. Tổng hợp theo BR-U16-30, chỉ trên người học của lớp đó (bài của môn cũng tính theo từng lớp).

## F6 - Admin Dashboard (UC 58)
1. Admin đăng nhập, Post-Login chuyển tới `/admin` (Admin Dashboard, BR-U01-48). Kiểm actor là `ADMIN` `ACTIVE`.
2. Lấy số tài khoản theo vai trò × trạng thái qua `AccountLookupPort` (U01); số môn/lớp theo trạng thái và số ghi danh `ACTIVE` qua `ClassAccessPort` (U04); ghép thành `AdminStatistics` và trả về, không lưu (BR-U16-40, 41).
3. Trang hiện số liệu và các ô dẫn tới Account List (U01), Subject List (U04), Credit Package List, Payment History (U07), Setting List (U03), Audit Log (U02) (BR-U16-46).

## F7 - Phân bố điểm (luồng phụ của UC 22)
1. Student Assignments (U11) nhúng `GradeDistributionBadge` cho từng bài; badge gọi API của U16 với `classId`.
2. U16 kiểm Student ghi danh `ACTIVE` lớp và cờ `showGradeDistribution` của lớp (U04, Chủ nhiệm môn bật trong UC 52).
3. Lấy điểm `PUBLISHED` của người học trong lớp theo bài `GRADED` qua U15 (bài của môn: chỉ điểm đã công bố của lớp này), chỉ trả các khoảng đáp ứng BR-U16-42. Không trả điểm của người khác; quiz không có phân bố.

## F8 - Xuất bảng điểm (phần xuất của UC 40)
1. Trên Gradebook của U15, giảng viên lớp chọn lớp hoặc một bài và định dạng; U16 kiểm R3/R4 của lớp, bài và bộ lọc trước khi đọc dữ liệu (BR-U16-43).
2. Lấy bảng điểm từ U15, trạng thái nộp từ U11/U14 (BR-U16-44); bài của môn lấy theo lớp được xuất.
3. Tạo CSV hoặc XLSX dạng stream, escape giá trị không tin cậy, audit yêu cầu xuất; không lưu tệp xuất vào U03 (BR-U16-45).

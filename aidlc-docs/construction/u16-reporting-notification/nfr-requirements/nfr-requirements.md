# U16 Reporting & Notification - NFR Requirements

**Bản tài liệu 2026-10-08**: UC 11, 57; primary stories: US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-01 | Thông báo trong app xuất hiện ≤ 5 s sau khi nghiệp vụ commit. | BR-U16-04 |
| NFR-U16-02 | Một event cho lớp 200 người tạo xong thông báo ≤ 5 s (INSERT theo lô). | BR-U16-01 |
| NFR-U16-03 | Gửi email tối đa 1 email/giây (giới hạn tốc độ ở ứng dụng), trong trần thông báo 300/ngày (cấu hình). Trần phải phù hợp quota Brevo và chừa dung lượng cho OTP dùng chung tài khoản, theo shared-infrastructure §8. | BR-U16-12 |
| NFR-U16-04 | Báo cáo tiến độ 200 người p95 ≤ 1 s. | BR-U16-30 |
| NFR-U16-05 | Thống kê quản trị và phân bố điểm p95 ≤ 1 s; xuất bảng điểm lớp 200 người × 30 bài p95 ≤ 5 s khi dữ liệu nguồn bình thường. | US-RPT-002, 003 |

## 2. Toàn vẹn

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-10 | Unique `(source_event_id, account_id, type)`; gửi email idempotent theo `notifications.id` và `email_status`; bộ đếm trần ngày trong Redis `email:daily-count:{yyyyMMdd}` tăng trước khi gửi, giảm lại nếu gửi lỗi vĩnh viễn. | BR-U16-02, 12, 14 |
| NFR-U16-11 | Nhắc hạn tính theo hạn hiện tại mỗi lần quét; đổi hạn thì nhắc theo hạn mới, không nhắc trùng (`reminder_sent_at`). | BR-U16-22 |

## 3. Bảo mật và riêng tư

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-20 | Người dùng chỉ đọc thông báo của mình; kênh SSE thông báo gắn với phiên đăng nhập. | SEC-002 |
| NFR-U16-21 | Email không chứa điểm, nội dung bài, dữ liệu người khác; chỉ tiêu đề ngắn và đường dẫn tới hệ thống. | BR-U16-03 |
| NFR-U16-22 | `SMTP_*` trong `.env`; không log địa chỉ email người nhận (log `accountId`). | SEC-005, SEC-006 |
| NFR-U16-23 | Mẫu email tiếng Việt, escape mọi giá trị chèn (tên lớp, tên bài). | SEC-003 |
| NFR-U16-24 | Thống kê chỉ cho `ADMIN` và chỉ trả số đếm; phân bố lớp cần cờ cho phép và ngưỡng BR-U16-42; export kiểm quyền trước khi tạo file và escape CSV formula injection. | US-RPT-002, 003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-30 | Unit test mọi `BR-U16-xx`; ưu tiên và dời khi hết trần; tắt email từng loại. | NFR-004 |
| NFR-U16-31 | Integration test với Mailpit: event lặp không gửi trùng; SMTP lỗi thì retry; nhắc hạn bị hủy khi ngừng giao. | NFR-004 |
| NFR-U16-32 | Kiểm chứng sự kiện U05 gửi đúng người, thống kê từ chối vai trò khác `ADMIN` và không lộ dữ liệu cá nhân, phân bố lớp ẩn khi thiếu mẫu và export ngoài quyền bị từ chối. | US-CNT-004, US-RPT-002, 003 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U16-22 |
| SECURITY-05 | Compliant | NFR-U16-23 |
| SECURITY-08 | Compliant | NFR-U16-20 |
| SECURITY-09 | Compliant | SMTP trong `.env` |
| SECURITY-15 | Compliant | Lỗi thông báo không rollback nghiệp vụ |
| RESILIENCY-10 | Compliant | Timeout SMTP 10 s |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

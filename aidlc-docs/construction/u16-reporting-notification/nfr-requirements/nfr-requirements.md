# U16 Reporting & Notification - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-01 | Thông báo trong app xuất hiện ≤ 5 s sau khi nghiệp vụ commit. | BR-U16-04 |
| NFR-U16-02 | Một event cho lớp 200 người tạo xong thông báo ≤ 5 s (INSERT theo lô). | BR-U16-01 |
| NFR-U16-03 | Gửi email tối đa 1 email/giây (tránh Gmail chặn), trong trần 300/ngày. | BR-U16-12 |
| NFR-U16-04 | Báo cáo tiến độ 200 người p95 ≤ 1 s. | BR-U16-30 |
| NFR-U16-05 | Dashboard của người học p95 ≤ 1 s; xuất bảng điểm lớp 200 người × 30 bài p95 ≤ 5 s khi dữ liệu nguồn bình thường. | US-RPT-002, 003 |

## 2. Toàn vẹn

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-10 | Unique `(source_event_id, recipient_id, type)`; email outbox idempotent theo `id`; bộ đếm trần ngày trong Redis `u16:email:{yyyyMMdd}` tăng trước khi gửi, giảm lại nếu gửi lỗi vĩnh viễn. | BR-U16-02, 12, 14 |
| NFR-U16-11 | Job nhắc hạn so `remindAt` với hạn hiện tại; lệch thì bỏ qua (đổi lịch). | BR-U16-22 |

## 3. Bảo mật và riêng tư

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-20 | Người dùng chỉ đọc thông báo của mình; kênh SSE thông báo gắn với phiên đăng nhập. | SEC-002 |
| NFR-U16-21 | Email không chứa điểm, nội dung bài, dữ liệu người khác; chỉ tiêu đề ngắn và đường dẫn tới hệ thống. | BR-U16-03 |
| NFR-U16-22 | `SMTP_*` trong `.env`; không log địa chỉ email người nhận (log `accountId`). | SEC-005, SEC-006 |
| NFR-U16-23 | Mẫu email tiếng Việt, escape mọi giá trị chèn (tên lớp, tên bài). | SEC-003 |
| NFR-U16-24 | Dashboard chỉ trả điểm `PUBLISHED` của chính người học; phân bố lớp cần cờ cho phép và ngưỡng BR-U16-42; export kiểm quyền trước khi tạo file và escape CSV formula injection. | US-RPT-002, 003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U16-30 | Unit test mọi `BR-U16-xx`; ưu tiên và dời khi hết trần; tắt email từng loại. | NFR-004 |
| NFR-U16-31 | Integration test với Mailpit: event lặp không gửi trùng; SMTP lỗi thì retry; nhắc hạn bị hủy khi ngừng giao. | NFR-004 |
| NFR-U16-32 | Kiểm chứng sự kiện U05 gửi đúng người, dashboard không lộ điểm chưa công bố/người khác, phân bố lớp ẩn khi thiếu mẫu và export ngoài quyền bị từ chối. | US-CNT-004, US-RPT-002, 003 |

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

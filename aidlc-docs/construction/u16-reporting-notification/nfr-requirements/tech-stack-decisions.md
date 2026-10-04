# U16 Reporting & Notification - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Email | Spring `JavaMailSender` (SMTP Gmail App Password; Mailpit khi dev), timeout 10 s | Như U01 |
| Mẫu email | Thymeleaf text/HTML, tiếng Việt | Escape mặc định |
| Realtime | SSE dùng chung `SseHub` (U14) với kênh theo người dùng | Không thêm hạ tầng |
| Lịch | `ScheduledScanner` của U03 (nhắc hạn mỗi phút, xếp lại email dời, dọn dẹp) và queue `jobs.email` | Dùng lại U03, không cần bảng job |
| Xuất bảng điểm | Apache POI XSSF cho XLSX; CSV ghi stream UTF-8, escape công thức | Dùng thư viện POI đã có ở U09; không cần lưu file xuất |

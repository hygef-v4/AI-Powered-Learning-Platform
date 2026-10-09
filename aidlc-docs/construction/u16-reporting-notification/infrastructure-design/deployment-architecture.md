# U16 Reporting & Notification - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 12, 58 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); luồng phụ: phân bố điểm của UC 22, tiến độ nộp của UC 37, phần xuất tệp của UC 40; primary stories: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
+---------------------------+      +----------------------------------+      +------------------------------+
| rabbitmq platform.events  |----->| worker U16                       |----->| postgres                     |
| queue jobs.notification   |      | NotificationListener, scanners   |      | notifications                |
+---------------------------+      +----------------------------------+      | assignments.reminder_sent_at |
                                       |                         |           +------------------------------+
                                       | jobs.email              | platform.realtime
                                       v                         v
                     +----------------------------+   +----------------------------+
                     | worker EmailSendHandler    |   | backend SseHub             |
                     | redis email:daily-count    |   | kenh theo accountId        |
                     +----------------------------+   +----------------------------+
                                       |                         |
                                       | SMTP                    | SSE chuong thong bao
                                       v                         v
                     +----------------------------+   +----------------------------+
                     | Brevo 587, Mailpit 1025    |   | trinh duyet                |
                     +----------------------------+   +----------------------------+
                                                                 |
                                                                 | REST
                                                                 v
                     +---------------------------------------------------------------+
                     | nginx --> backend U16: thong bao, Admin Dashboard, tien do,   |
                     | phan bo diem, xuat bang diem                                  |
                     +---------------------------------------------------------------+
```

**Text alternative**: Worker của U16 nghe event nghiệp vụ trên `platform.events` (queue `jobs.notification`), ghi thông báo (kèm trạng thái email) vào PostgreSQL và gửi việc email lên queue `jobs.email`; `EmailSendHandler` kiểm trần ngày bằng bộ đếm Redis rồi gửi qua SMTP (Brevo khi demo/production, Mailpit khi dev). Scanner của U16 nhắc hạn nộp, xếp lại email bị dời và xóa thông báo cũ. Worker phát tín hiệu lên `platform.realtime`; `SseHub` ở backend đẩy chuông thông báo xuống trình duyệt qua SSE. Trình duyệt gọi REST qua Nginx tới backend để đọc thông báo và cài đặt email (popup thông báo), xem Admin Dashboard, tiến độ nộp, phân bố điểm và xuất bảng điểm.

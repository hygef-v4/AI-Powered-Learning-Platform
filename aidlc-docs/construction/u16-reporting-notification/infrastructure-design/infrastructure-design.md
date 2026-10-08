# U16 Reporting & Notification - Infrastructure Design

**Bản tài liệu 2026-10-08**: UC 11, 57; primary stories: US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `NotificationController`, `PreferenceController`, `ProgressController`, `StatisticsController`, `GradeDistributionController`, `GradebookExportController`, kênh SSE thông báo | `backend` |
| `NotificationListener`, `NotificationFanout`, `EmailSendHandler`, `EmailDeferredScanner`, `DeadlineReminderScanner`, `NotificationRetentionScanner` | `worker` |
| Bảng `notifications`; cột `accounts.email_preferences` (U01 tạo bảng), `assignments.reminder_sent_at` (U08 tạo bảng) | `postgres` |
| Bộ đếm trần email | `redis`, khóa `email:daily-count:{yyyyMMdd}` (TTL 48 giờ) |
| RabbitMQ | queue `jobs.notification` (U16 khai báo) bind `platform.events` với `enrollment.activated`, `class.*`, `payment.paid`, `assignment.opened`, `group.*`, `grade.published`; việc `EMAIL_SEND` trên `jobs.email` (priority queue); nhắc hạn, email dời và dọn dẹp chạy bằng `ScheduledScanner`; phát realtime qua fanout `platform.realtime` |
| SMTP | Brevo `smtp-relay.brevo.com:587` STARTTLS (SMTP login + SMTP key) khi demo/production; `mailpit:1025` khi dev |

- Dùng chung fanout `platform.realtime` với U14; `SseHub` phân kênh theo khóa: `groupDocumentId` (tài liệu nhóm U14) và `accountId` (U16).

## 2. Nginx

- `GET /api/v1/me/notifications/stream` (SSE): cấu hình như SSE của U14 (`proxy_buffering off`, `proxy_read_timeout 1h`).
- Export CSV/XLSX trả stream qua backend với `Content-Disposition: attachment`, không lưu vào U03; giới hạn lớp/bài theo quyền trước khi bắt đầu stream.

## 3. Migration

`V20260925_2300__u16_notifications.sql`:
- `notifications (id, account_id FK, type, title, body, link, source_event_id, email_status, email_sent_at, read_at, created_at)` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md); unique `(source_event_id, account_id, type)`, index `(account_id, read_at, created_at)`, `(email_status, created_at)`.
- `ALTER TABLE accounts ADD COLUMN email_preferences jsonb NOT NULL DEFAULT '{}'` (cần migration U01 chạy trước).
- `NotificationRetentionScanner` hằng ngày xóa thông báo quá 180 ngày.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | Compliant | `SMTP_*` trong secret CI/CD (đã có) |
| RESILIENCY-10 | Compliant | Timeout SMTP 10 s |
| Rule còn lại | N/A | Dùng chung deploy, health của backend |

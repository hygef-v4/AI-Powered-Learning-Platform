# U16 Reporting & Notification - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `NotificationController`, `PreferenceController`, `ProgressController`, `LearnerDashboardController`, `GradebookExportController`, kênh SSE thông báo | `backend` |
| `NotificationListener`, `NotificationFanout`, `EmailDispatcher`, `EmailSendHandler`, `DeadlineReminderHandler` | `worker` |
| Bảng `notifications`, `email_outbox`, `notification_preferences`; nhắc hạn là job trong bảng `jobs` (U02) | `postgres` |
| Bộ đếm trần email | `redis`, khóa `email:daily-count:{yyyyMMdd}` (TTL 48 giờ) |
| RabbitMQ | queue `jobs.notification` (U16 khai báo) bind `platform.events` với `enrollment.activated`, `class.*`, `payment.paid`, `assignment.opened`, `group.*`, `grade.published`; job trên queue `jobs.scheduled` (`EMAIL_DISPATCH`, `DEADLINE_REMINDER`) và `jobs.email` (`EMAIL_SEND`); phát realtime qua fanout `platform.realtime` |
| SMTP | Gmail `smtp.gmail.com:587` STARTTLS (App Password) khi demo; `mailpit:1025` khi dev |

- Dùng chung fanout `platform.realtime` với U14; `SseHub` phân kênh theo `groupId` (tài liệu nhóm U14) và `accountId` (U16).

## 2. Nginx

- `GET /api/v1/me/notifications/stream` (SSE): cấu hình như SSE của U14 (`proxy_buffering off`, `proxy_read_timeout 1h`).
- Export CSV/XLSX trả stream qua backend với `Content-Disposition: attachment`, không lưu vào U03; giới hạn lớp/bài theo quyền trước khi bắt đầu stream.

## 3. Migration

`V20260925_2300__u16_notifications.sql`:
- `notifications` unique `(source_event_id, recipient_id, type)`, index `(recipient_id, read_at, created_at)`.
- `email_outbox` index `(status, scheduled_for, priority)`.
- `notification_preferences (account_id, type)` PK.
- Job U02 `U16_RETENTION` hằng ngày xóa thông báo quá 180 ngày.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | Compliant | `SMTP_*` trong secret CI/CD (đã có) |
| RESILIENCY-10 | Compliant | Timeout SMTP 10 s |
| Rule còn lại | N/A | Dùng chung deploy, health của backend |

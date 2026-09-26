# U16 Reporting & Notification - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `NotificationController`, `PreferenceController`, `ProgressController`, `LearnerDashboardController`, `GradebookExportController`, kênh SSE thông báo | `backend` |
| `NotificationListener`, `NotificationFanout`, `EmailDispatcher`, `EmailSendHandler`, `DeadlineReminderHandler` | `worker` |
| Bảng `notifications`, `email_outbox`, `notification_preferences`; nhắc hạn là job trong bảng `jobs` (U02) | `postgres` |
| Bộ đếm trần email | `redis`, khóa `u16:email:{yyyyMMdd}` (TTL 48 giờ) |
| RabbitMQ | queue `u16.notification-listener` bind `platform.events` với `u04.enrollment.activated`, `u05.class.*`, `u07.payment.paid`, `u08.assignment.*`, `u12.group.*`, `u14.group.submitted`, `u15.grade.published`; queue `jobs.u16.email-dispatch`, `jobs.u16.email-send`, `jobs.u16.deadline-reminder`; phát realtime qua fanout `platform.realtime` |
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

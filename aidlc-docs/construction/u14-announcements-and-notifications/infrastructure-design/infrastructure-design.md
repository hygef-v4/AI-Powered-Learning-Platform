# U14 Announcements & Notifications — infrastructure-design

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, SSE), `worker` (listener sự kiện, job email, scanner) |
| Bảng | `announcements`, `notifications`, `email_deliveries`, `notification_preferences`, `deadline_reminders` trong `postgres` |
| Migration | `db/migration/notifications/V20260925_2000__create_notification_tables.sql`: `announcements` (index `(class_id, status, created_at)`; FK `class_id` → `course_classes`); `notifications` (unique `(source_event_id, recipient_id, type)`, index `(recipient_id, read_at, created_at)`, `(created_at)`); `email_deliveries` (unique `notification_id`, index `(status, scheduled_for, priority)`); `notification_preferences` (khóa `(account_id, type)`); `deadline_reminders` (khóa `(assignment_id, class_id, closes_at)`); chạy sau `materials` (1900) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`; `DELETE` chỉ trên `notifications` (dọn quá hạn) |
| RabbitMQ | Queue `jobs.notification` bind `platform.events`; job `EMAIL_SEND` trên `jobs.email` |
| Biến môi trường | `U14_EMAIL_DAILY_CAP` (mặc định 300) |
| Nginx | Kênh SSE `/api/v1/me/notifications/events` không đệm, như U12 |
| Mạng ra | Không gọi trực tiếp; SMTP qua U01 |

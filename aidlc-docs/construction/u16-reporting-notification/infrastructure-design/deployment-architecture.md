# U16 Reporting & Notification - Deployment Architecture

```
 [rabbitmq: platform.events] --> [worker: U16] --> [postgres: notifications, assignments.reminder_sent_at]
                                      |    |
                                      |    +--jobs.email--> [worker: EmailSendHandler] --> SMTP (Brevo 587 / Mailpit 1025)
                                      +--> [rabbitmq: platform.realtime] --> [backend: SseHub]
                                                                                  |
 Trình duyệt <========== SSE (chuông thông báo) =================================+
 Trình duyệt --REST--> [nginx] --> [backend: thông báo, cài đặt, tiến độ] --> [redis: trần email]
```

**Text alternative**: Worker của U16 nghe event nghiệp vụ trên `platform.events`, ghi thông báo (kèm trạng thái email) vào PostgreSQL, gửi việc email lên queue `jobs.email`; handler gửi qua SMTP (Brevo khi demo/production, Mailpit khi dev). Scanner của U16 nhắc hạn nộp và xếp lại email bị dời. Worker phát tín hiệu lên `platform.realtime`; backend đẩy chuông thông báo xuống trình duyệt qua SSE. Người dùng đọc thông báo, cài đặt email và giảng viên xem tiến độ qua REST; bộ đếm trần email nằm trong Redis.

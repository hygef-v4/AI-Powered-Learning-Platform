# U16 Reporting & Notification - Deployment Architecture

```
 [rabbitmq: platform.events] --> [worker: U16] --> [postgres: notifications, outbox]
                                      |    |
                                      |    +--> SMTP (Gmail 587 / Mailpit 1025)
                                      +--> [rabbitmq: platform.realtime] --> [backend: SseHub]
                                                                                  |
 Trình duyệt <========== SSE (chuông thông báo) =================================+
 Trình duyệt --REST--> [nginx] --> [backend: thông báo, cài đặt, tiến độ] --> [redis: trần email]
```

**Text alternative**: Worker của U16 nghe event nghiệp vụ trên `platform.events`, ghi thông báo và hàng đợi email vào PostgreSQL, gửi email qua SMTP (Gmail khi demo, Mailpit khi dev) và phát tín hiệu lên `platform.realtime`; backend đẩy chuông thông báo xuống trình duyệt qua SSE. Người dùng đọc thông báo, cài đặt email và giảng viên xem tiến độ qua REST; bộ đếm trần email nằm trong Redis.

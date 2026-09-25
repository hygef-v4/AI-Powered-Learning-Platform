# U16 Reporting & Notification - Logical Components

## 1. Sơ đồ

```
 events u04/u07/u08/u12/u14/u15 --> worker: NotificationListener --> NotificationFanout (P1)
                                                                        |
                                            fanout realtime <-----------+ (sau commit)
                                                  |
 Trình duyệt <==SSE== backend: SseHub <-----------+
 Trình duyệt --REST--> backend: NotificationController, PreferenceController, ProgressController
 worker: EmailDispatcher (P2) --> EmailSendHandler (P3) --> SMTP (Gmail / Mailpit)
 worker: DeadlineReminderHandler (P4) --> NotificationFanout
```

**Text alternative**: Worker nghe event của các unit, tạo thông báo theo lô và phát tín hiệu realtime; backend đẩy tới trình duyệt qua SSE. Người dùng đọc thông báo, cài đặt email và giảng viên xem tiến độ qua REST. Worker điều phối email theo trần và ưu tiên, gửi qua SMTP; job nhắc hạn tạo thông báo cho người chưa nộp.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `NotificationListener`, `NotificationFanout` | worker | F1; P1 |
| `EmailDispatcher`, `EmailSendHandler` | worker | F2; P2, P3 |
| `DeadlineReminderHandler` | worker | F3; P4 |
| `NotificationController`, `PreferenceController` | backend | F4 |
| `ProgressController`, `ProgressService` | backend | F5; P5 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U16_EMAIL_DAILY_CAP` | 300 |
| `U16_EMAIL_PER_SECOND` | 1 |
| `U16_REMINDER_HOURS_BEFORE` | 24 |
| `U16_RETENTION_DAYS` | 180 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log email |
| SECURITY-05 | Compliant | Escape mẫu |
| SECURITY-08 | Compliant | Kênh/danh sách theo người dùng |
| SECURITY-09 | Compliant | SMTP trong `.env` |
| SECURITY-15 | Compliant | P3 |
| RESILIENCY-10 | Compliant | Timeout SMTP |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

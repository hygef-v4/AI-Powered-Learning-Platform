# U16 Reporting & Notification - Logical Components

**Bản tài liệu 2026-10-08**: UC 11, 57; primary stories: US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 events u04/u05/u07/u08/u12/u14/u15 --> worker: NotificationListener --> NotificationFanout (P1)
                                                                        |
                                            fanout realtime <-----------+ (sau commit)
                                                  |
 Trình duyệt <==SSE== backend: SseHub <-----------+
 Trình duyệt --REST--> backend: NotificationController, PreferenceController, ProgressController,
                          StatisticsController, GradeDistributionController, GradebookExportController
 worker: jobs.email --> EmailSendHandler (P2, P3) --> SMTP (Brevo / Mailpit); EmailDeferredScanner (P2)
 worker: DeadlineReminderScanner (P4) --> NotificationFanout
```

**Text alternative**: Worker nghe event của các unit, kể cả thông báo lớp từ U05, tạo thông báo theo lô và phát tín hiệu realtime; backend đẩy tới trình duyệt qua SSE. Người dùng đọc thông báo, cài đặt email; giảng viên xem tiến độ và xuất bảng điểm; Administrator xem thống kê; Student xem phân bố điểm qua REST. Worker gửi email từ queue `jobs.email` theo trần và ưu tiên qua SMTP; scanner nhắc hạn tạo thông báo cho người chưa nộp.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `NotificationListener`, `NotificationFanout` | worker | F1; P1 |
| `EmailSendHandler`, `EmailDeferredScanner` | worker | F2; P2, P3 |
| `DeadlineReminderScanner` | worker | F3; P4 |
| `NotificationRetentionScanner` | worker | Xóa thông báo quá 180 ngày |
| `NotificationController`, `PreferenceController` | backend | F4 |
| `ProgressController`, `ProgressService` | backend | F5; P5 |
| `StatisticsController`, `StatisticsService` | backend | F6 bước 1; chỉ `ADMIN`, chỉ số đếm |
| `GradeDistributionController`, `GradeDistributionService` | backend | F6 bước 2; chỉ đọc điểm đã công bố, ngưỡng BR-U16-42 |
| `GradebookExportController`, `GradebookExportService` | backend | F7; CSV/XLSX theo yêu cầu |

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

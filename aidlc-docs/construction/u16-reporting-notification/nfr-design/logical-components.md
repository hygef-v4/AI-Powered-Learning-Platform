# U16 Reporting & Notification - Logical Components

**Bản tài liệu 2026-10-09**: UC 12, 58 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); luồng phụ: phân bố điểm của UC 22, tiến độ nộp của UC 37, phần xuất tệp của UC 40; primary stories: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
+---------------------------------+        +---------------------------------+
| event U04 U05 U07 U08 U12 U14   |------->| worker NotificationListener     |
| U15 tren platform.events        |        +---------------------------------+
+---------------------------------+                         |
                                                            v
+---------------------------------+        +---------------------------------+
| worker DeadlineReminderScanner  |------->| NotificationFanout P1           |
| P4                              |        +---------------------------------+
+---------------------------------+             |                     |
                                                | sau commit          | EMAIL_SEND
                                                v                     v
                              +---------------------------+  +---------------------------+
                              | fanout platform.realtime  |  | worker jobs.email         |
                              +---------------------------+  | EmailSendHandler P2 P3    |
                                                |            | EmailDeferredScanner P2   |
                                                v            +---------------------------+
                              +---------------------------+               |
                              | backend SseHub            |               v
                              +---------------------------+  +---------------------------+
                                                |            | SMTP Brevo hoac Mailpit   |
                                                | SSE        +---------------------------+
                                                v
+---------------------------------+        +---------------------------------+
| backend REST                    |<-------| trinh duyet                     |
| NotificationController          |        +---------------------------------+
| EmailPreferenceController       |
| SubmissionProgressController    |
| AdminDashboardController        |
| GradeDistributionController     |
| GradebookExportController       |
+---------------------------------+
```

**Text alternative**: Worker nghe event của U04, U05, U07, U08, U12, U14, U15 và tạo thông báo theo lô qua `NotificationFanout` (bài của môn thì mở rộng tới người học mọi lớp `OPEN` của môn); scanner nhắc hạn cũng tạo thông báo qua `NotificationFanout`. Sau commit, fanout gửi tín hiệu realtime để `SseHub` ở backend đẩy tới trình duyệt và gửi việc `EMAIL_SEND` cho các dòng có email; `EmailSendHandler` gửi qua SMTP theo trần và ưu tiên, `EmailDeferredScanner` xếp lại email dời. Trình duyệt gọi REST: mọi người dùng đọc thông báo trong popup, Student cài đặt email; giảng viên lớp xem tiến độ nộp và xuất bảng điểm; Admin xem Admin Dashboard; Student xem phân bố điểm.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `NotificationListener`, `NotificationFanout` | worker | F1; P1 |
| `EmailSendHandler`, `EmailDeferredScanner` | worker | F2; P2, P3 |
| `DeadlineReminderScanner` | worker | F3; P4 |
| `NotificationRetentionScanner` | worker | Xóa thông báo quá 180 ngày |
| `NotificationController`, `EmailPreferenceController` | backend | F4 (UC 12) |
| `SubmissionProgressController`, `SubmissionProgressService` | backend | F5; P5; chỉ R3/R4 của lớp |
| `AdminDashboardController`, `AdminDashboardService` | backend | F6 (UC 58); chỉ `ADMIN`, chỉ số đếm |
| `GradeDistributionController`, `GradeDistributionService` | backend | F7; chỉ đọc điểm đã công bố theo lớp, ngưỡng BR-U16-42 |
| `GradebookExportController`, `GradebookExportService` | backend | F8; CSV/XLSX theo yêu cầu, chỉ R3/R4 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U16_EMAIL_DAILY_CAP` | 300 |
| `U16_EMAIL_PER_SECOND` | 1 |
| `U16_REMINDER_HOURS_BEFORE` | 24 |
| `U16_RETENTION_DAYS` | 180 |
| `U16_FANOUT_BATCH_SIZE` | 500 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log email |
| SECURITY-05 | Compliant | Escape mẫu |
| SECURITY-08 | Compliant | Kênh/danh sách theo người dùng; tiến độ, xuất theo R3/R4; Admin Dashboard chỉ `ADMIN` |
| SECURITY-09 | Compliant | SMTP trong `.env` |
| SECURITY-15 | Compliant | P3 |
| RESILIENCY-10 | Compliant | Timeout SMTP |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

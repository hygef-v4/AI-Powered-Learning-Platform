# U16 Reporting & Notification - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U16. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story trong phạm vi**: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. **Use case**: UC 18 View Statistics, UC 36, UC 38. Báo cáo độ lệch điểm AI nằm ngoài phạm vi.
- **Thiết kế nguồn**: `construction/u16-reporting-notification/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port / event | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort`, `AccountLookupPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `JobPort`, `JobHandler`, `PendingSweeper`, `ScheduledScanner` | U03 | Dùng thật |
| `ClassAccessPort`, `enrollment.activated` | U04 | Dùng thật |
| `class.announcement-posted` | U05 | Thông báo trong app theo thành viên lớp |
| `payment.paid` | U07 | Dùng thật (U07 phát, BR-U07-53) |
| `AssignmentQueryPort`, event `assignment.opened` | U08 | Dùng thật |
| `SubmissionQueryPort` | U11 | Dùng thật |
| `GroupMembershipPort`, `group.*` | U12 | Dùng thật |
| `GroupSubmissionQueryPort`, `group.submitted`, `SseHub`, `platform.realtime` | U14 | Dùng thật; `SseHub` của U14 đã phân kênh theo khóa, U16 chỉ đăng ký kênh `accountId` |
| `grade.published` | U15 | Dùng thật |
| `GradebookQueryPort`, `GradeQueryPort` | U15 | Chỉ đọc điểm giảng viên đã chốt/công bố; không lấy điểm AI đề xuất |

### Dữ liệu U16 sở hữu

PostgreSQL `notifications` (kèm `email_status`); cột `accounts.email_preferences` (U16 thêm), `assignments.reminder_sent_at` (U08 tạo, U16 ghi); Redis `email:daily-count:*`; queue nghe event `jobs.notification` (U16 khai báo); việc `EMAIL_SEND` trên `jobs.email`; `DeadlineReminderScanner`, `EmailDeferredScanner`, `NotificationRetentionScanner` đăng ký `ScheduledScanner` (U03).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  reporting/
    api/                NotificationController, PreferenceController, ProgressController,
                        StatisticsController, GradeDistributionController, GradebookExportController, DTO
    application/        NotificationFanout, ProgressService, PreferenceService,
                        StatisticsService, GradeDistributionService, GradebookExportService
    email/              EmailSendHandler, EmailDeferredScanner, templates (Thymeleaf, tiếng Việt)
    domain/             Notification, NotificationType, EmailStatus, EmailPreferences
    infrastructure/     JPA repository
    worker/             NotificationListener, DeadlineReminderScanner, NotificationRetentionScanner
/backend/src/main/resources/db/migration/reporting/
/backend/src/main/resources/templates/email/u16/
/frontend/src/components/notifications/
/frontend/src/app/notifications/
/frontend/src/components/progress/, /frontend/src/components/reporting/
/frontend/src/app/admin/ (AdminStatisticsPanel), /frontend/src/app/teaching/classes/[id]/gradebook/
/contracts/openapi/reporting.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `spring-boot-starter-thymeleaf`, Guava (`RateLimiter`). Cấu hình U16 theo `logical-components.md` §3; Nginx route SSE thông báo.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain và loại thông báo (gồm `GROUP_LEADER_REQUESTED`, `GROUP_LEADER_REQUEST_REJECTED`); mẫu email tiếng Việt (ghi danh, bài mới mở, điểm công bố, nhắc hạn) không chứa điểm/nội dung (BR-U16-03, 10).
- [ ] **Bước 3** - `NotificationListener` cho mọi event, gồm thông báo lớp từ U05 và `grade.published` (người nhận lấy từ payload), + `NotificationFanout` theo lô, idempotent, `email_preferences`, gửi việc `EMAIL_SEND`, tín hiệu realtime (F1, P1, BR-U16-01…06, 11).
- [ ] **Bước 4** - Đăng ký kênh theo `accountId` vào `SseHub` của U14 (không sửa code U14); endpoint SSE chuông thông báo `GET /api/v1/me/notifications/stream`.
- [ ] **Bước 5** - `EmailSendHandler` trên `jobs.email` (ưu tiên, trần Redis, `DEFERRED`, 1 email/giây, idempotent theo `email_status`, retry, `FAILED`, bỏ nhắc đã quá hạn) và `EmailDeferredScanner` (00:05 xếp lại) (F2, P2, P3, BR-U16-12…14).
- [ ] **Bước 6** - `DeadlineReminderScanner`: mỗi phút chọn bài `OPEN` tới mốc nhắc theo hạn hiện tại, chỉ gửi người chưa nộp, ghi `reminder_sent_at` cùng transaction (F3, P4, BR-U16-20…22).
- [ ] **Bước 7** - `PreferenceService` (ghi `accounts.email_preferences`), `ProgressService` (một query), `NotificationRetentionScanner` 180 ngày (F4, F5, P5, BR-U16-05, 30, 31).
- [ ] **Bước 7a** - `StatisticsService` (chỉ `ADMIN`; đếm tài khoản theo vai trò × trạng thái qua U01, môn/lớp theo trạng thái và ghi danh `ACTIVE` qua U04; BR-U16-40, 41), `GradeDistributionService` (BR-U16-42) và `GradebookExportService`: chỉ bài `GRADED`, điểm cuối đã chốt hoặc đã công bố, đánh dấu chưa nộp/chưa chốt; kết quả `PRACTICE` và AI luyện tập nằm ngoài điểm chính thức/phân bố/CSV/XLSX; phân bố lớp chỉ khi bật cờ và đủ ngưỡng; không lưu tệp, không có điểm tổng/hệ số (F6, F7, BR-U16-40…45, FR-030).
- [ ] **Bước 8** - Kiểm listener nhận đủ event của các unit nguồn (gồm `payment.paid` do U07 phát, `group.leader-requested` do U12 phát); U16 không sửa code của unit khác.
- [ ] **Bước 9** - Unit test mọi `BR-U16-xx`.
- [ ] **Bước 10** - Tóm tắt: `aidlc-docs/construction/u16-reporting-notification/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 11** - Flyway `V20260925_2300__u16_notifications.sql` theo `infrastructure-design.md` §3.
- [ ] **Bước 12** - JPA repository.
- [ ] **Bước 13** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test với Mailpit (Testcontainers): event lặp không gửi trùng; hết trần dời sang ngày sau; SMTP lỗi retry; scanner không nhắc bài ngừng giao và nhắc lại đúng một lần khi gia hạn; SSE chuông nhận thông báo; xuất CSV/XLSX chỉ có điểm cuối hợp lệ.
- [ ] **Bước 14** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 15** - `/contracts/openapi/reporting.yaml` (gồm SSE).
- [ ] **Bước 16** - Controller + DTO + validation.
- [ ] **Bước 17** - Test MockMvc: không đọc thông báo người khác; thống kê ngoài `ADMIN` bị `403` và không trả dữ liệu cá nhân; báo cáo tiến độ và xuất bảng điểm ngoài quyền `404`; CSV chống công thức; nhóm nhỏ không lộ phân bố lớp.
- [ ] **Bước 18** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 19** - `NotificationBell` (SSE), `NotificationDropdown`, `NotificationListPage`.
- [ ] **Bước 20** - `NotificationEmailToggles` trên `NotificationListPage`; `CheckProgressDialog` (`ProgressSummary`, `ProgressTable`) mở từ Assignment List; `AdminStatisticsPanel` trên Admin Menu; `GradeDistributionBadge` cho Assignment List của Student (U11 gắn vào); nút xuất bảng điểm CSV/XLSX cho giảng viên và Chủ nhiệm môn có quyền (BR-U16-43).
- [ ] **Bước 21** - Test frontend: số chưa đọc cập nhật qua SSE, tắt email từng loại.
- [ ] **Bước 22** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 23** - Cập nhật `README.md`: cấu hình SMTP (Brevo SMTP, Mailpit), trần email, cách unit khác phát event để có thông báo.
- [ ] **Bước 24** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-NTF-001 (UC 38) | 2, 3, 4, 5, 19, 20 |
| US-RPT-001 (UC 36) | 6, 7, 7a, 20 |
| US-RPT-002 (UC 18 View Statistics) | 7a, 15-17, 20 |
| US-RPT-003 (UC 36) | 7a, 15-17, 20 |

## 5. Ngoài phạm vi

- Báo cáo độ lệch điểm AI ngoài phạm vi dự án; email OTP tài khoản thuộc U01.

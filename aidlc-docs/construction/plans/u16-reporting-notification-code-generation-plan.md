# U16 Reporting & Notification - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U16. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story trong phạm vi**: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. **Use case**: UC-OPS-01, UC-RPT-01, 02, 03. Báo cáo độ lệch điểm AI nằm ngoài phạm vi.
- **Thiết kế nguồn**: `construction/u16-reporting-notification/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port / event | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort`, `AccountLookupPort` | U01 | Dùng thật |
| `JobPort`, `AuditPort` | U02 | Dùng thật |
| `ClassAccessPort`, `u04.enrollment.activated` | U04 | Dùng thật |
| `u05.class.announcement-posted`, `u05.class.question-posted`, `u05.class.answer-posted` | U05 | Thông báo trong app theo thành viên lớp |
| `u07.payment.paid` | U07 | Dùng thật (U07 thêm event, BR-U07-53) |
| `AssignmentQueryPort`, `u08.assignment.*` | U08 | Dùng thật |
| `SubmissionQueryPort` | U11 | Dùng thật |
| `GroupMembershipPort`, `u12.group.*` | U12 | Dùng thật |
| `GroupSubmissionQueryPort`, `u14.group.submitted`, `SseHub`, `platform.realtime` | U14 | Dùng thật (thêm kênh theo `accountId`) |
| `u15.grade.published` | U15 | Dùng thật |
| `GradebookQueryPort`, `GradeQueryPort` | U15 | Chỉ đọc điểm giảng viên đã chốt/công bố; không lấy điểm AI đề xuất |

### Dữ liệu U16 sở hữu

PostgreSQL `notifications`, `email_outbox`, `notification_preferences`, `deadline_reminders`; Redis `u16:email:*`; queue `u16.notification-listener`, `jobs.u16.*`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u16/
    api/                NotificationController, PreferenceController, ProgressController,
                        LearnerDashboardController, GradebookExportController, DTO
    application/        NotificationFanout, ProgressService, PreferenceService,
                        LearnerDashboardService, GradebookExportService
    email/              EmailDispatcher, EmailSendHandler, templates (Thymeleaf, tiếng Việt)
    domain/             Notification, NotificationType, EmailOutbox, NotificationPreference,
                        DeadlineReminder
    infrastructure/     JPA repository
    worker/             NotificationListener, DeadlineReminderHandler, RetentionHandler
/backend/src/main/resources/db/migration/u16/
/backend/src/main/resources/templates/email/u16/
/frontend/src/components/notifications/
/frontend/src/app/notifications/, /frontend/src/app/settings/notifications/
/frontend/src/app/teaching/publications/[id]/progress/
/frontend/src/app/learn/dashboard/, /frontend/src/app/teaching/classes/[id]/gradebook/
/contracts/openapi/u16-notifications.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - `pom.xml`: `spring-boot-starter-thymeleaf`, Guava (`RateLimiter`). Cấu hình U16 theo `logical-components.md` §3; Nginx route SSE thông báo.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain và loại thông báo; mẫu email tiếng Việt (ghi danh, bài mới mở, điểm công bố, nhắc hạn) không chứa điểm/nội dung (BR-U16-03, 10).
- [ ] **Bước 3** - `NotificationListener` cho mọi event, gồm bài đăng/câu hỏi/trả lời lớp từ U05, + `NotificationFanout` theo lô, idempotent, preference, tín hiệu realtime (F1, P1, BR-U16-01…06, 11).
- [ ] **Bước 4** - `SseHub` (U14) thêm kênh theo người dùng; endpoint SSE chuông thông báo.
- [ ] **Bước 5** - `EmailDispatcher` (ưu tiên, trần Redis, dời ngày sau, 1 email/giây) và `EmailSendHandler` (idempotent, retry, `FAILED`, bỏ nhắc đã quá hạn) (F2, P2, P3, BR-U16-12…14).
- [ ] **Bước 6** - `DeadlineReminderHandler`: lên lịch khi bài mở, bỏ qua khi hạn đổi, hủy khi ngừng giao/đóng, người nhận chưa nộp (F3, P4, BR-U16-20…22).
- [ ] **Bước 7** - `PreferenceService`, `ProgressService` (một query), `RetentionHandler` 180 ngày (F4, F5, P5, BR-U16-05, 30, 31).
- [ ] **Bước 7a** - `LearnerDashboardService` và `GradebookExportService`: điểm đã công bố của người học; phân bố lớp chỉ khi bật cờ và đủ ngưỡng; CSV/XLSX theo yêu cầu, không lưu tệp, không có điểm tổng/hệ số hay điểm AI đề xuất (F6, F7, BR-U16-40…45).
- [ ] **Bước 8** - U07 phát `u07.payment.paid` sau commit (BR-U07-53) nếu U07 chưa có.
- [ ] **Bước 9** - Unit test mọi `BR-U16-xx`.
- [ ] **Bước 10** - Tóm tắt: `aidlc-docs/construction/u16-reporting-notification/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 11** - Flyway `V20260925_2300__u16_notifications.sql` theo `infrastructure-design.md` §3.
- [ ] **Bước 12** - JPA repository.
- [ ] **Bước 13** - Integration test với Mailpit (Testcontainers): event lặp không gửi trùng; hết trần dời sang ngày sau; SMTP lỗi retry; nhắc bị hủy khi ngừng giao; SSE chuông nhận thông báo; xuất CSV/XLSX chỉ có điểm cuối hợp lệ.
- [ ] **Bước 14** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 15** - `/contracts/openapi/u16-notifications.yaml` (gồm SSE).
- [ ] **Bước 16** - Controller + DTO + validation.
- [ ] **Bước 17** - Test MockMvc: không đọc thông báo/dashboard người khác; báo cáo tiến độ và xuất bảng điểm ngoài quyền `404`; CSV chống công thức; nhóm nhỏ không lộ phân bố lớp.
- [ ] **Bước 18** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 19** - `NotificationBell` (SSE), `NotificationDropdown`, `NotificationListPage`.
- [ ] **Bước 20** - `NotificationSettingsPage`; `SubmissionProgressPage` (`ProgressSummary`, `ProgressTable`); dashboard kết quả cá nhân và nút xuất bảng điểm CSV/XLSX cho giảng viên có quyền.
- [ ] **Bước 21** - Test frontend: số chưa đọc cập nhật qua SSE, tắt email từng loại.
- [ ] **Bước 22** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 23** - Cập nhật `README.md`: cấu hình SMTP (Gmail App Password, Mailpit), trần email, cách unit khác phát event để có thông báo.
- [ ] **Bước 24** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-NTF-001 (UC-OPS-01) | 2, 3, 4, 5, 19, 20 |
| US-RPT-001 (UC-RPT-01) | 6, 7, 20 |
| US-RPT-002 (UC-RPT-02) | 7a, 15-17, 20 |
| US-RPT-003 (UC-RPT-03) | 7a, 15-17, 20 |

## 5. Ngoài phạm vi

- Báo cáo độ lệch điểm AI ngoài phạm vi dự án; email OTP tài khoản thuộc U01.

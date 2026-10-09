# U16 Reporting & Notification - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 12, 58 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); luồng phụ: phân bố điểm của UC 22, tiến độ nộp của UC 37, phần xuất tệp của UC 40; primary stories: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U16. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story trong phạm vi**: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003.
- **Primary UC hiện hành**: UC 12 View Notifications, UC 58 View Admin Dashboard theo bản 73 UC. Luồng phụ U16 đóng góp: phân bố điểm trên Student Assignments (UC 22, U11), tiến độ nộp trên Submission Detail (UC 37, U15), xuất CSV/XLSX trên Gradebook (UC 40, U15). Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-09**: Admin chỉ dùng chức năng User và quản trị (không vào lớp, không R2/R3/R4, không ví), trang đích `/admin` là Admin Dashboard; UC 12 không có màn riêng (chuông và popup trên thanh điều hướng mọi vai trò); bỏ bình luận dưới thông báo lớp; bài của môn gửi thông báo và nhắc hạn cho mọi lớp `OPEN` của môn; quiz luyện tập phát hành chỉ báo trong app (`QUIZ_PUBLISHED`), không nhắc hạn; phân bố điểm và xuất bảng điểm của bài của môn theo từng lớp, không gồm quiz; U10 đã xóa.
- **Thiết kế nguồn**: `construction/u16-reporting-notification/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port / event | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort`, `AccountLookupPort` | U01 | Dùng thật (`countByRoleAndStatus` cho UC 58) |
| Shell thanh điều hướng, `RoleGuard` | U01 | U16 gắn `NotificationBell`; route `/admin` chỉ Admin |
| `AuditPort` | U02 | Dùng thật |
| `JobPort`, `JobHandler`, `PendingSweeper`, `ScheduledScanner` | U03 | Dùng thật |
| `ClassAccessPort`, `ClassScopePort`, `enrollment.activated` | U04 | Dùng thật; cần U04 thêm truy vấn lớp `OPEN` của một môn (bài của môn) |
| `class.announcement-posted` | U05 | Thông báo trong app cho người học lớp, trừ actor |
| `payment.paid` | U07 | Dùng thật (U07 phát, BR-U07-53) |
| `AssignmentQueryPort`, `AssignmentExtensionPort`, event `assignment.opened` | U08 | Dùng thật; payload `classId` hoặc `subjectId`, `type` |
| `SubmissionQueryPort` | U11 | Dùng thật; U11 gắn `GradeDistributionBadge` vào Student Assignments |
| `GroupMembershipPort`, `group.*` | U12 | Dùng thật |
| `GroupSubmissionQueryPort`, `group.submitted`, `SseHub`, `platform.realtime` | U14 | Dùng thật; `SseHub` của U14 đã phân kênh theo khóa, U16 chỉ đăng ký kênh `accountId` |
| `grade.published` | U15 | Dùng thật |
| `GradebookQueryPort`, `GradeQueryPort` | U15 | Chỉ đọc điểm giảng viên đã chốt/công bố theo lớp; không lấy điểm AI đề xuất; U15 gắn `SubmissionProgressPanel`, `GradebookExportAction` |

### Dữ liệu U16 sở hữu

PostgreSQL `notifications` (kèm `email_status`); cột `accounts.email_preferences` (U16 thêm), `assignments.reminder_sent_at` (U08 tạo, U16 ghi); Redis `email:daily-count:*`; queue nghe event `jobs.notification` (U16 khai báo); việc `EMAIL_SEND` trên `jobs.email`; `DeadlineReminderScanner`, `EmailDeferredScanner`, `NotificationRetentionScanner` đăng ký `ScheduledScanner` (U03).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  reporting/
    api/                NotificationController, EmailPreferenceController, SubmissionProgressController,
                        AdminDashboardController, GradeDistributionController, GradebookExportController, DTO
    application/        NotificationFanout, NotificationRecipientResolver, SubmissionProgressService,
                        EmailPreferenceService, AdminDashboardService, GradeDistributionService,
                        GradebookExportService
    email/              EmailSendHandler, EmailDeferredScanner, templates (Thymeleaf, tiếng Việt)
    domain/             Notification, NotificationType, EmailStatus, EmailPreferences
    infrastructure/     JPA repository
    worker/             NotificationListener, DeadlineReminderScanner, NotificationRetentionScanner
/backend/src/main/resources/db/migration/reporting/V20260925_2300__create_notifications.sql
/backend/src/main/resources/templates/email/notifications/
/frontend/src/components/notifications/   NotificationBell, NotificationPopup, NotificationEmailToggles
/frontend/src/components/reporting/       GradeDistributionBadge, SubmissionProgressPanel, GradebookExportAction
/frontend/src/app/admin/                  AdminDashboardPage, AdminStatisticsPanel, AdminNavTiles
/contracts/openapi/reporting.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `spring-boot-starter-thymeleaf`, Guava (`RateLimiter`). Cấu hình U16 theo `logical-components.md` §3; Nginx route SSE thông báo.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain và loại thông báo (gồm `QUIZ_PUBLISHED`, `ANNOUNCEMENT_POSTED`, `GROUP_LEADER_REQUESTED`, `GROUP_LEADER_REQUEST_REJECTED`) kèm `link` theo `domain-entities.md` §2; mẫu email tiếng Việt cho bốn loại có email (ghi danh, bài tập mới mở, điểm công bố, nhắc hạn) không chứa điểm/nội dung (BR-U16-03, 07, 10).
- [ ] **Bước 3** - `NotificationListener` cho mọi event, `NotificationRecipientResolver` (lớp; bài/quiz của môn qua mọi lớp `OPEN` của môn; `grade.published` lấy từ payload; thông báo lớp trừ actor) và `NotificationFanout` theo lô, idempotent, `email_preferences`, gửi việc `EMAIL_SEND`, tín hiệu realtime; quiz → `QUIZ_PUBLISHED` chỉ app (F1, P1, BR-U16-01…09, 11).
- [ ] **Bước 4** - Đăng ký kênh theo `accountId` vào `SseHub` của U14 (không sửa code U14); endpoint SSE chuông thông báo `GET /api/v1/me/notifications/stream`.
- [ ] **Bước 5** - `EmailSendHandler` trên `jobs.email` (ưu tiên, trần Redis, `DEFERRED`, 1 email/giây, idempotent theo `email_status`, retry, `FAILED`, bỏ nhắc đã quá hạn) và `EmailDeferredScanner` (00:05 xếp lại) (F2, P2, P3, BR-U16-12…14).
- [ ] **Bước 6** - `DeadlineReminderScanner`: mỗi phút chọn bài tập `OPEN` (không phải quiz, có `closes_at`) tới mốc nhắc theo hạn hiện tại, chỉ gửi người chưa nộp (bài của môn: mọi lớp `OPEN` của môn), ghi `reminder_sent_at` cùng transaction (F3, P4, BR-U16-20…22).
- [ ] **Bước 7** - `EmailPreferenceService` (chỉ bốn loại có email, ghi `accounts.email_preferences`), `SubmissionProgressService` (một query, theo `classId`, R3/R4), `NotificationRetentionScanner` 180 ngày (F4, F5, P5, BR-U16-05, 11, 15, 30, 31).
- [ ] **Bước 7a** - `AdminDashboardService` (chỉ `ADMIN`; đếm tài khoản theo vai trò × trạng thái qua U01, môn/lớp theo trạng thái và ghi danh `ACTIVE` qua U04; BR-U16-40, 41), `GradeDistributionService` (theo lớp, bài `GRADED`, BR-U16-42) và `GradebookExportService` (R3/R4, không Admin): chỉ bài `GRADED` của lớp gồm bài của môn giao cho lớp, điểm cuối đã chốt hoặc đã công bố, đánh dấu chưa nộp/chưa chốt; quiz, kết quả `PRACTICE` và AI luyện tập nằm ngoài phân bố/CSV/XLSX; không lưu tệp, không có điểm tổng/hệ số (F6–F8, BR-U16-40…46, FR-030).
- [ ] **Bước 8** - Kiểm listener nhận đủ event của các unit nguồn (gồm `payment.paid` do U07 phát, `group.leader-requested` do U12 phát, `assignment.opened` có `subjectId`/`type` do U08 phát); U16 không sửa code của unit khác.
- [ ] **Bước 9** - Unit test mọi `BR-U16-xx`.
- [ ] **Bước 10** - Tóm tắt: `aidlc-docs/construction/u16-reporting-notification/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 11** - Flyway `db/migration/reporting/V20260925_2300__create_notifications.sql` theo `infrastructure-design.md` §3.
- [ ] **Bước 12** - JPA repository.
- [ ] **Bước 13** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test với Mailpit (Testcontainers): event lặp không gửi trùng; hết trần dời sang ngày sau; SMTP lỗi retry; scanner không nhắc bài ngừng giao, không nhắc quiz và nhắc lại đúng một lần khi gia hạn; bài của môn tới đủ các lớp `OPEN`; SSE chuông nhận thông báo; xuất CSV/XLSX chỉ có điểm cuối hợp lệ.
- [ ] **Bước 14** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 15** - `/contracts/openapi/reporting.yaml` (gồm SSE). Sửa theo §7.
- [ ] **Bước 16** - Controller + DTO + validation.
- [ ] **Bước 17** - Test MockMvc: không đọc thông báo người khác; Admin Dashboard ngoài `ADMIN` bị `403` và không trả dữ liệu cá nhân; tiến độ và xuất bảng điểm ngoài R3/R4 (gồm Admin, Chủ nhiệm môn không dạy lớp) `404`; bài của môn thiếu `classId` bị `400`; CSV chống công thức; nhóm nhỏ không lộ phân bố lớp; `email-preferences` từ chối khóa ngoài bốn loại.
- [ ] **Bước 18** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 19** - `NotificationBell` (SSE) và `NotificationPopup` (`NotificationList`, `NotificationItem`, `MarkAllReadButton`) gắn vào thanh điều hướng của shell U01 cho mọi vai trò; không có trang `/notifications`.
- [ ] **Bước 20** - `NotificationEmailToggles` trong popup (chỉ Student); `AdminDashboardPage` tại `/admin` (`AdminStatisticsPanel`, `AdminNavTiles` tới Account List, Subject List, Credit Package List, Payment History, Setting List, Audit Log); `GradeDistributionBadge` cho Student Assignments (U11 gắn vào); `SubmissionProgressPanel` cho Submission Detail và `GradebookExportAction` cho Gradebook (U15 gắn vào).
- [ ] **Bước 21** - Test frontend: số chưa đọc cập nhật qua SSE, bấm thông báo đánh dấu đã đọc và mở link, toggle email chỉ hiện với Student, Admin Dashboard hiện đủ sáu lối vào.
- [ ] **Bước 22** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 23** - Cập nhật `README.md`: cấu hình SMTP (Brevo SMTP, Mailpit), trần email, cách unit khác phát event để có thông báo.
- [ ] **Bước 24** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-NTF-001 (UC 12 View Notifications) | 2, 3, 4, 5, 7, 19, 20, 21 |
| US-RPT-001 (nhắc hạn; tiến độ luồng phụ UC 37; phân bố điểm luồng phụ UC 22) | 6, 7, 7a, 17, 20 |
| US-RPT-002 (UC 58 View Admin Dashboard) | 7a, 15-17, 20, 21 |
| US-RPT-003 (phần xuất của UC 40 View/Export GradeBook) | 7a, 15-17, 20 |

## 5. Ngoài phạm vi

- Báo cáo độ lệch điểm AI ngoài phạm vi dự án; email OTP tài khoản thuộc U01; màn Gradebook, Submission Detail (U15), Student Assignments (U11) và các màn quản trị mở từ Admin Dashboard thuộc unit sở hữu.

## 6. Revision implementation scope - 2026-10-08
- [ ] Progress/export chỉ R3/R4, không grant theo subject manager; test SM/Admin có/không phân công dạy, ngoài scope 404.
- [ ] Notification trên thanh điều hướng UC 12, Admin Dashboard UC 58, GradebookExportAction phần xuất của UC 40; các luồng email/nhắc hạn/phân bố giữ nguyên (cập nhật số UC theo §7).

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] `reporting.yaml`: enum `NotificationType` thêm `QUIZ_PUBLISHED` (giữ `ANNOUNCEMENT_POSTED`, thiết kế đã dùng tên này thay `CLASS_ANNOUNCEMENT`); `EmailPreferences.propertyNames` chỉ còn `ENROLLED`, `ASSIGNMENT_OPENED`, `GRADE_PUBLISHED`, `DEADLINE_REMINDER`.
- [ ] `reporting.yaml`: `getSubmissionProgress` thêm query `classId` (bắt buộc với bài của môn, phải khớp lớp của bài của lớp); `x-roles` của `getSubmissionProgress`, `exportGradebook` bỏ `ADMIN` (chỉ `TEACHER`, `SUBJECT_MANAGER`, kiểm R3/R4); `x-roles` của `getEmailPreferences`, `updateEmailPreferences` chỉ `STUDENT`.
- [ ] `reporting.yaml`: `getAdminStatistics` đổi summary thành "Admin Dashboard (UC 58)", giữ đường dẫn `/api/v1/admin/statistics`; mô tả UC theo bản 73 UC (UC 12, 22, 37, 40, 58).
- [ ] Code: `NotificationRecipientResolver` xử lý `assignment.opened` có `subjectId` (mọi lớp `OPEN` của môn) và `type = MULTIPLE_CHOICE_QUIZ` → `QUIZ_PUBLISHED` chỉ app; listener chấp nhận payload mới của U08 (`classId` hoặc `subjectId`, `type`) theo `contracts/messages/assignment-events.json` sau khi U08 cập nhật.
- [ ] Code: `DeadlineReminderScanner` bỏ quiz, yêu cầu `closes_at`, bài của môn lấy người chưa nộp ở mọi lớp `OPEN`.
- [ ] Code: thay `NotificationListPage` (`/notifications`) bằng `NotificationPopup` trên thanh điều hướng; `NotificationEmailToggles` vào popup, chỉ Student.
- [ ] Code: đổi Statistic thành `AdminDashboardPage` tại `/admin` kèm `AdminNavTiles`; đổi tên `StatisticsController`/`StatisticsService` thành `AdminDashboardController`/`AdminDashboardService`; bỏ Admin Sidebar.
- [ ] Code: thay `CheckProgressDialog` (popup trên Assignment List) bằng `SubmissionProgressPanel` cho Submission Detail; `ProgressController`/`ProgressService` đổi thành `SubmissionProgressController`/`SubmissionProgressService`; `PreferenceController`/`PreferenceService` đổi thành `EmailPreferenceController`/`EmailPreferenceService`.
- [ ] Code: phân bố điểm và xuất bảng điểm tính bài của môn theo từng lớp, loại quiz; tiến độ, xuất bảng điểm từ chối Admin.
- [ ] Migration: đổi `V20260925_2300__u16_notifications.sql` thành `db/migration/reporting/V20260925_2300__create_notifications.sql` (chưa áp dụng nên sửa trực tiếp); `type` thêm `QUIZ_PUBLISHED`, dùng `ANNOUNCEMENT_POSTED`.
- [ ] Mẫu email chuyển từ `templates/email/u16/` sang `templates/email/notifications/`.
- [ ] Test: unit test fan-out bài của môn, quiz không email, scanner không chọn quiz; MockMvc Admin bị từ chối tiến độ/xuất, Student mới thấy toggle email.
- [ ] Admin Dashboard đọc thêm số liệu AI qua `AiUsageStatsPort` (U13); bài/quiz của môn lấy lớp nhận qua `ClassAccessPort.listOpenClassesOfSubject` (U04).

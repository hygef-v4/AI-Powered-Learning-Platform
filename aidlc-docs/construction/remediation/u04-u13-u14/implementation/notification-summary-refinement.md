# Notification summary refinement — 2026-10-24

Approved by the user after reviewing per-type copy and recipient roles: “ok sửa cho tôi đi”. Branch: `fix-u04-u13-u14`. Plan: `../plans/u14-notification-summary-plan.md`.

## Resulting behavior

In-app notifications describe the relevant action instead of asking an authenticated user to sign in. Fixed text is selected by `NotificationType`; it never interpolates event payload, grades, comments, member details or secrets. Existing routing already distinguishes the assigned teaching account, students and Admins, so no role-routing change is needed.

| Type | In-app summary |
| --- | --- |
| ENROLLED | Xem thông tin lớp và các hoạt động học tập. |
| ASSIGNMENT_OPENED | Xem nội dung và hạn nộp bài. |
| QUIZ_PUBLISHED | Mở Quiz để xem và bắt đầu luyện tập. |
| GRADE_PUBLISHED | Xem kết quả chấm bài của bạn. |
| DEADLINE_REMINDER | Hạn nộp bài còn khoảng 24 giờ. |
| ANNOUNCEMENT | Xem nội dung thông báo từ giảng viên. |
| LEADER_CHANGE_REQUESTED | Xem yêu cầu để phê duyệt hoặc từ chối. |
| LEADER_CHANGED | Xem thông tin trưởng nhóm mới. |
| LEADER_REQUEST_REJECTED | Xem trạng thái yêu cầu trong nhóm của bạn. |
| GROUP_MEMBERSHIP_CHANGED | Xem danh sách thành viên mới nhất. |
| GROUP_SUBMITTED | Xem trạng thái nộp bài của nhóm. |
| BACKUP_FAILED | Xem chi tiết lần sao lưu thất bại để kiểm tra và xử lý. |

## Implementation

- New package-local `backend/src/main/java/edu/aiplatform/notifications/application/NotificationSummaries.java` provides an exhaustive enum switch and legacy-display adapter.
- `NotificationEventService.accept` uses that copy for newly created event notifications. Resolved recipients, titles, links and email types are unchanged.
- `NotificationService.list` adapts only the exact stored legacy value “Đăng nhập để xem thông tin liên quan.” while building feed DTOs. It leaves the persisted entity, timestamp, recipient, read state and link untouched. Specific/custom summaries pass through unchanged, including existing reminder summaries.
- `DeadlineReminderScanner` uses the shared reminder wording for future reminders, fixing spacing around “24 giờ”. Selection, scheduling, deduplication and expiry are unchanged.
- EmailDeliveryService continues its own minimal sign-in template, independent of the in-app summary. No frontend/API shape/database schema/migration change.

## Regression scope

New event-service regressions verify the teaching request action, Admin backup handling, grade-payload privacy, and preservation of resolved delivery metadata. Feed regressions cover all12 enum types with the old stored text, unchanged entity/history/read state, and preservation of already-specific summaries. The existing email privacy test now supplies an actual private summary and verifies it is excluded while the sign-in link remains.

Focused build/test and local UI evidence are recorded separately in `../verification/u14-notification-summary-2026-10-24.md`. The earlier full backend verification and U04/U13/U14 black-box results remain historical evidence; U14-15 real reminder observation is still pending.

Subsequent manual follow-up closed the U14-15 expected behavior using documented +5minute rescheduling and stopped-assignment evidence; original +2day E2E data was not literally exercised. See the verification report's latest follow-up; no additional code change was required.

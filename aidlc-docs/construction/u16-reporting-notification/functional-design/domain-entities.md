# U16 Reporting & Notification - Domain Entities

## 1. Phạm vi sở hữu

U16 sở hữu thông báo trong app, hàng đợi email thông báo (có trần ngày), cài đặt nhận email của người dùng, nhắc hạn nộp tự động, dashboard cá nhân và xuất bảng điểm theo yêu cầu. U16 **không** sở hữu: email OTP tài khoản (U01 qua U02), điểm/bài nộp gốc (chỉ đọc qua port/event). Dashboard và tệp xuất tính khi đọc, không có bảng điểm tổng riêng.

## 2. `Notification`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `recipientId` | UUID | |
| `type` | enum | `ENROLLED`, `ASSIGNMENT_OPENED`, `DEADLINE_REMINDER`, `GRADE_PUBLISHED`, `GROUP_LEADER_CHANGED`, `GROUP_MEMBERSHIP_CHANGED`, `GROUP_SUBMITTED`, `PAYMENT_PAID`, `CLASS_ANNOUNCEMENT`, `CLASS_QUESTION`, `CLASS_ANSWER` |
| `title`, `body` | chuỗi | Không chứa điểm số, dữ liệu người khác |
| `link` | chuỗi | Đường dẫn nội bộ |
| `sourceEventId` | UUID | Unique theo `(sourceEventId, recipientId, type)` (chống trùng) |
| `readAt`, `createdAt` | | |

## 3. `EmailOutbox`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `notificationId` | UUID | |
| `toAccountId` | UUID | Email tra từ U01 lúc gửi |
| `template` | enum | Theo `type` |
| `status` | enum | `QUEUED`, `DEFERRED` (hết trần ngày), `SENT`, `FAILED`, `SKIPPED` (người dùng tắt) |
| `scheduledFor`, `attempts`, `lastError`, `sentAt` | | |

## 4. `NotificationPreference`

`accountId`, `type`, `emailEnabled` (mặc định `true`). Thông báo trong app luôn bật.

## 5. `DeadlineReminder`

`publicationId`, `remindAt` (hạn chính − 24 giờ), `status` (`SCHEDULED`, `DONE`, `CANCELLED`), `sentCount`. Một lần mỗi publication.

## 6. Contract

| Contract / event nghe | Nguồn | Tạo thông báo |
|---|---|---|
| `u04.enrollment.activated` | U04 | `ENROLLED` (app + email) |
| `u08.assignment.opened` | U08 | `ASSIGNMENT_OPENED` cho người học của lớp (app + email); lên lịch `DeadlineReminder` |
| `u08.assignment.retired`, `u08.assignment.closed` | U08 | Hủy nhắc |
| `u15.grade.published` | U15 | `GRADE_PUBLISHED` (app + email, không ghi điểm) |
| `u12.group.leader-changed`, `u12.group.membership-changed` | U12 | App |
| `u14.group.submitted` | U14 | App cho thành viên nhóm |
| `u07.payment.paid` (U07 thêm event) | U07 | App |
| `u05.class.announcement-posted`, `u05.class.question-posted`, `u05.class.answer-posted` | U05 | App cho người còn quyền trong lớp theo BR-U16-06; không gửi email. Event trả lời mang `questionAuthorId` để chọn người nhận, U16 kiểm lại quyền lớp qua U04 |
| `AccountLookupPort` | U01 | Email tài khoản |
| `SubmissionQueryPort`, `GroupSubmissionQueryPort`, `AssignmentQueryPort` | U11, U14, U08 | Báo cáo tiến độ, ai chưa nộp |
| `GradebookQueryPort`, `GradeQueryPort` | U15 | Điểm cuối/trạng thái, không trả đề xuất AI cho dashboard hoặc xuất bảng điểm |
| `ClassAccessPort` | U04 | Lớp người học đang ghi danh, người quản lý lớp, `showGradeDistribution` |

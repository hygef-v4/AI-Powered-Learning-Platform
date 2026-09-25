# U16 Reporting & Notification - Domain Entities

## 1. Phạm vi sở hữu

U16 sở hữu thông báo trong app, hàng đợi email thông báo (có trần ngày), cài đặt nhận email của người dùng, nhắc hạn nộp tự động và báo cáo tiến độ nộp bài. U16 **không** sở hữu: email OTP tài khoản (U01 qua U02), dữ liệu nghiệp vụ gốc (chỉ đọc qua port/event).

## 2. `Notification`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `recipientId` | UUID | |
| `type` | enum | `ENROLLED`, `ASSIGNMENT_OPENED`, `DEADLINE_REMINDER`, `GRADE_PUBLISHED`, `GROUP_LEADER_CHANGED`, `GROUP_MEMBERSHIP_CHANGED`, `GROUP_SUBMITTED`, `PAYMENT_PAID` |
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
| `ClassAccessPort`, `AccountLookupPort` | U04, U01 | Danh sách người học, email |
| `SubmissionQueryPort`, `GroupSubmissionQueryPort`, `AssignmentQueryPort` | U11, U14, U08 | Báo cáo tiến độ, ai chưa nộp |

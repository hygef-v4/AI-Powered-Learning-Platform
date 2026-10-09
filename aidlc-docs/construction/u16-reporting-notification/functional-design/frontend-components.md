# U16 Reporting & Notification - Frontend Components

**Bản tài liệu 2026-10-09**: UC 12, 58 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); luồng phụ: phân bố điểm của UC 22, tiến độ nộp của UC 37, phần xuất tệp của UC 40; primary stories: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Popup thông báo (screen flow không có màn riêng) | `NotificationBell`, `NotificationPopup` | 12 | Student, Teacher, Subject Manager, Admin | Thanh điều hướng của shell U01 |
| Admin Dashboard | `AdminDashboardPage` | 58 | Admin | Post-Login (trang đích `/admin`) |
| Student Assignments (màn của U11) | `GradeDistributionBadge` | 22 (luồng phụ) | Student | Class Dashboard |
| Submission Detail (màn của U15) | `SubmissionProgressPanel` | 37 (luồng phụ) | Teacher, Subject Manager được giao dạy | Teacher Class Detail |
| Gradebook (của U15) | `GradebookExportAction` | 40 (phần xuất) | Teacher, Subject Manager được giao dạy | Teacher Class Detail (U15 chốt vị trí; Page-2 không có màn Gradebook riêng) |

Bỏ so với bản 2026-10-08: trang `NotificationListPage` (`app/notifications/`), popup Check Progress trên Assignment List, màn Statistic và Admin Sidebar.

## 2. Cây component

```
components/notifications/          gắn vào thanh điều hướng của shell U01, mọi vai trò
  NotificationBell                 số chưa đọc, cập nhật qua SSE; bấm mở popup
  NotificationPopup                popup thông báo (UC 12)
    NotificationList               mới nhất trước, 20 mỗi trang, nút "Xem thêm"
      NotificationItem             tiêu đề, thời gian, chấm chưa đọc; bấm thì đánh dấu đã đọc và mở link
    MarkAllReadButton              "Đánh dấu tất cả đã đọc"
    NotificationEmailToggles       phần "Email", chỉ Student: bật/tắt 4 loại có email
app/admin/                         AdminDashboardPage   màn Admin Dashboard (/admin)
  AdminStatisticsPanel             tài khoản theo vai trò/trạng thái, tổng tài khoản, môn, lớp, ghi danh
  AdminNavTiles                    ô dẫn tới Account List, Subject List, Credit Package List,
                                   Payment History, Setting List, Audit Log
components/reporting/
  GradeDistributionBadge           U11 gắn vào từng dòng bài trên Student Assignments
  SubmissionProgressPanel          U15 gắn vào Submission Detail
    ProgressSummary                đã nộp / đang làm / chưa bắt đầu / trễ, thời gian còn lại
    ProgressTable                  người học (hoặc nhóm), trạng thái, thời điểm nộp
  GradebookExportAction            U15 gắn vào Gradebook: chọn cả lớp hoặc một bài, CSV/XLSX
```

Route `/admin` theo `RoleGuard` của U01 chỉ cho Admin; các component trong `components/reporting/` không có route riêng, quyền do API kiểm. Backend vẫn kiểm mọi quyền.

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `NotificationBell` | SSE nhận tín hiệu thì tăng số; mất kết nối thì tải lại số chưa đọc; Admin hiện chuông nhưng hiện chưa có loại nào gửi cho Admin (BR-U16-08) | `GET /api/v1/me/notifications/stream`, `GET /api/v1/me/notifications/unread-count` |
| `NotificationPopup`, `NotificationList`, `NotificationItem` | Tải trang đầu khi mở; "Xem thêm" tải trang sau; bấm thông báo đánh dấu đã đọc rồi chuyển tới `link`, màn đích tự kiểm quyền (BR-U16-07) | `GET /api/v1/me/notifications`, `POST /api/v1/me/notifications/{id}/read` |
| `MarkAllReadButton` | Đánh dấu tất cả, số chưa đọc về 0 | `POST /api/v1/me/notifications/read-all` |
| `NotificationEmailToggles` | Chỉ hiện với Student; đổi toggle là lưu ngay, lỗi thì trả toggle về trạng thái cũ (BR-U16-11, 15) | `GET`, `PUT /api/v1/me/email-preferences` |
| `AdminDashboardPage`, `AdminStatisticsPanel` | Chỉ `ADMIN`; tải số liệu mỗi lần mở; chỉ số đếm, không danh sách người (BR-U16-40, 41) | `GET /api/v1/admin/statistics` |
| `AdminNavTiles` | Liên kết tới màn của U01, U04, U07, U03, U02 (BR-U16-46) | - |
| `GradeDistributionBadge` | Chỉ hiện khi lớp bật và bài đủ mẫu; không có dữ liệu thì ẩn; không hiện với quiz | `GET /api/v1/me/classes/{classId}/grade-distribution` |
| `SubmissionProgressPanel` | Tải khi mở Submission Detail, nút "Làm mới"; bài của môn luôn gửi `classId` của lớp đang xem | `GET /api/v1/assignments/{id}/progress?classId=` |
| `GradebookExportAction` | Chọn cả lớp hoặc một bài và CSV/XLSX; trình duyệt tải tệp stream | `GET /api/v1/classes/{classId}/gradebook/export?format=csv\|xlsx&assignmentId=` |

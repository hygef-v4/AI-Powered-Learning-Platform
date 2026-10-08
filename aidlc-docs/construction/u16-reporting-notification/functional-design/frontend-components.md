# U16 Reporting & Notification - Frontend Components

**Bản tài liệu 2026-10-08**: UC 11, 57; primary stories: US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
components/notifications/
  NotificationBell         số chưa đọc, cập nhật qua SSE
  NotificationDropdown     10 mới nhất, "Xem tất cả", "Đánh dấu đã đọc"
app/notifications/         NotificationListPage (phân trang)
  NotificationEmailToggles toggle bật/tắt email từng loại, ngay trên trang (không có trang cài đặt riêng)
components/progress/
  CheckProgressDialog      popup Check Progress mở từ Assignment List của giảng viên (UC 34)
    ProgressSummary        đã nộp / đang làm / chưa bắt đầu / trễ, thời gian còn lại
    ProgressTable          người học (hoặc nhóm), trạng thái, thời điểm nộp
components/reporting/
  GradeDistributionBadge   phân bố điểm ẩn danh của một bài; U11 gắn vào các danh sách bài theo loại của Student
app/admin/ (màn Statistic, điều hướng Admin Sidebar)
  AdminStatisticsPanel     thống kê (UC 57) hiện trên Statistic, phía trên các nút dẫn tới trang quản lý: tài khoản theo vai trò/trạng thái, môn, lớp, ghi danh
app/teaching/classes/[id]/gradebook/  GradebookExportAction (CSV/XLSX theo lớp hoặc bài)
```

| Component | Hành vi | API |
|---|---|---|
| `NotificationBell` | SSE `GET /api/v1/me/notifications/stream`; mất kết nối thì tải lại số chưa đọc | `GET /api/v1/me/notifications/unread-count` |
| `NotificationListPage` | | `GET /api/v1/me/notifications`, `POST .../{id}/read`, `POST .../read-all` |
| `NotificationEmailToggles` | Đổi toggle là lưu ngay | `GET`, `PUT /api/v1/me/email-preferences` (ghi `accounts.email_preferences`) |
| `CheckProgressDialog` | Popup trên Assignment List, làm mới realtime | `GET /api/v1/assignments/{id}/progress` |
| `AdminStatisticsPanel` | Chỉ `ADMIN`; tải khi mở Statistic; số đếm, không danh sách người | `GET /api/v1/admin/statistics` |
| `GradeDistributionBadge` | Chỉ hiện khi lớp bật và đủ mẫu; không có thì ẩn | `GET /api/v1/me/classes/{classId}/grade-distribution` |
| `GradebookExportAction` | Chọn lớp/bài và CSV/XLSX; tải khi có quyền | `GET /api/v1/classes/{id}/gradebook/export?format=csv|xlsx&assignmentId=` |

## UC/screen mapping
NotificationBell/Dropdown nằm trên Navigation Bar UC 11; NotificationListPage/email preference là hỗ trợ. AdminStatisticsPanel dùng nhãn Statistic UC 57, mở từ Admin Sidebar. GradebookExportAction thuộc UC 37 trên Class Detail với R3/R4; phân bố điểm/tiến độ là supporting flows.

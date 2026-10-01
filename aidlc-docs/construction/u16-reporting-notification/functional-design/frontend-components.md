# U16 Reporting & Notification - Frontend Components

```
components/notifications/
  NotificationBell         số chưa đọc, cập nhật qua SSE
  NotificationDropdown     10 mới nhất, "Xem tất cả", "Đánh dấu đã đọc"
app/notifications/         NotificationListPage (phân trang)
  NotificationEmailToggles toggle bật/tắt email từng loại, ngay trên trang (không có trang cài đặt riêng)
app/teaching/publications/[id]/progress   SubmissionProgressPage
  ProgressSummary          đã nộp / đang làm / chưa bắt đầu / trễ, thời gian còn lại
  ProgressTable            người học (hoặc nhóm), trạng thái, thời điểm nộp
app/learn/dashboard/       StudentResultDashboard (trang đích Student: lớp đã ghi danh, bài sắp hạn, thông báo, trạng thái, điểm công bố)
app/teaching/classes/[id]/gradebook/  GradebookExportAction (CSV/XLSX theo lớp hoặc bài)
```

| Component | Hành vi | API |
|---|---|---|
| `NotificationBell` | SSE `GET /api/v1/me/notifications/stream`; mất kết nối thì tải lại số chưa đọc | `GET /api/v1/me/notifications/unread-count` |
| `NotificationListPage` | | `GET /api/v1/me/notifications`, `POST .../{id}/read`, `POST .../read-all` |
| `NotificationEmailToggles` | Đổi toggle là lưu ngay | `GET`, `PUT /api/v1/me/notification-preferences` |
| `SubmissionProgressPage` | | `GET /api/v1/publications/{id}/progress` |
| `StudentResultDashboard` | Chỉ dữ liệu của mình; phân bố lớp chỉ hiện khi đủ điều kiện ẩn danh | `GET /api/v1/me/dashboard`, `GET /api/v1/me/classes/{classId}/grade-distribution` |
| `GradebookExportAction` | Chọn lớp/bài và CSV/XLSX; tải khi có quyền | `GET /api/v1/classes/{id}/gradebook/export?format=csv|xlsx&publicationId=` |

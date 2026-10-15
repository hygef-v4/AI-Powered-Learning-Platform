# U14 Announcements & Notifications — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Class Announcements | `ClassAnnouncementsPage` | 63, 64 | Student, Teacher, Subject Manager của lớp |
| Student Class Detail (phần Thông báo) | `ClassAnnouncementsCard` | 63 | Student |
| Chuông thông báo trên thanh điều hướng | `NotificationBell`, `NotificationPopup` | 65 | Mọi vai trò |

## 2. Cây component

```
app/announcements/                 ClassAnnouncementsPage   Class Announcements
  AnnouncementFilters, AnnouncementFeed
  AnnouncementEditorDialog                                  tạo, sửa (giảng viên); quan trọng, gửi email
  DeleteAnnouncementDialog
(Student Class Detail, StudentClassSection của U05)  ClassAnnouncementsCard
(thanh điều hướng, HeaderWidget của U01)
  NotificationBell                                          số chưa đọc, kết nối SSE
    NotificationPopup                                       danh sách, đánh dấu đã đọc, tất cả đã đọc
      EmailPreferencePanel                                  chỉ hiện với Student
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `ClassAnnouncementsPage`, `AnnouncementFeed` | `GET /api/v1/me/announcements` | Mới nhất trước; lọc lớp; markdown đã làm sạch |
| `AnnouncementEditorDialog` | `POST .../announcements`, `PATCH /api/v1/announcements/{id}` | Ô quan trọng, gửi email chỉ khi tạo; `409` báo tải lại |
| `DeleteAnnouncementDialog` | `DELETE /api/v1/announcements/{id}` | Xác nhận |
| `NotificationBell`, `NotificationPopup` | `/api/v1/me/notifications/...` | SSE; mất kết nối thì tải lại số chưa đọc |
| `EmailPreferencePanel` | `GET`, `PUT /api/v1/me/notification-preferences` | Bật, tắt từng loại email học tập |

Route `/announcements` cho Student, Teacher, Subject Manager (dùng `RoleGuard` của U01); chuông hiện cho mọi vai trò. Backend vẫn kiểm quyền thật.

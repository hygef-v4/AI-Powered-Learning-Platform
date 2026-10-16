# U14 Announcements & Notifications — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AnnouncementController`, `MyAnnouncementController`, `NotificationController`, `NotificationPreferenceController` | backend | API mục 4 của domain-entities |
| `AnnouncementService` | backend | Tạo, sửa, xóa, xem thông báo lớp (F1–F3) |
| `NotificationEventListener`, `RecipientResolver`, `NotificationService` | worker, backend | Tạo thông báo từ sự kiện, xem, đánh dấu (F4, F5, P1) |
| `EmailDeliveryService`, `EmailSendJobHandler`, `EmailCapPolicy` | backend, worker | Email, trần ngày (F6, P2) |
| `DeadlineReminderScanner` | worker | Nhắc hạn (F7, P3) |
| `NotificationPreferenceService` | backend | Tùy chọn email (F8) |
| `NotificationMaintenanceScanner` | worker | Gửi email hoãn, dọn thông báo (F9, P4) |
| Repository các bảng của U14 | backend, worker | Chỉ dùng trong package |

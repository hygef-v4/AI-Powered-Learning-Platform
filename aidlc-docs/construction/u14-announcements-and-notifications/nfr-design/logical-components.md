# U14 Announcements & Notifications — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

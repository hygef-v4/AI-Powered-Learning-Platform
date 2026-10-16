# U14 Announcements & Notifications — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Thiết kế: [business-logic-model](../../u14-announcements-and-notifications/functional-design/business-logic-model.md), [business-rules](../../u14-announcements-and-notifications/functional-design/business-rules.md), [domain-entities](../../u14-announcements-and-notifications/functional-design/domain-entities.md), [frontend-components](../../u14-announcements-and-notifications/functional-design/frontend-components.md), [NFR](../../u14-announcements-and-notifications/nfr-requirements/nfr-requirements.md), [NFR design](../../u14-announcements-and-notifications/nfr-design/nfr-design-patterns.md), [infrastructure](../../u14-announcements-and-notifications/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/announcements/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../announcements/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/announcements/` | Chỉ có `index.ts` trống |
| `backend/src/main/java/edu/aiplatform/notifications/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../notifications/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/notifications/` | Chỉ có `index.ts` trống |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/notifications/V20260925_2000__create_notification_tables.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `Announcement`, `Notification`, `EmailDelivery`, `NotificationPreference`, `DeadlineReminder` và enum; repository chỉ dùng trong package.
- [ ] **Bước 3 — Thông báo lớp:** `AnnouncementService` (F1–F3), phát `class.announcement-posted`.
- [ ] **Bước 4 — Sự kiện:** khai báo queue `jobs.notification`; `NotificationEventListener`, `RecipientResolver`, `NotificationService` (F4, F5, P1).
- [ ] **Bước 5 — Email:** `EmailDeliveryService`, `EmailSendJobHandler`, `EmailCapPolicy`, mẫu nội dung tối thiểu (F6, P2).
- [ ] **Bước 6 — Nhắc hạn:** `DeadlineReminderScanner` (F7, P3).
- [ ] **Bước 7 — Tùy chọn, dọn dữ liệu:** `NotificationPreferenceService` (F8), `NotificationMaintenanceScanner` (F9, P4).
- [ ] **Bước 8 — API:** controller theo domain-entities mục 4; `contracts/openapi/notifications.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 9 — Frontend:** `ClassAnnouncementsPage` (đăng ký `DashboardNavEntry`), `ClassAnnouncementsCard` (`StudentClassSection`), `NotificationBell` và `NotificationPopup` (slot `HeaderWidget` của U01), `EmailPreferencePanel`.
- [ ] **Bước 10 — Unit test (người code viết):** quyền tạo, sửa, xóa thông báo; sửa không phát lại sự kiện; người nhận theo từng loại sự kiện; bài của môn gửi cho mọi lớp mở; sự kiện lặp không tạo trùng; tắt email thì `SKIPPED`; chạm trần thì `DEFERRED` và thứ tự ưu tiên khi đưa lại; nhắc hạn một lần, đổi hạn nhắc lại, bài mở muộn không nhắc; nội dung email không có điểm; dọn 180 ngày; component frontend. Integration test do tester viết.
- [ ] **Bước 11 — Review:** đối chiếu UC 63–65, story, BR-U14 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Đăng thông báo quan trọng có email: người học nhận trong app và trong Mailpit theo tùy chọn.
- SMTP lỗi: nghiệp vụ không rollback, email `FAILED` sau 5 lượt.
- Bài hết hạn sau 24 giờ có người chưa nộp (cá nhân và nhóm): nhắc đúng người, đúng một lần.
- Chuông nhận thông báo qua SSE khi backend có hai instance.
- Phân quyền 4 role trên mọi endpoint, gồm đọc thông báo của người khác và đăng thông báo lớp không dạy.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.

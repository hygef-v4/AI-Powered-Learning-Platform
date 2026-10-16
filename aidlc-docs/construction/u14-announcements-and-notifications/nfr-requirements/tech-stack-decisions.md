# U14 Announcements & Notifications — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA | Như các unit khác |
| Nhận sự kiện | Listener trên queue `jobs.notification` bind `platform.events` | Hạ tầng của U03 |
| Email | Job `EMAIL_SEND` trên `jobs.email`, gửi bằng `IMail` của U01 (Brevo, Mailpit) | Dùng chung SMTP |
| Trần email | Biến `U14_EMAIL_DAILY_CAP` (mặc định 300, bằng hạn mức gói miễn phí của Brevo) | Đổi được khi đổi gói |
| Realtime | `ISseHub` của U12, kênh theo tài khoản | Dùng chung |
| Test | JUnit 5, Vitest; Testcontainers cho integration test do tester viết | Theo NFR-004 |

# U14 Announcements & Notifications — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

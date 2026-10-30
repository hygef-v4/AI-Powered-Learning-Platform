# U14 Announcements & Notifications — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package announcements, notifications) --> postgres
browser <--SSE-- nginx <-- backend (ISseHub của U12)
RabbitMQ platform.events --> jobs.notification --> worker (tạo thông báo) --> postgres
worker --> jobs.email --> worker (EmailSendJobHandler) --> IMail (U01) --> Brevo / Mailpit
```

**Text alternative**: Người dùng dùng trình duyệt qua Nginx tới backend để xem, đăng thông báo lớp và xem thông báo của mình; chuông nhận cập nhật qua SSE. Sự kiện nghiệp vụ trên exchange `platform.events` vào queue `jobs.notification`; worker tạo thông báo và bản ghi email, rồi job email gửi qua `IMail` của U01 tới Brevo hoặc Mailpit.

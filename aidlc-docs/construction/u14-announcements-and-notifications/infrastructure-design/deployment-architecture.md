# U14 Announcements & Notifications — deployment-architecture

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --> nginx --> backend (package announcements, notifications) --> postgres
browser <--SSE-- nginx <-- backend (SseHub của U12)
RabbitMQ platform.events --> jobs.notification --> worker (tạo thông báo) --> postgres
worker --> jobs.email --> worker (EmailSendJobHandler) --> MailPort (U01) --> Brevo / Mailpit
```

**Text alternative**: Người dùng dùng trình duyệt qua Nginx tới backend để xem, đăng thông báo lớp và xem thông báo của mình; chuông nhận cập nhật qua SSE. Sự kiện nghiệp vụ trên exchange `platform.events` vào queue `jobs.notification`; worker tạo thông báo và bản ghi email, rồi job email gửi qua `MailPort` của U01 tới Brevo hoặc Mailpit.

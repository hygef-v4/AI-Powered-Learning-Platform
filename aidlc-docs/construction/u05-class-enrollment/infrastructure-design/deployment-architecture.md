# U05 Class & Enrollment — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

```
browser --> nginx --> backend (package academics) --> postgres (course_classes, enrollments)
backend --> RabbitMQ (platform.events) : enrollment.activated
```

**Text alternative**: Student, Teacher và Subject Manager dùng trình duyệt, qua Nginx tới backend; package `academics` đọc và ghi bảng lớp và ghi danh trong PostgreSQL, gửi sự kiện ghi danh lên RabbitMQ để U14 thông báo.

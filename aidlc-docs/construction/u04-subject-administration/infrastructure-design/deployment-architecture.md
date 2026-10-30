# U04 Subject Administration — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

```
browser --> nginx --> backend (package subjects) --> postgres (bảng subjects)
```

**Text alternative**: Admin dùng trình duyệt, qua Nginx tới backend; package `subjects` đọc và ghi bảng `subjects` trong PostgreSQL. U04 không có thành phần chạy ở worker.

# U04 Subject Administration — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA, bảng `subjects` | Như các unit khác |
| Khóa lạc quan | Cột `version` với `@Version` | Đơn giản |
| Test | JUnit 5; Testcontainers PostgreSQL cho integration test do tester viết | Theo NFR-004 |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

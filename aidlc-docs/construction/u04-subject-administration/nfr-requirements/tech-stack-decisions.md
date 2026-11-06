# U04 Subject Administration — tech-stack-decisions — chuẩn JDBC

> Quyết định persistence 2026-11-06: [Spring JDBC cho toàn dự án](../../persistence-conventions.md). Thay lựa chọn ORM trong thiết kế cũ; snapshot/evidence có ngày là lịch sử. Code còn JPA được ghi là khoảng cách migration.

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring JDBC (JdbcTemplate/NamedParameterJdbcTemplate/JdbcClient), bảng `subjects` | Như các unit khác |
| Khóa lạc quan — chuẩn đích JDBC | UPDATE với expected storage version trong WHERE, tăng version tường minh và kiểm affected rows | Giữ CAS và quy tắc public version của U04; code U04 hiện còn JPA/@Version, cần migration riêng |
| Test | JUnit 5; Testcontainers PostgreSQL cho integration test do tester viết | Theo NFR-004 |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

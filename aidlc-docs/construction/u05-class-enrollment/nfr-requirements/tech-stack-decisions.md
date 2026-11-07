# U05 Class & Enrollment — tech-stack-decisions — chuẩn JDBC

> Quyết định persistence 2026-11-06: [Spring JDBC cho toàn dự án](../../persistence-conventions.md). Thay lựa chọn ORM trong thiết kế cũ; snapshot/evidence có ngày là lịch sử. Code còn JPA được ghi là khoảng cách migration.

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring JDBC (JdbcTemplate/NamedParameterJdbcTemplate/JdbcClient), bảng `course_classes`, `enrollments` | Như các unit khác |
| Khóa hàng cho BR-U05-22 | Khóa tài khoản U01 trước, rồi `ISubjectMutationGuard.lockActive` khóa dòng môn U04; đọc lại lớp/phạm vi sau khóa | Tuần tự hóa ghi danh cùng môn và thay đổi account/subject; giữ thứ tự khóa hiện hữu |
| Đọc CSV | Đọc dòng đơn giản, một cột, không thêm thư viện | CSV một cột |
| Frontend slot | Registry component trong `shared/class-slots` | Unit sau gắn phần của mình mà U05 không phụ thuộc ngược |
| Test | JUnit 5; Testcontainers PostgreSQL cho integration test do tester viết | Theo NFR-004 |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

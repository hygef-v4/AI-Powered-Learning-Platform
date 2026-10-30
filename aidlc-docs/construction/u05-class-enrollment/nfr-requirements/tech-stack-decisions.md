# U05 Class & Enrollment — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA, bảng `course_classes`, `enrollments` | Như các unit khác |
| Khóa hàng cho BR-U05-22 | `SELECT ... FOR UPDATE` trên ghi danh của sinh viên trong môn | Chặn ghi danh đồng thời |
| Đọc CSV | Đọc dòng đơn giản, một cột, không thêm thư viện | CSV một cột |
| Frontend slot | Registry component trong `shared/class-slots` | Unit sau gắn phần của mình mà U05 không phụ thuộc ngược |
| Test | JUnit 5; Testcontainers PostgreSQL cho integration test do tester viết | Theo NFR-004 |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

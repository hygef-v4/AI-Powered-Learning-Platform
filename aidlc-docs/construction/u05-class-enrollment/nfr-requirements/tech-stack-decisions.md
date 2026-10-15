# U05 Class & Enrollment — tech-stack-decisions

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA, bảng `course_classes`, `enrollments` | Như các unit khác |
| Khóa hàng cho BR-U05-22 | `SELECT ... FOR UPDATE` trên ghi danh của sinh viên trong môn | Chặn ghi danh đồng thời |
| Đọc CSV | Đọc dòng đơn giản, một cột, không thêm thư viện | CSV một cột |
| Frontend slot | Registry component trong `shared/class-slots` | Unit sau gắn phần của mình mà U05 không phụ thuộc ngược |
| Test | JUnit 5; Testcontainers PostgreSQL cho integration test do tester viết | Theo NFR-004 |

# U04 Subject Administration — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Tách phần môn học từ U04 cũ (Subject, Class & Enrollment); phần lớp và ghi danh thuộc U05. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## P1 - Đọc phạm vi không cache
- `isSubjectManager` chạy `SELECT 1 FROM subjects WHERE id = ? AND manager_id = ?` mỗi lần gọi.

## P2 - Khóa lạc quan
- `UPDATE subjects SET ..., version = version + 1 WHERE id = ? AND version = ?`; 0 dòng thì `409`.

## P3 - Gọi U05 qua contract có timeout
- `SubjectClassesPort` được gọi với timeout 1 giây; lỗi trả "không khả dụng" ở phần hiển thị, và lưu trữ bị từ chối (fail closed).
- Tới khi U05 xong, adapter tạm báo không khả dụng.

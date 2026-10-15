# U05 Class & Enrollment — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## P1 - Đọc phạm vi không cache
- `isTeacherOf`: `SELECT 1 FROM course_classes WHERE id = ? AND teacher_id = ?`; `isEnrolledStudent`: `SELECT 1 FROM enrollments WHERE class_id = ? AND account_id = ? AND status = 'ACTIVE'`; trạng thái lớp do unit gọi tự kiểm (BR-U05-31).

## P2 - Ghi danh có khóa hàng
- Trong một transaction: khóa các ghi danh của sinh viên ở các lớp chưa lưu trữ cùng môn (`FOR UPDATE`), kiểm BR-U05-22, rồi ghi.
- Danh sách: mỗi dòng một transaction riêng để dòng lỗi không làm hỏng dòng khác.

## P3 - Khóa lạc quan
- `UPDATE course_classes SET ..., version = version + 1 WHERE id = ? AND version = ?`; 0 dòng thì `409`.

## P4 - Sự kiện sau commit
- `enrollment.activated` gửi qua `EventPublisherPort` sau commit; mất sự kiện chỉ mất thông báo.

## P5 - Slot frontend
- Trang của U05 render vùng theo registry; unit chưa đăng ký thì vùng đó trống, không lỗi.

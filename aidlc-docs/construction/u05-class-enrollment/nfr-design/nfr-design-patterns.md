# U05 Class & Enrollment — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

## P1 - Đọc phạm vi không cache
- `isTeacherOf`: `SELECT 1 FROM course_classes WHERE id = ? AND teacher_id = ?`; `isEnrolledStudent`: `SELECT 1 FROM enrollments WHERE class_id = ? AND account_id = ? AND status = 'ACTIVE'`; trạng thái lớp do unit gọi tự kiểm (BR-U05-31).

## P2 - Ghi danh có khóa hàng
- Trong một transaction: khóa các ghi danh của sinh viên ở các lớp chưa lưu trữ cùng môn (`FOR UPDATE`), kiểm BR-U05-22, rồi ghi.
- Danh sách: mỗi dòng một transaction riêng để dòng lỗi không làm hỏng dòng khác.

## P3 - Khóa lạc quan
- `UPDATE course_classes SET ..., version = version + 1 WHERE id = ? AND version = ?`; 0 dòng thì `409`.

## P4 - Sự kiện sau commit
- `enrollment.activated` gửi qua `IEventPublisher` sau commit; mất sự kiện chỉ mất thông báo.

## P5 - Slot frontend
- Trang của U05 render vùng theo registry; unit chưa đăng ký thì vùng đó trống, không lỗi.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

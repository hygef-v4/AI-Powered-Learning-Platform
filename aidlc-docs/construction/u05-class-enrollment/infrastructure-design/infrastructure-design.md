# U05 Class & Enrollment — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` |
| Bảng | `course_classes`, `enrollments` trong `postgres` |
| Migration | `db/migration/classes/V20260925_1100__create_classes_enrollments.sql`: `course_classes` (unique `(subject_id, code)`, index `(subject_id, status)`, index `teacher_id`, FK `subject_id` → `subjects`, FK `teacher_id` → `accounts`, `show_grade_distribution` mặc định `false`, `version`); `enrollments` (khóa chính `(class_id, account_id)`, index `(account_id, status)`); chạy sau `subjects` (1000) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`, không `DELETE` trên hai bảng |
| Sự kiện | `enrollment.activated` qua exchange `platform.events` của U03 |
| Worker, queue, secret, dịch vụ ngoài | Không có |

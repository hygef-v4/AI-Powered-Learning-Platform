# U05 Class & Enrollment — infrastructure-design

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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

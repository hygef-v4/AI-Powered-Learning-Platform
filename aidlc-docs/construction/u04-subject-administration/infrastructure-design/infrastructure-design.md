# U04 Subject Administration — infrastructure-design

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` |
| Bảng | `subjects` trong `postgres` |
| Migration | `db/migration/subjects/V20260925_1000__create_subjects.sql`: bảng `subjects`, unique `code`, FK `manager_id` → `accounts`, cột `version`; chạy sau `accounts` (0930) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`, không `DELETE` |
| Worker, queue, secret, dịch vụ ngoài | Không có |

Bảng `course_classes` và `enrollments` do U05 tạo, có FK `subject_id` → `subjects`.

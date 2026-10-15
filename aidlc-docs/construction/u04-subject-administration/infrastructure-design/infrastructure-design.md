# U04 Subject Administration — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Tách phần môn học từ U04 cũ (Subject, Class & Enrollment); phần lớp và ghi danh thuộc U05. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` |
| Bảng | `subjects` trong `postgres` |
| Migration | `db/migration/subjects/V20260925_1000__create_subjects.sql`: bảng `subjects`, unique `code`, FK `manager_id` → `accounts`, cột `version`; chạy sau `accounts` (0930) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`, không `DELETE` |
| Worker, queue, secret, dịch vụ ngoài | Không có |

Bảng `course_classes` và `enrollments` do U05 tạo, có FK `subject_id` → `subjects`.

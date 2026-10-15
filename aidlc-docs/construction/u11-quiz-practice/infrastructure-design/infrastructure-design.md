# U11 Quiz Practice — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API), `worker` (scanner tự nộp) |
| Bảng | `quiz_attempts` trong `postgres` |
| Migration | `db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql`: unique `(quiz_id, account_id, attempt_no)`, unique riêng phần `(quiz_id, account_id)` với `IN_PROGRESS`, index `(status, deadline_at)`, `(quiz_id, class_id)`, `(account_id, submitted_at)`; FK `quiz_id` → `quizzes`, `class_id` → `course_classes`, `account_id` → `accounts`; chạy sau `quizzes` (1600) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`; không `DELETE` |
| Mạng ra | Không có |

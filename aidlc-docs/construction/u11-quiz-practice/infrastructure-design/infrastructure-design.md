# U11 Quiz Practice — infrastructure-design

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API), `worker` (scanner tự nộp) |
| Bảng | `quiz_attempts` trong `postgres` |
| Migration | `db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql`: unique `(quiz_id, account_id, attempt_no)`, unique riêng phần `(quiz_id, account_id)` với `IN_PROGRESS`, index `(status, deadline_at)`, `(quiz_id, class_id)`, `(account_id, submitted_at)`; FK `quiz_id` → `quizzes`, `class_id` → `course_classes`, `account_id` → `accounts`; chạy sau `quizzes` (1600) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`; không `DELETE` |
| Mạng ra | Không có |

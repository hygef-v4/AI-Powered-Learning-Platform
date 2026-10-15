# U10 Quiz Authoring — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Lấy ngân hàng câu hỏi của U06 cũ, phần Quiz của U08 cũ và cài đặt Quiz của U09 cũ. Ngân hàng chỉ còn câu trắc nghiệm theo UC 46, 47. Làm Quiz thuộc U11. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mục | Giá trị |
|---|---|
| Chạy ở | `backend` (API, nhập tệp, tool MCP), `worker` (handler đề xuất AI) |
| Bảng | `questions`, `quizzes`, `quiz_items`, `quiz_ai_drafts` trong `postgres` |
| Migration | `db/migration/quizzes/V20260925_1600__create_quiz_tables.sql`: `questions` (unique `(lineage_id, version_no)`, unique riêng phần `lineage_id` với `scope = 'BANK'` và `DRAFT`, index `(subject_id, status)`, `(quiz_id)`; FK `subject_id` → `subjects`); `quizzes` (index `(class_id, status)`, `(subject_id, scope, status)`; unique riêng phần `source_quiz_id` với `origin = 'NEW_VERSION'` và `DRAFT`, `REVIEWED`; FK `class_id` → `course_classes`, `subject_id` → `subjects`); `quiz_items` (khóa `(quiz_id, question_id)`); `quiz_ai_drafts` (index `(quiz_id, status)`, `(subject_id, status)`); chạy sau `grading` (1500) |
| Quyền database | User `app` được `SELECT`, `INSERT`, `UPDATE`; `DELETE` chỉ với câu nháp chưa từng kích hoạt, Quiz chưa từng phát hành, `quiz_items`, `quiz_ai_drafts` (kiểm trong code) |
| Sự kiện | `quiz.published` qua exchange `platform.events` của U03 |
| Nginx | Giới hạn body API nhập tệp 6 MB |
| Mạng ra | Không có |

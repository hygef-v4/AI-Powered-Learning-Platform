# U15 Grading - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `GradingService`, `BulkGradeService`, `PublishService`, `GradebookService`, `LearnerGradeController` | `backend` |
| `SubmissionGradeListener`, `QuizScorer` | `worker` |
| Bảng `grades`, `grade_history`; cột công bố điểm của `publications` (U08 tạo bảng) | `postgres` |
| RabbitMQ | queue `u15.grading-listener` bind `platform.events` với `u11.submission.submitted`, `u13.code.graded`, `u14.group.submitted`; phát `u15.grade.published` |

U15 không có secret, Redis key hay dịch vụ ngoài riêng.

## 2. Migration

`V20260925_2200__u15_grading.sql`:
- `grades`: unique `(target_kind, target_id, learner_id)`; `CHECK (final_score IS NULL OR (final_score >= 0 AND final_score <= max_score))`; index `(publication_id, learner_id)`, `(publication_id, status)`.
- `grade_history`: `REVOKE UPDATE, DELETE ON grade_history FROM app`.
- `ALTER TABLE publications ADD COLUMN grades_released_by uuid, grades_released_at timestamptz` (cần migration U08 chạy trước).

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

# U15 Grading - Infrastructure Design

**Bản tài liệu 2026-10-08**: UC 34, 35, 36, 37; primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `GradingService`, `BulkGradeService`, `PublishService`, `GradebookService`, `StudentGradeController` | `backend` |
| `QuizScorer` | thư viện dùng trong `backend` (nộp bài) |
| `SubmissionSubmittedAdapter`, `GroupSubmittedAdapter`, `CodeGradedAdapter`, `PracticeResultAdapter` | `backend`, `worker` |
| Bảng `evaluations`; cột `grades_released_at` của `assignments` (U08 tạo bảng) | `postgres` |
| RabbitMQ | Không có job riêng; phát `grade.published` (chỉ cho thông báo U16) |

U15 không có secret, Redis key hay dịch vụ ngoài riêng.

## 2. Migration

`V20260925_1950__u15_grading.sql`:
- `evaluations (id, attempt_id FK, group_document_id FK, account_id FK, kind, method, score, max_score, rubric_checks jsonb, feedback, ai_score, ai_feedback, status, history jsonb, published_at, version)` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md).
- `CHECK ((attempt_id IS NOT NULL) <> (group_document_id IS NOT NULL))`; `CHECK (score IS NULL OR (score >= 0 AND score <= max_score))`.
- Unique một phần: `(kind, attempt_id) WHERE attempt_id IS NOT NULL`; `(kind, group_document_id, coalesce(account_id, '00000000-0000-0000-0000-000000000000'))` khi có `group_document_id`.
- Index `(account_id, kind)`, `(status)`.
- Cột `assignments.grades_released_at` có sẵn trong migration U08.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

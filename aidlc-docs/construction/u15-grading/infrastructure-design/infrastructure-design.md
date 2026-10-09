# U15 Grading - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `GradingController`, `GradebookController`, `StudentGradeController`, `GradingService`, `BulkGradeService`, `PublishService`, `GradebookService` | `backend` |
| `QuizScorer` | thư viện dùng trong `backend` (nộp quiz luyện tập) |
| `SubmissionSubmittedAdapter`, `GroupSubmittedAdapter`, `CodeGradedAdapter`, `PracticeResultAdapter` | `backend`, `worker` |
| Bảng `evaluations` | `postgres` |
| RabbitMQ | Không có job riêng; phát `grade.published` (chỉ cho thông báo U16) |

U15 không có secret, Redis key hay dịch vụ ngoài riêng.

## 2. Migration

`db/migration/grading/V20260925_1950__create_evaluations.sql` (chạy sau migration của U04, U08, U11, U14):
- `evaluations (id, kind, assignment_id FK assignments, class_id FK classes NULL, attempt_id FK attempts NULL, group_document_id FK group_documents NULL, account_id FK accounts NULL, method, score, max_score, rubric_checks jsonb, feedback, ai_score, ai_feedback, status, history jsonb, published_at, version)` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md).
- `CHECK ((attempt_id IS NOT NULL) <> (group_document_id IS NOT NULL))`; `CHECK (kind = 'PRACTICE' OR class_id IS NOT NULL)`; `CHECK (score IS NULL OR (score >= 0 AND score <= max_score))`.
- Unique một phần: `(kind, attempt_id) WHERE attempt_id IS NOT NULL`; `(kind, group_document_id, coalesce(account_id, '00000000-0000-0000-0000-000000000000'))` khi có `group_document_id`.
- Index `(assignment_id, class_id, status)` (Submission Detail, sổ điểm, thời điểm công bố theo lớp), `(account_id, kind)`, `(class_id)`.
- Không có bảng công bố riêng (chỉ bảng của ERD); U15 không ghi cột `assignments.grades_released_at` (U08 bỏ cột). Thời điểm công bố của (bài, lớp) = MIN/MAX `published_at` của `evaluations` theo `assignment_id`, `class_id`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

# U06 Rubric & Question Bank - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service, nhập file, `RubricScorer` | `backend` |
| Bảng `questions`, `rubrics` | `postgres` |
| Ảnh trong khung tài liệu | U03 (Google Drive) |

U06 không chạy trong `worker`, không có queue, Redis key, secret hay kết nối ra ngoài riêng. Nhập file chạy đồng bộ trong request (≤ 10 s); file nhận vào đi qua multipart giới hạn 5 MB, không lưu lại.

## 2. Migration

`db/migration/questionbank/V20260925_1300__create_questions_rubrics.sql` (chưa áp dụng nên sửa trực tiếp):
- `questions` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md): unique `(lineage_id, version)`; partial unique `(lineage_id) WHERE status = 'DRAFT'`; CHECK `question_type IN ('MCQ_SINGLE', 'MCQ_MULTI', 'ESSAY', 'DOCUMENT', 'CODE')`, cột `assignment_type` để lọc, `scope_type IN ('SUBJECT', 'ASSIGNMENT')`; `definition jsonb`, `default_points numeric(6,2)`; index `(scope_type, scope_id, status)`, B-tree `lineage_id`, GIN `tags`.
- `rubrics`: `assignment_id` (index), `scope_type IN ('CLASS', 'SUBJECT')`, `scope_id`, `criteria jsonb`, `total_points numeric(6,2)`, `locked_at`, `updated_at`. FK `assignment_id` → `assignments` thêm trong migration của U08 vì bảng `assignments` tạo sau.
- User `app` được DELETE vì bản nháp câu hỏi chưa kích hoạt và rubric chưa khóa xóa được; service chặn xóa bản khác.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

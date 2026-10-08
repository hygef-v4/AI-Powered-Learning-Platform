# U06 Rubric & Question Bank - Infrastructure Design

**Bản tài liệu 2026-10-08**: UC 32, 33, 44, 55, 56; primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service, nhập file, `RubricScorer` | `backend` |
| Bảng `questions`, `rubrics` | `postgres` |
| Ảnh trong khung tài liệu | U03 (Google Drive) |

U06 không chạy trong `worker`, không có queue, Redis key, secret hay kết nối ra ngoài riêng. Nhập file chạy đồng bộ trong request (≤ 10 s); file nhận vào đi qua multipart giới hạn 5 MB, không lưu lại.

## 2. Migration

`V20260925_1300__u06_questions_rubrics.sql`:
- `questions` và `rubrics` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md); unique `(lineage_id, version)`; `questions.definition jsonb`, `rubrics.criteria jsonb`, `rubrics.total_points numeric(6,2)`; index `(scope_type, scope_id, status)`, GIN trên `questions.tags`.
- Mỗi bảng: partial unique `(lineage_id) WHERE status = 'DRAFT'`.
- B-tree `lineage_id` trên cả hai bảng.
- User `app` được DELETE vì bản `DRAFT` chưa kích hoạt xóa được; service chặn xóa bản khác.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

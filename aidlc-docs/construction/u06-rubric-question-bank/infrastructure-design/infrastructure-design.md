# U06 Rubric & Question Bank - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service, nhập file, `RubricScorer` | `backend` |
| Bảng `bank_items` | `postgres` |
| XML mẫu Draw.io | U03 (Google Drive) |

U06 không chạy trong `worker`, không có queue, Redis key, secret hay kết nối ra ngoài riêng. Nhập file chạy đồng bộ trong request (≤ 10 s); file nhận vào đi qua multipart giới hạn 5 MB, không lưu lại.

## 2. Migration

`V20260925_1300__u06_bank_items.sql`:
- `bank_items` theo `domain-entities.md`; `definition jsonb`, `default_points numeric(6,2)`.
- Unique `(stable_key, version_no)`; partial unique `(stable_key) WHERE status = 'DRAFT'`.
- Index `(subject_id, scope_type, class_id, item_type, status)`, GIN `tags`, B-tree `stable_key`.
- User `app` được DELETE vì bản `DRAFT` chưa kích hoạt xóa được; service chặn xóa bản khác.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

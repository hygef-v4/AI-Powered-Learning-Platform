# U10 Template & Copy - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Template, copy và diff | `backend` |
| Bảng `template_releases` và cột lineage của `assignments` (U08 tạo bảng) | `postgres` |

U10 không chạy trong `worker`, không có queue, Redis key, secret hay dịch vụ ngoài.

## 2. Migration

Migration U10 (giữ tiền tố version đã dự kiến, đổi mô tả thành `u10_template_copy` khi sinh mã):
- `template_releases` (FK `assignments`), unique `(template_assignment_id)`, index `(subject_id, status)`.
- `ALTER TABLE assignments ADD COLUMN lineage_kind, source_class_id, lineage_actor_id, lineage_at` (dùng cùng `source_assignment_id` của U08); lineage ghi một lần khi tạo bài, service không cho sửa.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

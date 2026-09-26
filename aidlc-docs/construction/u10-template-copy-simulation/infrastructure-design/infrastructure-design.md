# U10 Template, Copy & Simulation - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Template, copy, diff, chính sách thi thử | `backend` |
| Bảng `template_releases`; cột lineage của `assignments` và cột chính sách thi thử của `publications` (U08 tạo bảng) | `postgres` |

U10 không chạy trong `worker`, không có queue, Redis key, secret hay dịch vụ ngoài.

## 2. Migration

`V20260925_1700__u10_template_copy_simulation.sql`:
- `template_releases` (FK `assignments`), unique `(template_assignment_id)`, index `(subject_id, status)`.
- `ALTER TABLE assignments ADD COLUMN lineage_kind, source_class_id, lineage_actor_id, lineage_at` (dùng cùng `source_assignment_id` của U08); lineage ghi một lần khi tạo bài, service không cho sửa.
- `ALTER TABLE publications ADD COLUMN simulation_policy jsonb, policy_locked_at timestamptz`; `CHECK (simulation_policy IS NULL OR (simulation_policy->>'maxAttempts')::int BETWEEN 1 AND 10)`; mặc định `maxAttempts` = 3 do service đặt.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

# U12 Group & Allocation - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Nhóm của lớp, chia ngẫu nhiên, trưởng nhóm, sẵn sàng, tra cứu thành viên | `backend` |
| Bảng `student_groups`, `group_members`, `leader_change_requests` | `postgres` |
| Event | `group.membership-changed`, `group.leader-changed`, `group.leader-request-rejected` trên `platform.events` |

U12 không chạy trong `worker`, không có queue riêng, Redis key hay secret.

## 2. Migration

`V20260925_1900__u12_groups.sql`:
- `student_groups (id, class_id, name, leader_id, created_by, version)` unique `(class_id, name)`, index `(class_id)`; `class_id` tham chiếu lớp của U04 (không FK chéo unit).
- `group_members (group_id, student_id, joined_at, removed_at)`; ràng buộc mỗi sinh viên tối đa một nhóm đang hiệu lực trong một lớp được kiểm trong transaction lưu nhóm có `pg_advisory_xact_lock(class_id)`.
- `leader_change_requests` partial unique `(group_id) WHERE status = 'PENDING'`.
- `REVOKE DELETE ON group_members, leader_change_requests FROM app`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

# U12 Group & Allocation - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Bộ nhóm, trưởng nhóm, sẵn sàng, tra cứu thành viên | `backend` |
| Bảng `student_groups`, `group_members`, `leader_change_requests` | `postgres` |
| Event | `group.membership-changed`, `group.leader-changed` trên `platform.events` |

U12 không chạy trong `worker`, không có queue riêng, Redis key hay secret.

## 2. Migration

`V20260925_1900__u12_groups.sql`:
- `student_groups (id, assignment_id FK, name, leader_id, copied_from_assignment_id, created_by, version)` unique `(assignment_id, name)`, index `(assignment_id)`.
- `group_members (group_id, learner_id, joined_at, removed_at)`; ràng buộc mỗi người học tối đa một nhóm đang hiệu lực trong một bài được kiểm trong transaction lưu bộ nhóm có `pg_advisory_xact_lock(assignment_id)`.
- `leader_change_requests` partial unique `(group_id) WHERE status = 'PENDING'`.
- `REVOKE DELETE ON group_members, leader_change_requests FROM app`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

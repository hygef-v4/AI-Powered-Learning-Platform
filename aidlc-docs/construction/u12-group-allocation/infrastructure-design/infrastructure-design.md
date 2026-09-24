# U12 Group & Allocation - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Bộ nhóm, trưởng nhóm, phân công, sẵn sàng, tra cứu | `backend` |
| Bảng `group_sets`, `student_groups`, `group_members`, `part_allocations`, `leader_change_requests` | `postgres` |
| Event | `u12.group.part-assigned`, `u12.group.leader-changed` trên `platform.events` |

U12 không chạy trong `worker`, không có queue riêng, Redis key hay secret.

## 2. Migration

`V20260925_1900__u12_groups.sql`:
- `group_sets (id, assignment_id UNIQUE FK, copied_from_group_set_id, version, ...)`.
- `student_groups` unique `(group_set_id, name)`.
- `group_members` partial unique `(group_set_id, learner_id) WHERE removed_at IS NULL` (cột `group_set_id` lặp lại để đặt ràng buộc).
- `part_allocations` partial unique `(group_id, part_id) WHERE superseded_at IS NULL`, index `(assignee_id)`.
- `leader_change_requests` partial unique `(group_id) WHERE status = 'PENDING'`.
- `REVOKE DELETE ON group_members, part_allocations, leader_change_requests FROM app`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

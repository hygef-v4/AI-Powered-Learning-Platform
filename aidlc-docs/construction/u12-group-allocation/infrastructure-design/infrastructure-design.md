# U12 Group & Allocation - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Bộ nhóm, trưởng nhóm, sẵn sàng, tra cứu thành viên | `backend` |
| Bảng `group_sets`, `student_groups`, `group_members`, `leader_change_requests` | `postgres` |
| Event | `u12.group.membership-changed`, `u12.group.leader-changed` trên `platform.events` |

U12 không chạy trong `worker`, không có queue riêng, Redis key hay secret.

## 2. Migration

`V20260925_1900__u12_groups.sql`:
- `group_sets (id, assignment_id UNIQUE FK, copied_from_group_set_id, version, ...)`.
- `student_groups` unique `(group_set_id, name)`.
- `group_members` partial unique `(group_set_id, learner_id) WHERE removed_at IS NULL` (cột `group_set_id` lặp lại để đặt ràng buộc).
- `leader_change_requests` partial unique `(group_id) WHERE status = 'PENDING'`.
- `REVOKE DELETE ON group_members, leader_change_requests FROM app`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

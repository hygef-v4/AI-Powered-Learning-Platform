# U12 Group & Allocation - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Nhóm của lớp, chia ngẫu nhiên, trưởng nhóm, sẵn sàng, tra cứu thành viên | `backend` |
| Bảng `student_groups`, `group_members`, `leader_change_requests` | `postgres` |
| Event | `group.membership-changed`, `group.leader-requested`, `group.leader-changed`, `group.leader-request-rejected` trên `platform.events` (chỉ cho thông báo U16) |

U12 không chạy trong `worker`, không có queue riêng, Redis key hay secret.

## 2. Migration

`V20260925_1450__u12_groups.sql`:
- `student_groups` theo [database](../../../../docs/database.md): FK `class_id` → `course_classes`, unique `(class_id, name)`, cột `version`.
- `group_members` (bảng nối): khóa chính `(group_id, account_id)`, `is_leader`, partial unique `(group_id) WHERE is_leader`; mỗi sinh viên tối đa một nhóm trong một lớp được kiểm trong transaction lưu nhóm có `pg_advisory_xact_lock(class_id)`.
- `leader_change_requests`: FK `group_id`, `requester_id`, `nominee_id`; partial unique `(group_id) WHERE status = 'PENDING'`.
- `REVOKE DELETE ON leader_change_requests FROM app`; gỡ thành viên xóa dòng `group_members` và ghi audit.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

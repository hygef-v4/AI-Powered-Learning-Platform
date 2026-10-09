# U12 Group & Allocation - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 16, 17 và luồng phụ nhóm (quản lý nhóm, duyệt yêu cầu đổi trưởng nhóm trong tab Students của UC 32; sẵn sàng phát hành bài nhóm của UC 45) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Nhóm của lớp, chia ngẫu nhiên, trưởng nhóm, sẵn sàng, Nhóm của tôi, tra cứu thành viên | `backend` |
| Bảng `student_groups`, `group_members`, `leader_change_requests` | `postgres` |
| Event | `group.membership-changed`, `group.leader-requested`, `group.leader-changed`, `group.leader-request-rejected` trên `platform.events` (chỉ cho thông báo U16) |

U12 không chạy trong `worker`, không có queue riêng, Redis key hay secret.

## 2. Migration

`db/migration/groups/V20260925_1450__create_groups.sql`:
- `student_groups` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md): FK `class_id` → `course_classes`, unique `(class_id, name)`, cột `version`.
- `group_members` (bảng nối): khóa chính `(group_id, account_id)`, `is_leader`, partial unique `(group_id) WHERE is_leader`, index `(account_id)`; mỗi sinh viên tối đa một nhóm trong một lớp được kiểm trong transaction lưu nhóm có `pg_advisory_xact_lock(class_id)`.
- `leader_change_requests`: FK `group_id`, `requester_id`, `nominee_id` (cho phép NULL); partial unique `(group_id) WHERE status = 'PENDING'`; index `(requester_id, created_at)` cho yêu cầu gần nhất của mình.
- `REVOKE DELETE ON leader_change_requests FROM app`; gỡ thành viên xóa dòng `group_members` và ghi audit.

Nếu baseline cũ `V20260925_1450__u12_groups.sql` đã áp dụng ở môi trường nào, không sửa file cũ: thêm migration forward-only cho `nominee_id` cho phép NULL và index mới.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |

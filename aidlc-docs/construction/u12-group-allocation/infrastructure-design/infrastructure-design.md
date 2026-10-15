# U12 Group & Allocation — infrastructure-design

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 54, 55, 57. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

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

## Quy tắc tích hợp revision hiện hành

- Groups belong class, not assessment. Teacher assigned R3/R4 creates/allocates; Admin/manager without teaching assignment denied.
- Members ACTIVE enrolled, one effective group/class; exactly one leader in members. Random assignment only ungrouped students, preserve existing groups, preview balanced difference <=1 before save.
- Leader-change request PENDING/APPROVED/REJECTED/CANCELLED; max one pending/group; reason/proposed member validated. Only assigned teacher decides, requester may cancel pending.
- Changing/removing leader must select replacement atomically. Removal current membership revokes edit/submit next call but preserves revision authorship.
- Publication readiness for ASSIGNMENT submissionMode GROUP through GroupReadinessPort; không separate Group product type.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Balanced preview preserve grouped students; concurrent version conflict; exactly leader; nonteacher denied.
- Member removal denies U14/AI context next call while historical author retained; group readiness missing leader blocks publish.

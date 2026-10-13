# U02 Audit — infrastructure-design

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 73. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u02-audit-job-and-outbox` không quyết định chức năng hiện hành.

Hạ tầng chung ở `construction/shared-infrastructure.md`. Bảng theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md). RabbitMQ và worker thuộc U03.

## 1. Ánh xạ thành phần

| Thành phần | Container |
|---|---|
| `AuditStore`, `AuditQueryService`, `AuditController` | `backend` (và `worker` khi handler ghi audit) |
| Bảng `audit_logs` | `postgres` |

## 2. PostgreSQL

| Mục | Giá trị |
|---|---|
| User migration | `migrator`, chủ sở hữu schema, chỉ Flyway dùng lúc khởi động backend (tạo trong khung dự án, plan U03) |
| User ứng dụng | `app`, quyền `SELECT, INSERT, UPDATE, DELETE` trên bảng nghiệp vụ; riêng `audit_logs` chỉ `SELECT, INSERT` |
| Migration U02 | `db/migration/audit/V20260925_0900__create_audit_logs.sql`: bảng `audit_logs` (chưa có FK; migration `V20260925_0930__create_accounts.sql` của U01 thêm FK `actor_id` → `accounts`) + index `occurred_at`, `(actor_id, occurred_at)`, `(object_type, object_id)`, `action`; `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app` |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |

## Quy tắc tích hợp revision hiện hành

- Audit append-only; chỉ Admin query, không sửa/xóa. Ghi actor/action/object/result/occurredAt/correlationId; payload sanitize.
- Success ghi cùng transaction owner; denied/failure dùng transaction riêng khi nghiệp vụ rollback. Audit id do caller cung cấp, dedup khóa chính.
- Thêm actions AI_CONNECTION_CHANGED, QUOTA_CHANGED, MCP_REVOKED, BACKUP_COMPLETED/FAILED, RESTORE_VERIFIED và activity policy changed; không log secrets/raw prompt/nội dung nhạy cảm.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Id lặp không trùng; rollback denied vẫn ghi; field secrets/prompt bị loại; filter malformed từ chối; không Admin denied.

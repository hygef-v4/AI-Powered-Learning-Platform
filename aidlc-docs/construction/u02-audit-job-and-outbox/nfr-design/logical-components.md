# U02 Audit — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u02-audit-job-and-outbox` giữ tên cũ để không phá liên kết, là thiết kế của U02 Audit hiện hành.

## 1. Sơ đồ

```
 Backend (unit bất kỳ)                    Backend (API admin)
 +------------------------------+         +-------------------------+
 | service nghiệp vụ            |         | AuditController         |
 |   -> ghi dòng nghiệp vụ      |         |   -> AuditQueryService  |
 |   -> IAudit.record -------+-INSERT->|      (IAuthorization)|
 +------------------------------+    |    +-----------+-------------+
                                     v                |
                           PostgreSQL: audit_logs <---+ SELECT
```

**Text alternative**: Service nghiệp vụ của mọi unit gọi `IAudit.record` để INSERT vào `audit_logs` trong cùng transaction. Admin tra cứu qua `AuditController` → `AuditQueryService`, kiểm quyền bằng `IAuthorization` của U01, đọc `audit_logs`, lấy email người thực hiện qua `IAccountLookup` của U01 và tự ghi một dòng `AUDIT_QUERIED`. Worker cũng gọi `IAudit` khi handler cần ghi audit.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `IAudit` / `AuditStore` | Backend, worker | P1: một hàm `record`; `SUCCESS` INSERT trong transaction của unit gọi, `DENIED`/`FAILURE` bằng transaction riêng; kiểm khóa cấm bằng `ForbiddenKeyGuard` (`shared/`) |
| `AuditQueryService` | Backend | Tra cứu audit cho ADMIN, lấy email người thực hiện qua `IAccountLookup` (U01), tự ghi `AUDIT_QUERIED` |
| `AuditController` | Backend | `GET /api/v1/admin/audit-logs` |

## 3. Cấu hình

Không có biến riêng; dùng kết nối PostgreSQL chung.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Rule còn lại | N/A | Không áp dụng cho U02 hoặc ngoài phạm vi đồ án |

## Quy tắc tích hợp revision hiện hành

- Audit append-only; chỉ Admin query, không sửa/xóa. Ghi actor/action/object/result/occurredAt/correlationId; payload sanitize.
- Success ghi cùng transaction owner; denied/failure dùng transaction riêng khi nghiệp vụ rollback. Audit id do caller cung cấp, dedup khóa chính.
- Thêm actions AI_CONNECTION_CHANGED, QUOTA_CHANGED, MCP_REVOKED, BACKUP_COMPLETED/FAILED, RESTORE_VERIFIED và activity policy changed; không log secrets/raw prompt/nội dung nhạy cảm.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Id lặp không trùng; rollback denied vẫn ghi; field secrets/prompt bị loại; filter malformed từ chối; không Admin denied.

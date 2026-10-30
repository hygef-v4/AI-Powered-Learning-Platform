# U02 Audit — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

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

| Source | Trách nhiệm |
|---|---|
| [audit/controller](../../../../backend/src/main/java/edu/aiplatform/audit/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [audit/service](../../../../backend/src/main/java/edu/aiplatform/audit/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [audit/repository](../../../../backend/src/main/java/edu/aiplatform/audit/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [audit/model](../../../../backend/src/main/java/edu/aiplatform/audit/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [audit/contract](../../../../backend/src/main/java/edu/aiplatform/audit/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

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

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).

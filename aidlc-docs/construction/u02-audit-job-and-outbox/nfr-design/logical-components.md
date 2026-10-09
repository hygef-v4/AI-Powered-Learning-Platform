# U02 Audit - Logical Components

**Bản tài liệu 2026-10-09**: UC 73 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Backend (unit bất kỳ)                    Backend (API admin)
 +------------------------------+         +-------------------------+
 | service nghiệp vụ            |         | AuditController         |
 |   -> ghi dòng nghiệp vụ      |         |   -> AuditQueryService  |
 |   -> AuditPort.record -------+-INSERT->|      (AuthorizationPort)|
 +------------------------------+    |    +-----------+-------------+
                                     v                |
                           PostgreSQL: audit_logs <---+ SELECT
```

**Text alternative**: Service nghiệp vụ của mọi unit gọi `AuditPort.record` để INSERT vào `audit_logs` trong cùng transaction. Admin tra cứu qua `AuditController` → `AuditQueryService`, kiểm quyền bằng `AuthorizationPort` của U01, đọc `audit_logs`, lấy email người thực hiện qua `AccountLookupPort` của U01 và tự ghi một dòng `AUDIT_QUERIED`. Worker cũng gọi `AuditPort` khi handler cần ghi audit.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AuditPort` / `AuditStore` | Backend, worker | P1: INSERT trong transaction của unit gọi, kiểm khóa cấm bằng `ForbiddenKeyGuard` (`shared/`) |
| `AuditQueryService` | Backend | Tra cứu audit cho ADMIN, lấy email người thực hiện qua `AccountLookupPort` (U01), tự ghi `AUDIT_QUERIED` |
| `AuditController` | Backend | `GET /api/v1/admin/audit-logs` |

## 3. Cấu hình

Không có biến riêng; dùng kết nối PostgreSQL chung.

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Rule còn lại | N/A | Không áp dụng cho U02 hoặc ngoài phạm vi đồ án |

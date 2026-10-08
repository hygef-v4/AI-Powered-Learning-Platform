# U02 Audit - Business Logic Model

**Bản tài liệu 2026-10-08**: UC 70; primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Truy vết

| Luồng | Nguồn |
|---|---|
| A1 Ghi audit | US-AUD-001 S2, S3 |
| A2 Tra cứu audit | US-AUD-001 S1, UC 70 |

## 2. Audit

### A1 - Ghi audit
1. Trong transaction nghiệp vụ, unit gọi `AuditPort.record(event)` với `id` mới, `occurred_at` là thời điểm thao tác.
2. U02 kiểm danh sách khóa cấm trong `details` (BR-U02-05); vi phạm thì từ chối (lỗi lập trình, thao tác rollback).
3. INSERT vào `audit_logs` trong cùng transaction; `id` đã tồn tại thì bỏ qua (BR-U02-02, 03).
4. Sự kiện `DENIED`/`FAILURE` ghi bằng transaction riêng (`REQUIRES_NEW`) để còn lại dù thao tác chính rollback.

### A2 - Tra cứu audit
1. Gọi U01 `authorize(actor, AUDIT_READ)`. Không phải `ADMIN` hoặc U01 lỗi → từ chối (BR-U02-07, 50).
2. Kiểm bộ lọc hợp lệ, trang ≤ 100.
3. Truy vấn, sắp xếp `occurred_at` giảm dần.
4. Ghi audit `AUDIT_QUERIED` với bộ lọc đã dùng.

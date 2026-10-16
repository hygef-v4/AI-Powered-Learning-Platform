# U02 Audit — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u02-audit-job-and-outbox` giữ tên cũ để không phá liên kết, là thiết kế của U02 Audit hiện hành.

## 1. Truy vết

| Luồng | Nguồn |
|---|---|
| A1 Ghi audit | US-AUD-001 S2, S3 |
| A2 Tra cứu audit | US-AUD-001 S1, UC 13 (màn Audit Log) |

## 2. Audit

### A1 - Ghi audit
1. Trong transaction nghiệp vụ, unit gọi `IAudit.record(event)` với `id` mới, `occurred_at` là thời điểm thao tác.
2. U02 kiểm danh sách khóa cấm trong `details` (BR-U02-05); vi phạm thì từ chối (lỗi lập trình, thao tác rollback).
3. `result = SUCCESS`: INSERT vào `audit_logs` trong cùng transaction; `id` đã tồn tại thì bỏ qua (BR-U02-02, 03).
4. `result = DENIED` hoặc `FAILURE`: cũng gọi `record`, U02 tự ghi bằng transaction riêng (`REQUIRES_NEW`) để còn lại dù thao tác chính rollback (BR-U02-02).

### A2 - Tra cứu audit
1. Admin mở màn Audit Log từ Admin Dashboard; bộ lọc mặc định 7 ngày gần nhất.
2. Lọc theo người thực hiện: Admin nhập email, frontend tra `accountId` bằng API danh sách tài khoản của U01 rồi gửi `actorId` (BR-U02-08).
3. Gọi U01 `authorize(actor, AUDIT_READ)`. Không phải `ADMIN` hoặc U01 lỗi → từ chối (BR-U02-07, 50).
4. Kiểm bộ lọc hợp lệ (`from` ≤ `to`, `result` đúng giá trị), trang ≤ 100.
5. Truy vấn, sắp xếp `occurred_at` giảm dần.
6. Lấy email của các người thực hiện trong trang qua `IAccountLookup.getContact` (U01); `actor_id` rỗng hiện là hệ thống (BR-U02-09).
7. Ghi audit `AUDIT_QUERIED` với bộ lọc đã dùng.

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

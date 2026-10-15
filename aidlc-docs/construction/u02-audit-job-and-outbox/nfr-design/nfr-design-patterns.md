# U02 Audit — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: snapshot. Folder `u02-audit-job-and-outbox` giữ tên cũ để không phá liên kết, là thiết kế của U02 Audit hiện hành.

## P1 - Audit ghi trong transaction
- `AuditPort.record` INSERT vào `audit_logs` bằng transaction hiện tại của unit gọi (propagation `REQUIRED`): nghiệp vụ commit thì có audit, rollback thì không (BR-U02-02).
- `recordDenied`/`recordFailure` dùng propagation `REQUIRES_NEW` để sự kiện bị từ chối hoặc lỗi còn lại dù nghiệp vụ rollback.
- `INSERT ... ON CONFLICT (id) DO NOTHING` (BR-U02-03). Kiểm khóa cấm trong `details` trước khi ghi (BR-U02-05).

## P2 - Chặn sửa audit ở tầng database
- Ứng dụng kết nối bằng user `app`; `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app` trong migration (NFR-U02-30).

Việc nền (gửi sau commit, thử lại, sweeper, scanner, cách ly queue, timeout) chuyển sang U03 P8–P13.

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

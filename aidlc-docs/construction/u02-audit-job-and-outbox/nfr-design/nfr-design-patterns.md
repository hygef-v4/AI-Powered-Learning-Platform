# U02 Audit - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 73 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Audit ghi trong transaction
- `AuditPort.record` INSERT vào `audit_logs` bằng transaction hiện tại của unit gọi (propagation `REQUIRED`): nghiệp vụ commit thì có audit, rollback thì không (BR-U02-02).
- `recordDenied`/`recordFailure` dùng propagation `REQUIRES_NEW` để sự kiện bị từ chối hoặc lỗi còn lại dù nghiệp vụ rollback.
- `INSERT ... ON CONFLICT (id) DO NOTHING` (BR-U02-03). Kiểm khóa cấm trong `details` trước khi ghi (BR-U02-05).

## P2 - Chặn sửa audit ở tầng database
- Ứng dụng kết nối bằng user `app`; `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app` trong migration (NFR-U02-30).

Việc nền (gửi sau commit, thử lại, sweeper, scanner, cách ly queue, timeout) chuyển sang U03 P8–P13.

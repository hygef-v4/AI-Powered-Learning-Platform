# U02 Audit - NFR Design Patterns

## P1 - Audit ghi trong transaction
- `AuditPort.record` INSERT vào `audit_logs` bằng transaction hiện tại của unit gọi (propagation `REQUIRED`): nghiệp vụ commit thì có audit, rollback thì không (BR-U02-02).
- `recordDenied`/`recordFailure` dùng propagation `REQUIRES_NEW` để sự kiện bị từ chối hoặc lỗi còn lại dù nghiệp vụ rollback.
- `INSERT ... ON CONFLICT (id) DO NOTHING` (BR-U02-03). Kiểm khóa cấm trong `details` trước khi ghi (BR-U02-05).

## P2 - Chặn sửa audit ở tầng database
- Ứng dụng kết nối bằng user `app`; `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app` trong migration (NFR-U02-30).

Việc nền (gửi sau commit, thử lại, sweeper, scanner, cách ly queue, timeout) chuyển sang U03 P8–P13.

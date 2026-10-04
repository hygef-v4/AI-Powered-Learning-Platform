# U02 Audit - Infrastructure Design

Hạ tầng chung ở `construction/shared-infrastructure.md`. Bảng theo [database](../../../../docs/database.md). RabbitMQ và worker thuộc U03.

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
| Migration U02 | `V20260925_0900__u02_audit_logs.sql`: bảng `audit_logs` (chưa có FK; migration `V20260925_0930__u01_accounts.sql` của U01 thêm FK `actor_id` → `accounts`) + index `occurred_at`, `(actor_id, occurred_at)`, `(object_type, object_id)`, `action`; `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app` |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |

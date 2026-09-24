# U01 Account & Access - Infrastructure Design

Hạ tầng chung ở `construction/shared-infrastructure.md`. File này chỉ ghi phần U01 dùng.

## 1. Ánh xạ thành phần logic

| Thành phần (NFR Design) | Chạy ở | Ghi chú |
|---|---|---|
| `RateLimitFilter`, `JwtAuthFilter`, các service U01 | Container `backend`, package `u01` | Một backend modular monolith |
| `OtpMailHandler` | Container `worker` | Consumer RabbitMQ queue `u01.otp-delivery` |
| Bảng `accounts`, `account_import_batches`, `account_import_rows` | Container `postgres` | Migration Flyway trong thư mục của U01 |
| Refresh token, OTP, bucket rate limit | Container `redis`, database 0 | Khóa có tiền tố `u01:` |
| Outbox | Bảng outbox của U02 trong `postgres` → relay sang RabbitMQ | U02 sở hữu relay |
| Gửi mail | SMTP bên ngoài ở production; Mailpit ở local | Cấu hình `SMTP_*` |

## 2. RabbitMQ

| Thành phần | Giá trị |
|---|---|
| Exchange | `u01.events` (direct) |
| Queue | `u01.otp-delivery`, durable |
| Retry | Queue trễ `u01.otp-delivery.retry` với TTL 30 s, 1, 2, 4, 8 phút |
| Dead-letter | `u01.otp-delivery.dlq`; cảnh báo khi có message |
| Payload | `{ schemaVersion, jobId, correlationId, accountId, purpose }` - không chứa mã OTP |

## 3. Redis

| Tiền tố | Nội dung | TTL |
|---|---|---|
| `u01:refresh:{hash}` | Refresh session | Idle 2 giờ, kiểm trần 7 ngày |
| `u01:otp:{accountId}:{purpose}` | Băm OTP, số lượt còn lại | 10 phút |
| `u01:rl:*` | Bucket4j | Theo cửa sổ |

Redis **không bật persistence**: VPS khởi động lại thì mọi người phải đăng nhập lại và OTP đang chờ mất hiệu lực. Chấp nhận được vì Redis chỉ giữ dữ liệu tạm có TTL.

## 4. Cấu hình

Biến môi trường như `logical-components.md` mục 4; giá trị bí mật lấy từ CI/CD (shared-infrastructure mục 4).

## 5. Quan sát riêng U01

| Loại | Chỉ số / luật |
|---|---|
| Metric | `u01_login_total{result}`, `u01_account_temp_locked_total`, `u01_otp_requested_total{purpose}`, `u01_otp_sent_total{result}`, `u01_rate_limited_total{bucket}`, `u01_access_denied_total` |
| Cảnh báo | > 20 đăng nhập thất bại/5 phút từ một IP; > 10 lần từ chối quyền/5 phút cho một tài khoản; có thay đổi role; `u01.otp-delivery.dlq` > 0 |

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-01 | **Ngoại lệ được chấp nhận** | Không mã hóa at rest, không TLS nội bộ |
| SECURITY-02 | Compliant | Nginx access log vào Loki |
| SECURITY-06, 07, 09 | Compliant | Datastore không public, firewall chỉ 80/443/SSH, SSH bằng khóa, tắt root |
| SECURITY-04 | Compliant | Header ở Nginx |
| SECURITY-10 | Compliant | Image khóa phiên bản, tag SHA |
| SECURITY-12 | Ngoại lệ đã ghi ở NFR Requirements | - |
| SECURITY-14 | Compliant một phần | Có cảnh báo; log không thật sự append-only (ngoại lệ trong shared-infrastructure) |
| RESILIENCY-04 | Compliant | Direct deploy, rollback bằng tag SHA |
| RESILIENCY-05, 06, 07 | Compliant | Prometheus/Grafana, healthcheck mọi container, cảnh báo 80% |
| RESILIENCY-08, 11, 12 | **Ngoại lệ được chấp nhận** | Một VPS, không backup |
| RESILIENCY-13 | Compliant tối thiểu | Runbook dựng lại: cài Docker, chạy pipeline deploy; dữ liệu không khôi phục được |

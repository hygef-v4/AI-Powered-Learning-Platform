# U01 Account & Access - Infrastructure Design

Hạ tầng chung ở `construction/shared-infrastructure.md`. File này chỉ ghi phần U01 dùng.

## 1. Ánh xạ thành phần logic

| Thành phần (NFR Design) | Chạy ở | Ghi chú |
|---|---|---|
| `RateLimitFilter`, `JwtAuthFilter`, các service U01 | Container `backend`, package `u01` | Một backend modular monolith |
| `OtpMailHandler` | Container `worker` | Handler của job `U01_OTP_DELIVERY`, nhận từ queue `jobs.u01.otp-delivery` |
| Bảng `accounts` (gồm cột số dư credit do U07 ghi), `app_settings` (bảng cấu hình dùng chung, U01 tạo) | Container `postgres` | Migration Flyway trong thư mục của U01 |
| Refresh token, OTP, bucket rate limit | Container `redis`, database 0 | Khóa có tiền tố `u01:` |
| Job | Bảng `jobs` của U02 trong `postgres`; gửi RabbitMQ sau commit, quét gửi lại job kẹt quá 5 phút | U02 sở hữu |
| Gửi mail | SMTP bên ngoài ở production; Mailpit ở local | Cấu hình `SMTP_*` |

## 2. RabbitMQ

| Thành phần | Giá trị |
|---|---|
| Queue | `jobs.u01.otp-delivery`, durable, routing key `U01_OTP_DELIVERY` trên exchange `jobs` của U02 |
| Retry | Do bảng `jobs` của U02 điều khiển (backoff 30 s → 8 phút, lượt quét mỗi phút); không có queue trễ |
| Hết lượt | Job `FAILED`, log ERROR; không có DLQ |
| Payload | `{ schemaVersion, jobId, jobType, correlationId }`; `accountId` và `purpose` nằm trong `payloadRef` của bảng `jobs`, không có mã OTP |

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

Log có cấu trúc với các sự kiện `LOGIN_FAILED`, `ACCOUNT_TEMP_LOCKED`, `ROLE_CHANGED`, `ACCESS_DENIED`, `OTP_DELIVERY_FAILED`. Không có metric hay cảnh báo.

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Log che mật khẩu, OTP, token, số điện thoại; audit sự kiện đăng nhập/đổi quyền |
| SECURITY-04 | Compliant | Header ở Nginx |
| SECURITY-05 | Compliant | Validate email, hồ sơ, CSV; rate limit endpoint public |
| SECURITY-08 | Compliant | `authorize()` mặc định từ chối, kiểm role + phạm vi phía server |
| SECURITY-09 | Compliant | Không default password, secret từ biến môi trường |
| SECURITY-12 | Compliant (rút gọn) | bcrypt, ≥ 8 ký tự, khóa tạm, cookie HttpOnly; không MFA, không kiểm mật khẩu lộ theo phạm vi đồ án |
| SECURITY-15 | Compliant | Lỗi an toàn, fail-closed khi phụ thuộc lỗi |
| RESILIENCY-04 | Compliant | Deploy Compose, rollback bằng tag |
| RESILIENCY-06 | Compliant | Healthcheck container, `/health` |
| RESILIENCY-10 | Compliant | Timeout mọi phụ thuộc; không circuit breaker |
| Rule còn lại | N/A | Ngoài phạm vi đồ án (`requirements.md` mục 12-13) |

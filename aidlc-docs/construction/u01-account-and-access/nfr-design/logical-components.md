# U01 Account & Access - Logical Components

## 1. Sơ đồ

```
 Browser (Next.js)
   | cookie access_token / refresh_token
   v
 +-------------------- Backend (Spring Boot, package u01) --------------------+
 |  RateLimitFilter --> JwtAuthFilter --> Controllers                        |
 |                                         |                                 |
 |        +--------------------------------+---------------------------+     |
 |        v                v                v                          v     |
 |  AuthService     ActivationService  AccountAdminService   ProfileService  |
 |        |                |                |                          |     |
 |        +-------+--------+-------+--------+                          |     |
 |                v                v                                   v     |
 |       AuthorizationService   OtpService                       AvatarPort |
 |                |                |                              (-> U03)   |
 |                v                v                                         |
 |        ScopePort (-> U04)   OutboxPort (-> U02)                           |
 +---------------------------------------------------------------------------+
        |                 |                     |
        v                 v                     v
   PostgreSQL           Redis              Worker: OtpMailHandler --> SMTP
   accounts          refresh, OTP,                                (Mailpit local)
   import batch      rate buckets
```

**Text alternative**: Trình duyệt gửi cookie tới backend. Request đi qua `RateLimitFilter`, rồi `JwtAuthFilter`, rồi controller. Controller gọi `AuthService`, `ActivationService`, `AccountAdminService` hoặc `ProfileService`. Các service dùng `AuthorizationService` (hỏi U04 qua `ScopePort`) và `OtpService` (ghi outbox qua U02). `ProfileService` dùng `AvatarPort` do U03 cung cấp. Dữ liệu tài khoản ở PostgreSQL; refresh token, OTP và bucket rate limit ở Redis. Worker `OtpMailHandler` đọc outbox và gửi SMTP, local dùng Mailpit.

## 2. Thành phần

| Thành phần | Trách nhiệm | Phục vụ |
|---|---|---|
| `RateLimitFilter` | Bucket4j trên Redis cho endpoint public | NFR-U01-20, 21, 53 |
| `JwtAuthFilter` | Kiểm chữ ký, hạn JWT; dựng actor; không gọi Redis/DB | NFR-U01-04, 10 |
| `AuthService` | Đăng nhập, refresh, đăng xuất, khóa tạm, đổi/đặt lại mật khẩu | F3, F4, F5, F6 |
| `ActivationService` | Yêu cầu và hoàn tất kích hoạt | F1, F2 |
| `OtpService` | Ghi outbox yêu cầu OTP; xác minh mã người dùng nhập | BR-U01-20…27 |
| `PasswordPolicy` | ≥ 8 ký tự, chữ + số, không chứa tên email, ≤ 72 byte | BR-U01-30, 31, NFR-U01-14 |
| `TokenService` | Phát và xoay JWT/refresh; kiểm `credentialVersion` khi refresh | NFR-U01-10, 11 |
| `ProfileService` | Sửa tên, số điện thoại, ảnh | F7 |
| `AccountAdminService` | Tạo, đổi role, vô hiệu hóa/mở lại, bảo vệ admin cuối | F9, F11, F12 |
| `AccountImportService` | Kiểm và commit CSV theo lô, idempotent theo checksum | F10 |
| `AuthorizationService` | `authorize(actor, action, resourceRef)` mặc định từ chối | F13 |
| `OtpMailHandler` (worker) | Nhận job, sinh mã, lưu băm vào Redis, gửi SMTP, retry, dead-letter | NFR-U01-30, 31 |
| `SensitiveDataMasker` | Che dữ liệu nhạy cảm trong log | NFR-U01-50 |

## 3. Kho dữ liệu

| Kho | Khóa / bảng | TTL |
|---|---|---|
| PostgreSQL | `accounts`, `account_import_batches`, `account_import_rows` | Vĩnh viễn |
| Redis | `refresh:{hash}` | Idle 2 giờ, trần 7 ngày |
| Redis | `otp:{accountId}:{purpose}` | 10 phút |
| Redis | `rl:*` (Bucket4j) | Theo cửa sổ nạp lại |

## 4. Cấu hình (biến môi trường)

| Khóa | Mặc định | Bí mật |
|---|---|---|
| `U01_JWT_SECRET` | - | Có |
| `U01_ACCESS_TTL` | 15m | Không |
| `U01_REFRESH_IDLE_TTL` / `U01_REFRESH_MAX_TTL` | 2h / 7d | Không |
| `U01_BCRYPT_COST` | 12 | Không |
| `U01_OTP_TTL` / `U01_OTP_MAX_ATTEMPTS` | 10m / 5 | Không |
| `U01_LOGIN_LOCK_THRESHOLD` / `U01_LOGIN_LOCK_DURATION` | 5 / 15m | Không |
| `U01_RL_*` | Theo bảng P6 | Không |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` | Mailpit ở local | Mật khẩu là bí mật |

## 5. Compliance

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

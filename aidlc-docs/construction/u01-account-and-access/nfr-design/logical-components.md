# U01 Account & Access — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u01-account-and-access` là thiết kế của U01 hiện hành.

## 1. Sơ đồ

```
 Browser (Next.js)
   | cookie access_token / refresh_token
   v
 +-------------------- Backend (Spring Boot, package identity) --------------+
 |  RateLimitFilter --> JwtAuthFilter --> Controllers                        |
 |                                         |                                 |
 |        +--------------------------------+---------------------------+     |
 |        v                v                v                          v     |
 |  AuthService     ActivationService  AccountAdminService   ProfileService  |
 |        |                |                |                          |     |
 |        +-------+--------+-------+--------+                          |     |
 |                v                v                                         |
 |       AuthorizationService   OtpService                                   |
 |                |                |                                         |
 |                v                v                                         |
 |   Subject/IClassScope (U04/U05) IJob (-> U03)                       |
 +---------------------------------------------------------------------------+
        |                 |                     |
        v                 v                     v
   PostgreSQL           Redis              Worker: OtpMailHandler --> SMTP
   accounts          refresh, OTP,                                (Mailpit local)
                     rate buckets
```

**Text alternative**: Trình duyệt gửi cookie tới backend. Request đi qua `RateLimitFilter`, rồi `JwtAuthFilter`, rồi controller. Controller gọi `AuthService`, `ActivationService`, `AccountAdminService` hoặc `ProfileService`. Các service dùng `AuthorizationService` (hỏi U04 qua `ISubjectScope` và U05 qua `IClassScope`; U01 khai báo, U04 và U05 cài) và `OtpService` (gửi việc qua U03). ProfileService chỉ sửa displayName/phoneNumber. Dữ liệu tài khoản ở PostgreSQL; refresh token, OTP và bucket rate limit ở Redis. Worker `OtpMailHandler` nhận việc từ RabbitMQ và gửi SMTP, local dùng Mailpit.

## 2. Thành phần

| Source | Trách nhiệm |
|---|---|
| [identity/controller](../../../../backend/src/main/java/edu/aiplatform/identity/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [identity/service](../../../../backend/src/main/java/edu/aiplatform/identity/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [identity/repository](../../../../backend/src/main/java/edu/aiplatform/identity/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [identity/model](../../../../backend/src/main/java/edu/aiplatform/identity/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [identity/integration](../../../../backend/src/main/java/edu/aiplatform/identity/integration) | Adapter hạ tầng/provider, listener và sender. |
| [identity/config](../../../../backend/src/main/java/edu/aiplatform/identity/config) | Properties, cấu hình bean và topology. |
| [identity/contract](../../../../backend/src/main/java/edu/aiplatform/identity/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

## 3. Kho dữ liệu

| Kho | Khóa / bảng | TTL |
|---|---|---|
| PostgreSQL | `accounts` | Vĩnh viễn |
| Redis | `session:{sid}`, `session:refresh:{hash}` | Idle 2 giờ, trần 7 ngày |
| Redis | `account:sessions:{accountId}` | Theo phiên dài nhất còn lại |
| Redis | `otp:{accountId}:{purpose}` | 10 phút |
| Redis | `ticket:{hash}` | 10 phút |
| Redis | `ratelimit:auth:*` (Bucket4j) | Theo cửa sổ nạp lại |

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
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_STARTTLS_ENABLED`, `SMTP_FROM` | Mailpit local/test; Brevo SMTP demo/production | SMTP_PASSWORD là SMTP key của Brevo; không dùng API key |

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

## Quy tắc tích hợp revision hiện hành

- Tài khoản trường cấp, không public registration. Admin tạo/import không gửi OTP hoặc đặt mật khẩu; người dùng yêu cầu activation ở login đầu và tự đặt mật khẩu sau OTP.
- Account role cao nhất trong STUDENT/TEACHER/SUBJECT_MANAGER/ADMIN; authorization theo action/resource/current state, không chỉ role JWT. Admin chỉ quản trị, không học vụ.
- Profile cá nhân chỉ displayName/phoneNumber, email định danh/role/status bất biến trong /me. Reset không tiết lộ account tồn tại.
- MCP credentials xác thực actor qua U06 và cùng IAuthorization; không dùng provider API key làm ALAP authentication. Student không shared quota.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Tạo/import không gửi OTP, activation hợp lệ/sai/hết hạn/rate limit; recovery không lộ account.
- Đăng nhập/đăng xuất/phiên revoked; R1 own profile; Admin bị chặn học vụ; Student shared quota bị từ chối.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).

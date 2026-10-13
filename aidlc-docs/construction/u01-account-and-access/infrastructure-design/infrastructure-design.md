# U01 Account & Access — infrastructure-design

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u01-account-and-access` không quyết định chức năng hiện hành.

Hạ tầng chung ở `construction/shared-infrastructure.md`. File này chỉ ghi phần U01 dùng.

## 1. Ánh xạ thành phần logic

| Thành phần (NFR Design) | Chạy ở | Ghi chú |
|---|---|---|
| `RateLimitFilter`, `JwtAuthFilter`, các service U01 | Container `backend`, package `edu.aiplatform.identity` | Một backend modular monolith |
| `OtpMailHandler` | Container `worker` | Handler của việc `OTP_DELIVERY`, nhận từ queue `jobs.email` |
| Bảng `accounts` (gồm `email_preferences` do U16 ghi; quota/usage ở bảng riêng U07) | Container `postgres` | Migration Flyway trong thư mục của U01 |
| Refresh token, OTP, bucket rate limit | Container `redis`, database 0 | Khóa theo mục 3 |
| Việc nền | Message RabbitMQ gửi sau commit, không có bảng job | U03 sở hữu |
| Gửi mail | Brevo SMTP ở demo/production; Mailpit ở local/test | Cấu hình `SMTP_*` |

## 2. RabbitMQ

| Thành phần | Giá trị |
|---|---|
| Queue | `jobs.email`, durable, routing key `OTP_DELIVERY` trên exchange `jobs` của U03 |
| Retry | Queue thử lại `jobs.retry.*` của U03 (backoff 30 s → 8 phút, tối đa 5 lượt) |
| Hết lượt | Log ERROR; người dùng yêu cầu OTP lại; không có DLQ |
| Payload | `{ schemaVersion, jobType, idempotencyKey, payload: { accountId, purpose }, attempt, correlationId }`; không có mã OTP |

## 3. Redis

| Tiền tố | Nội dung | TTL |
|---|---|---|
| `session:refresh:{hash}` | Refresh session | Idle 2 giờ, kiểm trần 7 ngày |
| `otp:{accountId}:{purpose}` | Băm OTP, số lượt còn lại | 10 phút |
| `ticket:{hash}` | `VerificationTicket` sau khi OTP đúng | 10 phút |
| `ratelimit:auth:*` | Bucket4j | Theo cửa sổ |

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

## Quy tắc tích hợp revision hiện hành

- Tài khoản trường cấp, không public registration. Admin tạo/import không gửi OTP hoặc đặt mật khẩu; người dùng yêu cầu activation ở login đầu và tự đặt mật khẩu sau OTP.
- Account role cao nhất trong STUDENT/TEACHER/SUBJECT_MANAGER/ADMIN; authorization theo action/resource/current state, không chỉ role JWT. Admin chỉ quản trị, không học vụ.
- Profile cá nhân chỉ displayName/phoneNumber, email định danh/role/status bất biến trong /me. Reset không tiết lộ account tồn tại.
- MCP credentials xác thực actor qua U13 và cùng AuthorizationPort; không dùng provider API key làm ALAP authentication. Student không shared quota.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Tạo/import không gửi OTP, activation hợp lệ/sai/hết hạn/rate limit; recovery không lộ account.
- Đăng nhập/đăng xuất/phiên revoked; R1 own profile; Admin bị chặn học vụ; Student shared quota bị từ chối.

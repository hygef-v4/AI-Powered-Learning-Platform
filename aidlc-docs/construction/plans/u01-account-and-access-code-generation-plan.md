# U01 Account & Access - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U01. Mỗi bước xong thì đánh `[x]` ngay.
>
> Cập nhật 2026-09-25: dùng U02 thật cho job/audit (cạnh `C` U02 → U01), U03 rút gọn chỉ còn `AVATAR`, thêm `AccountLookupPort` cho U04/U16, ghi rõ bên khai báo và bên cài của port `C`.

## 1. Bối cảnh

- **Loại dự án**: greenfield, workspace root `AI-Powered-Learning-Platform/`. Code **không** nằm trong `aidlc-docs/`.
- **Story**: US-IAM-001…007. **Use case**: UC-IAM-01…12. Phần "gán phạm vi môn" của US-IAM-005 thuộc U04 (BR-U04-01); U01 chỉ đổi role.
- **Thiết kế nguồn**: `construction/u01-account-and-access/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Thứ tự**: U01 và U02 cùng wave 1, code song song. U03, U04 code sau U01.

### Khung dự án dùng chung

Bước 1-6 dưới đây là khung dự án cho mọi unit (Maven, cấu hình, `shared/`, frontend, Docker Compose, CI). U01 hoặc U02, unit nào code trước thì làm; unit kia đánh dấu `[x]` ở cả hai plan.

### Phụ thuộc

| Port | Bên khai báo / bên cài | Xử lý lượt này |
|---|---|---|
| `AuditPort`, `JobPort`, `JobHandler`, `JobHandlerRegistry` | U02 khai báo và cài (`C` U02 → U01) | **Dùng U02 thật.** Bước 9-10 cần U02 Bước 3-4 (domain, port) xong; chạy OTP đầu-cuối cần U02 Bước 5, 7, 13, 14. Hai người code U01/U02 thống nhất chữ ký port trước |
| `AvatarPort` | U01 khai báo, U03 cài (`C`) | U03 code sau: U01 dùng adapter tạm `AvatarUnavailableAdapter` (`isAvailable() = false`, ẩn nút đổi ảnh). U03 Bước 7 cài thật và bỏ adapter tạm |
| `SubjectScopePort`, `ClassScopePort` | U01 khai báo, U04 cài (`C`) | U04 code sau: adapter tạm `NoAssignmentScopeAdapter` trả "không phụ trách môn/lớp nào", nên hành động cần phạm vi bị **từ chối** (fail closed); kiểm hạ role cho qua vì chưa có phân công. U04 Bước 10 cài thật và bỏ adapter tạm |

### U01 cung cấp

| Port | Cho | Ghi chú |
|---|---|---|
| `AuthorizationPort.authorize(actor, action, resourceRef)` | Mọi unit | Thay `FakeAuthorizationPort` của U02 (và của U03 nếu đã có) |
| `AccountLookupPort` | U04, U16 | `findLearners(query, limit ≤ 20)`, `findByEmails(emails)`, `getContact(accountId)` (email, tên hiển thị, role, trạng thái); không trả mật khẩu, số điện thoại |

### Dữ liệu U01 sở hữu

PostgreSQL `accounts` (U07 thêm cột số dư credit bằng migration của U07), `app_settings` (bảng cấu hình dùng chung, U01 tạo, mỗi unit ghi khóa có tiền tố của mình); Redis `session:refresh:*`, `otp:*`, `ratelimit:auth:*`; queue `jobs.email` (khai báo qua topology của U02).

## 2. Cấu trúc thư mục

```
/backend                      Maven, Spring Boot (image dùng cho backend và worker)
  pom.xml
  src/main/java/edu/aiplatform/
    PlatformApplication.java
    shared/                   error handler, correlation ID, log masking, security mặc định từ chối
    u01/
      api/                    controller, DTO, cookie
      application/            AuthService, ActivationService, OtpService, TokenService,
                              ProfileService, AccountAdminService, AccountImportService,
                              AuthorizationService, AccountLookupService
      domain/                 Account, AccountStatus, Role, PasswordPolicy, LoginThrottle
      infrastructure/         JPA, Redis, JWT, SMTP, Bucket4j
      worker/                 OtpMailHandler (đăng ký với JobHandlerRegistry của U02)
      port/                   AuthorizationPort, AccountLookupPort (U01 cung cấp);
                              AvatarPort, SubjectScopePort, ClassScopePort (U01 khai báo)
      adapter/temp/           AvatarUnavailableAdapter, NoAssignmentScopeAdapter
  src/main/resources/
    application.yml, application-local.yml
    db/migration/u01/         Flyway
  src/test/java/...           unit + integration (Testcontainers)
/frontend                     Next.js App Router
  src/app/(auth)/...          login, activate, reset-password
  src/app/profile/...
  src/app/admin/accounts/...
  src/components/ui/          component tự viết
  src/lib/api/                client gọi backend
/contracts/openapi/u01-identity.yaml
/infra
  docker-compose.yml, docker-compose.local.yml
  nginx/
/.github/workflows/ci.yml
```

## 3. Các bước

### Nhóm A - Khung dự án (dùng chung)

- [ ] **Bước 1** - Tạo `/backend/pom.xml`: Spring Boot 3.x, Java 17, Web, Security, Data JPA, Validation, Data Redis, Actuator, Flyway, PostgreSQL, Mail, Bucket4j + Redis, jjwt, Commons CSV, Testcontainers, JUnit 5. Khóa phiên bản.
- [ ] **Bước 2** - `PlatformApplication`, `application.yml` (đọc mọi cấu hình U01 từ biến môi trường theo `logical-components.md` §4), `application-local.yml`.
- [ ] **Bước 3** - Hạ tầng dùng chung trong `shared/`: global error handler trả problem-details an toàn, filter correlation ID, bộ che dữ liệu nhạy cảm trong log, cấu hình Spring Security mặc định từ chối.
- [ ] **Bước 4** - Khung `/frontend`: Next.js + TypeScript strict + Tailwind, ESLint, Vitest + Testing Library, `src/lib/api` gửi cookie, component UI cơ bản (Button, Input, PasswordField, OtpInput, Dialog, Table, Alert).
- [ ] **Bước 5** - `/infra/docker-compose.yml` và `docker-compose.local.yml`: nginx, frontend, backend, postgres (`pgvector/pgvector:pg16`), redis, rabbitmq, mailpit (local); mạng `edge`/`internal`, giới hạn tài nguyên, healthcheck. Service `worker` do U02 Bước 2 thêm.
- [ ] **Bước 6** - `.github/workflows/ci.yml`: test backend + frontend, build image tag SHA. Bước deploy qua SSH để dạng khung, chưa bật.

### Nhóm B - Domain và business logic (US-IAM-001…007)

- [ ] **Bước 7** - Domain: `Account`, `AccountStatus` (3 trạng thái), `Role`, `Profile`, `LoginThrottle`, chuẩn hóa email, kiểm tên miền theo `u01.allowedEmailDomains`, chuyển trạng thái hợp lệ (BR-U01-03, 70…73).
- [ ] **Bước 8** - `PasswordPolicy` (BR-U01-30, 31; ≤ 72 byte), `PasswordHasher` bcrypt cost cấu hình.
- [ ] **Bước 9** - Port: khai báo `AvatarPort`, `SubjectScopePort`, `ClassScopePort` + `AvatarUnavailableAdapter`, `NoAssignmentScopeAdapter`; khai báo `AuthorizationPort`, `AccountLookupPort`. Dùng `AuditPort`, `JobPort` của U02 (cần U02 Bước 4).
- [ ] **Bước 10** - `OtpService` tạo job `OTP_DELIVERY` qua `JobPort.enqueue` trong cùng transaction (idempotency key `accountId:purpose:phút`); `OtpMailHandler` chạy ở `worker`, đăng ký với `JobHandlerRegistry` của U02: sinh mã 6 số, lưu băm Redis 10 phút, 5 lượt, gửi SMTP; lỗi thì U02 retry theo backoff, hết lượt job `FAILED` + log ERROR (BR-U01-20…27, NFR-U01-30, 31).
- [ ] **Bước 11** - `ActivationService`: F1, F2 (US-IAM-001).
- [ ] **Bước 12** - `TokenService`: JWT HMAC 15 phút; refresh ngẫu nhiên lưu băm, idle 2 giờ, trần 7 ngày, xoay vòng, phát hiện dùng lại; refresh kiểm `credentialVersion` (BR-U01-44…46, P1).
- [ ] **Bước 13** - `AuthService`: đăng nhập với hash giả cho email không tồn tại, khóa tạm 5 lần/15 phút, refresh, đăng xuất (chỉ phiên hiện tại), quên mật khẩu, đổi mật khẩu tăng `credentialVersion` (F3-F6; US-IAM-002, 003, 006).
- [ ] **Bước 14** - `ProfileService`: F7 (US-IAM-004); đổi ảnh qua `AvatarPort.validateAvatar`, tắt khi adapter tạm báo chưa hỗ trợ.
- [ ] **Bước 15** - `AuthorizationService` cài `AuthorizationPort`: mặc định từ chối, kết hợp role + `SubjectScopePort`/`ClassScopePort`; không gọi được U04 → từ chối (F13; BR-U01-62, 93; US-IAM-005). Thay `FakeAuthorizationPort` của U02 (và U03 nếu có) bằng bean này.
- [ ] **Bước 16** - `AccountAdminService`: tạo, danh sách, đổi role (tăng `credentialVersion`, BR-U01-63), vô hiệu hóa/mở lại, bảo vệ admin tự hạ và admin cuối, chặn hạ role khi còn phụ trách môn/lớp (F8, F9, F11, F12; BR-U01-64; US-IAM-005, 007).
- [ ] **Bước 17** - `AccountImportService`: CSV ≤ 1000 dòng, kiểm từng dòng, trả kết quả không lưu; xác nhận thì kiểm lại và tạo dòng hợp lệ trong một transaction; audit kèm checksum; cấm tạo ADMIN (F10; BR-U01-80…83; US-IAM-007).
- [ ] **Bước 18** - `AccountLookupService` cài `AccountLookupPort` cho U04, U16 (chỉ trả email, tên hiển thị, role, trạng thái).
- [ ] **Bước 19** - Audit qua `AuditPort` của U02 mọi sự kiện BR-U01-90; không đưa mật khẩu, OTP, token, số điện thoại vào payload.
- [ ] **Bước 20** - Unit test cho mọi `BR-U01-xx` ở bước 7-19 (mock port của U02, U03, U04).
- [ ] **Bước 21** - Tóm tắt business logic: `aidlc-docs/construction/u01-account-and-access/code/business-logic-summary.md`.

### Nhóm C - Repository và migration

- [ ] **Bước 22** - Flyway `V20260924_1600__u01_accounts.sql`: `accounts` (email duy nhất, status 3 giá trị `PENDING`/`ACTIVE`/`DISABLED`, `credential_version`, `failed_login_count`, `locked_until`, `phone_number`, `avatar_ref`), `app_settings` (`key` khóa chính, `value` JSON, `updated_by`, `updated_at`); seed khóa `u01.allowedEmailDomains` và một ADMIN ở trạng thái `PENDING` lấy email từ biến môi trường.
- [ ] **Bước 23** - Repository JPA, adapter Redis cho refresh/OTP, Bucket4j proxy manager.
- [ ] **Bước 24** - Integration test Testcontainers (PostgreSQL, Redis, RabbitMQ, Mailpit): kích hoạt đầu-cuối qua bảng `jobs` + worker của U02 (cần U02 Bước 13-14), đăng nhập, khóa tạm, refresh dùng lại, import lặp lại cùng file.
- [ ] **Bước 25** - Tóm tắt repository: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 26** - `/contracts/openapi/u01-identity.yaml`: toàn bộ endpoint U01, lỗi problem-details, cookie.
- [ ] **Bước 27** - Controller + DTO + validation: `auth` (login, refresh, logout, activation-requests, activations, password-reset-requests, password-resets), `me` (profile, password), `admin/accounts` (list, create, role, status, imports). `RateLimitFilter`, `JwtAuthFilter`, cookie theo P2.
- [ ] **Bước 28** - Test API: MockMvc cho mọi endpoint, gồm negative test bảo mật (NFR-U01-61).
- [ ] **Bước 29** - Tóm tắt API: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 30** - Trang auth: `LoginPage`, `ActivationPage`, `PasswordResetPage` với `RequestOtpStep` và `VerifyOtpAndSetPasswordStep` dùng chung.
- [ ] **Bước 31** - `ProfilePage`: `ProfileForm`, `AvatarUploader` (bọc `FileUploader` của U03 với `purpose = AVATAR`; ẩn khi backend báo chưa hỗ trợ), `ChangePasswordForm`, `LogoutButton`.
- [ ] **Bước 32** - Admin: `AccountListPage`, `CreateAccountDialog`, `AccountDetailPage` (`RoleChangeDialog`, `StatusToggleDialog`), `ImportAccountsPage`.
- [ ] **Bước 33** - Test frontend: validation form, thông điệp trung tính, hành vi nút theo phản hồi backend.
- [ ] **Bước 34** - Tóm tắt frontend: `code/frontend-summary.md`.

### Nhóm F - Tài liệu và triển khai

- [ ] **Bước 35** - Cấu hình Nginx: định tuyến, header SEC-004 (CSP theo `shared-infrastructure.md`), HTTP ở local, giới hạn log Docker.
- [ ] **Bước 36** - `README.md` ở root: chạy local bằng Docker Compose (gồm `worker`), biến môi trường, tài khoản admin seed, cách xem OTP trong Mailpit, chạy test.
- [ ] **Bước 37** - Chạy toàn bộ test backend và frontend; ghi kết quả vào `code/test-results.md`.

## 4. Truy vết story

| Story | Bước |
|---|---|
| US-IAM-001 Kích hoạt | 7, 8, 10, 11, 22, 24, 27, 30 |
| US-IAM-002 Đăng nhập/đăng xuất | 12, 13, 27, 30, 31 |
| US-IAM-003 Quên mật khẩu | 10, 13, 27, 30 |
| US-IAM-004 Hồ sơ | 14, 27, 31 |
| US-IAM-005 Role (phạm vi môn ở U04) | 15, 16, 19, 27, 32 |
| US-IAM-006 Đổi mật khẩu | 8, 13, 27, 31 |
| US-IAM-007 Vòng đời tài khoản | 16, 17, 19, 22, 27, 32 |
| Contract cho unit khác (`AuthorizationPort`, `AccountLookupPort`) | 9, 15, 18 |

## 5. Ngoài phạm vi lượt này

- Cài thật `AvatarPort` (U03 Bước 7) và `SubjectScopePort`/`ClassScopePort` (U04 Bước 10); khi đó bỏ adapter tạm của U01.
- Code của U02 (job platform, worker, audit); U01 chỉ đăng ký handler `OTP_DELIVERY`.
- Bật bước deploy SSH trong CI.
- Test chịu lỗi (RESILIENCY-14) ngoài phạm vi đồ án.

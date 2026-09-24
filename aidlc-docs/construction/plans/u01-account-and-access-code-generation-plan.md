# U01 Account & Access - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U01. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Loại dự án**: greenfield, workspace root `AI-Powered-Learning-Platform/`. Code **không** nằm trong `aidlc-docs/`.
- **Story**: US-IAM-001…007. **Use case**: UC-IAM-01…12.
- **Thiết kế nguồn**: `construction/u01-account-and-access/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Quyết định code**: Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết; port + adapter giả cho U02/U03/U04.

### Phụ thuộc và adapter giả

| Port | Unit thật | Adapter giả lượt này | Thay khi |
|---|---|---|---|
| `AuditPort` | U02 | Ghi sự kiện ra log có cấu trúc | U02 xong |
| `OutboxPort` | U02 | Hàng đợi trong bộ nhớ, gọi `OtpMailHandler` bất đồng bộ trong backend, gửi qua SMTP (Mailpit ở local) | U02 + worker xong |
| `ScopePort` | U04 | Trả rỗng: không ai phụ trách môn/lớp; hạ role luôn qua được kiểm phân công | U04 xong |
| `AvatarPort` | U03 | `isAvailable() = false`; đổi ảnh bị tắt | U03 xong |

### Dữ liệu U01 sở hữu

`accounts`, `account_import_batches`, `account_import_rows` (PostgreSQL); `u01:refresh:*`, `u01:otp:*`, `u01:rl:*` (Redis).

## 2. Cấu trúc thư mục

```
/backend                      Maven, Spring Boot
  pom.xml
  src/main/java/edu/aiplatform/
    PlatformApplication.java
    shared/                   error handler, correlation ID, log masking
    u01/
      api/                    controller, DTO, cookie
      application/            service
      domain/                 Account, policy, rule
      infrastructure/         JPA, Redis, JWT, SMTP, Bucket4j
      port/                   AuditPort, OutboxPort, ScopePort, AvatarPort
      adapter/fake/           adapter giả
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
  nginx/, prometheus/, grafana/, loki/
/.github/workflows/ci.yml
```

## 3. Các bước

### Nhóm A - Khung dự án

- [ ] **Bước 1** - Tạo `/backend/pom.xml`: Spring Boot 3.x, Java 17, Web, Security, Data JPA, Validation, Data Redis, Actuator, Micrometer Prometheus, Flyway, PostgreSQL, Mail, Bucket4j + Redis, jjwt, Commons CSV, Testcontainers, JUnit 5. Khóa phiên bản.
- [ ] **Bước 2** - `PlatformApplication`, `application.yml` (đọc mọi cấu hình U01 từ biến môi trường theo `logical-components.md` §4), `application-local.yml`.
- [ ] **Bước 3** - Hạ tầng dùng chung trong `shared/`: global error handler trả problem-details an toàn, filter correlation ID, bộ che dữ liệu nhạy cảm trong log, cấu hình Spring Security mặc định từ chối.
- [ ] **Bước 4** - Khung `/frontend`: Next.js + TypeScript strict + Tailwind, ESLint, Vitest + Testing Library, `src/lib/api` gửi cookie, component UI cơ bản (Button, Input, PasswordField, OtpInput, Dialog, Table, Alert).
- [ ] **Bước 5** - `/infra/docker-compose.yml` và `docker-compose.local.yml`: nginx, frontend, backend, postgres, redis, rabbitmq, mailpit (local), prometheus, loki, promtail, grafana; mạng `edge`/`internal`, giới hạn tài nguyên, healthcheck.
- [ ] **Bước 6** - `.github/workflows/ci.yml`: test backend + frontend, build image tag SHA. Bước deploy qua SSH để dạng khung, chưa bật.

### Nhóm B - Domain và business logic (US-IAM-001…007)

- [ ] **Bước 7** - Domain: `Account`, `AccountStatus` (3 trạng thái), `Role`, `Profile`, `LoginThrottle`, chuẩn hóa email, chuyển trạng thái hợp lệ (BR-U01-70…73).
- [ ] **Bước 8** - `PasswordPolicy` (BR-U01-30, 31; ≤ 72 byte), `PasswordHasher` bcrypt cost cấu hình.
- [ ] **Bước 9** - Port và adapter giả: `AuditPort`, `OutboxPort`, `ScopePort`, `AvatarPort`.
- [ ] **Bước 10** - `OtpService` + `OtpMailHandler`: ghi outbox; handler sinh mã 6 số, lưu băm Redis 10 phút, 5 lượt, gửi SMTP, retry theo backoff (BR-U01-20…27, P9).
- [ ] **Bước 11** - `ActivationService`: F1, F2 (US-IAM-001).
- [ ] **Bước 12** - `TokenService`: JWT HMAC 15 phút; refresh ngẫu nhiên lưu băm, idle 2 giờ, trần 7 ngày, xoay vòng, phát hiện dùng lại (P1).
- [ ] **Bước 13** - `AuthService`: đăng nhập với hash giả cho email không tồn tại, khóa tạm 5 lần/15 phút, refresh, đăng xuất, quên mật khẩu, đổi mật khẩu (F3-F6; US-IAM-002, 003, 006).
- [ ] **Bước 14** - `ProfileService`: F7 (US-IAM-004).
- [ ] **Bước 15** - `AuthorizationService.authorize()` mặc định từ chối, kết hợp role + `ScopePort` (F13; US-IAM-005).
- [ ] **Bước 16** - `AccountAdminService`: tạo, danh sách, đổi role, vô hiệu hóa/mở lại, bảo vệ admin tự hạ và admin cuối (F8, F9, F11, F12; US-IAM-005, 007).
- [ ] **Bước 17** - `AccountImportService`: CSV ≤ 1000 dòng, kiểm từng dòng, commit lô, idempotent theo checksum, cấm tạo ADMIN (F10; US-IAM-007).
- [ ] **Bước 18** - Unit test cho mọi `BR-U01-xx` ở bước 7-17.
- [ ] **Bước 19** - Tóm tắt business logic: `aidlc-docs/construction/u01-account-and-access/code/business-logic-summary.md`.

### Nhóm C - Repository và migration

- [ ] **Bước 20** - Flyway `V20260924_1600__u01_accounts.sql`: `accounts` (email duy nhất, status 3 giá trị, `credential_version`, `failed_login_count`, `locked_until`, `phone_number`, `avatar_ref`), `account_import_batches`, `account_import_rows`; seed một ADMIN ở trạng thái chờ kích hoạt lấy email từ biến môi trường.
- [ ] **Bước 21** - Repository JPA, adapter Redis cho refresh/OTP, Bucket4j proxy manager.
- [ ] **Bước 22** - Integration test với Testcontainers (PostgreSQL, Redis, Mailpit).
- [ ] **Bước 23** - Tóm tắt repository: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 24** - `/contracts/openapi/u01-identity.yaml`: toàn bộ endpoint U01, lỗi problem-details, cookie.
- [ ] **Bước 25** - Controller + DTO + validation: `auth` (login, refresh, logout, activation-requests, activations, password-reset-requests, password-resets), `me` (profile, password), `admin/accounts` (list, create, role, status, imports). `RateLimitFilter`, `JwtAuthFilter`, cookie theo P2.
- [ ] **Bước 26** - Test API: MockMvc cho mọi endpoint, gồm negative test bảo mật (NFR-U01-61).
- [ ] **Bước 27** - Test chịu lỗi RESILIENCY-14: dừng Redis, PostgreSQL, Mailpit; U04 giả trả chậm (bảng §5 `nfr-design-patterns.md`).
- [ ] **Bước 28** - Tóm tắt API: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 29** - Trang auth: `LoginPage`, `ActivationPage`, `PasswordResetPage` với `RequestOtpStep` và `VerifyOtpAndSetPasswordStep` dùng chung.
- [ ] **Bước 30** - `ProfilePage`: `ProfileForm`, `AvatarUploader` (ẩn khi chưa hỗ trợ), `ChangePasswordForm`, `LogoutButton`.
- [ ] **Bước 31** - Admin: `AccountListPage`, `CreateAccountDialog`, `AccountDetailPage` (`RoleChangeDialog`, `StatusToggleDialog`), `ImportAccountsPage`.
- [ ] **Bước 32** - Test frontend: validation form, thông điệp trung tính, hành vi nút theo phản hồi backend.
- [ ] **Bước 33** - Tóm tắt frontend: `code/frontend-summary.md`.

### Nhóm F - Tài liệu và triển khai

- [ ] **Bước 34** - Cấu hình Nginx (định tuyến, header SEC-004, HTTP ở local), Prometheus scrape, datasource Grafana/Loki, luật cảnh báo U01.
- [ ] **Bước 35** - `README.md` ở root: chạy local bằng Docker Compose, biến môi trường, tài khoản admin seed, cách xem OTP trong Mailpit, chạy test.
- [ ] **Bước 36** - Chạy toàn bộ test backend và frontend; ghi kết quả vào `code/test-results.md`.

## 4. Truy vết story

| Story | Bước |
|---|---|
| US-IAM-001 Kích hoạt | 7, 8, 10, 11, 20, 25, 29 |
| US-IAM-002 Đăng nhập/đăng xuất | 12, 13, 25, 29, 30 |
| US-IAM-003 Quên mật khẩu | 10, 13, 25, 29 |
| US-IAM-004 Hồ sơ | 14, 25, 30 |
| US-IAM-005 Role và phạm vi | 15, 16, 25, 31 |
| US-IAM-006 Đổi mật khẩu | 8, 13, 25, 30 |
| US-IAM-007 Vòng đời tài khoản | 16, 17, 20, 25, 31 |

## 5. Ngoài phạm vi lượt này

- Adapter thật của U02, U03, U04; relay outbox sang RabbitMQ; container `worker` riêng (handler OTP tạm chạy trong backend).
- Bật bước deploy SSH trong CI.

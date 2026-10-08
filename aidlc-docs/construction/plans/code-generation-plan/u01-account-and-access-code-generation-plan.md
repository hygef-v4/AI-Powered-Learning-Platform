# U01 Account & Access - Code Generation Plan

**Bản tài liệu 2026-10-08**: UC 01, 02, 03, 04, 05, 06, 07, 58, 59, 60, 61, 62; primary stories: US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U01. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Cập nhật 2026-10-04: việc nền và worker chuyển từ U02 sang U03; thứ tự wave 1 là U03 và U02 song song → U01 → U04. U01 dùng `JobPort` của U03 và `AuditPort` của U02 đều thật; khung dự án nằm ở plan U03.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Loại dự án**: greenfield, workspace root `AI-Powered-Learning-Platform/`. Code **không** nằm trong `aidlc-docs/`.
- **Story**: US-IAM-001…007.
- **Primary UC hiện hành**: UC 01, 02, 03, 04, 05, 06, 07, 58, 59, 60, 61, 62. Supporting flows theo current-srs-contract.md.
- **Thiết kế nguồn**: `construction/u01-account-and-access/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Thứ tự**: wave 1, sau U03 và U02 (hai unit đó code song song trước); U04 code sau U01.

### Khung dự án dùng chung

Khung dự án (Maven, cấu hình, `shared/`, frontend, Docker Compose, CI) là **Bước K1-K6 của plan U03**; U03 code trước U01 nên đã có. Nếu chưa có thì làm các bước đó trước và đánh dấu ở plan U03.

### Phụ thuộc

| Port | Bên khai báo / bên cài | Xử lý lượt này |
|---|---|---|
| `AuditPort` | U02 khai báo và cài | **Dùng U02 thật** (U02 code trước U01) |
| `JobPort`, `JobHandler`, `JobHandlerRegistry` | U03 khai báo và cài | **Dùng U03 thật** (U03 code trước U01): đăng ký `OtpMailHandler` vào queue `jobs.email` |
| `SubjectScopePort`, `ClassScopePort` | U01 khai báo, U04 cài (`C`) | U04 code sau: adapter tạm `NoAssignmentScopeAdapter` trả "không phụ trách môn/lớp nào", nên hành động cần phạm vi bị **từ chối** (fail closed); kiểm hạ role cho qua vì chưa có phân công. U04 Bước 10 cài thật và bỏ adapter tạm |

### U01 cung cấp

| Port | Cho | Ghi chú |
|---|---|---|
| `AuthorizationPort.authorize(actor, action, resourceRef)` | Mọi unit | Thay `FakeAuthorizationPort` của U02 và U03 |
| `AccountLookupPort` | U04, U16 | `findStudents(query, limit ≤ 20)`, `findByEmails(emails)`, `getContact(accountId)` (email, tên hiển thị, role, trạng thái), `countByRoleAndStatus()` (UC 57, U16); không trả mật khẩu, số điện thoại |

### Dữ liệu U01 sở hữu

PostgreSQL `accounts` (U07 thêm cột số dư credit, U16 thêm `email_preferences` bằng migration của mình); cấu hình như `U01_ALLOWED_EMAIL_DOMAINS` là biến triển khai, không có bảng cấu hình; Redis `session:refresh:*`, `otp:*`, `ticket:*` (phiếu xác minh sau OTP đúng), `ratelimit:auth:*`; queue `jobs.email` (khai báo qua topology của U03).

## 2. Cấu trúc thư mục

```
/backend                      Maven, Spring Boot (image dùng cho backend và worker)
  pom.xml
  src/main/java/edu/aiplatform/
    PlatformApplication.java, shared/   (khung, đã có từ plan U03 K1-K3)
    identity/
      api/                    controller, DTO, cookie
      application/            AuthService, ActivationService, OtpService, TokenService,
                              ProfileService, AccountAdminService, AccountImportService,
                              AuthorizationService, AccountLookupService
      domain/                 Account, AccountStatus, Role, PasswordPolicy, LoginThrottle
      infrastructure/         JPA, Redis, JWT, SMTP, Bucket4j
      worker/                 OtpMailHandler (đăng ký với JobHandlerRegistry của U03)
      port/                   AuthorizationPort, AccountLookupPort (U01 cung cấp);
                              SubjectScopePort, ClassScopePort (U01 khai báo)
      adapter/temp/           NoAssignmentScopeAdapter
  src/main/resources/
    application.yml, application-local.yml
    db/migration/identity/         Flyway
  src/test/java/...           unit test
/frontend                     Next.js App Router
  src/app/(auth)/...          login, activate, reset-password
  src/app/profile/...
  src/app/admin/accounts/...
  src/components/ui/, src/lib/api/   (khung, plan U03 K4, gồm PasswordField, OtpInput)
/contracts/openapi/identity.yaml
/infra/nginx/                 route của U01 (Docker Compose, CI là khung, plan U03 K5-K6)
```

## 3. Các bước

### Nhóm A - Khung dự án

Bước 1-6 đã chuyển sang plan U03 (Bước K1-K6, quyết định 2026-10-04): U03 code đầu tiên nên dựng khung. U01 giữ số bước 7 trở đi.

### Nhóm B - Domain và business logic (US-IAM-001…007)

- [ ] **Bước 7** - Domain: `Account`, `AccountStatus` (3 trạng thái), `Role`, `Profile`, `LoginThrottle`, chuẩn hóa email, kiểm tên miền theo `U01_ALLOWED_EMAIL_DOMAINS`, chuyển trạng thái hợp lệ (BR-U01-03, 70…73).
- [ ] **Bước 8** - `PasswordPolicy` (BR-U01-30, 31; ≤ 72 byte), `PasswordHasher` bcrypt cost cấu hình.
- [ ] **Bước 9** - Port: không dùng AvatarPort; khai báo `SubjectScopePort`, `ClassScopePort` + `NoAssignmentScopeAdapter`; cài `AuthorizationPort` (interface đã có từ U02, U03), khai báo `AccountLookupPort`. Dùng `AuditPort` của U02 và `JobPort` của U03.
- [ ] **Bước 10** - `OtpService` gửi việc `OTP_DELIVERY` qua `JobPort.enqueue` (yêu cầu OTP không ghi database nên message gửi ngay) (idempotency key `accountId:purpose:phút`); `OtpMailHandler` chạy ở `worker`, đăng ký với `JobHandlerRegistry` của U03: sinh mã 6 số, lưu băm Redis 10 phút, 5 lượt, gửi qua Mail Port (Brevo SMTP ở demo/production, Mailpit local/test; cấu hình và SMTP key theo shared-infrastructure §8); lỗi thì U03 retry theo backoff, hết lượt thì log ERROR `OTP_DELIVERY_FAILED` (BR-U01-20…27, NFR-U01-30, 31).
- [ ] **Bước 11** - `ActivationService`: F1, F2 hai bước — xác minh OTP cấp `otpTicket`, rồi đặt mật khẩu bằng ticket (BR-U01-28; US-IAM-001); xong thì tự đăng nhập qua `TokenService` (BR-U01-13). `VerificationTicketStore` (Redis `ticket:*`) dùng chung cho F5.
- [ ] **Bước 12** - `TokenService`: JWT HMAC 15 phút; refresh ngẫu nhiên lưu băm, idle 2 giờ, trần 7 ngày, xoay vòng, phát hiện dùng lại; refresh kiểm `credentialVersion` (BR-U01-44…46, P1).
- [ ] **Bước 13** - `AuthService`: đăng nhập với hash giả cho email không tồn tại, khóa tạm 5 lần/15 phút, refresh, đăng xuất (chỉ phiên hiện tại), quên mật khẩu (xác minh OTP rồi mới đặt mật khẩu, BR-U01-28), đổi mật khẩu tăng `credentialVersion` (F3-F6; US-IAM-002, 003, 006).
- [ ] **Bước 14** - ProfileService: F7 (US-IAM-004), chỉ displayName/phoneNumber, từ chối avatar/ID người khác.
- [ ] **Bước 15** - `AuthorizationService` cài `AuthorizationPort`: mặc định từ chối, kết hợp role + `SubjectScopePort`/`ClassScopePort`; không gọi được U04 → từ chối (F13; BR-U01-62, 93; US-IAM-005). Thay `FakeAuthorizationPort` của U02 và U03 bằng bean này.
- [ ] **Bước 16** - `AccountAdminService`: tạo, danh sách, đổi role (tăng `credentialVersion`, BR-U01-63), vô hiệu hóa/mở lại, bảo vệ admin tự hạ và admin cuối, chặn hạ role khi còn phụ trách môn/lớp (F8, F9, F11, F12; BR-U01-64; US-IAM-005, 007).
- [ ] **Bước 17** - `AccountImportService`: CSV ≤ 1000 dòng, kiểm từng dòng, trả kết quả không lưu; xác nhận thì kiểm lại và tạo dòng hợp lệ trong một transaction; audit kèm checksum; cấm tạo ADMIN (F10; BR-U01-80…83; US-IAM-007).
- [ ] **Bước 18** - `AccountLookupService` cài `AccountLookupPort` cho U04, U16 (chỉ trả email, tên hiển thị, role, trạng thái; `countByRoleAndStatus()` cho UC 57).
- [ ] **Bước 19** - Audit qua `AuditPort` của U02 mọi sự kiện BR-U01-90; không đưa mật khẩu, OTP, token, số điện thoại vào payload.
- [ ] **Bước 20** - Unit test cho mọi `BR-U01-xx` ở bước 7-19 (mock port của U02, U03, U04).
- [ ] **Bước 21** - Tóm tắt business logic: `aidlc-docs/construction/u01-account-and-access/code/business-logic-summary.md`.

### Nhóm C - Repository và migration

- [ ] **Bước 22** - Flyway `V20260925_0930__u01_accounts.sql` (sau `V20260925_0900` của U02): `accounts` (email duy nhất, status 3 giá trị `PENDING`/`ACTIVE`/`DISABLED`, `credential_version`, `failed_login_count`, `locked_until`, `phone_number`); seed một ADMIN ở trạng thái `PENDING` lấy email từ biến môi trường; thêm FK `audit_logs.actor_id` → `accounts` (bảng `audit_logs` của U02 đã có).
- [ ] **Bước 23** - Repository JPA, adapter Redis cho refresh/OTP, Bucket4j proxy manager.
- [ ] **Bước 24** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers (PostgreSQL, Redis, RabbitMQ, Mailpit): kích hoạt đầu-cuối qua message `OTP_DELIVERY` + worker của U03, đăng nhập, khóa tạm, refresh dùng lại, import lặp lại cùng file.
- [ ] **Bước 25** - Tóm tắt repository: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 26** - `/contracts/openapi/identity.yaml`: toàn bộ endpoint U01, lỗi problem-details, cookie.
- [ ] **Bước 27** - Controller + DTO + validation: `auth` (login, refresh, logout, activation-requests, activation-otp-verifications, activations, password-reset-requests, password-reset-otp-verifications, password-resets), `me` (profile, password), admin/accounts (list, get detail, patch displayName/phoneNumber với version, create, role, status, imports). `RateLimitFilter`, `JwtAuthFilter`, cookie theo P2.
- [ ] **Bước 28** - Test API: MockMvc cho mọi endpoint, gồm negative test bảo mật (NFR-U01-61).
- [ ] **Bước 29** - Tóm tắt API: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 30** - Trang auth: `LoginPage` (điều hướng theo role sau đăng nhập tới My Classes, Assigned Classes hoặc Statistic (Admin Sidebar), BR-U01-48), `ActivationPage`, `PasswordResetPage` với `RequestOtpStep`, `VerifyOtpStep`, `SetNewPasswordStep` (ô mật khẩu chỉ mở sau khi OTP đúng, BR-U01-28) dùng chung; kích hoạt xong vào thẳng menu theo role, đặt lại mật khẩu xong về Login.
- [ ] **Bước 31** - `ProfilePage`: ProfileForm (displayName/phoneNumber), ChangePasswordDialog (popup trên Profile); `SignOutDialog` (popup trên Navigation).
- [ ] **Bước 32** - Admin: `AccountListPage`, `CreateAccountDialog`, `AccountDetailPage` (`RoleChangeDialog`, `StatusToggleDialog`), `ImportAccountsPanel` (trên `AccountListPage`).
- [ ] **Bước 33** - Test frontend: validation form, thông điệp trung tính, hành vi nút theo phản hồi backend.
- [ ] **Bước 34** - Tóm tắt frontend: `code/frontend-summary.md`.

### Nhóm F - Tài liệu và triển khai

- [ ] **Bước 35** - Nginx (khung ở U03 K5): thêm route `auth`, `me`, `admin/accounts`, header SEC-004 (CSP theo `shared-infrastructure.md`).
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
| Contract cho unit khác (`AuthorizationPort` thay adapter giả của U02, U03; `AccountLookupPort`) | 9, 15, 18 |
| UC 57 (số tài khoản cho U16) | 18 |

## 5. Ngoài phạm vi lượt này

- Cài thật `SubjectScopePort`/`ClassScopePort` (U04 Bước 10); khi đó bỏ adapter tạm của U01.
- Code của U03 (khung, việc nền, worker) và U02 (audit); U01 chỉ đăng ký handler `OTP_DELIVERY`.
- Bật bước deploy SSH trong CI.
- Test chịu lỗi (RESILIENCY-14) ngoài phạm vi đồ án.

## 6. Revision implementation scope - 2026-10-08
- [ ] Áp R1–R5 theo current-srs-contract.md: Admin được phân công R2/R4, không bypass scope và không kế thừa Student; denial matrix cho assignment có/không.
- [ ] Account Detail UC 60 và Update Account Information UC 61 với version/audit; không trả hash/OTP/token, không sửa email/mật khẩu qua Admin.
- [ ] Bỏ cập nhật avatar khỏi DTO/UI/call site U01; rà cột/port code cũ riêng, không xóa dữ liệu lịch sử tự động.

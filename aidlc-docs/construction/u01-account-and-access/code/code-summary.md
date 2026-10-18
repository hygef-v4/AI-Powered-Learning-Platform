# U01 Account & Access — code summary

Code viết ngày 2026-10-17 theo [plan](../../plans/code-generation-plan/u01-account-and-access-code-generation-plan.md) và kế hoạch làm việc tạm của người code. Đã chạy thử end-to-end với stack Docker local (PostgreSQL, Redis, RabbitMQ, Mailpit, Nginx) ngày 2026-10-17; integration test tự động do tester viết. Chưa commit.

## Business logic

| Luồng | Class chính |
|---|---|
| F1, F2 Kích hoạt | `OtpService` (yêu cầu trung tính, rate limit, gửi việc `OTP_DELIVERY` chỉ chứa ID; xác minh → `otpTicket`), `OtpMailHandler` (worker sinh mã, lưu băm, gửi mail, thử lại qua U03), `ActivationService` (đặt mật khẩu, `ACTIVE`, audit, tự đăng nhập) |
| F3, F4 Đăng nhập, đăng xuất, refresh | `AuthService` (một lỗi trung tính, bcrypt giả khi email lạ, khóa 15 phút sau 5 lần sai, xoay refresh, phát hiện dùng lại), `HomePaths` (F15) |
| F5 Đặt lại mật khẩu | `OtpService`, `PasswordResetService` (xóa mọi phiên trước và sau commit, không tự đăng nhập) |
| F6 Đổi mật khẩu | `PasswordChangeService` (đúng mật khẩu hiện tại, giữ phiên hiện tại, xóa phiên khác) |
| F7 Hồ sơ | `ProfileService` (xem, sửa tên/SĐT của chính mình, audit tên trường) |
| F8–F12, F14 Quản trị | `AccountAdminService` (danh sách, tạo `PENDING`, chi tiết, sửa có `version`, đổi role có kiểm phân công, chặn tự thao tác, khóa Admin cuối, vô hiệu hóa/mở lại; vô hiệu hóa gọi `ISubjectScope.releaseManagerAssignments` trong cùng transaction), `AccountImportService` (CSV kiểm rồi xác nhận) |
| F13 Phân quyền | `AuthorizationService` (cài `IAuthorization`, mặc định từ chối, role đọc từ DB, phạm vi qua `ISubjectScope`/`IClassScope`), `AccessPolicy` + `contract/Actions` |
| Phiên mỗi request | `SessionAuthority` + `web/JwtAuthFilter` (JWT, `sid` còn trên Redis, tài khoản `ACTIVE`, principal là `Actor`) |
| Contract cho unit khác | `AccountLookupService` (cài `IAccountLookup`, tối đa 20 kết quả), `SmtpMailAdapter` (cài `IMail`) |
| Khởi tạo | `BootstrapAdminInitializer` (Admin đầu tiên `PENDING`, không mật khẩu mặc định) |

Domain: `Account`, `AccountSearch`, `EmailPolicy`, `PasswordPolicy`, `ProfilePolicy`, `OtpPurpose`. Hạ tầng: `JwtService` (HS256, khóa ≥ 32 byte), `SessionStore`, `OtpChallengeStore`, `VerificationTicketStore`, `RequestThrottle` (Bucket4j trên Lettuce), `Hashing`, `IdentityConfig` (bcrypt), `IdentityProperties` (`platform.identity`).

## API

| Endpoint | UC | Ghi chú |
|---|---|---|
| `POST /api/v1/auth/login`, `/refresh`, `/logout` | 02, 03 | Public, không CSRF; cookie `access_token` (`Path=/api`, Lax), `refresh_token` (`Path=/api/v1/auth/refresh`, Strict) |
| `POST /api/v1/auth/activation/{request,verify,complete}` | 01 | `request` luôn `202`; `complete` đặt cookie |
| `POST /api/v1/auth/password-reset/{request,verify,complete}` | 04 | `complete` xóa cookie, không đăng nhập |
| `GET`, `PATCH /api/v1/me/profile`; `POST /api/v1/me/password` | 05–07 | Field lạ → 400 `INVALID_REQUEST` |
| `GET`, `POST /api/v1/admin/accounts`; `GET`, `PATCH /{id}`; `PUT /{id}/role`; `PUT /{id}/status` | 08–12 | `version` → 409 `VERSION_CONFLICT`; `ROLE_CHANGE_BLOCKED` kèm `managedSubjects`, `taughtClasses` |
| `POST /api/v1/admin/accounts/imports:validate`, `imports:commit` | 09 (phụ) | Multipart `file`, `.csv` ≤ 1 MB, ≤ 1000 dòng |

Lỗi: Problem Detail có `code`; `PASSWORD_POLICY` kèm `violations`; Redis/DB không khả dụng → 503 `SYSTEM_BUSY`.

## Migration

`db/migration/identity/V20260925_0930__create_accounts.sql`: bảng `accounts` (email chuẩn hóa, unique; role/status check; `ACTIVE` phải có mật khẩu; `email_preferences` cho U14; `version`), index `(role, status)`, FK `system_settings.updated_by`. `V20260925_0931__add_audit_actor_fk.sql`: FK `audit_logs.actor_id` → `accounts`.

## Frontend

- Route: `(auth)/login`, `(auth)/activate`, `(auth)/reset-password`, `/`, `/post-login`, `/profile`, `/admin/accounts`, `/admin/accounts/[id]`; `app/admin/layout.tsx` bọc cả Settings của U03 bằng `AuthenticatedLayout` (chỉ ADMIN).
- `features/identity`: `LoginPage`/`LoginForm`, `otp/OtpFlow` (email → OTP 10 phút, gửi lại sau 60 giây → mật khẩu), `ActivationPage`, `PasswordResetPage`, `profile/*`, `admin/*`, `shell/*` (`AppShell`, `AccountMenu` vẽ `accountMenuEntries`/`headerWidgets`, `SignOutDialog`, `RoleGuard`), `session.tsx`, `components/*` (AuthLayout, Stepper, OtpInput, PasswordField, PasswordChecklist).
- `shared/api/client.ts`: header `X-XSRF-TOKEN` cho request đổi trạng thái, 401 thì refresh một lần, `setUnauthenticatedHandler`.

## Cấu hình

`U01_ALLOWED_EMAIL_DOMAINS`, `U01_JWT_SECRET` (bắt buộc, ≥ 32 byte; thiếu thì backend và worker không khởi động), `U01_BOOTSTRAP_ADMIN_EMAIL`/`_NAME`, `U01_COOKIE_SECURE` (`false` chỉ ở local), các ngưỡng `U01_*`, `SMTP_FROM`. `server.forward-headers-strategy: native` để lấy IP client sau Nginx.

## Unit test

- Backend 208 test toàn repo (U01: domain, JWT, session store, OTP store/ticket, đăng nhập/khóa tạm, phân quyền, kích hoạt/đặt lại, hồ sơ/đổi mật khẩu, quản trị, nhập CSV, lookup và bảng quyền, chuỗi bảo mật web với MockMvc: 401/503/CSRF/cookie/field lạ/đường dẫn `imports:validate`). `ArchitectureTest` xanh.
- Frontend 50 test toàn repo (U01: client CSRF/refresh, LoginForm, RoleGuard, OtpInput, OtpFlow, passwordRules, ProfileForm, PasswordChangeDialog, màn quản trị). `next build` xanh. Màn đăng nhập, khôi phục mật khẩu, hồ sơ, danh sách và chi tiết tài khoản đã xem trên trình duyệt với API giả.

## Chạy thử end-to-end (2026-10-17, stack Docker local)

- Kịch bản T3 của kế hoạch chạy bằng script qua Nginx với Mailpit thật: 52/52 kiểm tra đạt (kích hoạt Admin đầu tiên qua OTP trong Mailpit, phản hồi OTP trung tính, nhập CSV và thêm tài khoản không gửi email, OTP sai 5 lần, khóa tạm và tự mở, đổi/đặt lại mật khẩu thu hồi phiên, hồ sơ và field lạ, CSRF, phân quyền gồm Settings của U03, đổi role, version 409, bảo vệ Admin, vô hiệu hóa/mở lại, xoay và dùng lại refresh token). T3-14 Redis tắt → 503 rồi tự hồi phục; T3-15 log không chứa mật khẩu, OTP, token, số điện thoại. Lưu và đăng nhập trên trình duyệt thật đều chạy.
- Lỗi tìm thấy và đã sửa: `SessionManagementFilter` xoay CSRF token sau mỗi request có JWT, làm request ghi kế tiếp bị 403 → `csrf.sessionAuthenticationStrategy(NullAuthenticatedSessionStrategy)` + test hồi quy; handler `HttpMessageNotReadableException` của U01 bị handler mặc định của Spring chặn → `@Order(HIGHEST_PRECEDENCE)` (chỉ áp dụng cho controller U01) + test.
- Hạ tầng (không thuộc U01, cần báo người giữ `infra/`): healthcheck RabbitMQ timeout 5 giây quá ngắn trên máy Windows/Docker Desktop (`rabbitmq-diagnostics` mất hơn 5 giây) nên compose báo unhealthy; máy có sẵn PostgreSQL ở 5432 hoặc tiến trình dùng 8080 thì cổng mở trong `docker-compose.local.yml` bị trùng. Lần chạy này dùng file override tạm trong `.tmp/`.

## Khác với thiết kế

- Redis lỗi → 503 cho mọi request có cookie (theo BR-U01-44; BR-U01-95, NFR-U01-40, P1 cần sửa cho khớp, Q04).
- `AuthorizationService` không tự ghi `ACCESS_DENIED`; unit gọi tự ghi (như U03) để tránh ghi hai lần.
- Field lạ trong body bị chặn bằng `StrictRequest` (`@JsonAnySetter`); `@JsonIgnoreProperties(ignoreUnknown = false)` không có tác dụng dưới cấu hình mặc định của Spring Boot.
- Hồ sơ cá nhân không có `version` (Q18); New Account không có SĐT (Q08); không có panel lịch sử ở Account Detail (Q09).
- CSV thêm lý do `INVALID_DISPLAY_NAME`; bước kiểm trả `VALID`, bước xác nhận trả `CREATED`.
- Mọi tự đổi role của Admin đều bị chặn (`SELF_CHANGE_BLOCKED`), vì người gọi luôn là ADMIN.
- `GlobalExceptionHandler` chỉ thêm 503 cho Redis/DB; lỗi validate và lỗi khác dùng Problem Detail mặc định của Spring Boot (không lộ stack trace), không thêm handler bắt mọi lỗi để tránh đổi hành vi của unit khác.
- Admin tạm vào `/admin/accounts` tới khi U15 có Admin Dashboard; `/classes`, `/manager` thuộc U05/U04, hiện 404.

## Còn lại

Chạy thử end-to-end với Docker local (kịch bản T3 trong kế hoạch tạm); tester viết integration test; báo nhóm các thay đổi file dùng chung (pom, `ArchitectureTest`, `SecurityConfig`, `GlobalExceptionHandler`, `client.ts`, `app/admin/layout.tsx`, compose).

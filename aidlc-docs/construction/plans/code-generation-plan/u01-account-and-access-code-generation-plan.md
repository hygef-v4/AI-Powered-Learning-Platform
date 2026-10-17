# U01 Account & Access — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U01 Account & Access thuộc Iter 1, người code: Người 1; phạm vi UC 01–12 giữ như U01 cũ, chỉ đổi liên kết sang các unit mới. 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Thiết kế: [business-logic-model](../../u01-account-and-access/functional-design/business-logic-model.md), [business-rules](../../u01-account-and-access/functional-design/business-rules.md), [domain-entities](../../u01-account-and-access/functional-design/domain-entities.md), [frontend-components](../../u01-account-and-access/functional-design/frontend-components.md), [NFR](../../u01-account-and-access/nfr-requirements/nfr-requirements.md), [NFR design](../../u01-account-and-access/nfr-design/nfr-design-patterns.md), [infrastructure](../../u01-account-and-access/infrastructure-design/infrastructure-design.md). Phụ thuộc và thứ tự: [unit-of-work](../../../inception/application-design/unit-of-work.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/identity/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../identity/contract/` | interface `IAccountLookup`, `IAuthorization`, `IClassScope`, `IMail`, `ISubjectScope`; kiểu dữ liệu `AccountContact`, `AccountStatus`, `Actor`, `AuthorizationDecision`, `MailMessage`, `ResourceRef`, `Role`, `ScopeKind` |
| `backend/.../identity/contract/stub/IdentityContractStubs` | Bản tạm trả dữ liệu mẫu cho `IAuthorization`, `IAccountLookup`, `IMail`, `ISubjectScope`, `IClassScope` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/identity/slots.ts` | Slot đã khai báo: `accountMenuEntries`, `headerWidgets`, `navEntries` (menu bên) |
| `frontend/src/shared/layout/` | Khung `AppShell` (thanh trên cùng, menu bên, popup Đăng xuất) đã có, làm cùng U03 theo design |
| `backend/.../shared/config/SecurityConfig` | Cấu hình nền: không phiên server, chỉ mở `/health`; U01 thêm filter JWT, cookie, CSRF |

## Checklist

- [ ] **Bước 1 — Migration:** tạo `db/migration/identity/V20260925_0930__create_accounts.sql` (chạy sau migration `audit_logs` của U02): bảng `accounts` với các cột theo domain-entities mục 2–5, `email_preferences` (U14 ghi), unique `school_email`, index `(role, status)`, thêm FK `audit_logs.actor_id` và `system_settings.updated_by` → `accounts`; seed Admin đầu tiên từ biến triển khai, không mật khẩu mặc định.
- [ ] **Bước 2 — Domain:** `Account` (aggregate), `Role`, `AccountStatus`, `PasswordCredential`, `Profile`, `LoginThrottle`, `PasswordPolicy` (BR-U01-30, 31), `EmailNormalizer` (BR-U01-02, 03); `IAccountRepository` chỉ dùng trong package `identity`.
- [ ] **Bước 3 — Lưu tạm trên Redis:** `SessionStore` (phiên `sid`, refresh token băm, danh sách phiên theo tài khoản; idle 2 giờ, tối đa 7 ngày, phát hiện dùng lại, xóa mọi phiên hoặc mọi phiên trừ phiên hiện tại), `OtpChallengeStore`, `VerificationTicketStore`, `RequestThrottleStore`; tên khóa theo infrastructure-design mục 3.
- [ ] **Bước 4 — Service:** `AuthService` (F3 đăng nhập, F4 đăng xuất, refresh, F15 `homePath`), `ActivationService` (F1, F2), `PasswordResetService` (F5), `PasswordChangeService` (F6), `ProfileService` (F7), `AccountAdminService` (F8, F9, F11, F12, F14), `AccountImportService` (F10), `AuthorizationService` (F13); mỗi thay đổi ghi audit qua `IAudit` trong cùng transaction.
- [ ] **Bước 5 — Worker:** `OtpMailHandler` nhận việc `OTP_DELIVERY` từ `IJob` của U03, sinh OTP 6 số, lưu băm, gửi qua `IMail`; mã rõ không đi qua queue, DB hay log.
- [ ] **Bước 6 — Bảo mật web:** `JwtAuthFilter` (chữ ký, hạn 15 phút, phiên `sid` còn, trạng thái `ACTIVE`), `RateLimitFilter` theo email và client, cookie HttpOnly/Secure/SameSite, chống CSRF cho route đổi trạng thái, bcrypt.
- [ ] **Bước 7 — API:** controller theo bảng API ở domain-entities mục 14, thêm các endpoint kích hoạt, đặt lại mật khẩu, refresh, đăng xuất; lỗi trả dạng an toàn và phản hồi trung tính theo BR-U01-12, 40.
- [ ] **Bước 8 — Interface:** `AuthorizationService` cài `IAuthorization` (F13), `AccountLookupService` cài `IAccountLookup`, `SmtpMailAdapter` cài `IMail` (timeout NFR-U01-44); tới khi U04, U05 xong, bản tạm trong khung trả `true` cho `ISubjectScope`, `IClassScope`.
- [ ] **Bước 9 — Frontend:** các màn và component theo frontend-components: `LoginPage`, `ActivationPage`, `PasswordResetPage`, `PostLoginRedirect`, nối `AppShell` có sẵn với phiên thật (thẻ hồ sơ, `SignOutDialog` gọi `logout`, menu bên vẽ `navEntries`, lối vào `accountMenuEntries`, vùng `headerWidgets`), `RoleGuard`, `ProfilePage`, `PasswordChangeDialog`, `AccountListPage`, `ImportAccountsPanel`, `NewAccountDialog`, `AccountDetailPage`, `RoleChangeDialog`, `RoleChangeRejectedDialog`, `StatusToggleDialog`; đăng ký `NavEntry` mục Tài khoản.
- [ ] **Bước 10 — Unit test (người code viết):** chính sách mật khẩu, khóa tạm sau 5 lần sai, OTP hết hạn/sai 5 lần/mã mới thay mã cũ, phản hồi trung tính, thu hồi phiên khi đổi mật khẩu/role/trạng thái, chặn đổi role (BR-U01-64, 65, 66), kiểm CSV, `authorize` cho từng role; test form frontend. Integration test do tester viết, không thuộc bước này.
- [ ] **Bước 11 — Cấu hình:** `U01_ALLOWED_EMAIL_DOMAINS`, khóa ký JWT, `SMTP_*`, Redis; cập nhật hướng dẫn chạy local.
- [ ] **Bước 12 — Review:** đối chiếu UC, story, BR với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Tạo tài khoản và nhập CSV không gửi email; kích hoạt với OTP đúng, sai, hết hạn, vượt tần suất; đặt lại mật khẩu không lộ tài khoản có tồn tại hay không.
- Đăng nhập đúng, sai, bị khóa tạm; đăng xuất; phiên bị thu hồi sau đổi mật khẩu, đổi role, vô hiệu hóa.
- Sửa hồ sơ của chính mình; sửa hồ sơ người khác bị từ chối; Admin bị từ chối ở chức năng học vụ; route đổi trạng thái thiếu CSRF bị từ chối.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.

# U01 Account & Access — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u01-account-and-access` không quyết định chức năng hiện hành.

U01 nằm trong **Web Shell** (xác thực, hồ sơ, khung sau đăng nhập) và **Admin Console** (quản trị tài khoản). Ẩn/hiện UI chỉ hỗ trợ trải nghiệm; quyền thật do backend quyết định. Tên API là tên nghiệp vụ, đường dẫn cuối cùng chốt ở OpenAPI.

## 1. Màn hình theo screen flow

| Màn | Component | UC |
|---|---|---|
| User Login | `LoginPage` | 02 |
| Activate Account | `ActivationPage` | 01 |
| Password Reset | `PasswordResetPage` | 04 |
| Post-Login (khung sau đăng nhập) | `PostLoginRedirect`, `AccountMenu`, `SignOutDialog` | 01, 02, 03 |
| User Profile | `ProfilePage` | 06, 07 |
| Password Change (popup) | `PasswordChangeDialog` | 05 |
| Account List | `AccountListPage`, `ImportAccountsPanel` | 08, luồng phụ của 09 |
| New Account (popup) | `NewAccountDialog` | 09 |
| Account Detail | `AccountDetailPage` | 10, 11, 12 |

Class Dashboard, Manager Dashboard (U04) và Admin Dashboard (U16) là đích của Post-Login, không thuộc U01.

## 2. Cây component

```
app/(auth)/                       (Web Shell, chưa đăng nhập)
  login/            LoginPage             màn User Login
    LoginForm
    ActivationLink                -> ActivationPage
    ForgotPasswordLink            -> PasswordResetPage
  activate/         ActivationPage        màn Activate Account
    RequestOtpStep
    VerifyOtpStep                 bước 2: chỉ nhập OTP
    SetNewPasswordStep            bước 3: chỉ mở sau khi OTP đúng
  reset-password/   PasswordResetPage     màn Password Reset
    RequestOtpStep                (dùng chung)
    VerifyOtpStep                 (dùng chung)
    SetNewPasswordStep            (dùng chung)
app/post-login/     PostLoginRedirect     chuyển tới dashboard theo role (BR-U01-48)
components/shell/                 (khung sau đăng nhập)
  AccountMenu                     tên, role; lối vào User Profile, Logout
  NotificationBell                (U16) chuông và popup thông báo cho mọi vai trò
  SignOutDialog                   popup xác nhận Logout
  RoleGuard                       chặn route theo role
app/profile/        ProfilePage           màn User Profile
  ProfileForm
  PasswordChangeDialog            popup Password Change (nút "Đổi mật khẩu")
app/admin/accounts/               (Admin Console, mở từ Admin Dashboard)
  AccountListPage                 màn Account List
    AccountFilters
    AccountTable
    ImportAccountsPanel           nút "Nhập CSV", luồng phụ của UC 09, hiện ngay trên trang
      CsvUploadStep
      ImportPreviewTable
      ImportConfirmStep
    NewAccountDialog              popup New Account (nút "Thêm tài khoản")
  [id]/             AccountDetailPage     màn Account Detail
    AccountSummary
    AccountEditForm
    RoleChangeDialog
    StatusToggleDialog
components/ui/                    (khung chung, U03)
  PasswordField                   hiển thị điều kiện chính sách
  OtpInput                        6 ô số
```

## 3. Web Shell

### LoginForm (User Login)

| Mục | Nội dung |
|---|---|
| State | `email`, `password`, `submitting`, `errorMessage` |
| Validation phía client | Email đúng định dạng, mật khẩu không rỗng |
| API | `authenticate` → `SessionInfo { accountId, displayName, role, homePath }` |
| Hành vi lỗi | Mọi thất bại hiện **một** câu: "Email hoặc mật khẩu không đúng, hoặc tài khoản chưa sẵn sàng." Không phân biệt khóa tạm, chưa kích hoạt hay không tồn tại |
| Thành công | Chuyển tới `homePath` (BR-U01-48): Student, Teacher → `/classes` (Class Dashboard); Subject Manager → `/manager` (Manager Dashboard); Admin → `/admin` (Admin Dashboard) |

### ActivationPage (Activate Account) / PasswordResetPage (Password Reset)

Hai trang dùng chung ba bước `RequestOtpStep` → `VerifyOtpStep` → `SetNewPasswordStep`, khác `purpose`; ô mật khẩu chỉ hiện sau khi OTP được xác minh (BR-U01-28).

**RequestOtpStep**

| Mục | Nội dung |
|---|---|
| State | `email`, `submitting`, `cooldownSeconds` |
| API | `requestActivation` hoặc `requestPasswordReset` |
| Sau khi gửi | Luôn hiện: "Nếu email hợp lệ, mã gồm 6 chữ số sẽ được gửi. Mã có hiệu lực 10 phút. Không nhận được mã, hãy liên hệ quản trị." Khóa nút "Gửi lại" trong thời gian chờ |

**VerifyOtpStep**

| Mục | Nội dung |
|---|---|
| State | `otpCode`, `errorMessage` |
| Validation phía client | OTP đúng 6 chữ số |
| API | `verifyActivationOtp` hoặc `verifyPasswordResetOtp` → `otpTicket` |
| Lỗi | Mã sai hoặc hết hạn: "Mã không hợp lệ hoặc đã hết hạn." Ở lại bước này |
| Thành công | Giữ `otpTicket` trong bộ nhớ trang, chuyển sang `SetNewPasswordStep` |

**SetNewPasswordStep**

| Mục | Nội dung |
|---|---|
| State | `newPassword`, `confirmPassword`, `errorMessage` |
| Validation phía client | Mật khẩu ≥ 8 ký tự có chữ và số; hai ô khớp |
| API | `activateAccount(otpTicket, newPassword)` hoặc `resetPassword(otpTicket, newPassword)` |
| Lỗi | Mật khẩu bị backend từ chối: hiện lý do chính sách, nhập lại; ticket hết hạn: quay về `RequestOtpStep` |
| Thành công | Kích hoạt: backend đã tạo phiên, chuyển thẳng tới dashboard theo role như `LoginPage` (BR-U01-13, 48). Đặt lại mật khẩu: chuyển về User Login kèm thông báo |

### Khung sau đăng nhập (Post-Login)

| Component | Hành vi |
|---|---|
| `PostLoginRedirect` | Sau đăng nhập, kích hoạt, hoặc khi mở `/`: lấy `homePath` từ phiên (hoặc `GET /me/profile` khi tải lại trang) rồi chuyển tới dashboard |
| `AccountMenu` | Hiện tên và role; lối vào User Profile và Logout. Subject Manager có thêm nút sang Class Dashboard |
| `SignOutDialog` | Xác nhận rồi gọi `logout`, xóa trạng thái phía client, về User Login (UC 03) |
| `RoleGuard` | `/admin/*` chỉ `ADMIN`; `/manager/*` chỉ `SUBJECT_MANAGER`; `/classes/*` chỉ `STUDENT`, `TEACHER`, `SUBJECT_MANAGER`. Sai role thì chuyển về `homePath`. Admin không vào Manager Dashboard hay Class Dashboard (BR-U01-48). Backend vẫn tự kiểm quyền |

### ProfilePage (User Profile)

| Component | State | API | Ghi chú |
|---|---|---|---|
| `ProfileForm` | `displayName`, `phoneNumber`, `dirty`, `saving` | `getProfile`, `updateProfile` | Email, role, trạng thái chỉ đọc (UC 06); chỉ sửa tên, số điện thoại (UC 07). Nút "Đổi mật khẩu" mở popup Password Change |

### PasswordChangeDialog (popup Password Change)

| Mục | Nội dung |
|---|---|
| State | `currentPassword`, `newPassword`, `confirmPassword`, `errorMessage` |
| Validation phía client | Mật khẩu mới ≥ 8 ký tự có chữ và số; hai ô khớp |
| API | `changePassword` |
| Thành công | Báo "Các thiết bị khác đã bị đăng xuất", đóng popup |

## 4. Admin Console

### AccountListPage (Account List)

| Mục | Nội dung |
|---|---|
| State | `filters { email, role, status }`, `page`, `items`, `loading` |
| API | `listAccounts` |
| Cột | Email, tên hiển thị, role, trạng thái, ngày tạo |
| Hành động | Bấm một dòng mở Account Detail; nút "Thêm tài khoản" mở popup New Account; nút "Nhập CSV" mở `ImportAccountsPanel` ngay trên trang |

### NewAccountDialog (popup New Account)

| Mục | Nội dung |
|---|---|
| State | `email`, `displayName`, `role` |
| Validation | Email đúng định dạng và domain trường; tên không rỗng |
| API | `createAccount` |
| Sau khi tạo | Hiện "Tài khoản đã tạo ở trạng thái chờ kích hoạt. Người dùng tự kích hoạt ở lần đăng nhập đầu." rồi đóng popup và tải lại Account List. Không có nút gửi mail |

### AccountDetailPage (Account Detail)

| Component | API | Hành vi |
|---|---|---|
| `AccountSummary` | `getAccount` | Email, tên, số điện thoại, role, trạng thái, ngày tạo (UC 10) |
| `AccountEditForm` | `updateAccount` (PATCH kèm `version`) | Sửa tên, số điện thoại; email chỉ đọc. `409` thì báo dữ liệu đã đổi và tải lại (UC 11) |
| `RoleChangeDialog` | `assignRole` | Cảnh báo người dùng sẽ bị đăng xuất. Backend từ chối vì role mới không giữ được phân công môn/lớp thì hiện danh sách môn/lớp. Nút bị tắt với chính mình khi hạ quyền (UC 11) |
| `StatusToggleDialog` | `changeAccountStatus` | "Vô hiệu hóa" hoặc "Mở lại". Tắt với chính mình. Backend từ chối admin cuối cùng thì hiện lý do (UC 12) |

### ImportAccountsPanel (luồng phụ của UC 09)

Nút "Nhập CSV" trên `AccountListPage` mở vùng nhập ngay trong trang; danh sách tài khoản tải lại sau khi xác nhận.

| Bước | State | API | Hành vi |
|---|---|---|---|
| `CsvUploadStep` | `file` | `validateAccountImport` | Chỉ nhận `.csv`; có nút tải file mẫu `email,display_name,role` |
| `ImportPreviewTable` | `rows[]` | - | Mỗi dòng: kết quả, lý do lỗi dễ hiểu. Đếm số dòng hợp lệ và lỗi |
| `ImportConfirmStep` | `file` (gửi lại) | `commitAccountImport` | Backend kiểm lại rồi chỉ tạo dòng hợp lệ. Thông báo không có email nào được gửi |

## 5. Nguyên tắc chung

- Mật khẩu, OTP không lưu vào local storage, không đưa lên URL.
- Thông báo lỗi xác thực không bao giờ tiết lộ tài khoản có tồn tại hay trạng thái của nó.
- Route có guard theo role để trải nghiệm tốt, nhưng mọi API vẫn tự kiểm quyền phía server.
- Admin chỉ thấy Admin Dashboard và các màn quản trị; không có lối vào Manager Dashboard hay Class Dashboard.
- Profile không có tải ảnh đại diện.
- Nhãn tiếng Việt, thiết kế desktop-first.

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

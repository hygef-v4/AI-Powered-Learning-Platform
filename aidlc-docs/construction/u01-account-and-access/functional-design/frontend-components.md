# U01 Account & Access - Frontend Components

U01 nằm trong **Web Shell** (xác thực, hồ sơ) và **Admin Console** (quản trị tài khoản). Ẩn/hiện UI chỉ hỗ trợ trải nghiệm; quyền thật do backend quyết định. Tên API là tên nghiệp vụ, đường dẫn cuối cùng chốt ở OpenAPI.

## 1. Cây component

```
web-shell/auth/
  LoginPage
    LoginForm
    ActivationLink          -> ActivationPage
    ForgotPasswordLink      -> PasswordResetPage
  ActivationPage
    RequestOtpStep
    VerifyOtpAndSetPasswordStep
  PasswordResetPage
    RequestOtpStep          (dùng chung)
    VerifyOtpAndSetPasswordStep (dùng chung)
web-shell/profile/
  ProfilePage
    ProfileForm
    AvatarUploader          (tắt khi AvatarPort chưa sẵn sàng)
    ChangePasswordForm
    LogoutButton
admin-console/accounts/
  AccountListPage
    AccountFilters
    AccountTable
  AccountDetailPage
    AccountSummary
    RoleChangeDialog
    StatusToggleDialog
  CreateAccountDialog
  ImportAccountsPage
    CsvUploadStep
    ImportPreviewTable
    ImportConfirmStep
shared/
  PasswordField             (hiển thị điều kiện chính sách)
  OtpInput                  (6 ô số)
  NeutralMessage
```

## 2. Web Shell

### LoginForm

| Mục | Nội dung |
|---|---|
| State | `email`, `password`, `submitting`, `errorMessage` |
| Validation phía client | Email đúng định dạng, mật khẩu không rỗng |
| API | `authenticate` |
| Hành vi lỗi | Mọi thất bại hiện **một** câu: "Email hoặc mật khẩu không đúng, hoặc tài khoản chưa sẵn sàng." Không phân biệt khóa tạm, chưa kích hoạt hay không tồn tại |
| Thành công | Điều hướng theo role |

### ActivationPage / PasswordResetPage

Hai trang dùng chung `RequestOtpStep` và `VerifyOtpAndSetPasswordStep`, khác `purpose`.

**RequestOtpStep**

| Mục | Nội dung |
|---|---|
| State | `email`, `submitting`, `cooldownSeconds` |
| API | `requestActivation` hoặc `requestPasswordReset` |
| Sau khi gửi | Luôn hiện: "Nếu email hợp lệ, mã gồm 6 chữ số sẽ được gửi. Mã có hiệu lực 10 phút. Không nhận được mã, hãy liên hệ quản trị." Khóa nút "Gửi lại" trong thời gian chờ |

**VerifyOtpAndSetPasswordStep**

| Mục | Nội dung |
|---|---|
| State | `otpCode`, `newPassword`, `confirmPassword`, `errorMessage` |
| Validation phía client | OTP đúng 6 chữ số; mật khẩu ≥ 8 ký tự có chữ và số; hai ô mật khẩu khớp |
| API | `activateAccount` hoặc `resetPassword` |
| Lỗi | Mã sai hoặc hết hạn: "Mã không hợp lệ hoặc đã hết hạn." Mật khẩu bị backend từ chối: hiện lý do chính sách |
| Thành công | Chuyển về `LoginPage` kèm thông báo; không tự đăng nhập |

### ProfilePage

| Component | State | API | Ghi chú |
|---|---|---|---|
| `ProfileForm` | `displayName`, `phoneNumber`, `dirty`, `saving` | `getProfile`, `updateProfile` | Email và role chỉ đọc |
| `AvatarUploader` | `file`, `uploading` | U03 upload → `updateProfile(avatarRef)` | Chỉ nhận ảnh; ẩn nếu backend báo chưa hỗ trợ |
| `ChangePasswordForm` | `currentPassword`, `newPassword`, `confirmPassword` | `changePassword` | Thành công báo "Các thiết bị khác đã bị đăng xuất" |
| `LogoutButton` | - | `logout` | Chỉ phiên hiện tại |

## 3. Admin Console

### AccountListPage

| Mục | Nội dung |
|---|---|
| State | `filters { email, role, status }`, `page`, `items`, `loading` |
| API | `listAccounts` |
| Cột | Email, tên hiển thị, role, trạng thái, ngày tạo |
| Hành động | Mở chi tiết, tạo tài khoản, nhập CSV |

### CreateAccountDialog

| Mục | Nội dung |
|---|---|
| State | `email`, `displayName`, `role` |
| Validation | Email đúng định dạng và domain trường; tên không rỗng |
| API | `createAccount` |
| Sau khi tạo | Hiện "Tài khoản đã tạo ở trạng thái chờ kích hoạt. Người dùng tự kích hoạt ở lần đăng nhập đầu." Không có nút gửi mail |

### AccountDetailPage

| Component | API | Hành vi |
|---|---|---|
| `RoleChangeDialog` | `assignRole` | Cảnh báo người dùng sẽ bị đăng xuất. Backend từ chối vì còn phụ trách môn/lớp thì hiện danh sách môn/lớp. Nút bị tắt với chính mình khi hạ quyền |
| `StatusToggleDialog` | `changeAccountStatus` | "Vô hiệu hóa" hoặc "Mở lại". Tắt với chính mình. Backend từ chối admin cuối cùng thì hiện lý do |

### ImportAccountsPage

| Bước | State | API | Hành vi |
|---|---|---|---|
| `CsvUploadStep` | `file` | `validateAccountImport` | Chỉ nhận `.csv`; có nút tải file mẫu `email,display_name,role` |
| `ImportPreviewTable` | `rows[]` | - | Mỗi dòng: kết quả, lý do lỗi dễ hiểu. Đếm số dòng hợp lệ và lỗi |
| `ImportConfirmStep` | `file` (gửi lại) | `commitAccountImport` | Backend kiểm lại rồi chỉ tạo dòng hợp lệ. Thông báo không có email nào được gửi |

## 4. Nguyên tắc chung

- Mật khẩu, OTP không lưu vào local storage, không đưa lên URL.
- Thông báo lỗi xác thực không bao giờ tiết lộ tài khoản có tồn tại hay trạng thái của nó.
- Route admin có guard theo role để trải nghiệm tốt, nhưng mọi API vẫn tự kiểm quyền phía server.
- Nhãn tiếng Việt, thiết kế desktop-first.

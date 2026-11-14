# U01 Account & Access — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u01-account-and-access` là thiết kế của U01 hiện hành.

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

U01 nằm trong **Web Shell** (xác thực, hồ sơ, khung sau đăng nhập) và **Admin Console** (quản trị tài khoản). Ẩn/hiện UI chỉ hỗ trợ trải nghiệm; quyền thật do backend quyết định. Tên API là tên nghiệp vụ, đường dẫn cuối cùng chốt ở OpenAPI.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC |
|---|---|---|---|
| 1 | User Login (`Main`) | `LoginPage` | 02 |
| 2 | Activate Account, bước 1–3 (`c02a`, `c02b`, `c02`) | `ActivationPage` | 01 |
| 3 | Password Reset, bước 1–3 (`c03a`, `c03`, `c04`) | `PasswordResetPage` | 04 |
| 4 | User Profile (`c05`) | `ProfilePage` | 06, 07 |
| 5 | Password Change, khối trên trang Hồ sơ (`c06`) | `PasswordChangeForm` | 05 |
| – | Khung sau đăng nhập: thanh trên cùng, menu bên, popup Đăng xuất (`c15`) | `AppShell`, `LogoutDialog`, `PostLoginRedirect`, `RoleGuard` | 01, 02, 03 |
| 11 | Account List, vùng Nhập Excel (`a02`, `a02b`) | `AccountListPage`, `ImportAccountsPanel` | 08, luồng phụ của 09 |
| 12 | New Account, popup (`a03`) | `NewAccountDialog` | 09 |
| 13 | Account Detail và popup Đổi vai trò, Đổi vai trò bị từ chối, Vô hiệu hóa / Mở lại (`a04`, `a04b`, `a04c`, `a04d`) | `AccountDetailPage`, `RoleChangeDialog`, `RoleChangeRejectedDialog`, `StatusToggleDialog` | 10, 11, 12 |

Class Dashboard, Manager Dashboard (U05) và Admin Dashboard (U15) là đích của Post-Login, không thuộc U01.

## 2. Cây component

```
app/(auth)/                       (chưa đăng nhập; hai cột: form bên trái, khối giới thiệu bên phải)
  login/            LoginPage             #1 User Login
    LoginForm
  activate/         ActivationPage        #2 Activate Account
    OtpFlow                       thanh bước 1 Email · 2 Mã OTP · 3 Đặt mật khẩu và ba bước dưới đây
    RequestOtpStep                bước 1 (c02a)
    VerifyOtpStep                 bước 2 (c02b): chỉ nhập OTP
    SetNewPasswordStep            bước 3 (c02): chỉ mở sau khi OTP đúng
  reset-password/   PasswordResetPage     #3 Password Reset (c03a, c03, c04), dùng chung ba bước
app/post-login/     PostLoginRedirect     chuyển tới dashboard theo role (BR-U01-48)
shared/layout/      AppShell              khung sau đăng nhập (code đã có, làm cùng U03; U01 nối phiên thật)
  TopBar                          logo, lối vào AccountMenuEntry, vùng HeaderWidget, thẻ hồ sơ, nút Đăng xuất
  SideNav                         menu bên theo role, mục lấy từ NavEntry
  LogoutDialog                    popup Đăng xuất (c15)
  RoleGuard                       chặn route theo role
app/profile/        ProfilePage           #4 User Profile
  ProfileForm
  PasswordChangeForm              #5 khối Đổi mật khẩu ngay dưới ProfileForm
app/admin/accounts/ AccountListPage       #11 Account List
    AccountFilters                tab vai trò, lọc trạng thái, ô tìm
    AccountTable
    ImportAccountsPanel           vùng Nhập Excel ngay trên trang (a02b)
    NewAccountDialog              #12 popup
  [id]/             AccountDetailPage     #13 Account Detail
    AccountInfoForm
    AccountStatusCard
    AccountHistoryLink            mở Audit Log lọc sẵn theo tài khoản
    RoleChangeDialog              a04b
    RoleChangeRejectedDialog      a04c
    StatusToggleDialog            a04d
shared/ui/                        bộ giao diện dùng chung (U03); U01 thêm PasswordField, OtpInput
```

## 3. Web Shell

### LoginPage (#1, `Main`)

| Mục | Nội dung |
|---|---|
| Trường | Email (gợi ý dạng email trường), Mật khẩu (nút hiện/ẩn) |
| Thao tác | Đăng nhập; link "Quên mật khẩu?" sang Password Reset; "Lần đầu sử dụng? Kích hoạt tài khoản" sang Activate Account |
| Khối bên phải | "Lần đầu sử dụng?": tài khoản do Phòng Đào tạo cấp; kích hoạt bằng email trường và mã OTP; quên mật khẩu thì khôi phục bằng mã gửi về email |
| Validation phía client | Email đúng định dạng, mật khẩu không rỗng |
| API | `authenticate` → `SessionInfo { accountId, displayName, role, homePath }` |
| Hành vi lỗi | Mọi thất bại hiện **một** câu: "Email hoặc mật khẩu không đúng, hoặc tài khoản chưa sẵn sàng." Không phân biệt khóa tạm, chưa kích hoạt hay không tồn tại |
| Thành công | Chuyển tới `homePath` (BR-U01-48): Student, Teacher → `/classes` (Class Dashboard); Subject Manager → `/manager` (Manager Dashboard); Admin → `/admin` (Admin Dashboard). Hết phiên ở trang nội bộ thì về `/login?next=<trang đang mở>` và đăng nhập xong quay lại đó; `safeNextPath` chỉ nhận đường dẫn cùng origin bắt đầu bằng một `/`, bỏ giá trị có `//`, `\`, ký tự điều khiển hay khoảng trắng (chặn open redirect) và bỏ trang công khai, không hợp lệ thì về `homePath` |

### ActivationPage (#2) / PasswordResetPage (#3)

Hai trang dùng chung `OtpFlow` (`features/identity/otp`) với thanh bước và ba bước, khác `purpose` và chữ trên nút; ô mật khẩu chỉ hiện sau khi OTP được xác minh (BR-U01-28).

| Bước | Trường và thao tác | API | Hành vi |
|---|---|---|---|
| 1. Email (`c02a`, `c03a`) | Email trường; nút "Gửi mã kích hoạt" hoặc "Gửi mã khôi phục" | `requestActivation` hoặc `requestPasswordReset` | Luôn hiện cùng một câu, không cho biết email có tồn tại: mã gồm 6 chữ số, hiệu lực 10 phút; khóa "Gửi lại" trong thời gian chờ (`cooldownSeconds`, design ghi 60 giây) |
| 2. Mã OTP (`c02b`, `c03`) | Email đã nhập (chỉ đọc, có link "Đổi email" về bước 1); OTP 6 ô; đếm ngược hết hạn và đếm ngược được gửi lại; nút "Xác nhận mã", "Gửi lại mã" | `verifyActivationOtp` hoặc `verifyPasswordResetOtp` → `otpTicket` | Mã sai hoặc hết hạn: "Mã không hợp lệ hoặc đã hết hạn.", ở lại bước này; ghi chú nhập sai quá 5 lần thì mã bị hủy, cần gửi mã mới (BR-U01-22). Đúng thì giữ `otpTicket` trong bộ nhớ trang |
| 3. Mật khẩu (`c02`, `c04`) | Mật khẩu mới, Nhập lại (nút hiện/ẩn); danh sách điều kiện "Ít nhất 8 ký tự", "Có cả chữ và số" đánh dấu theo giá trị đang nhập; nút "Kích hoạt và đăng nhập" hoặc "Đặt lại mật khẩu" | `activateAccount(otpTicket, newPassword)` hoặc `resetPassword(otpTicket, newPassword)` | Mật khẩu bị từ chối: hiện lý do, nhập lại với cùng ticket; ticket hết hạn: về bước 1. Kích hoạt xong vào thẳng dashboard theo role (BR-U01-13, 48). Đặt lại xong: báo các thiết bị khác đã bị đăng xuất, về User Login kèm thông báo đăng nhập bằng mật khẩu mới |

### Khung sau đăng nhập (AppShell)

| Vùng | Nội dung |
|---|---|
| Thanh trên cùng | Logo "Nền tảng học tập" (về `homePath`); lối vào do unit khác đăng ký qua `AccountMenuEntry` theo role (U06: "Kết nối AI" cho Student, Teacher, Subject Manager; Admin không có); vùng `HeaderWidget` (U14: chuông thông báo kèm số chưa đọc); thẻ hồ sơ (avatar chữ viết tắt, màu theo role; họ tên; nhãn role) mở User Profile; nút "Đăng xuất" màu đỏ |
| Menu bên | Mục theo role, lấy từ `NavEntry` do unit sở hữu màn đăng ký; mục đang mở tô nền xanh, icon đặc. Danh sách ở bảng dưới. Từ 2026-11-09 `AppShell` vẽ nhánh cây `NavTree` dưới mục có `href` trùng `NavContext.parentHref` (môn, lớp của U05) |
| `LogoutDialog` (`c15`) | "Đăng xuất?" — "Bạn sẽ thoát khỏi phiên trên thiết bị này. Bài đang làm đã được tự lưu."; Hủy, Đăng xuất. Xác nhận thì gọi `logout`, xóa trạng thái phía client, về User Login (UC 03) |
| Thông tin người dùng | Họ tên, role lấy từ `SessionInfo`; tải lại trang thì gọi `GET /api/v1/me/profile` |
| `PostLoginRedirect` | Sau đăng nhập, kích hoạt, hoặc khi mở `/`: lấy `homePath` rồi chuyển tới dashboard. Khi Admin Dashboard (U15, Iter 3) chưa có, `homePath` của Admin tạm là Account List |
| `RoleGuard` | `/admin/*` chỉ `ADMIN`; `/manager/*` chỉ `SUBJECT_MANAGER`; `/classes/*` chỉ `STUDENT`, `TEACHER`, `SUBJECT_MANAGER`. Sai role thì chuyển về `homePath`. Admin không vào Manager Dashboard hay Class Dashboard (BR-U01-48). Backend vẫn tự kiểm quyền |

Menu bên theo design (mỗi unit đăng ký mục của màn mình qua `NavEntry`, theo thứ tự dưới):

| Role | Mục (đường dẫn, unit đăng ký) |
|---|---|
| Admin | Tổng quan `/admin` (U15), Cài đặt `/admin/settings` (U03), Tài khoản `/admin/accounts` (U01), Môn học `/admin/subjects` (U04), Nhật ký audit `/admin/audit` (U02) |
| Subject Manager | Tổng quan `/manager` (U05), Môn học của tôi `/manager/subjects` (U05), Ngân hàng câu hỏi `/manager/questions` (U10), Quiz `/manager/quizzes` (U10), Học liệu `/manager/materials` (U13), Bài tập `/manager/assignments` (U07), Lớp học `/manager/classes` (U05); nhóm "Giảng dạy": Lớp được phân công `/classes` (U05), Thông báo lớp `/announcements` (U14) |
| Teacher | Lớp được phân công `/classes` (U05), Thông báo lớp `/announcements` (U14) |
| Student | Lớp của tôi `/classes` (U05), Bài tập `/my-assignments` (U08), Luyện tập Quiz `/quiz-practice` (U11), Thông báo lớp `/announcements` (U14) |

### ProfilePage (#4, `c05`)

| Mục | Nội dung |
|---|---|
| Bố cục | Từ 2026-11-11: khối "Thông tin cá nhân" (dải đầu gồm avatar chữ viết tắt, họ tên, email, nhãn trạng thái; lưới hai cột Họ và tên · Số điện thoại, Email trường · Vai trò; một cột dưới 640px) rồi khối "Đổi mật khẩu" (#5) cùng trang |
| Trường | Họ và tên, Số điện thoại (sửa được); Email trường, Vai trò, Trạng thái tài khoản (chỉ đọc), ghi chú "Email trường, vai trò và trạng thái do quản trị viên quản lý." |
| Thao tác | Lưu thay đổi |
| API | `getProfile`, `updateProfile` |
| Ghi chú | Avatar chỉ là chữ viết tắt; không có tải ảnh đại diện |

### PasswordChangeForm (#5, `c06`)

| Mục | Nội dung |
|---|---|
| Trường | Mật khẩu hiện tại, Mật khẩu mới, Nhập lại mật khẩu mới (mỗi ô có nút hiện/ẩn); điều kiện "Ít nhất 8 ký tự", "Có cả chữ và số" |
| Ghi chú dưới tiêu đề khối | "Các thiết bị khác sẽ bị đăng xuất. Phiên hiện tại được giữ lại." |
| Validation phía client | Mật khẩu mới ≥ 8 ký tự có chữ và số; hai ô khớp |
| API | `changePassword` |
| Thao tác | Đổi mật khẩu; xong thì xóa các ô và báo "Đã đổi mật khẩu". Từ 2026-11-11 là khối phẳng trên trang Hồ sơ, không còn popup |

## 4. Admin Console

### AccountListPage (#11, `a02`)

| Mục | Nội dung |
|---|---|
| Đầu trang | "Tài khoản" kèm tổng số; nút Nhập Excel, Thêm tài khoản |
| Bộ lọc | Tab vai trò: Tất cả, Sinh viên, Giảng viên, Chủ nhiệm môn, Quản trị viên; lọc Trạng thái (Chờ kích hoạt, Hoạt động, Vô hiệu hóa); ô "Lọc theo email hoặc tên" |
| Cột | Họ tên (avatar chữ viết tắt), Email, Vai trò, Trạng thái (nhãn màu), Ngày tạo; bấm dòng mở Account Detail |
| Phân trang | "Hiển thị a–b trong N tài khoản" |
| API | `listAccounts` (lọc role, trạng thái; `q` khớp email hoặc họ tên; trang) |

### ImportAccountsPanel (`a02b`, luồng phụ của UC 09)

Nút "Nhập Excel" mở vùng nhập ngay trên Account List; danh sách tải lại sau khi xác nhận.

| Bước | Nội dung | API |
|---|---|---|
| Chọn tệp | "Tệp Excel .xlsx, trang tính đầu tiên, dòng đầu là tiêu đề cột"; ô tệp (tên, dung lượng), nút "Chọn tệp khác", link "Tải tệp mẫu (email, họ tên, vai trò)" | `validateAccountImport` |
| Xem trước | Nhãn đếm số dòng, hợp lệ, lỗi; "Chỉ nhập các dòng hợp lệ; dòng lỗi được bỏ qua."; bảng Dòng, Email, Họ tên, Vai trò, Kết quả kiểm tra (Hợp lệ, hoặc lý do dễ hiểu như email đã tồn tại, không phải email trường, vai trò không hợp lệ), có phân trang | - |
| Xác nhận | Ghi chú "Tài khoản mới ở trạng thái Chờ kích hoạt. Hệ thống không gửi email; người dùng tự kích hoạt bằng email trường."; Hủy, "Xác nhận nhập N tài khoản" | `commitAccountImport` (gửi lại tệp; backend kiểm lại rồi chỉ tạo dòng hợp lệ) |

### NewAccountDialog (#12, `a03`)

| Mục | Nội dung |
|---|---|
| Trường | Email trường* (gợi ý "Dùng làm định danh đăng nhập, không đổi được sau khi tạo."), Họ và tên*, Vai trò* (Sinh viên, Giảng viên, Chủ nhiệm môn, Quản trị viên) |
| Validation | Email đúng định dạng (tên miền nào cũng được); tên không rỗng |
| Ghi chú | "Tài khoản ở trạng thái Chờ kích hoạt. Người dùng tự kích hoạt bằng email trường và mã OTP; số điện thoại sửa sau ở Chi tiết tài khoản." |
| API | `createAccount`; xong thì đóng popup và tải lại Account List. Không có nút gửi mail |

### AccountDetailPage (#13, `a04`)

| Vùng | Nội dung | API |
|---|---|---|
| Đầu trang | Nút quay lại; họ tên kèm nhãn trạng thái; đường dẫn "Tài khoản › email" | `getAccount` |
| Thông tin tài khoản | "Chỉ sửa họ tên và số điện thoại ở đây": Họ và tên*, Số điện thoại (tùy chọn), Email trường (chỉ đọc, "Email không đổi được"), Vai trò (chỉ đọc) kèm nút Đổi vai trò; Hủy, Lưu thay đổi | `updateAccount` (PATCH kèm `version`; `409` báo dữ liệu đã đổi và tải lại) (UC 11) |
| Trạng thái | Trạng thái, Ngày tạo; nút "Vô hiệu hóa tài khoản" hoặc "Mở lại tài khoản", tắt với chính mình | - |
| Lịch sử | "Lịch sử thay đổi của tài khoản xem ở Nhật ký audit." kèm link mở Audit Log (U02) lọc sẵn loại đối tượng tài khoản và mã tài khoản | - |

| Popup | Nội dung | API |
|---|---|---|
| Đổi vai trò (`a04b`) | Họ tên · email; Vai trò hiện tại; Vai trò mới*; cảnh báo "Người dùng sẽ bị đăng xuất khỏi mọi thiết bị và phải đăng nhập lại với quyền mới."; "Không hạ quyền được chính mình."; Hủy, Đổi vai trò | `assignRole` (UC 11) |
| Đổi vai trò bị từ chối (`a04c`) | "Không đổi được vai trò": người này đang giữ các môn, lớp dưới đây, cần chuyển cho người khác trước; bảng Loại, Mã, Tên, Vai trò đang giữ; Đóng | lỗi của `assignRole` |
| Vô hiệu hóa / Mở lại (`a04d`) | Vô hiệu hóa: "X sẽ bị đăng xuất và không đăng nhập được cho tới khi được mở lại. Dữ liệu bài làm vẫn được giữ."; "Không áp dụng cho chính mình. Nếu đây là Quản trị viên cuối cùng đang hoạt động, hệ thống từ chối và hiện lý do." Không có ô lý do (F12). Mở lại: báo tài khoản về Hoạt động, hoặc về Chờ kích hoạt nếu chưa từng đặt mật khẩu | `changeAccountStatus` (UC 12) |

## 5. Nguyên tắc chung

- Mật khẩu, OTP không lưu vào local storage, không đưa lên URL.
- Thông báo lỗi xác thực không bao giờ tiết lộ tài khoản có tồn tại hay trạng thái của nó.
- Route có guard theo role để trải nghiệm tốt, nhưng mọi API vẫn tự kiểm quyền phía server.
- Admin chỉ thấy Admin Dashboard và các màn quản trị; không có lối vào Manager Dashboard hay Class Dashboard.
- Profile không có tải ảnh đại diện.
- Nhãn tiếng Việt, thiết kế desktop-first; dùng bộ giao diện `shared/ui` của U03, không tự vẽ lại nút, bảng, popup.

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

## 6. Mã số sinh viên (MSSV) — 2026-11-13

Nguồn: [BR-U01-100…115](business-rules.md#11-mã-số-sinh-viên-mssv--2026-11-13); [FD Q4, Q6](../../plans/functional-design-plan/2026-11-13-u01-student-code-functional-design-plan.md).

### Quy ước chung

- Ô nhập MSSV có nhãn "Mã số sinh viên".
  - Khi rời ô, chữ thường được tự đổi thành in hoa.
  - Frontend kiểm trước theo `^[A-Z]{2}[0-9]{6}$` và báo "MSSV gồm 2 chữ cái và 6 chữ số, ví dụ SE123456." Backend vẫn là nơi kiểm cuối cùng.
- Câu báo cho từng mã lỗi:

| Mã lỗi | Câu báo |
|---|---|
| `STUDENT_CODE_EXISTS` | "MSSV này đã được dùng cho tài khoản khác." |
| `STUDENT_CODE_LOCKED` | "MSSV đã khóa vì sinh viên đã kích hoạt tài khoản." |
| `MISSING_STUDENT_CODE` | "Cần nhập MSSV cho tài khoản sinh viên." |
| `STUDENT_CODE_NOT_ALLOWED` | "Chỉ tài khoản sinh viên mới có MSSV." |

- Trong bảng, cột "MSSV" đứng trước cột họ tên. Ở màn hẹp, MSSV gộp vào ô họ tên thành dòng phụ. Tài khoản chưa có mã thì để trống.

### AccountListPage (bổ sung)

- Thêm cột "MSSV".
- Chữ gợi ý ô tìm đổi thành "Tìm theo email, tên hoặc MSSV".
- Không thêm bộ lọc theo tình trạng MSSV (Q7).

### NewAccountDialog (bổ sung)

- Ô "Mã số sinh viên*" chỉ hiện khi chọn vai trò Sinh viên. Đổi sang vai trò khác thì ô bị ẩn và giá trị đã nhập bị xóa.
- `createAccount` gửi thêm `studentCode`.

### AccountDetailPage (bổ sung)

Vùng Thông tin tài khoản có thêm MSSV khi tài khoản là sinh viên. Cách hiển thị tùy tình huống:

| Tình huống | Hiển thị |
|---|---|
| `studentCodeEditable`, đang `PENDING` | Ô có giá trị, sửa được, gợi ý "Chỉ sửa được khi sinh viên chưa kích hoạt." |
| Không `studentCodeEditable` | Chỉ đọc, gợi ý "MSSV không đổi được sau khi sinh viên kích hoạt tài khoản." Tài khoản cũ chưa có mã thì hiện "Chưa có MSSV" |

`updateAccount` chỉ gửi `studentCode` khi giá trị khác bản đã tải về.

Popup Đổi vai trò:

- Với tài khoản sinh viên, không hiện nút "Đổi vai trò". Thay vào đó hiện ghi chú "Không đổi được vai trò của tài khoản sinh viên."
- Với các tài khoản khác, danh sách "Vai trò mới" không có lựa chọn Sinh viên.
- Nếu backend vẫn trả lỗi `STUDENT_ROLE_CHANGE_NOT_ALLOWED`, hiện câu "Không đổi được vai trò vào hoặc ra khỏi Sinh viên."

### ImportAccountsPanel (bổ sung)

- Tệp mẫu có thêm cột `student_code`. Chữ hướng dẫn đổi thành "email, họ tên, vai trò, MSSV (bắt buộc với sinh viên)".
- Bảng xem trước có thêm cột MSSV và câu báo cho các mã lỗi mới.

### AssignStudentCodesPanel (`a02c`, F16): đã bỏ (2026-11-14)

### ProfilePage (bổ sung)

Tài khoản sinh viên thấy thêm dòng "Mã số sinh viên" ở dạng chỉ đọc. Chưa có mã thì hiện "Chưa có MSSV". Form sửa hồ sơ không có ô này.

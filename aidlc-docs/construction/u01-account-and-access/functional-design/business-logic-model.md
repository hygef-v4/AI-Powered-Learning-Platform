# U01 Account & Access - Business Logic Model

**Bản tài liệu 2026-10-08**: UC 01, 02, 03, 04, 05, 06, 07, 58, 59, 60, 61, 62; primary stories: US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Mỗi luồng ghi: đầu vào → các bước → kết quả, kèm rule (`BR-U01-xx` trong `business-rules.md`) và truy vết. Mọi lỗi trả về client đều ở dạng an toàn.

## 1. Bảng truy vết

| Luồng | Use case | Story |
|---|---|---|
| F1 Yêu cầu kích hoạt | UC 01 | US-IAM-001 |
| F2 Hoàn tất kích hoạt | UC 01 | US-IAM-001 |
| F3 Đăng nhập | UC 02 | US-IAM-002 |
| F4 Đăng xuất | UC 03 | US-IAM-002 |
| F5 Quên mật khẩu | UC 04 | US-IAM-003 |
| F6 Đổi mật khẩu | UC 05 | US-IAM-006 |
| F7 Xem và sửa hồ sơ | UC 06, 07 | US-IAM-004 |
| F8 Xem tài khoản | UC 58, 59, 60, 61, 62 | US-IAM-007 |
| F9 Tạo tài khoản | UC 58, 59, 60, 61, 62 | US-IAM-007 |
| F10 Nhập hàng loạt | UC 58, 59, 60, 61, 62 | US-IAM-007 |
| F11 Đổi role | UC 58, 59, 60, 61, 62 | US-IAM-005, US-IAM-007 |
| F12 Vô hiệu hóa và mở lại | UC 58, 59, 60, 61, 62 | US-IAM-007 |
| F13 Quyết định phân quyền | Mọi UC có kiểm quyền | US-IAM-005 |

## 2. Luồng kích hoạt

### F1 - Yêu cầu kích hoạt

**Vào**: `schoolEmail`, thông tin client.

1. Chuẩn hóa email (BR-U01-02).
2. Kiểm `RequestThrottle` theo email và client. Vượt ngưỡng → sang bước 6 (BR-U01-25).
3. Tìm tài khoản. Không có, ngoài domain, hoặc không ở `PENDING` → sang bước 6 (BR-U01-12).
4. Gửi việc `OTP_DELIVERY(ACTIVATION)` qua `JobPort.enqueue` (yêu cầu không ghi database nên message gửi ngay, BR-U03-50).
5. Worker sinh `OtpChallenge`, xóa challenge cũ, lưu băm và gửi mail (BR-U01-20, 21, 23). Mã rõ không đi qua queue hay DB.
6. Trả **`Accepted`** với cùng một thông báo cho mọi trường hợp.

**Ra**: phản hồi trung tính. Email chỉ được gửi ở bước 5.

### F2 - Hoàn tất kích hoạt

**Bước 2a — Xác minh OTP. Vào**: `schoolEmail`, `otpCode`.

1. Tìm challenge `ACTIVATION` còn hiệu lực. Không có → lỗi "mã không hợp lệ hoặc đã hết hạn".
2. So khớp mã. Sai → giảm `attemptsLeft`; về 0 thì xóa challenge (BR-U01-22). Trả lỗi như bước 1.
3. Đúng → xóa challenge, cấp `otpTicket` (BR-U01-24, 28); màn hình chuyển sang bước nhập mật khẩu mới.

**Bước 2b — Đặt mật khẩu. Vào**: `otpTicket`, `newPassword`.

4. Kiểm ticket còn hạn, đúng mục đích `ACTIVATION`. Không hợp lệ → lỗi "phiên xác minh đã hết hạn, hãy yêu cầu mã mới".
5. Kiểm mật khẩu mới (BR-U01-30, 31). Không đạt → lỗi chính sách, ticket vẫn giữ để nhập lại.
6. Băm mật khẩu, lưu `credential`, chuyển `ACTIVE`, xóa ticket (BR-U01-13, 32).
7. Ghi audit `ACCOUNT_ACTIVATED`.
8. Tự đăng nhập: xóa `failedLoginCount`, tạo refresh session gắn `credentialVersion`, cấp access token 15 phút như F3 bước 7 (BR-U01-13, 44).

**Ra**: phiên đăng nhập và điểm đến theo role (BR-U01-48); người dùng vào thẳng menu của mình.

## 3. Luồng xác thực

### F3 - Đăng nhập

**Vào**: `schoolEmail`, `password`, thông tin client.

1. Kiểm giới hạn theo client (BR-U01-43). Vượt → lỗi trung tính.
2. Tìm tài khoản. Không có → so khớp giả với hash mẫu để thời gian phản hồi đồng đều, trả lỗi trung tính.
3. `lockedUntil` còn hiệu lực → lỗi trung tính, ghi audit `LOGIN_BLOCKED_LOCKED` (BR-U01-41).
4. Trạng thái khác `ACTIVE` → lỗi trung tính (BR-U01-40).
5. Sai mật khẩu → tăng `failedLoginCount`; chạm 5 thì đặt `lockedUntil` + 15 phút và ghi audit `ACCOUNT_TEMP_LOCKED`. Trả lỗi trung tính, ghi audit `LOGIN_FAILED`.
6. Đúng → xóa `failedLoginCount`, `lockedUntil`.
7. Tạo refresh session gắn `credentialVersion` hiện tại, cấp access token 15 phút. Trả phiên và điểm đến theo role (BR-U01-44, 48). Không có bước MFA (BR-U01-47).

**Ra**: phiên đăng nhập hoặc lỗi trung tính **giống nhau** cho mọi nguyên nhân thất bại.

### F4 - Đăng xuất

1. Xóa `Session` hiện tại (BR-U01-45).
2. Phiên khác của người dùng giữ nguyên.

### F5 - Quên mật khẩu

**Vào**: `schoolEmail`.

1. Chuẩn hóa, kiểm `RequestThrottle`.
2. Chỉ khi tài khoản `ACTIVE`: gửi việc `OTP_DELIVERY(PASSWORD_RESET)` qua `JobPort.enqueue`; worker sinh mã và gửi mail như F1.
3. Luôn trả `Accepted` trung tính (US-IAM-003 S1).
4. Người dùng nhập `otpCode`: xác minh như F2 bước 1-3 (mục đích `PASSWORD_RESET`), nhận `otpTicket`.
5. Người dùng nhập `newPassword` kèm ticket: kiểm như F2 bước 4-5.
6. Lưu mật khẩu mới, tăng `credentialVersion` → **mọi phiên** bị thu hồi, xóa `failedLoginCount` và `lockedUntil`, ghi audit `PASSWORD_RESET`.

### F6 - Đổi mật khẩu

**Vào**: phiên hợp lệ, `currentPassword`, `newPassword`.

1. Sai mật khẩu hiện tại → từ chối, không đổi gì (US-IAM-006 S2).
2. Kiểm chính sách (BR-U01-30, 31).
3. Lưu, tăng `credentialVersion`, cấp lại phiên hiện tại với version mới; phiên khác vô hiệu (BR-U01-34).
4. Ghi audit `PASSWORD_CHANGED`.

## 4. Hồ sơ

### F7 - Xem và sửa hồ sơ

1. Chỉ thao tác trên `accountId` của phiên; định danh khác bị từ chối (BR-U01-54).
2. Sửa `displayName`, `phoneNumber` theo ràng buộc; email, role, trạng thái không sửa được (BR-U01-51).
3. DTO chỉ nhận tên/số điện thoại; avatar/email/role/status hoặc ID người khác bị từ chối trước ghi.
4. Ghi audit `PROFILE_UPDATED` với tên trường đã đổi, không ghi giá trị số điện thoại.

## 5. Quản trị tài khoản

### F8 - Xem tài khoản

Admin lọc theo role, trạng thái, email. Kết quả không có hash, OTP hay token.

### F9 - Tạo tài khoản

**Vào**: `schoolEmail`, `displayName`, `role`.

1. Kiểm quyền admin (BR-U01-67).
2. Kiểm email (BR-U01-02, 03). Trùng → lỗi rõ cho admin.
3. Tạo tài khoản `PENDING`, không mật khẩu, **không gửi mail** (BR-U01-10).
4. Tạo `ADMIN` bắt buộc qua luồng này, không qua nhập hàng loạt (BR-U01-84).
5. Ghi audit `ACCOUNT_CREATED`.

### F10 - Nhập hàng loạt

1. Kiểm quyền admin; kiểm file là CSV, ≤ 1000 dòng (BR-U01-80).
2. Kiểm từng dòng, trả `AccountImportResult` trong response, không lưu (BR-U01-82, 84).
3. Admin xem kết quả và xác nhận: frontend gửi lại cùng file.
4. Kiểm lại toàn bộ file (dữ liệu có thể đã đổi), tạo các dòng hợp lệ ở `PENDING` trong một transaction, không gửi mail (BR-U01-85). Email đã tồn tại (kể cả do lần nhập trước) báo lỗi dòng nên nhập lại cùng file không tạo trùng (BR-U01-83).
5. Ghi một audit `ACCOUNTS_IMPORTED` kèm checksum file, số dòng tạo và bị từ chối.

### F11 - Đổi role

**Vào**: `accountId` đích, `newRole`.

1. Kiểm quyền admin.
2. Đích là chính mình và là hạ quyền → từ chối (BR-U01-65).
3. Đích là `ADMIN` đang hoạt động cuối cùng và role mới khác `ADMIN` → từ chối (BR-U01-66).
4. Role mới thấp hơn và đích còn phụ trách môn/lớp (hỏi U04) → từ chối, nêu môn/lớp (BR-U01-64).
5. Lưu role, tăng `credentialVersion` → mọi phiên của đích bị thu hồi (BR-U01-63).
6. Ghi audit `ROLE_CHANGED` với giá trị trước/sau.

### F12 - Vô hiệu hóa và mở lại

**Vô hiệu hóa**:
1. Kiểm quyền; áp BR-U01-65, 66 như F11.
2. Chuyển `DISABLED`, tăng `credentialVersion`, xóa OTP còn hiệu lực (BR-U01-71).
3. Ghi audit `ACCOUNT_DISABLED`.

**Mở lại**:
1. Có mật khẩu → `ACTIVE`; chưa có → `PENDING` (BR-U01-72).
2. Xóa `failedLoginCount`, `lockedUntil`.
3. Ghi audit `ACCOUNT_ENABLED`.

## 6. Phân quyền

### F13 - `authorize(actor, action, resourceRef)`

1. Access token không hợp lệ hoặc hết hạn → từ chối. `credentialVersion` chỉ được kiểm lúc refresh (BR-U01-44).
2. Tài khoản không `ACTIVE` → từ chối.
3. Role không có quyền với `action` → từ chối.
4. `action` cần phạm vi → hỏi U04 xem actor có phụ trách môn/lớp chứa `resourceRef`. Không → từ chối.
5. Không gọi được U04 → **từ chối** (BR-U01-93).
6. Trả `AuthorizationDecision`. Mọi lần từ chối trên hành động nhạy cảm ghi audit `ACCESS_DENIED`.

Subject Manager/Administrator dùng chức năng Teacher chỉ khi isTeacherOf R4; tài nguyên môn khi isSubjectManager R2. Admin Full chỉ cấu trúc/tài khoản/statistic/gói/lịch sử/audit. Đọc scope hiện thời ở backend, không suy từ role hoặc cache dữ liệu cũ.

## 7. Sự kiện U01 phát ra

| Sự kiện | Người nhận |
|---|---|
| `ACCOUNT_CREATED`, `ACCOUNTS_IMPORTED`, `ACCOUNT_ACTIVATED`, `ROLE_CHANGED`, `ACCOUNT_DISABLED`, `ACCOUNT_ENABLED` | U02 audit (U16 đếm tài khoản qua `AccountLookupPort`, không nghe sự kiện này) |
| `LOGIN_FAILED`, `LOGIN_BLOCKED_LOCKED`, `ACCOUNT_TEMP_LOCKED`, `ACCESS_DENIED` | U02 audit |
| `PASSWORD_CHANGED`, `PASSWORD_RESET`, `PROFILE_UPDATED` | U02 audit |
| `OTP_DELIVERY_REQUESTED` | Job U03 → handler mail của U01 |

## F14 - Account Detail/Update Account Information (UC 60–61)
1. ADMIN GET theo ID: thông tin được phép, không hash/OTP/token.
2. PATCH displayName/phoneNumber hợp lệ với version; 409 nếu thay đổi cạnh tranh. Email bất biến; role/status dùng F11/F12, không đổi mật khẩu/gửi OTP.
3. Audit actor/thời gian/tên trường, không log số điện thoại.

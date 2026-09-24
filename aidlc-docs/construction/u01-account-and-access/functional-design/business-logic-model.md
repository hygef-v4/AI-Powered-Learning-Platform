# U01 Account & Access - Business Logic Model

Mỗi luồng ghi: đầu vào → các bước → kết quả, kèm rule (`BR-U01-xx` trong `business-rules.md`) và truy vết. Mọi lỗi trả về client đều ở dạng an toàn.

## 1. Bảng truy vết

| Luồng | Use case | Story |
|---|---|---|
| F1 Yêu cầu kích hoạt | UC-IAM-01 | US-IAM-001 |
| F2 Hoàn tất kích hoạt | UC-IAM-01 | US-IAM-001 |
| F3 Đăng nhập | UC-IAM-02 | US-IAM-002 |
| F4 Đăng xuất | UC-IAM-03 | US-IAM-002 |
| F5 Quên mật khẩu | UC-IAM-04 | US-IAM-003 |
| F6 Đổi mật khẩu | UC-IAM-05 | US-IAM-006 |
| F7 Xem và sửa hồ sơ | UC-IAM-06, UC-IAM-07 | US-IAM-004 |
| F8 Xem tài khoản | UC-IAM-08 | US-IAM-007 |
| F9 Tạo tài khoản | UC-IAM-09 | US-IAM-007 |
| F10 Nhập hàng loạt | UC-IAM-10 | US-IAM-007 |
| F11 Đổi role | UC-IAM-11 | US-IAM-005, US-IAM-007 |
| F12 Vô hiệu hóa và mở lại | UC-IAM-12 | US-IAM-007 |
| F13 Quyết định phân quyền | Mọi UC có kiểm quyền | US-IAM-005 |

## 2. Luồng kích hoạt

### F1 - Yêu cầu kích hoạt

**Vào**: `schoolEmail`, thông tin client.

1. Chuẩn hóa email (BR-U01-02).
2. Kiểm `RequestThrottle` theo email và client. Vượt ngưỡng → sang bước 6 (BR-U01-25).
3. Tìm tài khoản. Không có, ngoài domain, hoặc không ở `PENDING_ACTIVATION` → sang bước 6 (BR-U01-12).
4. Ghi yêu cầu `OTP_DELIVERY(ACTIVATION)` thành job U02 (`JobPort.enqueue`).
5. Worker sinh `OtpChallenge`, xóa challenge cũ, lưu băm và gửi mail (BR-U01-20, 21, 23). Mã rõ không đi qua queue hay DB.
6. Trả **`Accepted`** với cùng một thông báo cho mọi trường hợp.

**Ra**: phản hồi trung tính. Email chỉ được gửi ở bước 5.

### F2 - Hoàn tất kích hoạt

**Vào**: `schoolEmail`, `otpCode`, `newPassword`.

1. Tìm challenge `ACTIVATION` còn hiệu lực. Không có → lỗi "mã không hợp lệ hoặc đã hết hạn".
2. So khớp mã. Sai → giảm `attemptsLeft`; về 0 thì xóa challenge (BR-U01-22). Trả lỗi như bước 1.
3. Kiểm mật khẩu mới (BR-U01-30, 31). Không đạt → lỗi chính sách, challenge vẫn giữ.
4. Băm mật khẩu, lưu `credential`, chuyển `ACTIVE`, xóa challenge (BR-U01-13, 24, 32).
5. Ghi audit `ACCOUNT_ACTIVATED`.

**Ra**: thông báo kích hoạt thành công, chuyển về màn đăng nhập.

## 3. Luồng xác thực

### F3 - Đăng nhập

**Vào**: `schoolEmail`, `password`, thông tin client.

1. Kiểm giới hạn theo client (BR-U01-43). Vượt → lỗi trung tính.
2. Tìm tài khoản. Không có → so khớp giả với hash mẫu để thời gian phản hồi đồng đều, trả lỗi trung tính.
3. `lockedUntil` còn hiệu lực → lỗi trung tính, ghi audit `LOGIN_BLOCKED_LOCKED` (BR-U01-41).
4. Trạng thái khác `ACTIVE` → lỗi trung tính (BR-U01-40).
5. Sai mật khẩu → tăng `failedLoginCount`; chạm 5 thì đặt `lockedUntil` + 15 phút và ghi audit `ACCOUNT_TEMP_LOCKED`. Trả lỗi trung tính, ghi audit `LOGIN_FAILED`.
6. Đúng → xóa `failedLoginCount`, `lockedUntil`.
7. Tạo refresh session gắn `credentialVersion` hiện tại, cấp access token 15 phút. Trả phiên và điểm đến theo role (BR-U01-44). Không có bước MFA (BR-U01-47).

**Ra**: phiên đăng nhập hoặc lỗi trung tính **giống nhau** cho mọi nguyên nhân thất bại.

### F4 - Đăng xuất

1. Xóa `Session` hiện tại (BR-U01-45).
2. Phiên khác của người dùng giữ nguyên.

### F5 - Quên mật khẩu

**Vào**: `schoolEmail`.

1. Chuẩn hóa, kiểm `RequestThrottle`.
2. Chỉ khi tài khoản `ACTIVE`: tạo job `U01_OTP_DELIVERY(PASSWORD_RESET)` qua `JobPort.enqueue`; worker sinh mã và gửi mail như F1.
3. Luôn trả `Accepted` trung tính (US-IAM-003 S1).
4. Người dùng gửi `otpCode` + `newPassword`: xác minh như F2 bước 1-3.
5. Lưu mật khẩu mới, tăng `credentialVersion` → **mọi phiên** bị thu hồi, xóa `failedLoginCount` và `lockedUntil`, ghi audit `PASSWORD_RESET`.

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
3. Đổi ảnh: người dùng tải ảnh lên U03 trước, rồi gửi tham chiếu; U01 hỏi `AvatarPort` xác nhận chủ sở hữu và mục đích `AVATAR` (BR-U01-52).
4. Ghi audit `PROFILE_UPDATED` với tên trường đã đổi, không ghi giá trị số điện thoại.

## 5. Quản trị tài khoản

### F8 - Xem tài khoản

Admin lọc theo role, trạng thái, email. Kết quả không có hash, OTP hay token.

### F9 - Tạo tài khoản

**Vào**: `schoolEmail`, `displayName`, `role`.

1. Kiểm quyền admin (BR-U01-67).
2. Kiểm email (BR-U01-02, 03). Trùng → lỗi rõ cho admin.
3. Tạo tài khoản `PENDING_ACTIVATION`, không mật khẩu, **không gửi mail** (BR-U01-10).
4. Tạo `ADMIN` bắt buộc qua luồng này, không qua nhập hàng loạt (BR-U01-84).
5. Ghi audit `ACCOUNT_CREATED`.

### F10 - Nhập hàng loạt

1. Kiểm quyền admin; kiểm file là CSV, ≤ 1000 dòng (BR-U01-80).
2. Checksum đã có lô `COMMITTED` → trả lại kết quả lô cũ, không tạo gì (BR-U01-83).
3. Kiểm từng dòng, ghi `ImportRowResult` (BR-U01-82, 84). Lô ở `VALIDATED`.
4. Admin xem kết quả và xác nhận.
5. Tạo các dòng hợp lệ ở `PENDING_ACTIVATION`, không gửi mail (BR-U01-85). Lô sang `COMMITTED`.
6. Ghi một audit `ACCOUNTS_IMPORTED` kèm số dòng tạo và bị từ chối.

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
1. Có mật khẩu → `ACTIVE`; chưa có → `PENDING_ACTIVATION` (BR-U01-72).
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

`SUBJECT_MANAGER` được mọi quyền của `INSTRUCTOR` nhưng vẫn bị giới hạn bởi phạm vi ở bước 4.

## 7. Sự kiện U01 phát ra

| Sự kiện | Người nhận |
|---|---|
| `ACCOUNT_CREATED`, `ACCOUNTS_IMPORTED`, `ACCOUNT_ACTIVATED`, `ROLE_CHANGED`, `ACCOUNT_DISABLED`, `ACCOUNT_ENABLED` | U02 audit; U16 read model khi có |
| `LOGIN_FAILED`, `LOGIN_BLOCKED_LOCKED`, `ACCOUNT_TEMP_LOCKED`, `ACCESS_DENIED` | U02 audit |
| `PASSWORD_CHANGED`, `PASSWORD_RESET`, `PROFILE_UPDATED` | U02 audit |
| `OTP_DELIVERY_REQUESTED` | Job U02 → handler mail của U01 |

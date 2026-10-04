# U01 Account & Access - Domain Entities

Thiết kế độc lập công nghệ. Kiểu dữ liệu ghi ở mức nghiệp vụ; kiểu cột cuối cùng chốt ở Code Generation. Truy vết: `US-IAM-001`…`US-IAM-007`; UC 1–7 theo `docs/use-case-table.md`.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `Account` | Aggregate root | `accounts` | U01 |
| `PasswordCredential` | Value object của `Account` | `accounts` | U01 |
| `Profile` | Value object của `Account` | `accounts` | U01 |
| `LoginThrottle` | Value object của `Account` | `accounts` | U01 |
| `CreditBalance` | Value object của `Account` | `accounts` (`free_balance`, `free_period`, `purchased_balance`) | U07 |
| `EmailPreferences` | Value object của `Account` | `accounts.email_preferences` | U16 |
| `OtpChallenge` | Entity tạm thời | Redis | U01 |
| `VerificationTicket` | Entity tạm thời (`otpTicket`) | Redis | U01 |
| `Session` | Entity tạm thời | Redis | U01 |
| `RequestThrottle` | Bộ đếm tạm thời | Redis | U01 |
| `AccountImportResult` | Kết quả trả về | Không lưu | U01 |
| `AuthorizationDecision` | Kết quả trả về | Không lưu | U01 |

U01 **không** sở hữu: phân công môn/lớp (U04), audit (U02), file ảnh (U03), nghiệp vụ credit (U07).

## 2. `Account`

| Thuộc tính | Ý nghĩa | Ràng buộc |
|---|---|---|
| `accountId` | Định danh | Bất biến |
| `schoolEmail` | Email trường, dùng đăng nhập | Bắt buộc; chuẩn hóa chữ thường, bỏ khoảng trắng; duy nhất toàn hệ thống; thuộc tên miền trong biến triển khai `U01_ALLOWED_EMAIL_DOMAINS`; **không đổi sau khi tạo** |
| `role` | Vai trò cao nhất | `STUDENT`, `TEACHER`, `SUBJECT_MANAGER`, `ADMIN` |
| `status` | Trạng thái vòng đời | `PENDING`, `ACTIVE`, `DISABLED` |
| `credential` | `PasswordCredential` | Rỗng khi `PENDING` |
| `profile` | `Profile` | Bắt buộc có `displayName` |
| `throttle` | `LoginThrottle` | Mặc định không khóa |
| `credentialVersion` | Số phiên bản quyền | Tăng khi đổi hoặc đặt lại mật khẩu, đổi role, vô hiệu hóa; mọi phiên có version cũ không làm mới được |
| `credit` | `CreditBalance` | Do U07 ghi; U01 không đọc/ghi |
| `createdAt`, `updatedAt`, `version` | Thời gian và khóa lạc quan | Hệ thống quản lý |

### Trạng thái

```mermaid
stateDiagram-v2
    [*] --> PENDING: Admin tạo hoặc nhập
    PENDING --> ACTIVE: Người dùng xác minh OTP và đặt mật khẩu
    PENDING --> DISABLED: Admin vô hiệu hóa
    ACTIVE --> DISABLED: Admin vô hiệu hóa
    DISABLED --> ACTIVE: Admin mở lại, đã từng kích hoạt
    DISABLED --> PENDING: Admin mở lại, chưa từng kích hoạt
```

**Text alternative**: Tài khoản sinh ra ở `PENDING`. Người dùng xác minh OTP và đặt mật khẩu thì sang `ACTIVE`. Admin có thể chuyển `PENDING` hoặc `ACTIVE` sang `DISABLED`. Khi mở lại, tài khoản đã từng có mật khẩu về `ACTIVE`, chưa từng có mật khẩu về `PENDING`. Không có trạng thái `LOCKED`; khóa tạm do nhập sai là `LoginThrottle`.

## 3. `PasswordCredential`

| Thuộc tính | Ý nghĩa |
|---|---|
| `passwordHash` | Băm bằng thuật toán thích ứng; không bao giờ lưu hay ghi log bản rõ |
| `passwordChangedAt` | Lần đặt/đổi mật khẩu gần nhất |

## 4. `Profile`

| Thuộc tính | Ràng buộc |
|---|---|
| `displayName` | Bắt buộc, 1-150 ký tự sau khi cắt khoảng trắng |
| `phoneNumber` | Tùy chọn; chỉ chữ số, dấu `+` ở đầu, 8-15 chữ số; là dữ liệu cá nhân, không ghi log |
| `avatarFileId` | Tùy chọn; cột `avatar_file_id`, mã tệp Google Drive do `AvatarPort` (U03) xác nhận; U01 không lưu byte ảnh |

## 5. `LoginThrottle`

| Thuộc tính | Ý nghĩa |
|---|---|
| `failedLoginCount` | Số lần sai liên tiếp |
| `lockedUntil` | Mốc tự mở khóa; rỗng nếu không khóa |

## 6. `CreditBalance`

| Thuộc tính | Ý nghĩa |
|---|---|
| `freeBalance` | Credit tặng còn lại trong tháng |
| `freePeriod` | Tháng của phần tặng (`yyyy-MM`) |
| `purchasedBalance` | Credit đã mua còn lại |

U07 tạo/ghi ví nghiệp vụ cho mọi tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` hoặc `ADMIN`, khóa dòng tài khoản khi giữ/trừ credit (phần giữ/trừ nằm trên `ai_suggestions`, không có sổ cái). Cả bốn vai trò được tặng credit hằng tháng và mua credit; Student chỉ dùng credit cho AI chấm Practice Text/Diagram Essay hợp lệ. Quy tắc nghiệp vụ ở U07.

## 7. Cấu hình U01

Không có bảng cấu hình (database chỉ gồm bảng của ERD). Tên miền email trường là biến triển khai `U01_ALLOWED_EMAIL_DOMAINS` (BR-U01-03).

## 8. `OtpChallenge`

| Thuộc tính | Ràng buộc |
|---|---|
| `purpose` | `ACTIVATION` hoặc `PASSWORD_RESET` |
| `accountId` | Tài khoản đích |
| `codeHash` | Băm của mã 6 chữ số; không lưu bản rõ |
| `expiresAt` | Tạo lúc + 10 phút |
| `attemptsLeft` | Bắt đầu 5; về 0 thì xóa challenge |

Mỗi `(accountId, purpose)` có tối đa **một** challenge còn hiệu lực. Tạo mã mới xóa mã cũ.

### `VerificationTicket`

| Thuộc tính | Ràng buộc |
|---|---|
| `kind` | `OTP_ACTIVATION` hoặc `OTP_PASSWORD_RESET` (cấp sau khi OTP đúng, 10 phút) |
| `accountId` | Tài khoản đích |
| `tokenHash` | Băm của ticket ngẫu nhiên trả cho client |

Dùng một lần; xóa khi đặt mật khẩu thành công (BR-U01-28).

## 9. `Session`

| Thuộc tính | Ý nghĩa |
|---|---|
| `sessionId` | Định danh phiên |
| `accountId` | Chủ phiên |
| `credentialVersion` | Version lúc tạo; lệch với `Account.credentialVersion` thì không làm mới được |
| `refreshTokenHash` | Băm refresh token; phát hiện dùng lại thì thu hồi phiên |
| `issuedAt`, `lastUsedAt`, `expiresAt` | Idle 2 giờ tính từ `lastUsedAt`, tối đa 7 ngày tính từ `issuedAt`. Access token JWT sống 15 phút, không lưu phía server |

## 10. `RequestThrottle`

Bộ đếm theo `(hành động, email chuẩn hóa)` và `(hành động, client)` cho: đăng nhập, yêu cầu OTP kích hoạt, yêu cầu OTP đặt lại. Ngưỡng cụ thể ở NFR Requirements.

## 11. `AccountImportResult`

Không có use case xem lại lịch sử nhập nên kết quả không lưu. Bước kiểm tra trả kết quả ngay trong response; bước xác nhận gửi lại cùng file, hệ thống kiểm lại rồi tạo các dòng hợp lệ trong một transaction. Nhập lại cùng file không tạo trùng vì email đã tồn tại bị báo lỗi dòng (BR-U01-82). Mỗi lần xác nhận ghi một sự kiện audit (người nhập, checksum file, số dòng tạo/lỗi).

| Thuộc tính | Ý nghĩa |
|---|---|
| `fileChecksum` | SHA-256 của file, ghi vào audit |
| `rows` | Danh sách `ImportRowResult` |
| `createdCount`, `rejectedCount` | Tổng kết |

`ImportRowResult`:

| Thuộc tính | Ý nghĩa |
|---|---|
| `rowNumber` | Số dòng trong CSV |
| `schoolEmail`, `displayName`, `role` | Dữ liệu đọc được |
| `outcome` | `CREATED`, `REJECTED` |
| `reason` | Mã lỗi: `EMAIL_EXISTS`, `EMAIL_DOMAIN_NOT_ALLOWED`, `INVALID_EMAIL`, `INVALID_ROLE`, `DUPLICATE_IN_FILE`, `MISSING_FIELD` |

## 12. `AuthorizationDecision`

| Thuộc tính | Ý nghĩa |
|---|---|
| `allowed` | Có/không |
| `reasonCode` | Mã lý do an toàn cho log, không trả chi tiết cho client |
| `actorId`, `action`, `resourceRef` | Đầu vào đã đánh giá |

Phạm vi môn/lớp đến từ `SubjectScopePort`, `ClassScopePort` (U04 cài). U01 kết hợp role và phạm vi để ra quyết định.

## 13. Contract

### Port U01 cung cấp

| Port | Dùng bởi | Ghi chú |
|---|---|---|
| `AuthorizationPort.authorize(actor, action, resourceRef)` | Mọi unit | Mặc định từ chối; kết hợp role và phạm vi U04 |
| `AccountLookupPort` | U04, U16 | Tìm người học theo email/tên (≤ 20 kết quả), tra theo danh sách email, lấy email/tên hiển thị/role/trạng thái; `countByRoleAndStatus()` trả số đếm cho UC 18 View Statistics (U16); không trả mật khẩu hay số điện thoại |

### Port U01 dùng

| Port | Cung cấp bởi | Cạnh | Dùng để |
|---|---|---|---|
| `AuditPort.record` | U02 | `H` | Ghi sự kiện bảo mật/nghiệp vụ (U02 code trước U01) |
| `JobPort.enqueue` | U03 | `H` | Gửi việc `OTP_DELIVERY` sang RabbitMQ sau commit (không có bảng job); handler gửi mail do U01 sở hữu, chạy ở worker, retry hữu hạn |
| `AvatarPort` | U03 (chữ ký theo thiết kế U01) | `H` (U03 code trước) | Xác nhận ảnh thuộc người dùng, đúng mục đích `AVATAR` |
| `SubjectScopePort`, `ClassScopePort` | U01 khai báo, U04 cài | `C` (U04 code sau U01) | Đọc phạm vi phân công khi quyết định quyền và khi chặn hạ role |

U03 và U02 code trước U01; chỉ phạm vi môn/lớp (U04, code sau) dùng adapter tạm (xem code generation plan).

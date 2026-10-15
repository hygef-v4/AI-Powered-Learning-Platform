# U01 Account & Access — domain-entities

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U01 Account & Access thuộc Iter 1, người code: Người 1; phạm vi UC 01–12 giữ như U01 cũ, chỉ đổi liên kết sang các unit mới. 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u01-account-and-access` là thiết kế của U01 hiện hành.

Thiết kế độc lập công nghệ. Kiểu dữ liệu ghi ở mức nghiệp vụ; kiểu cột cuối cùng chốt ở Code Generation. Truy vết: `US-IAM-001`…`US-IAM-007`; UC 01–07, 08–12 theo `docs/usecase_table.md`.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `Account` | Aggregate root | `accounts` | U01 |
| `PasswordCredential` | Value object của `Account` | `accounts` | U01 |
| `Profile` | Value object của `Account` | `accounts` | U01 |
| `LoginThrottle` | Value object của `Account` | `accounts` | U01 |
| `EmailPreferences` | Value object của `Account` | `accounts.email_preferences` | U14 |
| `OtpChallenge` | Entity tạm thời | Redis | U01 |
| `VerificationTicket` | Entity tạm thời (`otpTicket`) | Redis | U01 |
| `Session` | Entity tạm thời | Redis | U01 |
| `RequestThrottle` | Bộ đếm tạm thời | Redis | U01 |
| `AccountImportResult` | Kết quả trả về | Không lưu | U01 |
| `AuthorizationDecision` | Kết quả trả về | Không lưu | U01 |

U01 **không** sở hữu: phân công Chủ nhiệm môn (U04), lớp, giảng viên và ghi danh (U05), audit (U02), quota, usage và AI connection secrets (U06).

## 2. `Account`

| Thuộc tính | Ý nghĩa | Ràng buộc |
|---|---|---|
| `accountId` | Định danh | Bất biến |
| `schoolEmail` | Email trường, dùng đăng nhập | Bắt buộc; chuẩn hóa chữ thường, bỏ khoảng trắng; duy nhất toàn hệ thống; thuộc tên miền trong biến triển khai `U01_ALLOWED_EMAIL_DOMAINS`; **không đổi sau khi tạo** |
| `role` | Vai trò cao nhất | `STUDENT`, `TEACHER`, `SUBJECT_MANAGER`, `ADMIN`; `ADMIN` không nhận phân công môn/lớp (BR-U01-60) |
| `status` | Trạng thái vòng đời | `PENDING`, `ACTIVE`, `DISABLED` |
| `credential` | `PasswordCredential` | Rỗng khi `PENDING` |
| `profile` | `Profile` | Bắt buộc có `displayName` |
| `throttle` | `LoginThrottle` | Mặc định không khóa |
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

## 5. `LoginThrottle`

| Thuộc tính | Ý nghĩa |
|---|---|
| `failedLoginCount` | Số lần sai liên tiếp |
| `lockedUntil` | Mốc tự mở khóa; rỗng nếu không khóa |

## 6. AI access boundary

Không số dư credit hoặc ví trong Account. U06 lưu quota educator, usage và bảo vệ kết nối cá nhân/tổ chức. Student không shared quota. Admin được cấu hình/test synthetic API chung, không gọi AI học vụ.

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
| `sessionId` | Định danh phiên (`sid`); access token mang mã này, xóa phiên là thu hồi |
| `accountId` | Chủ phiên |
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

Phạm vi môn đến từ `SubjectScopePort` (U04 cài), phạm vi lớp và ghi danh từ `ClassScopePort` (U05 cài). U01 kết hợp role và phạm vi để ra quyết định.

## 13. Contract

### Port U01 cung cấp

| Port | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `AuthorizationPort` | `authorize(actor, action, resourceRef)` | Mọi unit; tool MCP của U06 | Quyết định quyền phía server, mặc định từ chối: kiểm token, phiên `sid` còn, trạng thái `ACTIVE`, role, rồi phạm vi môn/lớp qua U04/U05 (F13) |
| `AccountLookupPort` | `findStudents(query, limit)`, `findAccounts(query, roles, limit)` (thêm mới), `findByEmails(emails)`, `getContact(accountId)`, `countByRoleAndStatus()` | U02 (email người thực hiện trên Audit Log), U04 (chọn Chủ nhiệm môn), U05 (thêm sinh viên, chọn giảng viên), U14 (địa chỉ nhận email), U15 (số đếm Admin Dashboard) | Tra thông tin liên hệ tối thiểu (email, tên hiển thị, role, trạng thái); không trả mật khẩu, số điện thoại; tối đa 20 kết quả mỗi lần tìm |
| `MailPort` | `send(MailMessage)` | U01 (OTP), U14 (email thông báo) | Gửi email qua SMTP (Brevo ở demo/production, Mailpit ở local/test) bằng `SmtpMailAdapter`, có timeout |

### Port U01 khai báo, unit khác cài

| Port | Phương thức | Cài bởi | Dùng để |
|---|---|---|---|
| `SubjectScopePort` | `isSubjectManager(accountId, subjectId)`, `listManagedSubjects(accountId)` | U04 | Kiểm phạm vi R2 khi phân quyền; chặn đổi role của người đang là Chủ nhiệm môn (BR-U01-64) |
| `ClassScopePort` | `isTeacherOf(accountId, classId)`, `isEnrolledStudent(accountId, classId)` (thêm mới), `subjectOfClass(classId)`, `listTaughtClasses(accountId)` | U05 | Kiểm phạm vi R3/R4 và R5 khi phân quyền; chặn đổi role của người đang dạy lớp (BR-U01-64) |

### Contract frontend U01 khai báo

| Contract | Cài bởi | Dùng để |
|---|---|---|
| `AccountMenuEntry` | U06 | Thêm mục AI Connection vào menu tài khoản mà U01 không phụ thuộc ngược U06 |
| `HeaderWidget` | U14 | Gắn chuông thông báo lên thanh điều hướng cho mọi vai trò |

### Port U01 dùng

| Port | Cung cấp bởi | Dùng để |
|---|---|---|
| `AuditPort.record` | U02 | Ghi sự kiện bảo mật và nghiệp vụ (mục 7 của business-logic-model) |
| `JobPort.enqueue` | U03 | Gửi việc `OTP_DELIVERY` sang worker qua RabbitMQ sau commit; handler gửi mail do U01 sở hữu, retry hữu hạn |

U02 và U03 cùng ở Iter 1 và được code trước hoặc song song với U01. U04 và U05 code sau U01 trong Iter 1: tới lúc đó U01 dùng adapter tạm trả "không có phạm vi" (fail closed), nên mọi hành động cần phạm vi môn/lớp bị từ chối cho tới khi nối adapter thật. Lớp `academics/application/ScopeQueryService` hiện có (code U04 cũ) đã cài hai port này và sẽ được U04/U05 tách ra.

## 14. API tài khoản và phiên

| API | UC | Ghi chú |
|---|---|---|
| `POST /api/v1/auth/login` | 02 | Trả `SessionInfo { accountId, displayName, role, homePath }`; `homePath` theo BR-U01-48: Class Dashboard, Manager Dashboard hoặc Admin Dashboard |
| `GET`, `PATCH /api/v1/me/profile` | 06, 07 | Chỉ `displayName`, `phoneNumber`; không có avatar |
| `GET /api/v1/admin/accounts` | 08 | Lọc role, trạng thái, email; phân trang |
| `POST /api/v1/admin/accounts` | 09 | Tạo một tài khoản `PENDING` |
| `GET /api/v1/admin/accounts/{id}` | 10 | Không trả hash, OTP, token |
| `PATCH /api/v1/admin/accounts/{id}` | 11 | `displayName`, `phoneNumber`, `version`; không email/role/status |
| `PUT /api/v1/admin/accounts/{id}/role` | 11 | Đổi role theo F11 |
| `PUT /api/v1/admin/accounts/{id}/status` | 12 | Vô hiệu hóa hoặc mở lại theo F12 |
| `POST /api/v1/admin/accounts/imports:validate`, `imports:commit` | Luồng phụ của 09 | Kiểm rồi xác nhận nhập CSV theo F10 |

Không có API ảnh đại diện. Cột avatar cũ nếu đã tồn tại có thể giữ tương thích, không đưa vào DTO; không tự xóa dữ liệu/schema đang có.

## Quy tắc tích hợp revision hiện hành

- Tài khoản trường cấp, không public registration. Admin tạo/import không gửi OTP hoặc đặt mật khẩu; người dùng yêu cầu activation ở login đầu và tự đặt mật khẩu sau OTP.
- Account role cao nhất trong STUDENT/TEACHER/SUBJECT_MANAGER/ADMIN; authorization theo action/resource/current state, không chỉ role JWT. Admin chỉ quản trị, không học vụ.
- Profile cá nhân chỉ displayName/phoneNumber, email định danh/role/status bất biến trong /me. Reset không tiết lộ account tồn tại.
- MCP credentials xác thực actor qua U06 và cùng AuthorizationPort; không dùng provider API key làm ALAP authentication. Student không shared quota.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Tạo/import không gửi OTP, activation hợp lệ/sai/hết hạn/rate limit; recovery không lộ account.
- Đăng nhập/đăng xuất/phiên revoked; R1 own profile; Admin bị chặn học vụ; Student shared quota bị từ chối.

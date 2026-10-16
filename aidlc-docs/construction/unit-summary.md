# Tổng hợp 15 unit

Gom từ `functional-design/domain-entities.md` (port, contract unit cung cấp; không có port unit dùng) và `functional-design/business-logic-model.md` (business flow) của từng unit, bản 2026-10-15. Chưa code, chưa kiểm runtime.

## Iter 1: khung web

| Unit | Người | UC | LOC (nghìn) | Làm gì |
|---|---|---|---|---|
| U01 Account & Access | Người 1 | 01–12 | 4 | Đăng nhập, kích hoạt bằng OTP, quên và đổi mật khẩu, hồ sơ, Admin quản lý tài khoản (tạo, nhập CSV, đổi role, khóa), phân quyền R1–R5 |
| U02 Audit | Người 2 | 13 | 1 | Ghi audit chỉ thêm, Admin tra cứu Audit Log |
| U03 Job and Settings | Người 3 | 14–15 | 3 | Tệp riêng tư trên Drive, việc nền qua RabbitMQ, sự kiện, Settings, backup mã hóa lên Drive |
| U04 Subject Administration | Người 4 | 16–19 | 2 | Admin quản lý môn, gán Chủ nhiệm môn, lưu trữ môn |
| U05 Class & Enrollment | Người 5 | 20–30 | 4 | Chủ nhiệm môn tạo lớp, gán giảng viên, ghi danh; Class Dashboard, Manager Dashboard, trang lớp cho từng vai trò |

### U01 Account & Access

Iter 1, Người 1, UC 01–12. Thiết kế: [u01-account-and-access](u01-account-and-access/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U01 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IAuthorization` | `authorize(actor, action, resourceRef)` | Mọi unit; tool MCP của U06 | Quyết định quyền phía server, mặc định từ chối: kiểm token, phiên `sid` còn, trạng thái `ACTIVE`, role, rồi phạm vi môn/lớp qua U04/U05 (F13) |
| `IAccountLookup` | `findStudents(query, limit)`, `findAccounts(query, roles, limit)` (thêm mới), `findByEmails(emails)`, `getContact(accountId)`, `countByRoleAndStatus()` | U02 (email người thực hiện trên Audit Log), U04 (chọn Chủ nhiệm môn), U05 (thêm sinh viên, chọn giảng viên), U14 (địa chỉ nhận email), U15 (số đếm Admin Dashboard) | Tra thông tin liên hệ tối thiểu (email, tên hiển thị, role, trạng thái); không trả mật khẩu, số điện thoại; tối đa 20 kết quả mỗi lần tìm |
| `IMail` | `send(MailMessage)` | U01 (OTP), U14 (email thông báo) | Gửi email qua SMTP (Brevo ở demo/production, Mailpit ở local/test) bằng `SmtpMailAdapter`, có timeout |

##### Interface U01 khai báo, unit khác cài

| Interface | Phương thức | Cài bởi | Dùng để |
|---|---|---|---|
| `ISubjectScope` | `isSubjectManager(accountId, subjectId)`, `listManagedSubjects(accountId)` | U04 | Kiểm phạm vi R2 khi phân quyền; chặn đổi role của người đang là Chủ nhiệm môn (BR-U01-64) |
| `IClassScope` | `isTeacherOf(accountId, classId)`, `isEnrolledStudent(accountId, classId)` (thêm mới), `subjectOfClass(classId)`, `listTaughtClasses(accountId)` | U05 | Kiểm phạm vi R3/R4 và R5 khi phân quyền; chặn đổi role của người đang dạy lớp (BR-U01-64) |

##### Contract frontend U01 khai báo

| Contract | Cài bởi | Dùng để |
|---|---|---|
| `AccountMenuEntry` | U06 | Thêm mục AI Connection vào menu tài khoản mà U01 không phụ thuộc ngược U06 |
| `HeaderWidget` | U14 | Gắn chuông thông báo lên thanh điều hướng cho mọi vai trò |

#### Business flow

##### F1 - Yêu cầu kích hoạt

**Vào**: `schoolEmail`, thông tin client.

1. Chuẩn hóa email (BR-U01-02).
2. Kiểm `RequestThrottle` theo email và client. Vượt ngưỡng → sang bước 6 (BR-U01-25).
3. Tìm tài khoản. Không có, ngoài domain, hoặc không ở `PENDING` → sang bước 6 (BR-U01-12).
4. Gửi việc `OTP_DELIVERY(ACTIVATION)` qua `IJob.enqueue` (yêu cầu không ghi database nên message gửi ngay, BR-U03-50).
5. Worker sinh `OtpChallenge`, xóa challenge cũ, lưu băm và gửi mail (BR-U01-20, 21, 23). Mã rõ không đi qua queue hay DB.
6. Trả **`Accepted`** với cùng một thông báo cho mọi trường hợp.

**Ra**: phản hồi trung tính. Email chỉ được gửi ở bước 5.

##### F2 - Hoàn tất kích hoạt

**Bước 2a — Xác minh OTP. Vào**: `schoolEmail`, `otpCode`.

1. Tìm challenge `ACTIVATION` còn hiệu lực. Không có → lỗi "mã không hợp lệ hoặc đã hết hạn".
2. So khớp mã. Sai → giảm `attemptsLeft`; về 0 thì xóa challenge (BR-U01-22). Trả lỗi như bước 1.
3. Đúng → xóa challenge, cấp `otpTicket` (BR-U01-24, 28); màn hình chuyển sang bước nhập mật khẩu mới.

**Bước 2b — Đặt mật khẩu. Vào**: `otpTicket`, `newPassword`.

4. Kiểm ticket còn hạn, đúng mục đích `ACTIVATION`. Không hợp lệ → lỗi "phiên xác minh đã hết hạn, hãy yêu cầu mã mới".
5. Kiểm mật khẩu mới (BR-U01-30, 31). Không đạt → lỗi chính sách, ticket vẫn giữ để nhập lại.
6. Băm mật khẩu, lưu `credential`, chuyển `ACTIVE`, xóa ticket (BR-U01-13, 32).
7. Ghi audit `ACCOUNT_ACTIVATED`.
8. Tự đăng nhập: xóa `failedLoginCount`, tạo phiên mới, cấp access token 15 phút như F3 bước 7 (BR-U01-13, 44).

**Ra**: phiên đăng nhập; người dùng qua Post-Login vào thẳng dashboard theo role (F15, BR-U01-48).

##### F3 - Đăng nhập

**Vào**: `schoolEmail`, `password`, thông tin client.

1. Kiểm giới hạn theo client (BR-U01-43). Vượt → lỗi trung tính.
2. Tìm tài khoản. Không có → so khớp giả với hash mẫu để thời gian phản hồi đồng đều, trả lỗi trung tính.
3. `lockedUntil` còn hiệu lực → lỗi trung tính, ghi audit `LOGIN_BLOCKED_LOCKED` (BR-U01-41).
4. Trạng thái khác `ACTIVE` → lỗi trung tính (BR-U01-40).
5. Sai mật khẩu → tăng `failedLoginCount`; chạm 5 thì đặt `lockedUntil` + 15 phút và ghi audit `ACCOUNT_TEMP_LOCKED`. Trả lỗi trung tính, ghi audit `LOGIN_FAILED`.
6. Đúng → xóa `failedLoginCount`, `lockedUntil`.
7. Tạo phiên mới (refresh token lưu băm trên Redis), cấp access token 15 phút mang mã phiên `sid`. Trả phiên kèm `homePath` theo role (BR-U01-44, 48; F15). Không có bước MFA (BR-U01-47).

**Ra**: phiên đăng nhập hoặc lỗi trung tính **giống nhau** cho mọi nguyên nhân thất bại.

##### F4 - Đăng xuất

Nút Logout nằm trong khung sau đăng nhập (Post-Login), hỏi xác nhận rồi gọi API.

1. Xóa `Session` hiện tại (BR-U01-45).
2. Phiên khác của người dùng giữ nguyên.
3. Xóa cookie, chuyển về User Login.

##### F15 - Điều hướng sau đăng nhập (Post-Login)

1. Đăng nhập (F3) hoặc kích hoạt (F2) thành công trả `homePath` theo role (BR-U01-48): Student, Teacher → Class Dashboard; Subject Manager → Manager Dashboard; Admin → Admin Dashboard.
2. Khung sau đăng nhập có lối vào User Profile (F7) và Logout (F4); Subject Manager có thêm nút sang Class Dashboard để vào lớp mình được giao dạy.
3. Admin không vào Manager Dashboard hay Class Dashboard: frontend ẩn lối vào và chặn route; backend từ chối mọi hành động cần phân công môn/lớp của Admin (F13, BR-U01-60).
4. Mở lại trang hoặc vào thẳng một URL: frontend đọc role từ `GET /me/profile` rồi áp cùng quy tắc.

##### F5 - Đặt lại mật khẩu (Reset Password)

Một màn Password Reset gồm ba bước: nhập email → nhập OTP → đặt mật khẩu mới.

**Vào**: `schoolEmail`.

1. Chuẩn hóa, kiểm `RequestThrottle`.
2. Chỉ khi tài khoản `ACTIVE`: gửi việc `OTP_DELIVERY(PASSWORD_RESET)` qua `IJob.enqueue`; worker sinh mã và gửi mail như F1.
3. Luôn trả `Accepted` trung tính (US-IAM-003 S1).
4. Người dùng nhập `otpCode`: xác minh như F2 bước 1-3 (mục đích `PASSWORD_RESET`), nhận `otpTicket`.
5. Người dùng nhập `newPassword` kèm ticket: kiểm như F2 bước 4-5.
6. Lưu mật khẩu mới, xóa **mọi phiên** của tài khoản (BR-U01-34), xóa `failedLoginCount` và `lockedUntil`, ghi audit `PASSWORD_RESET`.

##### F6 - Đổi mật khẩu

Màn Password Change, mở từ User Profile.

**Vào**: phiên hợp lệ, `currentPassword`, `newPassword`.

1. Sai mật khẩu hiện tại → từ chối, không đổi gì (US-IAM-006 S2).
2. Kiểm chính sách (BR-U01-30, 31).
3. Lưu, xóa mọi phiên khác của tài khoản, giữ phiên hiện tại (BR-U01-34).
4. Ghi audit `PASSWORD_CHANGED`.

##### F7 - Xem và sửa hồ sơ

Màn User Profile, mở từ khung sau đăng nhập; hiện email, tên, số điện thoại, role, trạng thái của chính mình (UC 06) và cho sửa tên, số điện thoại (UC 07).

1. Chỉ thao tác trên `accountId` của phiên; định danh khác bị từ chối (BR-U01-54).
2. Sửa `displayName`, `phoneNumber` theo ràng buộc; email, role, trạng thái không sửa được (BR-U01-51).
3. DTO chỉ nhận tên/số điện thoại; avatar/email/role/status hoặc ID người khác bị từ chối trước ghi.
4. Ghi audit `PROFILE_UPDATED` với tên trường đã đổi, không ghi giá trị số điện thoại.

Mọi màn quản trị tài khoản mở từ Admin Dashboard và chỉ dành cho `ADMIN`.

##### F8 - Xem danh sách tài khoản (Account List)

Admin lọc theo role, trạng thái, email; chọn một dòng mở Account Detail (F14). Kết quả không có hash, OTP hay token.

##### F9 - Tạo tài khoản (New Account)

Màn New Account mở từ nút "Thêm tài khoản" trên Account List; tạo xong quay về Account List.

**Vào**: `schoolEmail`, `displayName`, `role`.

1. Kiểm quyền admin (BR-U01-67).
2. Kiểm email (BR-U01-02, 03). Trùng → lỗi rõ cho admin.
3. Tạo tài khoản `PENDING`, không mật khẩu, **không gửi mail** (BR-U01-10).
4. Tạo `ADMIN` bắt buộc qua luồng này, không qua nhập hàng loạt (BR-U01-84).
5. Ghi audit `ACCOUNT_CREATED`.

##### F10 - Nhập hàng loạt (luồng phụ của UC 09)

Nút "Nhập CSV" trên Account List; giữ theo quyết định của người dùng ngày 2026-10-09.

1. Kiểm quyền admin; kiểm file là CSV, ≤ 1000 dòng (BR-U01-80).
2. Kiểm từng dòng, trả `AccountImportResult` trong response, không lưu (BR-U01-82, 84).
3. Admin xem kết quả và xác nhận: frontend gửi lại cùng file.
4. Kiểm lại toàn bộ file (dữ liệu có thể đã đổi), tạo các dòng hợp lệ ở `PENDING` trong một transaction, không gửi mail (BR-U01-85). Email đã tồn tại (kể cả do lần nhập trước) báo lỗi dòng nên nhập lại cùng file không tạo trùng (BR-U01-83).
5. Ghi một audit `ACCOUNTS_IMPORTED` kèm checksum file, số dòng tạo và bị từ chối.

##### F11 - Đổi role (UC 11, trên Account Detail)

**Vào**: `accountId` đích, `newRole`.

1. Kiểm quyền admin.
2. Đích là chính mình và là hạ quyền → từ chối (BR-U01-65).
3. Đích là `ADMIN` đang hoạt động cuối cùng và role mới khác `ADMIN` → từ chối (BR-U01-66).
4. Role mới không giữ được phân công hiện có của đích (hỏi U04 về môn, U05 về lớp) → từ chối, nêu môn/lớp (BR-U01-64). Ví dụ: hạ Subject Manager đang quản lý môn, hạ Teacher đang dạy lớp, hoặc đổi người đang phụ trách môn/lớp sang `ADMIN`.
5. Lưu role, xóa mọi phiên của đích (BR-U01-63).
6. Ghi audit `ROLE_CHANGED` với giá trị trước/sau.

##### F12 - Vô hiệu hóa và mở lại (UC 12, trên Account Detail)

**Vô hiệu hóa**:
1. Kiểm quyền; áp BR-U01-65, 66 như F11.
2. Chuyển `DISABLED`, xóa mọi phiên, xóa OTP còn hiệu lực (BR-U01-71).
3. Ghi audit `ACCOUNT_DISABLED`.

**Mở lại**:
1. Có mật khẩu → `ACTIVE`; chưa có → `PENDING` (BR-U01-72).
2. Xóa `failedLoginCount`, `lockedUntil`.
3. Ghi audit `ACCOUNT_ENABLED`.

##### F14 - Xem chi tiết và sửa thông tin tài khoản (UC 10, 11)

1. ADMIN mở Account Detail từ Account List; GET theo ID trả email, tên hiển thị, số điện thoại, role, trạng thái, ngày tạo, `version`; không có hash, OTP hay token (UC 10).
2. Sửa `displayName`, `phoneNumber` bằng PATCH kèm `version`; version lệch → `409`, tải lại rồi sửa. Email không đổi được; role đổi theo F11, trạng thái theo F12; Admin không đặt mật khẩu hay gửi OTP (UC 11).
3. Ghi audit `ACCOUNT_UPDATED` với actor, thời điểm, tên trường đã đổi; không ghi giá trị số điện thoại.

##### F13 - `authorize(actor, action, resourceRef)`

1. Access token không hợp lệ, hết hạn, hoặc phiên `sid` của nó đã bị xóa → từ chối (BR-U01-44). Actor từ MCP do U06 dựng từ credential MCP còn hiệu lực (U06 tự kiểm hạn và thu hồi); các bước sau áp như với access token.
2. Tài khoản không `ACTIVE` → từ chối.
3. Role không có quyền với `action` → từ chối.
4. `action` cần phạm vi → hỏi U04 (`ISubjectScope`: actor có là Chủ nhiệm của môn) hoặc U05 (`IClassScope`: actor có dạy hoặc đang ghi danh vào lớp) chứa `resourceRef`. Không → từ chối.
5. Không gọi được U04/U05 → **từ chối** (BR-U01-93).
6. Trả `AuthorizationDecision`. Mọi lần từ chối trên hành động nhạy cảm ghi audit `ACCESS_DENIED`.

Quy tắc theo role (BR-U01-60):

| Role | Được dùng |
|---|---|
| `STUDENT` | Chức năng User và lớp mình đã ghi danh (R5) |
| `TEACHER` | Chức năng User và lớp mình được giao dạy (R3) |
| `SUBJECT_MANAGER` | Chức năng Teacher cho lớp được giao dạy (R4) và tài nguyên của môn được giao quản lý (R2) |
| `ADMIN` | Chức năng User và chức năng quản trị: tài khoản, môn, Settings/quota, API chung (synthetic test), backup Dashboard, audit. Không dùng chức năng Subject Manager/Teacher, không nhận phân công môn/lớp |

Phạm vi môn/lớp đọc ở backend tại thời điểm request, không suy từ role hoặc dữ liệu cache cũ.

### U02 Audit

Iter 1, Người 2, UC 13. Thiết kế: [u02-audit-job-and-outbox](u02-audit-job-and-outbox/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U02 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IAudit` | `record(AuditEvent)` | Mọi unit, cả handler ở worker | Ghi audit, chọn transaction theo `result`: `SUCCESS` ghi trong transaction của unit gọi (nghiệp vụ rollback thì mất theo); `DENIED`, `FAILURE` ghi bằng transaction riêng để còn lại khi nghiệp vụ rollback (BR-U02-02) |
| `IAuditQuery` | `query(actor, AuditFilter, PageRequest)` → `Page<AuditLogView>` | API `GET /api/v1/admin/audit-logs` của chính U02 (màn Audit Log, UC 13) | Tra cứu cho Admin; mỗi dòng kèm email người thực hiện lấy lúc đọc |

Kiểu dữ liệu đi kèm: `AuditEvent` (`id` do unit gọi tạo, `actorId`, `action`, `objectType`, `objectId`, `result`, `reason`, `details` đã che, `occurredAt`), `AuditResult` (`SUCCESS`, `DENIED`, `FAILURE`), `AuditFilter`, `AuditLogView` (`AuditEvent` + `actorEmail`, rỗng nghĩa là hệ thống).

#### Business flow

##### A1 - Ghi audit
1. Trong transaction nghiệp vụ, unit gọi `IAudit.record(event)` với `id` mới, `occurred_at` là thời điểm thao tác.
2. U02 kiểm danh sách khóa cấm trong `details` (BR-U02-05); vi phạm thì từ chối (lỗi lập trình, thao tác rollback).
3. `result = SUCCESS`: INSERT vào `audit_logs` trong cùng transaction; `id` đã tồn tại thì bỏ qua (BR-U02-02, 03).
4. `result = DENIED` hoặc `FAILURE`: cũng gọi `record`, U02 tự ghi bằng transaction riêng (`REQUIRES_NEW`) để còn lại dù thao tác chính rollback (BR-U02-02).

##### A2 - Tra cứu audit
1. Admin mở màn Audit Log từ Admin Dashboard; bộ lọc mặc định 7 ngày gần nhất.
2. Lọc theo người thực hiện: Admin nhập email, frontend tra `accountId` bằng API danh sách tài khoản của U01 rồi gửi `actorId` (BR-U02-08).
3. Gọi U01 `authorize(actor, AUDIT_READ)`. Không phải `ADMIN` hoặc U01 lỗi → từ chối (BR-U02-07, 50).
4. Kiểm bộ lọc hợp lệ (`from` ≤ `to`, `result` đúng giá trị), trang ≤ 100.
5. Truy vấn, sắp xếp `occurred_at` giảm dần.
6. Lấy email của các người thực hiện trong trang qua `IAccountLookup.getContact` (U01); `actor_id` rỗng hiện là hệ thống (BR-U02-09).
7. Ghi audit `AUDIT_QUERIED` với bộ lọc đã dùng.

### U03 Job and Settings

Iter 1, Người 3, UC 14–15. Thiết kế: [u03-job-and-settings](u03-job-and-settings/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U03 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IArtifact` | `store(actor, purpose, file)` → `FileRef`; `attach(fileRef, actor, purpose)` → `StoredFileInfo`; `issueDownloadToken(fileId, accountId)` → `DownloadToken`; `open(fileId)` → `InputStream` | U07, U08, U12 (ảnh trong tài liệu, nhúng ảnh khi xuất DOCX), U13 (học liệu) | Lưu tệp riêng tư, gắn tệp vào đối tượng, cấp link tải ngắn hạn sau khi unit sở hữu đã kiểm quyền, đọc tệp phía server |
| `IJob` | `enqueue(jobType, payload, idempotencyKey)` | U01, U03, U06, U12, U13, U14 | Gửi việc nền sau commit, không ghi database |
| `IEventPublisher` | `publish(DomainEvent)` | U03, U05, U07, U09, U10, U12, U14 | Phát sự kiện thông báo sau commit, không đảm bảo giao hàng |
| `ISettings` | `get(key)` (có kiểu) | U03, U08 | Đọc giá trị cài đặt hiện hành, cache 30 giây |
| `IBackupStatus` | `latest()`, `list(page)` | U15 | Trạng thái và lịch sử backup cho Admin Dashboard, không có dữ liệu tải về |

##### Contract U03 cung cấp (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `IJobHandler` | `jobType()`, `queue()`, `handle(message)`, `onFailed(message, error)`, `timeout()` | U01, U03, U06, U12, U13, U14 | Xử lý một loại việc nền trong worker và chuyển dòng nghiệp vụ sang lỗi khi hết lượt |
| `IPendingSweeper` | `jobType()`, `findStalePending(olderThan)` | U03, U06, U12, U13, U14 | Trả các dòng còn chờ quá 5 phút để U03 gửi lại message bị mất |
| `IScheduledScanner` | `name()`, `scan(now)` | U03 (lịch backup), U06 (giải phóng quota giữ quá hạn), U07 (mở, đóng bài theo lịch), U08 (tự nộp khi hết giờ, dọn dữ liệu hoạt động), U12 (tự nộp bài nhóm), U14 (nhắc hạn, gửi lại email hoãn) | Việc hẹn giờ chạy mỗi phút, cập nhật idempotent |
| `SettingDefinition` | khai báo `key`, nhóm, kiểu, mặc định, giới hạn | U03, U08 | Thêm mục cài đặt khóa–giá trị vào Settings |
| `ISettingsSectionProvider` | `sectionKey()`, `title()`, `summary()` | U06 | Thêm phần cài đặt do unit khác lưu (API AI chung, quota giảng viên) vào Setting List mà U03 không phụ thuộc ngược vào U06 |

#### Business flow

##### F1 - Upload
**Vào**: phiên đăng nhập, `purpose`, tệp multipart.
1. Kiểm role được upload theo `purpose` qua `IAuthorization` (BR-U03-04).
2. Lấy trần dung lượng và loại cho phép của `purpose` từ `ISettings`.
3. Đọc stream ra thư mục tạm; vượt trần thì dừng ngay và trả `413` (BR-U03-02).
4. Xác định loại từ magic bytes, đối chiếu danh sách cho phép; không khớp thì trả `415` và ghi audit (BR-U03-03, 40).
5. Tính SHA-256 (BR-U03-05).
6. Tải lên Drive vào thư mục của `purpose`, ghi thuộc tính tệp (`ownerAccountId`, `purpose`, `mediaType`, `originalFileName` đã làm sạch, `sha256`), nhận `fileId`.
7. Bước 6 lỗi: xóa tệp Drive nếu đã tạo, thử 3 lần; vẫn lỗi thì gửi việc `DRIVE_CLEANUP`; trả lỗi (BR-U03-06).
8. Xóa tệp tạm trong mọi trường hợp.
9. Trả `{ fileRef, mediaType, byteSize, originalFileName, expiresAt }`; không trả `fileId`, không ghi database.

##### F2 - Gắn vào đối tượng
1. Unit sở hữu (U13 với học liệu; U07, U08, U12 với ảnh trong tài liệu) gọi `IArtifact.attach(fileRef, actor, purpose)` trong transaction nghiệp vụ.
2. U03 kiểm chữ ký HMAC, hạn 1 giờ, người gắn là người đã tải lên, đúng `purpose` (BR-U03-30).
3. Trả `{ fileId, mediaType, byteSize, originalFileName }`; unit sở hữu lưu `fileId` vào dòng của mình.

##### F3 - Tải về
1. Frontend gọi API của unit sở hữu (ví dụ "xem học liệu"); unit đó tự kiểm quyền nghiệp vụ (BR-U03-20).
2. Unit sở hữu gọi `issueDownloadToken(fileId, accountId)`; U03 sinh token ngẫu nhiên 256 bit, lưu bản băm vào Redis 5 phút.
3. Frontend nhận URL `/api/v1/files/download/{token}` và mở.
4. Khi tải: tìm token, kiểm `accountId` khớp phiên; sai người thì trả `404` và ghi audit (BR-U03-21, 40).
5. Đọc thuộc tính, stream từ Drive với header an toàn; SVG kèm CSP sandbox (BR-U03-10, 22, 23).
6. Drive báo abuse thì ghi audit và trả `410` (BR-U03-24).

##### F4 - Đọc tệp phía server
1. Unit cần đọc nội dung tệp gọi `IArtifact.open(fileId)`: U07, U08, U12 nhúng ảnh khi xuất DOCX.
2. U03 trả stream từ Drive; abuse thì báo lỗi vĩnh viễn như F3.

##### F5 - Giới hạn tải lên cho frontend
1. Frontend gọi `GET /api/v1/files/policies` (người đã đăng nhập).
2. U03 đọc trần và loại cho phép của từng `purpose` qua `ISettings`.
3. `FileUploader` dùng kết quả để kiểm sơ bộ; backend vẫn kiểm lại ở F1.

##### J1 - Gửi việc nền
1. Unit gọi ghi dòng nghiệp vụ ở trạng thái chờ trong transaction của mình.
2. Gọi `IJob.enqueue(jobType, payload, idempotencyKey)`; U03 kiểm `jobType` đã đăng ký và payload không có khóa cấm (BR-U03-52).
3. Có transaction thì đăng ký gửi sau commit; không có thì gửi ngay (BR-U03-50).
4. Gửi `JobMessage` (`attempt = 0`) tới exchange `jobs`, routing key là `jobType`. Gửi lỗi thì log WARN; J3 gửi lại.

##### J2 - Worker xử lý việc nền
1. Listener của queue nhận `JobMessage`, gọi handler của unit sở hữu.
2. Handler đọc dòng nghiệp vụ; không còn chờ thì ack và bỏ (BR-U03-51).
3. Thành công: handler cập nhật dòng; worker ack (BR-U03-53).
4. Lỗi tạm và `attempt` < 5: gửi bản sao `attempt + 1` vào queue thử lại theo backoff, ack bản gốc.
5. Lỗi vĩnh viễn hoặc hết lượt: gọi `onFailed`, unit chuyển dòng sang lỗi, log ERROR, ack (BR-U03-56).
6. Quá timeout của loại việc thì coi như lỗi tạm.

##### J3 - Gửi lại việc bị mất (mỗi phút)
1. Với mỗi `IPendingSweeper` đã đăng ký, lấy các dòng còn chờ và cập nhật lần cuối quá 5 phút.
2. Gửi lại `JobMessage` cho từng dòng; handler idempotent nên trùng không gây hại (BR-U03-58, 59).

##### J4 - Việc hẹn giờ (mỗi phút)
1. Worker gọi lần lượt các `IScheduledScanner` đã đăng ký (BR-U03-60).
2. Mỗi scanner đọc mốc thời gian trên bảng của unit mình và cập nhật có điều kiện, nên chạy lại không gây trùng.
3. Một scanner lỗi không chặn scanner khác.

##### E1 - Phát sự kiện
1. Sau commit, unit gọi `IEventPublisher.publish(event)` với `schemaVersion`, `eventId`, `eventType`, `sourceUnit`.
2. U03 kiểm payload không có khóa cấm, gửi lên exchange `platform.events` với routing key `eventType`. Lỗi thì log WARN, không thử lại (BR-U03-70).
3. U14 nghe qua queue `jobs.notification` để tạo thông báo trong app và email.

##### S1 - Xem cài đặt (UC 14)
1. Admin mở Setting List từ Admin Dashboard; frontend gọi `GET /api/v1/admin/settings`.
2. Kiểm quyền qua `IAuthorization`; không phải `ADMIN` thì trả `403` và ghi audit bị từ chối (BR-U03-80).
3. Đọc `system_settings`, ghép với `SettingDefinition` để có tên, mô tả, giới hạn; nhóm theo Tệp, Backup, Hoạt động làm bài.
4. Với mỗi `ISettingsSectionProvider` đã đăng ký (API AI chung, quota giảng viên của U06), gọi `summary()` có timeout 1 giây; lỗi thì đánh dấu phần đó "không khả dụng" (BR-U03-89).
5. Trả danh sách nhóm: mỗi mục khóa–giá trị có giá trị hiện hành, người sửa và thời điểm sửa gần nhất, `version`; mỗi phần ủy quyền có tên và tóm tắt đã che.
6. Bấm một mục khóa–giá trị thì mở Setting Detail; frontend gọi `GET /api/v1/admin/settings/{key}` để lấy mô tả và giới hạn.
7. Bấm phần API AI chung hoặc quota thì mở panel do U06 cung cấp; panel gọi API của U06.

##### S2 - Sửa cài đặt (UC 15)
1. Trên Setting Detail, Admin nhập giá trị mới và lưu; frontend gọi `PATCH /api/v1/admin/settings/{key}` kèm `value`, `version`.
2. Kiểm quyền `ADMIN` (BR-U03-80).
3. Tìm `SettingDefinition`; không có thì trả `404`.
4. Kiểm kiểu, khoảng giá trị, tập cho phép; kiểm ràng buộc giữa các mục (ví dụ bật backup cần đã đặt giờ); sai thì trả `400` kèm lý do (BR-U03-82, 88).
5. Cập nhật có điều kiện `version` khớp; không khớp thì trả `409`, frontend báo tải lại.
6. Ghi audit `SETTING_UPDATED` với giá trị trước và sau trong cùng transaction (BR-U03-83).
7. Xóa cache của khóa ở instance hiện tại; instance khác nhận giá trị mới trong tối đa 30 giây (BR-U03-86).
8. Trả giá trị và `version` mới.
9. Sửa API AI chung hoặc quota đi qua API của U06; U03 không tham gia transaction đó (BR-U03-89).

##### S3 - Unit đọc cài đặt
1. Unit gọi `ISettings.get(key)` khi cần: U03 lúc upload và lúc chạy backup, U08 lúc dọn dữ liệu hoạt động.
2. Trả giá trị trong cache nếu chưa quá 30 giây; quá hạn thì đọc lại bảng.
3. Không đọc được database và không còn cache thì báo lỗi tạm thời.

##### S4 - Khởi tạo mục khi khởi động
1. Gom mọi `SettingDefinition` đã khai báo.
2. Mục chưa có trong `system_settings` thì thêm với giá trị mặc định (hoặc rỗng), `version` 1, chưa có người sửa.
3. Mục đã có thì giữ nguyên (BR-U03-84).

##### S5 - Đăng ký phần cài đặt ủy quyền
1. Unit có cài đặt không dạng khóa–giá trị cài `ISettingsSectionProvider` (`sectionKey`, `title`, `summary`) và đăng ký với U03 khi khởi động.
2. Frontend của unit đó đăng ký panel cho `sectionKey` vào registry của màn Settings.
3. Chưa có unit nào đăng ký thì Setting List không hiện phần đó (trước khi U06 hoàn tất ở Iter 2).

##### B1 - Lên lịch
1. `BackupScheduleScanner` chạy mỗi phút trong worker.
2. Đọc `backup.enabled`, `backup.dailyAt` qua `ISettings`; tắt hoặc chưa cấu hình thì bỏ qua.
3. Đến hoặc qua giờ chạy của hôm nay và chưa có lần chạy cho ngày đó thì thêm `backup_runs` trạng thái `CREATED` với khóa duy nhất là ngày chạy (chạy lại không tạo trùng).
4. Trong cùng transaction, gửi việc `BACKUP_RUN` qua `IJob` (queue `jobs.backup`).

##### B2 - Chạy backup
1. Handler nhận `BACKUP_RUN`, chuyển `CREATED` → `RUNNING` có điều kiện; dòng không còn `CREATED` thì bỏ (BR-U03-51).
2. Kiểm dung lượng đĩa tạm; không đủ thì `FAILED` ngay.
3. Chạy `pg_dump` ra tệp tạm (một snapshot nhất quán), ghi phiên bản schema (Flyway) và phiên bản ứng dụng.
4. Mã hóa dump (BR-U03-93), upload vào thư mục backup riêng, kiểm kích thước và checksum sau upload. Không chép tệp trên Drive (BR-U03-91).
5. Ghi `SUCCESS` với tham chiếu archive, dung lượng, thời điểm xong; ghi audit `BACKUP_COMPLETED` (BR-U03-92).
6. Chạy B3.
7. Xóa tệp tạm trong mọi trường hợp.

##### B3 - Giữ bản trong 7 ngày
1. Lấy các lần `SUCCESS` có `snapshot_at` cũ hơn 7 ngày; luôn chừa bản thành công mới nhất.
2. Xóa archive của các lần đó trên Drive; đánh dấu `purgedAt`, giữ dòng lịch sử (BR-U03-96).

##### B4 - Lỗi backup
1. Lỗi tạm (Drive 429/5xx, mạng): handler thử lại theo J2.
2. Lỗi vĩnh viễn hoặc hết lượt: `onFailed` chuyển `FAILED` kèm lỗi đã che, gửi `DRIVE_CLEANUP` để xóa phần upload dở của lần đó.
3. Ghi audit `BACKUP_FAILED`, phát sự kiện `backup.failed` sau commit; U14 gửi thông báo trong app và email cho Admin.
4. Bản thành công trước đó giữ nguyên (BR-U03-95).

##### B5 - Xem trạng thái và khôi phục
1. U15 gọi `IBackupStatus.latest()` và `list(page)` để hiện trên Admin Dashboard: lần thành công gần nhất, lịch sử, dung lượng, lỗi; không có nút tải (BR-U03-97).
2. Khôi phục do người vận hành làm theo runbook trong môi trường cô lập: tải archive, giải mã bằng khóa giữ ngoài VPS, `pg_restore` vào database mới.
3. Tệp trên Drive không được khôi phục: database trỏ tới tệp đang có trên Drive; tệp đã bị xóa sau thời điểm backup thì thiếu (BR-U03-91).
4. Kiểm theo danh sách: số dòng các bảng chính, quyền, tài liệu và XML sơ đồ, bài nộp và điểm, quota và usage, audit; đo RPO/RTO và lưu bằng chứng (BR-U03-98).

### U04 Subject Administration

Iter 1, Người 4, UC 16–19. Thiết kế: [u04-subject-administration](u04-subject-administration/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U04 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `ISubjectScope` (contract của U01, U04 cài) | `isSubjectManager(accountId, subjectId)`, `listManagedSubjects(accountId)` | U01 | Kiểm quyền R2 theo phân công hiện thời; chặn đổi role người đang là Chủ nhiệm môn |
| `ISubjectQuery` | `getSubject(subjectId)` → `SubjectRef`, `isActive(subjectId)`, `listManagedSubjects(accountId)` → `List<SubjectRef>`, `countSubjectsByStatus()` | U05, U07, U10, U13, U15 | Đọc mã, tên, trạng thái, Chủ nhiệm môn; kiểm môn còn `ACTIVE`; số đếm cho Admin Dashboard |

`SubjectRef`: `subjectId`, `code`, `name`, `status`, `managerId`.

##### Contract U04 cung cấp (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `ISubjectClasses` | `listClassesOfSubject(subjectId)` → `List<SubjectClassSummary>`, `countClasses(subjectIds)` → số lớp theo môn, `countUnarchivedClasses(subjectId)` | U05 | Hiện lớp của môn chỉ đọc trên Subject Detail và số lớp trên Subject List; chặn lưu trữ môn khi còn lớp chưa lưu trữ, mà U04 không phụ thuộc ngược U05 |

`SubjectClassSummary`: `classId`, `code`, `name`, `term`, `status`, `teacherName`, `activeStudentCount`.

#### Business flow

##### F1 - Xem danh sách môn (UC 16)
1. Admin mở Subject List từ Admin Dashboard; frontend gọi `GET /api/v1/admin/subjects` kèm từ khóa, trạng thái, trang.
2. Kiểm quyền `ADMIN` qua `IAuthorization`; không phải thì `403` và audit (BR-U04-01, 51).
3. Truy vấn `subjects` theo mã hoặc tên, lọc trạng thái, sắp theo mã, tối đa 100 dòng (BR-U04-50).
4. Lấy tên Chủ nhiệm môn của các dòng qua `IAccountLookup.getContact`.
5. Lấy số lớp của các môn trong trang qua `ISubjectClasses.countClasses`; U05 chưa trả lời thì cột số lớp hiện "không khả dụng".
6. Trả danh sách; bấm một dòng mở Subject Detail.

##### F2 - Thêm môn (UC 17)
1. Trên Subject List, Admin bấm "Thêm môn học"; popup mở.
2. Admin nhập mã, tên, mô tả; tùy chọn chọn Chủ nhiệm môn từ ô tìm (frontend gọi API tài khoản của U01, lọc role `SUBJECT_MANAGER`, trạng thái `ACTIVE`).
3. Backend kiểm quyền `ADMIN`.
4. Chuẩn hóa và kiểm mã: định dạng, độ dài, chưa tồn tại (BR-U04-02); kiểm tên, mô tả (BR-U04-06).
5. Có Chủ nhiệm môn thì kiểm qua `IAccountLookup.getContact`: phải `SUBJECT_MANAGER` và `ACTIVE` (BR-U04-03).
6. Lưu môn trạng thái `ACTIVE`, `version` 1.
7. Ghi audit `SUBJECT_CREATED` (kèm Chủ nhiệm môn nếu có) trong cùng transaction.
8. Đóng popup, danh sách tải lại.

##### F3 - Xem chi tiết môn (UC 18)
1. Admin mở Subject Detail; frontend gọi `GET /api/v1/admin/subjects/{id}`.
2. Kiểm quyền `ADMIN`; môn không tồn tại thì `404`.
3. Trả mã, tên, mô tả, trạng thái, Chủ nhiệm môn (tên, email), `version`.
4. Tab Lớp học gọi `GET /api/v1/admin/subjects/{id}/classes`; U04 lấy danh sách qua `ISubjectClasses.listClassesOfSubject` và trả chỉ đọc (BR-U04-05). U05 không trả lời thì báo "không khả dụng".

##### F4 - Sửa thông tin môn (UC 19)
1. Trên tab Thông tin, Admin sửa tên hoặc mô tả, lưu kèm `version`; frontend gọi `PATCH /api/v1/admin/subjects/{id}`.
2. Kiểm quyền `ADMIN`; mã môn không sửa được.
3. Kiểm tên, mô tả (BR-U04-06).
4. Cập nhật có điều kiện `version` khớp; lệch thì `409`, frontend báo tải lại.
5. Ghi audit `SUBJECT_UPDATED` với tên trường đã đổi.

##### F5 - Đổi Chủ nhiệm môn (UC 17, 19)
1. Admin bấm "Đổi chủ nhiệm", chọn người hoặc chọn bỏ trống; frontend gọi `PUT /api/v1/admin/subjects/{id}/manager` kèm `managerId` (hoặc rỗng) và `version`.
2. Kiểm quyền `ADMIN`.
3. Có người mới thì kiểm `SUBJECT_MANAGER` và `ACTIVE` (BR-U04-03).
4. Cập nhật `manager_id` có điều kiện `version`.
5. Ghi audit `SUBJECT_MANAGER_CHANGED` với người cũ và người mới.
6. Từ request tiếp theo, người cũ mất quyền R2 với môn này, người mới có quyền (BR-U04-10).

##### F6 - Lưu trữ và mở lại môn (UC 19)
**Lưu trữ**:
1. Admin bấm Lưu trữ, xác nhận; frontend gọi `PUT /api/v1/admin/subjects/{id}/status` với `ARCHIVED` và `version`.
2. Kiểm quyền `ADMIN`.
3. Hỏi `ISubjectClasses.countUnarchivedClasses`. Còn lớp chưa lưu trữ thì từ chối và trả danh sách lớp đó; U05 không trả lời thì từ chối (BR-U04-04).
4. Chuyển `ARCHIVED` có điều kiện `version`; ghi audit `SUBJECT_ARCHIVED`.

**Mở lại**:
1. Admin bấm Mở lại; gửi `ACTIVE` và `version`.
2. Kiểm quyền, chuyển `ACTIVE`, ghi audit `SUBJECT_REOPENED`.

##### F7 - Phạm vi môn cho phân quyền
1. U01 gọi `ISubjectScope.isSubjectManager(accountId, subjectId)` khi một hành động cần R2.
2. U04 đọc `manager_id` hiện thời của môn, không cache; môn không tồn tại thì trả `false` (BR-U04-10).
3. Khi U01 chặn đổi role (BR-U01-64), gọi `listManagedSubjects(accountId)` để biết người đó còn quản lý môn nào.

##### F8 - Thông tin môn cho unit khác
1. U05 gọi `ISubjectQuery.isActive(subjectId)` trước khi tạo lớp, và `listManagedSubjects(accountId)` để dựng Manager Dashboard và Subject Detail của Subject Manager (UC 30).
2. U07, U10, U13 gọi `isActive` trước khi cho sửa tài nguyên cấp môn (BR-U04-07).
3. U15 gọi `countSubjectsByStatus()` cho Admin Dashboard.

### U05 Class & Enrollment

Iter 1, Người 5, UC 20–30. Thiết kế: [u05-class-enrollment](u05-class-enrollment/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U05 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IClassScope` (contract của U01, U05 cài) | `isTeacherOf(accountId, classId)`, `isEnrolledStudent(accountId, classId)`, `subjectOfClass(classId)`, `listTaughtClasses(accountId)` | U01 | Kiểm R3/R4, R5 theo phân công và ghi danh hiện thời; chặn hạ role người đang dạy |
| `ISubjectClasses` (contract của U04, U05 cài) | `listClassesOfSubject(subjectId)`, `countClasses(subjectIds)`, `countUnarchivedClasses(subjectId)` | U04 | Lớp của môn trên Subject Detail và Subject List; chặn lưu trữ môn |
| `IClassAccess` | `getClassRef(classId)` → `ClassRef`, `isActiveStudent(accountId, classId)`, `listActiveStudents(classId)`, `isClassManager(accountId, classId)`, `listOpenClassesOf(accountId)`, `listOpenClassesOfSubject(subjectId)`, `countClassesByStatus()`, `countActiveEnrollments()` | U07–U15 | Thông tin lớp, sinh viên đang học, Chủ nhiệm môn của lớp, lớp đang mở của một người hoặc một môn, số đếm |
| Sự kiện `enrollment.activated` | - | U14 | Thông báo ghi danh trong app và email |

`ClassRef`: `classId`, `subjectId`, `code`, `name`, `status`, `teacherId`, `showGradeDistribution`. `isClassManager` chỉ đúng với Chủ nhiệm môn của môn chứa lớp; Admin không có quyền này.

##### Contract U05 cung cấp (unit khác cài, phía frontend)

| Contract | Unit cài | Dùng để |
|---|---|---|
| `StudentClassSection` | U08 (bài tập của lớp), U12 (nhóm của tôi), U13 (học liệu), U14 (thông báo lớp) | Gắn phần nội dung vào Student Class Detail |
| `TeacherClassTab` | U13 (tab Materials), U07, U09, U10 (tab Evals), U12 (phần nhóm trong tab Students) | Gắn tab hoặc phần vào Teacher Class Detail |
| `StudentDetailSection` | U12 (nhóm của sinh viên) | Gắn phần nhóm vào popup Student Detail |
| `DashboardNavEntry` | U08 (Student Assignments), U11 (Quiz Practice History), U14 (Class Announcements) trên Class Dashboard; U07 (Assignment List), U10 (Question List, Quiz List), U13 (Material List) trên Manager Dashboard | Gắn lối vào màn của unit khác lên hai dashboard |

#### Business flow

##### F1 - Lớp đang học (UC 20)
1. Student vào Class Dashboard sau đăng nhập; frontend gọi `GET /api/v1/me/classes`.
2. Lấy ghi danh `ACTIVE` của sinh viên; bỏ lớp `DRAFT`.
3. Chia "Đang học" (`OPEN`) và "Đã kết thúc" (`ARCHIVED`); mỗi thẻ có mã môn, tên lớp, giảng viên (BR-U05-40).
4. Thanh bên có lối vào do unit khác đăng ký: Student Assignments (U08), Quiz Practice History (U11), Class Announcements (U14).

##### F2 - Chi tiết lớp đang học (UC 21)
1. Student bấm một lớp; frontend gọi `GET /api/v1/me/classes/{id}`.
2. Kiểm ghi danh `ACTIVE` và lớp `OPEN`; không đạt thì trả "không tìm thấy" (BR-U05-41, 42).
3. Trả thông tin lớp, môn, giảng viên.
4. Trang hiện các phần do unit khác gắn vào: học liệu (U13), bài tập (U08), thông báo (U14), nhóm của tôi (U12); mỗi phần gọi API của unit đó.

##### F3 - Lớp được giao dạy (UC 22)
1. Teacher hoặc Subject Manager mở Class Dashboard; frontend gọi `GET /api/v1/teaching/classes`.
2. Lấy lớp có `teacher_id` là người đó; chia "Sắp mở", "Đang dạy", "Đã kết thúc" (BR-U05-45).

##### F4 - Chi tiết lớp được giao dạy (UC 23)
1. Bấm một lớp; frontend gọi `GET /api/v1/teaching/classes/{id}`.
2. Kiểm người gọi là giảng viên chính; không phải thì trả "không tìm thấy" và ghi audit (BR-U05-46, 51).
3. Tab Class Detail: thông tin lớp chỉ đọc.
4. Tab Students: `GET /api/v1/teaching/classes/{id}/students` trả sinh viên `ACTIVE` (email, tên, ngày ghi danh); phần nhóm của U12 gắn vào tab này.
5. Bấm một sinh viên mở popup Student Detail: email, tên, ngày ghi danh; phần nhóm do U12 gắn vào (BR-U05-47).
6. Tab Materials, Evals do U13, U07, U09, U10 gắn vào.

##### F5 - Môn được giao quản lý (UC 30)
1. Subject Manager đăng nhập vào Manager Dashboard; frontend gọi `GET /api/v1/manager/subjects`.
2. Lấy các môn qua `ISubjectQuery.listManagedSubjects` (U04); đếm lớp theo trạng thái của từng môn.
3. Hiện thẻ môn; lối vào My Subject, Class List và các màn của unit khác (Material List, Question List, Quiz List, Assignment List); nút sang Class Dashboard nếu có lớp đang dạy (BR-U05-48).
4. Bấm một môn mở My Subject: `GET /api/v1/manager/subjects/{id}` kiểm người gọi là Chủ nhiệm môn; trả thông tin môn và số lớp theo trạng thái.

##### F6 - Lớp của môn (UC 24)
1. Mở Class List; frontend gọi `GET /api/v1/manager/classes` kèm môn, trạng thái, học kỳ, từ khóa.
2. Chỉ trả lớp của các môn người gọi đang quản lý; tối đa 100 dòng (BR-U05-50).

##### F7 - Tạo lớp (UC 26)
1. Trên Class List bấm "Tạo lớp"; popup mở.
2. Chọn môn, nhập mã, tên, mô tả, học kỳ; tùy chọn chọn giảng viên (ô tìm gọi `GET /api/v1/manager/teacher-candidates`; U05 gọi `IAccountLookup.findAccounts` với role `TEACHER`, `SUBJECT_MANAGER` và chỉ giữ người `ACTIVE`).
3. Kiểm người gọi là Chủ nhiệm môn của môn và môn `ACTIVE` (BR-U05-10).
4. Kiểm mã trong môn (BR-U05-11); kiểm giảng viên nếu có (BR-U05-12).
5. Tạo lớp `DRAFT`; ghi audit `CLASS_CREATED` (và `CLASS_TEACHER_CHANGED` nếu có giảng viên).

##### F8 - Chi tiết lớp của môn (UC 25)
1. Bấm một lớp trên Class List; frontend gọi `GET /api/v1/manager/classes/{id}`.
2. Kiểm người gọi là Chủ nhiệm môn của môn chứa lớp; không phải thì "không tìm thấy" và audit.
3. Trả thông tin lớp, giảng viên chính, danh sách sinh viên (`ACTIVE` và `REMOVED`), `version`; các nút F9–F14 nằm trên màn này.

##### F9 - Gán giảng viên (UC 27)
1. Bấm "Phân công giảng viên", tìm và chọn người; frontend gọi `PUT /api/v1/manager/classes/{id}/teacher` kèm `version`.
2. Kiểm quyền Chủ nhiệm môn và lớp chưa lưu trữ (BR-U05-16); người mới phải `TEACHER` hoặc `SUBJECT_MANAGER` đang `ACTIVE` (BR-U05-12).
3. Lớp `OPEN` không được bỏ trống giảng viên.
4. Cập nhật có điều kiện `version`; ghi audit `CLASS_TEACHER_CHANGED` (người cũ, người mới).
5. Từ request tiếp theo, người cũ mất quyền dạy lớp.

##### F10 - Thêm từng sinh viên (UC 28)
1. Gõ email hoặc tên; frontend gọi `GET /api/v1/manager/classes/{id}/student-candidates?q=`, U05 tra `IAccountLookup.findStudents` (tối đa 20).
2. Chọn sinh viên; frontend gọi `POST /api/v1/manager/classes/{id}/enrollments`.
3. Kiểm quyền Chủ nhiệm môn, lớp `DRAFT` hoặc `OPEN`, role `STUDENT`, trạng thái `ACTIVE` hoặc `PENDING` (BR-U05-20).
4. Khóa hàng theo (sinh viên, môn), kiểm chưa `ACTIVE` ở lớp chưa lưu trữ khác cùng môn (BR-U05-22).
5. Tạo ghi danh `ACTIVE` hoặc khôi phục bản `REMOVED` (BR-U05-21); ghi audit `STUDENT_ENROLLED`.
6. Lớp `OPEN` thì phát `enrollment.activated` sau commit (BR-U05-26).

##### F11 - Thêm sinh viên theo danh sách (UC 28)
1. Dán email hoặc chọn CSV một cột; frontend chặn quá 200 dòng.
2. `POST /api/v1/manager/classes/{id}/enrollments:batch`; kiểm quyền và giới hạn kích thước.
3. Chuẩn hóa chữ thường, đánh dấu trùng trong danh sách; tra U01 một lần bằng `findByEmails`.
4. Mỗi dòng chạy F10 bước 3–6 trong transaction riêng và ghi kết quả (BR-U05-24); dòng lỗi không làm hỏng dòng khác.
5. Trả bảng kết quả; không lưu lô.

##### F12 - Gỡ sinh viên (UC 28)
1. Bấm Gỡ ở dòng sinh viên, xác nhận; frontend gọi `PUT /api/v1/manager/classes/{id}/enrollments/{accountId}/status` với `REMOVED`.
2. Kiểm quyền và lớp chưa lưu trữ; chuyển `REMOVED`, ghi `removedAt`; ghi audit `STUDENT_REMOVED` (BR-U05-25).
3. Từ request tiếp theo, sinh viên mất quyền vào lớp; bài làm và điểm giữ nguyên.
4. Thêm lại người đã gỡ: cùng API với `ACTIVE`, chạy F10 bước 3–6.

##### F13 - Sửa thông tin lớp và phân bố điểm (UC 29)
1. Sửa tên, mô tả, học kỳ kèm `version`: `PATCH /api/v1/manager/classes/{id}`; mã lớp và môn không sửa được.
2. Bật hoặc tắt phân bố điểm: `PUT /api/v1/manager/classes/{id}/grade-distribution` kèm `version`, xác nhận khi bật.
3. Kiểm quyền Chủ nhiệm môn và lớp chưa lưu trữ (BR-U05-16); cập nhật có điều kiện `version`, lệch thì `409`.
4. Ghi audit `CLASS_UPDATED` hoặc `GRADE_DISTRIBUTION_CHANGED`.

##### F14 - Đổi trạng thái lớp (UC 29)
1. Bấm Mở, Lưu trữ hoặc Mở lại, xác nhận; frontend gọi `PUT /api/v1/manager/classes/{id}/status` kèm trạng thái mới và `version`.
2. Kiểm quyền Chủ nhiệm môn và chuyển hợp lệ (BR-U05-14).
3. Mở hoặc mở lại: cần giảng viên và môn `ACTIVE` (hỏi U04); mở lại kiểm BR-U05-15, vướng thì trả danh sách người vướng.
4. Cập nhật có điều kiện `version`; ghi audit `CLASS_STATUS_CHANGED`.
5. `DRAFT→OPEN`: phát `enrollment.activated` cho mọi ghi danh `ACTIVE` sau commit (BR-U05-26).
6. Lưu trữ: lớp chỉ đọc; dữ liệu giữ nguyên (BR-U05-16).

##### F15 - Trả phạm vi lớp
1. U01 gọi `IClassScope.isTeacherOf` (R3/R4) hoặc `isEnrolledStudent` (R5) khi quyết định quyền; U05 đọc `teacher_id` và ghi danh hiện thời, không cache (BR-U05-30).
2. U01 gọi `listTaughtClasses` khi chặn hạ role người đang dạy lớp.
3. U04 gọi `ISubjectClasses` để hiện lớp của môn và chặn lưu trữ môn.
4. U07–U15 gọi `IClassAccess`: thông tin lớp, sinh viên `ACTIVE`, Chủ nhiệm môn của lớp, lớp `OPEN` của một người hoặc của một môn, số đếm cho Admin Dashboard.

## Iter 2: nghiệp vụ chính

| Unit | Người | UC | LOC (nghìn) | Làm gì |
|---|---|---|---|---|
| U06 AI Connections | Người 5 | 31–33 | 4 | API AI cá nhân, API chung của trường kèm quota theo ngày, credential MCP, lịch sử dùng AI, chạy task AI nền |
| U07 Assignment Authoring | Người 4 | 34–36 | 5 | Soạn đề dạng tài liệu (cá nhân hoặc nhóm, có hoặc không sơ đồ), rubric từng phần, duyệt, phát hành, lịch, version, copy, DOCX, AI soạn nháp, gắn tài liệu tham khảo |
| U08 Assignment Work | Người 3 | 37–41 | 4 | Sinh viên làm bài cá nhân, tự lưu, nộp, tự nộp khi hết hạn, lịch sử nộp, phản hồi AI cho bài Practice, dữ liệu rời trang |
| U09 Assignment Grading | Người 2 | 42–45 | 3 | Bài nộp và tiến độ, chấm theo checklist rubric, AI đề xuất chấm, chốt, công bố, sổ điểm, xuất CSV/XLSX, phân bố điểm ẩn danh |
| U10 Quiz Authoring | Người 1 | 46–48 | 3 | Ngân hàng câu trắc nghiệm có phiên bản, nhập tệp, soạn Quiz của lớp hoặc môn, phát hành, AI soạn câu |

### U06 AI Connections

Iter 2, Người 5, UC 31–33. Thiết kế: [u06-ai-connections](u06-ai-connections/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U06 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IAiTask` | `submit(AiTaskRequest)` → `AiTaskView`, `get(taskId)`, `cancel(actor, taskId)` | U07, U08, U09, U10 | Gửi task AI theo nguồn người dùng chọn, giữ chỗ quota, chạy nền, trả trạng thái |
| `ISettingsSectionProvider` (contract của U03, U06 cài: `AiConnectionsSection`, `TeacherQuotaSection`) | `sectionKey()`, `title()`, `summary()` | U03 | Hiện phần API AI của trường và quota giảng viên trên Setting List |
| `IJobHandler` (`AI_TASK`), `IPendingSweeper`, `IScheduledScanner` (contract của U03, U06 cài) | theo U03 | U03 | Chạy task trong worker, gửi lại message bị mất, dọn task quá hạn |
| `AccountMenuEntry` (contract của U01, U06 cài) | - | U01 | Mục AI Connection trong menu tài khoản cho Student, Teacher, Subject Manager |
| Component `AiSourcePicker`, `AiTaskStatus` | - | Frontend U07, U08, U09, U10 | Popup chọn nguồn AI có quota và ước tính; theo dõi trạng thái task |
| Panel `AiConnectionsPanel`, `TeacherQuotaPanel` (đăng ký vào `settingsSectionRegistry` của U03) | - | U03 | Sửa API chung, quota và xem tổng hợp usage trong Setting Detail |
| MCP server `/mcp` | `initialize`, `tools/list`, `tools/call` | Agent của người dùng | Cho agent đọc và ghi bản nháp đúng quyền |

`AiTaskRequest`: `actor`, `source` (`SHARED` hoặc `PERSONAL` kèm `provider`), `taskType`, `targetRef`, `inputVersion`, `requestRef`, `prompt` (system, user), `outputSchema`, `maxOutputTokens`. `AiTaskView`: `taskId`, `state`, `source`, `provider`, `model`, `applyResult`, `safeError`, usage tóm tắt.

##### Contract U06 khai báo (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `IAiTaskResultHandler` | `taskType()`, `onReady(AiTaskResult)` → `APPLIED`, `STALE`, `REJECTED`; `onFailed(AiTaskFailure)` | U07 (soạn Assignment), U08 (phản hồi AI bài luyện tập, UC 41), U09 (đề xuất chấm, UC 43), U10 (soạn câu hỏi, Quiz) | Nhận kết quả task, kiểm quyền và phiên bản đích rồi áp dụng, để U06 không phụ thuộc ngược unit gọi |
| `IMcpToolProvider` | `tools()` → `McpToolDefinition` (tên, mô tả, schema tham số, loại `READ` hoặc `WRITE`, role được dùng); `call(actor, toolName, args)` → `McpToolResult` | U07, U08, U09, U10, U12, U13 | Đăng ký tool MCP trên dữ liệu của mình; tự kiểm quyền và `expectedVersion` |
| `IReferenceMaterial` | `listSelectable(actor, scopeRef)`, `extractText(actor, materialIds, maxChars)` | U13 | Cho người soạn chọn tài liệu tham khảo được phép và lấy chữ đưa vào prompt; U07, U10 gọi khi AI soạn bài, soạn câu. Chưa có U13 thì trả rỗng |

Tool giai đoạn đầu theo FR-035:

| Tool | Loại | Unit cài |
|---|---|---|
| `read_assignment_context` (đề, rubric) | READ | U07 |
| `save_assignment_draft` (`expectedVersion`) | WRITE | U07 |
| `read_own_work` | READ | U08 |
| `read_authorized_submission` (giảng viên được giao) | READ | U08 |
| `record_private_feedback` | WRITE | U08 |
| `read_authorized_group_submission` (giảng viên được giao) | READ | U12 |
| `record_grading_proposal` | WRITE | U09 |
| `read_quiz_context`, `save_quiz_draft` (`expectedVersion`) | READ, WRITE | U10 |
| `read_authorized_material` | READ | U13 |

#### Business flow

##### F1 - Thêm hoặc thay API cá nhân (UC 31)
1. Mở AI Connection từ menu tài khoản, tab API cá nhân; frontend gọi `GET /api/v1/me/ai-connections` và `GET /api/v1/ai/providers` (provider và model cho phép).
2. Bấm Thêm (hoặc Thay) ở một provider, nhập key, chọn model; frontend gọi `PUT /api/v1/me/ai-connections/{provider}` (kèm `version` khi thay; đổi model không cần nhập lại key).
3. Kiểm người gọi là Student, Teacher hoặc Subject Manager đang `ACTIVE`, provider hợp lệ, model nằm trong danh sách cho phép, key đúng định dạng cơ bản (BR-U06-01, 02).
4. Mã hóa key, lưu kết nối `PERSONAL` `ACTIVE` với 4 ký tự cuối; thay thì ghi đè bản mã, tăng `version`, xóa kết quả kiểm tra cũ (BR-U06-03, 06).
5. Ghi audit `AI_CONNECTION_CHANGED`; trả kết nối dạng che, gợi ý bấm Kiểm tra.

##### F2 - Kiểm tra API cá nhân (UC 31)
1. Bấm Kiểm tra; UI báo có thể phát sinh chi phí nhỏ trên key; frontend gọi `POST /api/v1/me/ai-connections/{provider}:test`.
2. Kiểm chủ sở hữu và kết nối `ACTIVE`; giải mã key trong bộ nhớ.
3. Gửi yêu cầu tổng hợp cố định, tối đa 5 token ra, timeout 20 giây (BR-U06-04).
4. Lưu kết quả (`OK` hoặc lỗi đã làm sạch: key sai, hết hạn mức phía provider, model không hỗ trợ, không kết nối được) và thời điểm; có usage thì ghi bản ghi loại `CONNECTION_TEST`.
5. Ghi audit `AI_CONNECTION_TESTED`; trả kết quả.

##### F3 - Thu hồi API cá nhân (UC 31)
1. Bấm Thu hồi, xác nhận; frontend gọi `DELETE /api/v1/me/ai-connections/{provider}`.
2. Kiểm chủ sở hữu; xóa bản mã, chuyển `REVOKED`; ghi audit `AI_CONNECTION_CHANGED`.
3. Task chưa gửi đi dùng kết nối này chuyển `FAILED` khi tới lượt chạy; task đã gửi vẫn chạy xong (BR-U06-05).

##### F4 - Tạo credential MCP (UC 32)
1. Mở tab MCP Agent; frontend gọi `GET /api/v1/me/mcp-credentials`.
2. Bấm Tạo, nhập nhãn (tên agent), chọn quyền `READ_ONLY` hoặc `READ_WRITE` và hạn 7, 30 hoặc 90 ngày; frontend gọi `POST /api/v1/me/mcp-credentials`.
3. Kiểm role Student, Teacher hoặc Subject Manager đang `ACTIVE`; chưa có quá 5 credential còn hiệu lực (BR-U06-30).
4. Sinh token ngẫu nhiên 256 bit; lưu bản băm, 4 ký tự cuối và hạn (BR-U06-31).
5. Ghi audit `MCP_CREDENTIAL_CREATED`; trả token đúng một lần kèm địa chỉ `/mcp` và hướng dẫn cho từng ứng dụng agent đã kiểm chứng (BR-U06-50).

##### F5 - Thu hồi credential MCP (UC 32)
1. Bấm Thu hồi, xác nhận; frontend gọi `DELETE /api/v1/me/mcp-credentials/{id}`.
2. Kiểm chủ sở hữu; ghi `revokedAt`; ghi audit `MCP_REVOKED`.
3. Lời gọi tool tiếp theo dùng token này bị từ chối.

##### F6 - Agent gọi tool MCP (UC 32)
1. Agent gửi `POST /mcp` (JSON-RPC `initialize`, `tools/list`, `tools/call`) kèm `Authorization: Bearer`.
2. Băm token, tìm credential; không có, đã thu hồi hoặc hết hạn thì trả `401`.
3. Dựng actor từ tài khoản của credential (BR-U06-33).
4. `tools/list`: trả các tool đăng ký qua `IMcpToolProvider` mà role của actor được dùng; credential `READ_ONLY` chỉ thấy tool đọc.
5. `tools/call`: kiểm giới hạn 60 lời gọi mỗi phút; tìm tool; tool ghi cần `READ_WRITE`; kiểm tham số theo schema, không quá 64 KB.
6. Gọi `IMcpToolProvider.call` của unit sở hữu; unit này gọi `IAuthorization.authorize` với actor (U01 kiểm `ACTIVE`, role, phạm vi lớp hoặc môn) và kiểm `expectedVersion` khi ghi.
7. Cắt kết quả không quá 256 KB; ghi usage nguồn `MCP` (tên tool, kết quả; token `UNAVAILABLE` hoặc `EXTERNALLY_REPORTED`); cập nhật `lastUsedAt`; ghi audit `MCP_TOOL_CALLED`.
8. Bị từ chối thì trả lỗi JSON-RPC không lộ dữ liệu và ghi audit `ACCESS_DENIED`.

##### F7 - Xem lịch sử sử dụng (UC 33)
1. Từ AI Connection mở AI Usage History; frontend gọi `GET /api/v1/me/ai-usage` kèm khoảng thời gian, nguồn, trang (tối đa 100 dòng).
2. Chỉ trả bản ghi của người gọi.
3. Mỗi dòng: thời điểm, nguồn, provider và model, loại task hoặc tên tool, trạng thái, token vào và ra kèm nhãn số liệu; không có số thì hiện "Không có số liệu", không hiện 0 (BR-U06-40).
4. Teacher, Subject Manager có thẻ quota chung còn lại hôm nay (`GET /api/v1/ai/sources`).

##### F8 - Cấu hình API chung (UC 14, 15)
1. Admin mở Setting List; bấm phần "API AI của trường" mở `AiConnectionsPanel`; panel gọi `GET /api/v1/admin/ai/connections` (dạng che).
2. Thêm hoặc thay key, chọn model: `PUT /api/v1/admin/ai/connections/{provider}` kèm `version`; kiểm Admin; mã hóa, lưu kết nối `INSTITUTIONAL` `ACTIVE`.
3. Kiểm tra: `POST /api/v1/admin/ai/connections/{provider}:test` chạy như F2 bước 2–5; usage ghi nguồn `SHARED`, không trừ quota giảng viên.
4. Tắt hoặc bật: `PUT .../{provider}/status`; thu hồi: `DELETE .../{provider}` xóa bản mã.
5. Ghi audit `AI_CONNECTION_CHANGED`; yêu cầu AI mới dùng cấu hình hiện hành (BR-U06-05).

##### F9 - Cấu hình quota (UC 14, 15)
1. Bấm phần "Quota AI giảng viên" mở `TeacherQuotaPanel`; panel gọi `GET /api/v1/admin/ai/quota-policy` và `GET /api/v1/admin/ai/teacher-quotas?q=`.
2. Đặt mức mặc định mỗi ngày và trần ngày toàn hệ thống: `PUT /api/v1/admin/ai/quota-policy` kèm `version`; số nguyên không âm.
3. Đặt mức riêng: tìm giảng viên qua `IAccountLookup.findAccounts` (role `TEACHER`, `SUBJECT_MANAGER`), gọi `PUT /api/v1/admin/ai/teacher-quotas/{accountId}` kèm `version`; bỏ mức riêng: `DELETE` để về mặc định.
4. Kiểm Admin; người được đặt phải là Teacher hoặc Subject Manager; lệch `version` thì `409`.
5. Ghi audit `QUOTA_CHANGED` (trước, sau); mức mới áp cho lần giữ chỗ tiếp theo, không thu hồi phần đã dùng.

##### F10 - Tổng hợp usage API chung (UC 33, Admin)
1. Trong `TeacherQuotaPanel`, tab Usage gọi `GET /api/v1/admin/ai/usage-summary` kèm khoảng thời gian.
2. Trả tổng theo giảng viên (đã dùng hôm nay, mức áp dụng), theo ngày, theo provider và model; chỉ nguồn `SHARED`, không nội dung, không key (BR-U06-41).

##### F11 - Chọn nguồn và gửi task AI
1. Ở màn của unit gọi (soạn Assignment, soạn Quiz, xin phản hồi AI, đề xuất chấm), người dùng bấm nút AI; `AiSourcePicker` gọi `GET /api/v1/ai/sources`: API chung theo từng provider (chỉ Teacher, Subject Manager; kèm quota còn lại hoặc lý do không dùng được) và các kết nối cá nhân `ACTIVE`.
2. Người dùng chọn nguồn; UI hiện bên chịu chi phí và số token ước tính có nhãn "ước tính" (BR-U06-27); không tự chọn thay.
3. Frontend gọi API của unit gọi; unit này kiểm quyền trên đích, dựng prompt tối thiểu, gọi `IAiTask.submit` kèm `requestRef` (BR-U06-20).
4. U06 kiểm `requestRef`: trùng dấu vân tay thì trả task cũ, khác thì `409` (BR-U06-21).
5. Kiểm nguồn: `SHARED` chỉ Teacher, Subject Manager `ACTIVE` và kết nối chung `ACTIVE`; `PERSONAL` đúng chủ và `ACTIVE`; người gọi có ít hơn 3 task đang chờ hoặc chạy (BR-U06-11, 17).
6. `SHARED`: giữ chỗ quota (BR-U06-13); không đủ thì từ chối, báo thời điểm có cửa sổ mới.
7. Tạo task `QUEUED`, lưu prompt; sau commit gửi job `AI_TASK` qua `IJob` với `idempotencyKey` là `taskId`.
8. Trả `taskId`; `AiTaskStatus` hỏi `GET /api/v1/ai/tasks/{id}` mỗi 2 giây tới khi task kết thúc. Người gửi có thể hủy khi còn `QUEUED` (BR-U06-26).

##### F12 - Worker chạy task
1. Worker nhận `AI_TASK` trên queue `jobs.ai`; khóa dòng task.
2. Task `READY` chưa chuyển cho handler: chỉ chạy lại bước 8. Task `RUNNING` (đã có mốc gửi, chưa có kết quả): chuyển `UNKNOWN`, tính giữ chỗ là đã dùng, không gọi provider. Trạng thái khác `QUEUED`: bỏ qua (BR-U06-22).
3. Kết nối không còn `ACTIVE`: `FAILED`, giải phóng giữ chỗ.
4. Ghi mốc gửi (`RUNNING`, `dispatchedAt`), commit.
5. Giải mã key, gọi adapter của provider (timeout 120 giây) với prompt, JSON schema, `maxOutputTokens`.
6. Provider trả phản hồi: lưu kết quả và usage `PROVIDER_MEASURED`; quyết toán quota bằng token thật; đúng schema thì `READY`, sai thì `FAILED` (BR-U06-14, 23).
7. Provider từ chối không tính token: `FAILED` với lỗi đã làm sạch, giải phóng giữ chỗ. Hết giờ hoặc mất kết nối sau khi gửi: `UNKNOWN`, tính giữ chỗ là đã dùng với nhãn `ESTIMATED`.
8. `READY`: gọi `IAiTaskResultHandler.onReady` của unit gọi trong transaction riêng; handler kiểm quyền và `inputVersion` rồi áp dụng (bản nháp, phản hồi, đề xuất) hoặc trả `STALE`; U06 ghi kết quả áp dụng. `FAILED`, `UNKNOWN`: gọi `onFailed`.
9. Xóa prompt và kết quả khỏi task; giữ thông tin task và usage (BR-U06-24).

##### F13 - Dọn task quá hạn
1. `IPendingSweeper`: task `QUEUED` quá 5 phút được U03 gửi lại message.
2. `IScheduledScanner` chạy mỗi phút: task `QUEUED` quá 30 phút chuyển `CANCELLED`, giải phóng giữ chỗ (chưa gửi provider).
3. Task `RUNNING` quá 10 phút chuyển `UNKNOWN`, tính giữ chỗ là đã dùng.

##### F14 - Tóm tắt cho Setting List (UC 14)
1. U03 gọi `summary()` của `AiConnectionsSection` và `TeacherQuotaSection` (timeout 1 giây).
2. Trả bản tóm tắt đã che: trạng thái và model từng provider của API chung; mức mặc định, trần ngày, số giảng viên có mức riêng, hoặc "Chưa cấu hình".

### U07 Assignment Authoring

Iter 2, Người 4, UC 34–36. Thiết kế: [u07-assignment-authoring](u07-assignment-authoring/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U07 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IAssignmentQuery` | `get(assignmentId)` → `AssignmentView`; `getContent(assignmentId)` → khung, phần, `requiredDiagrams`, rubric; `listForClass(classId)`; `submissionWindow(assignmentId, now)` → `ON_TIME`, `LATE`, `CLOSED`; `listClosingBetween(from, to)` | U08, U09, U12, U14 | Đọc bài, lịch, nội dung đã khóa; biết còn nhận bài không; nhắc hạn |
| `IRubric` | `listRubrics(assignmentId)`, `score(rubricId, checkedItemIds)`, `maxPoints(assignmentId)` | U08 (hiện rubric), U09 (chấm), U12 | Đọc rubric đã khóa, tính điểm theo mục đạt |
| `IDocumentModel` | `validateSkeleton`, `validateForSave(skeleton, document)`, `validateForSubmit(skeleton, document, requiredDiagrams)`, `computeParts(skeleton)`, `toPlainText(document)`, `compactDiagramXml(xml)` | U08, U09, U12 | Kiểm bài làm không đụng khung, đủ sơ đồ khi nộp; chữ và XML rút gọn cho AI chấm |
| `IDocx` | `export(document, title, diagramLinker)`, `importPreview(docx, origin, diagramEnabled, diagramResolver)` | U08, U12 | Xuất, nhập DOCX cho bài làm, cùng cách giữ sơ đồ như đề |
| Sự kiện `assignment.opened` | - | U14 | Báo bài mới mở cho người học của lớp hoặc của mọi lớp `OPEN` thuộc môn |
| `TeacherClassTab`, `DashboardNavEntry` (contract của U05, U07 cài) | - | U05 | Phần Bài tập trong tab Evals; lối vào Assignment List trên Manager Dashboard |
| `IAiTaskResultHandler` cho `ASSIGNMENT_DRAFT`, `IMcpToolProvider` (contract của U06, U07 cài) | - | U06 | Nhận đề xuất AI; tool `read_assignment_context`, `save_assignment_draft` |
| `IScheduledScanner` (contract của U03, U07 cài) | - | U03 | Mở, đóng bài theo lịch |
| Component `DocumentEditor`, `DiagramBlock`, `DocumentViewer`, `RubricViewer` | - | Frontend U08, U09, U12 | Trình soạn và xem tài liệu dùng chung, sơ đồ Draw.io, xem rubric |

##### Contract U07 khai báo (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `IAssignmentLifecycleListener` | `onOpened(assignmentId)`, `onRetired(assignmentId)` | U08 (tự nộp lượt đang làm khi ngưng giao), U12 (tạo tài liệu nhóm khi mở, tự nộp khi ngưng giao) | Phản ứng bắt buộc trong cùng transaction mở bài, ngưng giao mà U07 không phụ thuộc ngược |
| `IGroupAssignmentSupport` | `checkReadiness(classId)` → lỗi, cảnh báo | U12 | Bật `GROUP`; kiểm lớp có nhóm, mỗi nhóm có thành viên và trưởng nhóm trước khi phát hành |
| Slot frontend `AssignmentRowAction` | - | U09 | Lối vào bài nộp (Submission Detail) trên từng dòng bài |
| Slot frontend `AssignmentFormSection` | - | U12 | Phần nhóm trên Assignment Form của bài `GROUP` |

Chưa có U08, U12 thì listener rỗng và `GROUP` không chọn được. Chưa có U13 thì `IReferenceMaterial` của U06 trả rỗng, AI soạn chỉ từ yêu cầu.

#### Business flow

##### F1 - Danh sách bài của lớp (UC 34)
1. Giảng viên mở tab Evals của Teacher Class Detail; phần Bài tập do U07 gắn qua `TeacherClassTab`; frontend gọi `GET /api/v1/classes/{classId}/assignments`.
2. Kiểm người gọi là giảng viên chính của lớp; không phải thì "không tìm thấy" và audit.
3. Trả bài của lớp và bài của môn chứa lớp (nhãn "Của môn", chỉ đọc). Mỗi dòng: tiêu đề, cá nhân hay nhóm, có sơ đồ, `GRADED` hay `PRACTICE`, trạng thái, version, lịch, nguồn (nhân bản, version, copy). Lọc theo trạng thái; mặc định ẩn `CLOSED`, `RETIRED`.
4. Bấm bài mở Assignment Form; mỗi dòng có lối vào bài nộp do U09 gắn qua `AssignmentRowAction`; nút "Tạo bài" (F3).

##### F2 - Danh sách bài của môn (UC 34)
1. Chủ nhiệm môn bấm Assignment List trên Manager Dashboard (U07 đăng ký `DashboardNavEntry`), chọn môn mình quản lý; frontend gọi `GET /api/v1/subjects/{subjectId}/assignments`.
2. Kiểm R2; trả bài của môn kèm trạng thái, version, lịch; lọc; nút "Tạo bài".

##### F3 - Tạo bài (UC 35)
1. Bấm "Tạo bài" ở tab Evals (bài của lớp) hoặc Assignment List (bài của môn).
2. Chọn cá nhân hay nhóm (nhóm chỉ ở lớp và khi U12 có), bật hay tắt sơ đồ, `GRADED` hay `PRACTICE` (nhóm chỉ `GRADED`). Bài của lớp chọn nguồn: bài trống hoặc copy bài lớp khác (F13).
3. Kiểm quyền (R3/R4 với lớp, R2 với môn) và lớp hoặc môn chưa lưu trữ (BR-U07-02, 03, 04).
4. Tạo bài `DRAFT`, `versionNo` 1, khung trống có một phần (cả tài liệu) kèm rubric trống; audit `ASSIGNMENT_CREATED`; mở Assignment Form.

##### F4 - Soạn đề và khung (UC 35)
1. Assignment Form gọi `GET /api/v1/assignments/{id}`: thông tin, cài đặt, khung, danh sách phần kèm tổng điểm và trạng thái rubric.
2. Sửa tiêu đề, hướng dẫn và cài đặt (`PATCH /api/v1/assignments/{id}` kèm `version`): bật hay tắt sơ đồ (BR-U07-14), `requiredDiagrams`, `GRADED` hay `PRACTICE`, theo dõi hoạt động và thông báo cho người học, tài liệu tham khảo gắn kèm (BR-U07-17).
3. Sửa khung trong trình soạn tài liệu: heading, đoạn, danh sách, bảng, ảnh (tải qua U03 `DOCUMENT_IMAGE`), sơ đồ (Draw.io nhúng, chỉ khi bật).
4. Lưu khung: `PUT /api/v1/assignments/{id}/skeleton` kèm `version`; kiểm cấu trúc, id block duy nhất, trần kích thước, XML sơ đồ an toàn, sơ đồ chỉ khi bật (BR-U07-10, 12).
5. Tính lại phần (BR-U07-11): phần mới tạo rubric trống, phần không còn thì xóa rubric, phần còn heading giữ rubric đã điền (BR-U07-20).
6. Bài đang `REVIEWED` thì về `DRAFT`; tăng `version`; trả danh sách phần kèm cảnh báo rubric trống.

##### F5 - Sửa rubric (UC 36)
1. Trên Assignment Form bấm một phần, mở popup Rubric Detail; frontend gọi `GET /api/v1/assignments/{id}/parts/{partId}/rubric`.
2. Thêm, sửa, xóa tiêu chí (tên, mô tả, loại `TEXT` hoặc `DIAGRAM`) và mục (nội dung, điểm); tổng điểm tự tính.
3. Lưu: `PUT .../parts/{partId}/rubric` kèm `version`; kiểm quyền, bài `DRAFT` hoặc `REVIEWED`, rubric chưa khóa, không vượt giới hạn và không có điểm sai (BR-U07-21); khi nháp vẫn cho lưu rubric chưa đủ.
4. Ghi đè rubric; bài `REVIEWED` về `DRAFT`; audit `RUBRIC_UPDATED`; Assignment Form cập nhật tổng điểm bài.
5. Bài đã phát hành: popup chỉ xem (BR-U07-23).

##### F6 - AI soạn bản nháp (UC 35)
1. Trên Assignment Form của bài `DRAFT`, bấm "Soạn bằng AI"; nhập yêu cầu (chủ đề, mức độ, số phần, gợi ý về rubric); chọn tài liệu tham khảo từ danh sách của U13 qua `IReferenceMaterial` (chưa có U13 thì bỏ qua bước này); chọn nguồn AI trong `AiSourcePicker` của U06.
2. Frontend gọi `POST /api/v1/assignments/{id}/ai-drafts`; kiểm quyền và bài `DRAFT`.
3. Lấy chữ của tài liệu đã chọn qua `IReferenceMaterial.extractText` (U13 kiểm quyền, cắt theo trần); dựng prompt gồm yêu cầu, cài đặt bài, chữ tài liệu và JSON schema của kết quả (BR-U07-52).
4. Gọi `IAiTask.submit` (loại `ASSIGNMENT_DRAFT`, đích là bài, `inputVersion` là `version` hiện tại, `requestRef`); tạo đề xuất `PENDING` kèm provenance; trả `taskId` cho `AiTaskStatus`.
5. U06 gọi `onReady`: bài còn `DRAFT` thì lưu đề xuất `READY` và trả `APPLIED`; bài đã xóa hoặc đã phát hành thì trả `REJECTED`. `onFailed` chuyển đề xuất sang `FAILED`.
6. Người soạn xem đề xuất. Áp dụng: `POST .../ai-drafts/{draftId}:apply` kèm `version`; cảnh báo nếu khung đã có nội dung; thay khung (và hướng dẫn nếu chọn), tính lại phần như F4 bước 5, điền gợi ý rubric; đề xuất `APPLIED`; audit `ASSIGNMENT_AI_DRAFT_APPLIED`.
7. Bỏ: đề xuất `DISCARDED`, bài không đổi.

##### F7 - Xuất, nhập DOCX (UC 35)
Xuất:
1. Bấm Xuất DOCX, chọn có kèm rubric hay không; frontend gọi `GET /api/v1/assignments/{id}/docx`.
2. Kiểm quyền đọc bài.
3. Dựng DOCX: heading theo style Word, đoạn, danh sách, bảng, ảnh (đọc qua U03). Mỗi sơ đồ: PNG dựng từ SVG xem trước, alt text "mô tả [drawio:assignmentId/blockId]", hyperlink tới trang xem sơ đồ, khối `mxfile` trong PNG (BR-U07-60).
4. Trả file, không lưu.

Nhập:
1. Trên bài `DRAFT` bấm Nhập DOCX, chọn file ≤ 20 MB; frontend gọi `POST /api/v1/assignments/{id}/skeleton:import-docx`.
2. Đọc file trong bộ nhớ, chặn file nén bất thường; chuyển style Word thành heading, đoạn, danh sách, bảng; ảnh lưu qua U03 (`store`, chưa `attach`).
3. Mỗi ảnh tìm sơ đồ theo thứ tự BR-U07-61; XML qua parser an toàn; sơ đồ theo mã chỉ lấy khi người nhập có quyền đọc bài nguồn; bài tắt sơ đồ thì giữ là ảnh.
4. Trả bản xem trước: block, số sơ đồ khôi phục theo từng cách, số ảnh, phần bị bỏ (BR-U07-62).
5. Người soạn chọn "Thay khung" hoặc "Thêm vào cuối"; lưu như F4 bước 4–6, `attach` các ảnh; audit `ASSIGNMENT_DOCX_IMPORTED`.

##### F8 - Xem trước và duyệt (UC 35)
1. Bấm Xem trước: hiện đề như người học thấy (hướng dẫn, khung, rubric, cài đặt).
2. Bấm Duyệt: `POST /api/v1/assignments/{id}/review` kèm `version`.
3. Kiểm quyền và bài `DRAFT`; kiểm theo BR-U07-32 và BR-U07-24.
4. Có lỗi thì trả danh sách mục cần sửa; đạt thì `REVIEWED`; audit `ASSIGNMENT_REVIEWED`.

##### F9 - Phát hành (UC 35)
1. Bài `REVIEWED`, bấm Phát hành, nhập `opensAt`, `closesAt`, `lateUntil` (tùy chọn), `maxAttempts`; frontend gọi `POST /api/v1/assignments/{id}/publish` kèm `version`.
2. Kiểm quyền; bài của lớp cần lớp `OPEN` (`IClassAccess`), bài của môn cần môn `ACTIVE` (`ISubjectQuery`); kiểm lịch (BR-U07-33); bật theo dõi hoạt động thì `activity.retentionDays` phải đã đặt (BR-U07-16).
3. Bài `GROUP`: gọi `IGroupAssignmentSupport.checkReadiness(classId)`; lỗi thì từ chối; có sinh viên chưa có nhóm thì cần `confirmUngroupedStudents`.
4. Một transaction: ghi lịch, kiểm lại rồi khóa mọi rubric, chuyển `SCHEDULED`; audit `ASSIGNMENT_PUBLISHED`.
5. Từ đây khung, rubric, cài đặt không sửa được; muốn đổi thì nhân bản hoặc tạo version mới.

##### F10 - Mở, đóng theo lịch
1. `IScheduledScanner` của U07 chạy mỗi phút trong worker.
2. Bài `SCHEDULED` có `opensAt` đã tới chuyển `OPEN` bằng câu lệnh có điều kiện; trong cùng transaction gọi `IAssignmentLifecycleListener.onOpened` (U12 tạo tài liệu nhóm); sau commit phát `assignment.opened` kèm `classId` hoặc `subjectId`.
3. Bài `OPEN` có `lateUntil` (hoặc `closesAt`) đã qua chuyển `CLOSED`.
4. Chạy lại không tạo trùng.

##### F11 - Sửa lịch, ngưng giao (UC 34, 35)
Sửa lịch:
1. Frontend gọi `PATCH /api/v1/assignments/{id}/schedule` kèm `version`; kiểm quyền (bài của môn chỉ Chủ nhiệm môn).
2. `SCHEDULED`: sửa theo BR-U07-33; `OPEN`: chỉ kéo dài `closesAt`, `lateUntil` (BR-U07-35).
3. Lưu; scanner dùng lịch mới; audit `ASSIGNMENT_SCHEDULE_CHANGED`.

Ngưng giao:
1. Bấm Ngưng giao, nhập lý do; frontend gọi `POST /api/v1/assignments/{id}/retire`.
2. Kiểm quyền và bài `SCHEDULED` hoặc `OPEN`.
3. Một transaction: chuyển `RETIRED`, gọi `onRetired` (U08, U12 tự nộp bài đang làm); audit `ASSIGNMENT_RETIRED` kèm lý do.

##### F12 - Nhân bản, version mới (UC 35)
1. Nhân bản: `POST /api/v1/assignments/{id}/clone`; kiểm quyền với bài nguồn; tạo `DRAFT` cùng lớp hoặc môn theo BR-U07-40, 43; nhân bản rubric; audit `ASSIGNMENT_CLONED`.
2. Version mới: bấm "Sửa" trên bài `CLOSED` hoặc `RETIRED`, frontend gọi `POST /api/v1/assignments/{id}/versions`; kiểm chưa có version đang mở (BR-U07-41); tạo `DRAFT` `versionNo + 1`; audit `ASSIGNMENT_VERSION_CREATED`.
3. Mở Assignment Form của bài mới.

##### F13 - Copy bài giữa lớp (UC 35)
1. Trong "Tạo bài" ở tab Evals chọn "Copy bài lớp khác"; frontend gọi `GET /api/v1/classes/{classId}/assignment-copy-sources`: các lớp mình dạy (trừ lớp đích) và bài của lớp đó.
2. Chọn bài; frontend gọi `POST /api/v1/classes/{classId}/assignments:copy` kèm `sourceAssignmentId`.
3. Kiểm người gọi dạy cả lớp nguồn và lớp đích; ngoài quyền thì "không tìm thấy" (BR-U07-42).
4. Một transaction: tạo `DRAFT` ở lớp đích theo BR-U07-43, nhân bản rubric; audit `ASSIGNMENT_COPIED`; mở Assignment Form.

##### F14 - Xóa bài nháp (UC 35)
1. Bấm Xóa, xác nhận; frontend gọi `DELETE /api/v1/assignments/{id}` kèm `version`.
2. Kiểm quyền và bài `DRAFT` hoặc `REVIEWED` (BR-U07-31).
3. Xóa bài, rubric, đề xuất AI; audit `ASSIGNMENT_DELETED`.

##### F15 - Tool MCP
1. U06 gọi `IMcpToolProvider.call` của U07 với actor dựng từ credential MCP.
2. `read_assignment_context(assignmentId)`: kiểm quyền qua `IAuthorization`. Giảng viên có quyền với bài: trả tiêu đề, hướng dẫn, khung dạng chữ (sơ đồ ở dạng XML rút gọn), phần, rubric, `version`. Người học: chỉ bài `OPEN`, `CLOSED` của lớp đang học, nội dung như Assignment Detail.
3. `save_assignment_draft(assignmentId, expectedVersion, draft)`: chỉ giảng viên có quyền, bài `DRAFT`, `expectedVersion` khớp; kiểm `draft` theo schema của đề xuất AI; lưu thành đề xuất `READY` nguồn MCP (provenance: nhãn agent). Người soạn áp dụng như F6 bước 6.
4. Ngoài quyền: trả lỗi cho U06, ghi audit.

##### F16 - Interface cho unit khác
1. U08 đọc bài, lịch, `submissionWindow` (`ON_TIME`, `LATE`, `CLOSED`) và nội dung đã khóa (khung, phần, `requiredDiagrams`, rubric) qua `IAssignmentQuery`; kiểm bài làm qua `IDocumentModel`.
2. U09 đọc rubric đã khóa và tính điểm qua `IRubric.score`; dùng `toPlainText`, `compactDiagramXml` khi dựng prompt AI chấm.
3. U12 đọc khung và phần cho tài liệu nhóm; cài `IGroupAssignmentSupport` và `IAssignmentLifecycleListener`.
4. U14 nhận `assignment.opened`; đọc bài sắp hết hạn qua `listClosingBetween` để nhắc hạn.

### U08 Assignment Work

Iter 2, Người 3, UC 37–41. Thiết kế: [u08-assignment-work](u08-assignment-work/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U08 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `ISubmissionQuery` | `listSubmissions(assignmentId, classId)` → trạng thái từng sinh viên và lượt được chấm; `getSubmission(attemptId)` → nội dung, `receiptHash`, `submittedAt`, `late`, `classId`, `accountId`; `listStudentsWithoutSubmission(assignmentId, classId)` | U09, U14 | Chấm và theo dõi nộp bài; nhắc hạn người chưa nộp |
| `IActivityWrite` | `record(actor, workRef, events)` | U11, U12 | Ghi sự kiện rời trang của lượt Quiz và tài liệu nhóm sau khi unit đó đã kiểm quyền |
| `IActivityQuery` | `summary(workRef, accountId)` → `ActivitySummary` | U09 | Hiện dữ liệu rời trang trong UC 42 sau khi U09 kiểm phân công |
| `IAssignmentLifecycleListener` (contract của U07, U08 cài) | `onRetired` (tự nộp lượt đang làm), `onOpened` (không làm gì) | U07 | Tự nộp khi bài bị ngưng giao |
| `StudentClassSection`, `DashboardNavEntry` (contract của U05, U08 cài) | - | U05 | Phần Bài tập trên Student Class Detail; lối vào Student Assignments trên Class Dashboard |
| `IAiTaskResultHandler` cho `PRACTICE_FEEDBACK`, `IMcpToolProvider` (contract của U06, U08 cài) | - | U06 | Nhận phản hồi AI; tool `read_own_work`, `read_authorized_submission`, `record_private_feedback` |
| `IScheduledScanner`, `SettingDefinition` (contract của U03, U08 cài) | - | U03 | Tự nộp khi hết hạn, xóa dữ liệu hoạt động quá hạn; khai báo `activity.retentionDays` |
| Hook `useActivityTracker`, component `ActivityTimeline` | - | Frontend U09, U11, U12 | Gửi sự kiện rời trang từ trình duyệt; hiện dòng thời gian cho giảng viên |

##### Contract U08 khai báo (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `ISubmissionListener` | `onSubmitted(attemptId)` | U09 | Tạo dòng chờ chấm trong cùng transaction nộp bài `GRADED` mà U08 không phụ thuộc ngược U09 |
| `IGroupWorkStatus` | `statusFor(accountId, assignmentIds)` | U12 | Trạng thái bài nhóm của người học trên Student Assignments |
| Slot frontend `StudentAssignmentResult` | - | U09 | Điểm đã công bố và phân bố ẩn danh trên từng dòng Student Assignments |
| Slot frontend `SubmissionResultSection` | - | U09 | Điểm và nhận xét đã công bố của lượt được chấm trên Submission History |
| Slot frontend `GroupWorkEntry` | - | U12 | Nút mở bài nhóm trên Assignment Detail |
| Slot frontend `AssignmentMaterialSection` | - | U13 | Tài liệu tham khảo gắn kèm trên Assignment Detail |

Chưa có U09, U12, U13 thì: không tạo dòng chờ chấm, không hiện điểm; bài nhóm chỉ có nhãn, không có nút; không hiện tài liệu gắn kèm.

#### Business flow

##### F1 - Danh sách bài của tôi (UC 37)
1. Student mở Student Assignments từ thanh bên Class Dashboard (U08 đăng ký `DashboardNavEntry`) hoặc từ phần Bài tập trên Student Class Detail (`StudentClassSection`, lọc sẵn lớp); frontend gọi `GET /api/v1/me/assignments` kèm lớp, trạng thái.
2. Lấy các lớp `OPEN` đang ghi danh (`IClassAccess.listOpenClassesOf`), rồi bài của từng lớp (`IAssignmentQuery.listForClass`, gồm bài của môn); bỏ bài người học không được thấy (BR-U08-01).
3. Ghép trạng thái của mình: bài cá nhân từ lượt làm; bài nhóm từ `IGroupWorkStatus` (khi U12 có, chưa có thì chỉ hiện nhãn "Bài nhóm").
4. Trả danh sách lọc theo lớp, trạng thái, sắp theo hạn gần nhất; cột điểm và phân bố ẩn danh do U09 gắn qua slot `StudentAssignmentResult`.
5. Bấm bài mở Assignment Detail; nút lịch sử mở Submission History.

##### F2 - Chi tiết bài (UC 38)
1. Frontend gọi `GET /api/v1/me/assignments/{assignmentId}` kèm `classId`.
2. Kiểm R5 với lớp và bài (BR-U08-01); không đạt thì "không tìm thấy".
3. Trả hướng dẫn, khung chỉ đọc (`IAssignmentQuery.getContent`), rubric từng phần (`IRubric.listRubrics`), cá nhân hay nhóm, `requiredDiagrams`, lịch, số lượt đã dùng và còn lại, lượt gần nhất, chính sách AI, thông báo theo dõi, `referenceMaterialIds` để U13 hiện tài liệu gắn kèm (BR-U08-03).
4. Hiện nút Bắt đầu hoặc Tiếp tục khi đủ điều kiện; bài nhóm hiện nút của U12 (slot `GroupWorkEntry`); nút Lịch sử nộp.

##### F3 - Bắt đầu hoặc tiếp tục lượt (UC 39)
1. Bấm Bắt đầu (hoặc Tiếp tục); frontend gọi `POST /api/v1/assignments/{assignmentId}/attempts` kèm `classId`.
2. Đã có lượt `IN_PROGRESS` thì trả lượt đó.
3. Khóa hàng theo (bài, người học); kiểm BR-U08-10, 11: ghi danh, bài cá nhân, `submissionWindow`, còn lượt.
4. Tạo lượt `IN_PROGRESS`: `attemptNo` kế tiếp, chụp theo BR-U08-12, nội dung bắt đầu từ khung, `deadlineAt`.
5. Mở Assignment Workspace; nếu theo dõi bật thì `useActivityTracker` bắt đầu gửi sự kiện (F11).

##### F4 - Làm bài và tự lưu (UC 39)
1. Workspace dùng `DocumentEditor` của U07 ở chế độ người học: block của người soạn bị khóa; người học thêm đoạn, danh sách, bảng, ảnh, sơ đồ (nếu bật) ở bất kỳ đâu.
2. 10 giây sau lần sửa cuối hoặc khi rời trang, frontend gọi `PUT /api/v1/attempts/{attemptId}/content` kèm `contentVersion` (nén gzip).
3. Kiểm chủ lượt, `IN_PROGRESS`, chưa quá `deadlineAt` cộng 30 giây, còn ghi danh; `validateForSave` (BR-U08-14).
4. Lệch `contentVersion` thì `409`; đạt thì lưu, tăng `contentVersion`, trả "Đã lưu lúc …".
5. Ảnh tải qua U03 (`store`) và được gắn (`attach`) khi lưu.

##### F5 - Nhập, xuất DOCX (UC 39, 40)
Nhập:
1. Trong Workspace bấm Nhập DOCX, chọn file ≤ 20 MB; frontend gọi `POST /api/v1/attempts/{attemptId}/docx:preview`.
2. Kiểm chủ lượt, `IN_PROGRESS`, chưa hết hạn; gọi `IDocx.importPreview` với block của người học và cờ sơ đồ; mã sơ đồ chỉ nhận khi trỏ tới đề của bài hoặc lượt của chính mình.
3. Trả bản xem trước; người học xác nhận thì thêm vào cuối bản nháp và lưu như F4 bước 2–4.

Xuất:
1. Trên Workspace hoặc Submission History bấm Tải DOCX; frontend gọi `GET /api/v1/attempts/{attemptId}/export.docx`.
2. Kiểm chủ lượt, hoặc giảng viên chính của lớp ghi trên lượt; gọi `IDocx.export` (sơ đồ kèm mã `[drawio:attemptId/blockId]`).

##### F6 - Nộp bài (UC 39)
1. Bấm Nộp, xác nhận; frontend lưu lần cuối rồi gọi `POST /api/v1/attempts/{attemptId}/submit` kèm `contentVersion`.
2. Lượt đã `SUBMITTED` thì trả lại biên nhận cũ (BR-U08-22).
3. Kiểm chủ lượt, `IN_PROGRESS`, trước `deadlineAt` cộng 30 giây, còn ghi danh; `validateForSubmit`. Có lỗi thì trả danh sách cần sửa; quá hạn thì ghi audit `SUBMIT_REJECTED`.
4. Một transaction: `SUBMITTED`, `submittedAt`, `late`, `receiptHash`, `submitMode = MANUAL`; gọi `ISubmissionListener.onSubmitted` (U09); audit `ASSIGNMENT_SUBMITTED`.
5. Trả biên nhận, mở Submission History.

##### F7 - Tự nộp
1. `IScheduledScanner` của U08 chạy mỗi phút: lượt `IN_PROGRESS` có `deadlineAt` cộng 30 giây đã qua được nộp bằng bản đã lưu gần nhất, `submitMode = AUTO_DEADLINE`, không kiểm điều kiện nộp, ghi cảnh báo (ví dụ thiếu sơ đồ bắt buộc); gọi `onSubmitted`; audit.
2. U07 ngưng giao bài thì gọi `onRetired` của U08: trong cùng transaction nộp mọi lượt `IN_PROGRESS` của bài với `AUTO_RETIRED` như bước 1.
3. Câu lệnh nộp có điều kiện trạng thái nên chạy lại không nộp hai lần.

##### F8 - Lịch sử nộp (UC 40)
1. Từ Assignment Detail hoặc Student Assignments mở Submission History; frontend gọi `GET /api/v1/assignments/{assignmentId}/attempts/mine` kèm `classId`.
2. Trả các lượt của mình: lượt thứ, thời điểm nộp, trễ, cách nộp, nhãn "được chấm" (bài `GRADED`, lượt nộp cuối), cảnh báo khi tự nộp.
3. Chọn một lượt: `GET /api/v1/attempts/{attemptId}` trả nội dung chỉ đọc và biên nhận; điểm và nhận xét đã công bố do U09 gắn qua slot `SubmissionResultSection`.
4. Bài `PRACTICE`: hiện lịch sử phản hồi AI của lượt (`GET /api/v1/attempts/{attemptId}/feedback`) và nút "Xin phản hồi AI" (F9); nút Tải DOCX (F5).

##### F9 - Xin phản hồi bằng API cá nhân (UC 41)
1. Trên lượt đã nộp của bài `PRACTICE` bấm "Xin phản hồi AI"; `AiSourcePicker` của U06 chỉ hiện kết nối cá nhân của người học; người học chọn nguồn.
2. Frontend gọi `POST /api/v1/attempts/{attemptId}/feedback` kèm nguồn và `requestRef`.
3. Kiểm BR-U08-40, 44: chủ lượt, `SUBMITTED`, `PRACTICE`, còn ghi danh; lượt chưa có yêu cầu đang chờ; chưa quá 20 yêu cầu trong ngày.
4. Dựng prompt theo BR-U08-45 (`IAssignmentQuery.getContent`, `IRubric.listRubrics`, `IDocumentModel.toPlainText`, `compactDiagramXml`) kèm JSON schema của phản hồi.
5. Gọi `IAiTask.submit` (loại `PRACTICE_FEEDBACK`, đích là lượt, `inputVersion` là `receiptHash`); tạo phản hồi `PENDING`; audit `PRACTICE_FEEDBACK_REQUESTED`; trả `taskId` cho `AiTaskStatus`.
6. U06 gọi `onReady`: kiểm lại chủ lượt, ghi danh, hash, schema; đạt thì lưu phản hồi `READY` kèm provenance và trả `APPLIED`, không thì trả `STALE` hoặc `REJECTED`. `onFailed` chuyển phản hồi sang `FAILED`.
7. Người học xem phản hồi; muốn thêm thì xin lại, mỗi lần là một bản trong lịch sử.

##### F10 - Phản hồi qua agent MCP (UC 41)
1. Agent của người học gọi `read_own_work(attemptId)`: U08 kiểm actor là chủ lượt, lượt `SUBMITTED`, bài `PRACTICE`; trả hướng dẫn, khung, rubric, bài nộp dạng chữ và `submissionHash`.
2. Agent tự gọi mô hình của nó rồi gọi `record_private_feedback(attemptId, submissionHash, feedback)` (credential `READ_WRITE`).
3. U08 kiểm như F9 bước 3 (trừ yêu cầu đang chờ), hash khớp, schema đúng; lưu phản hồi `READY` nguồn MCP (nhãn agent, model nếu agent báo).
4. Giảng viên dùng `read_authorized_submission(attemptId)`: U08 kiểm actor là giảng viên chính của lớp ghi trên lượt, lượt đã nộp; trả bài nộp dạng chữ, không có dữ liệu người khác.

##### F11 - Ghi dữ liệu hoạt động
1. Lượt có chính sách bật thì `useActivityTracker` trên Workspace gửi lô sự kiện (BR-U08-51) tới `POST /api/v1/attempts/{attemptId}/activity`, tối đa 50 sự kiện mỗi lô, gửi lại khi mạng lỗi.
2. Kiểm chủ lượt, `IN_PROGRESS`, chính sách trong ảnh chụp bật, giới hạn tần suất (BR-U08-52).
3. Ghi sự kiện, bỏ trùng theo `clientEventId`, ghi giờ server.
4. U11 (lượt Quiz) và U12 (tài liệu nhóm) kiểm quyền của mình rồi gọi `IActivityWrite.record`, chạy tiếp bước 2–3 với loại công việc tương ứng.

##### F12 - Tóm tắt và dọn dữ liệu hoạt động
1. U09 kiểm giảng viên được giao lớp rồi gọi `IActivityQuery.summary(workRef, accountId)`.
2. U08 ghép `HIDDEN` với `VISIBLE` theo phiên thành khoảng rời trang; khoảng không có heartbeat quá 2 phút, sự kiện lệch thứ tự, giờ client bất thường được đánh dấu "không rõ" (BR-U08-53).
3. Trả trạng thái chính sách, số lần, tổng thời gian đã biết, các khoảng và khoảng không rõ; không có kết luận.
4. Scanner xóa theo lô sự kiện có giờ server cũ hơn `activity.retentionDays` (BR-U08-54).

##### F13 - Interface cho unit khác
1. U09 đọc bài nộp theo bài và lớp, nội dung lượt, trạng thái từng sinh viên (chưa làm, đang làm, đã nộp, nộp trễ) qua `ISubmissionQuery`; cài `ISubmissionListener` để tạo dòng chờ chấm.
2. U14 lấy sinh viên chưa nộp của bài sắp hết hạn qua `listStudentsWithoutSubmission` để nhắc 24 giờ trước hạn; với bài nhóm, U08 dựa vào trạng thái nhóm từ `IGroupWorkStatus` của U12.
3. U11, U12 ghi sự kiện qua `IActivityWrite`; U12 cài `IGroupWorkStatus` và slot `GroupWorkEntry`.

### U09 Assignment Grading

Iter 2, Người 2, UC 42–45. Thiết kế: [u09-assignment-grading](u09-assignment-grading/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U09 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IGroupEvaluation` | `onGroupSubmitted(groupDocumentId, submissionHash, assignmentId, classId, memberIds)` | U12 | Tạo hoặc đưa về chờ chấm đánh giá tài liệu nhóm và thành viên trong transaction nộp |
| Sự kiện `grade.published` | - | U14 | Báo người học vừa có điểm (bài, lớp, danh sách người học) |
| `ISubmissionListener` (contract của U08, U09 cài) | `onSubmitted(attemptId)` | U08 | Tạo đánh giá chờ chấm khi nộp bài `GRADED` |
| `IAiTaskResultHandler` cho `GRADING_PROPOSAL`, `IMcpToolProvider` (contract của U06, U09 cài) | - | U06 | Nhận đề xuất AI; tool `record_grading_proposal` |
| `TeacherClassTab` (contract của U05, U09 cài) | - | U05 | Phần Sổ điểm trong tab Evals |
| Slot `AssignmentRowAction` (của U07, U09 cài) | - | U07 | Lối vào Submission Detail kèm số đã nộp, chờ chấm trên dòng bài |
| Slot `StudentAssignmentResult`, `SubmissionResultSection` (của U08, U09 cài) | - | U08 | Điểm đã công bố, phân bố ẩn danh; điểm và nhận xét của lượt được chấm |

##### Contract U09 khai báo (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `IGroupSubmissionSource` | `getGroupSubmission(groupDocumentId)` → nội dung, thành viên, người làm từng phần, thời điểm nộp, trễ, hash; `listGroupSubmissions(assignmentId, classId)` → trạng thái từng nhóm | U12 | Hiện và chấm bài nhóm, tiến độ theo nhóm mà U09 không phụ thuộc ngược U12 |

Chưa có U12 thì không có đánh giá nhóm; Submission Detail chỉ có bài cá nhân.

#### Business flow

##### F1 - Tạo đánh giá khi nộp
1. U08 gọi `ISubmissionListener.onSubmitted(attemptId)` trong transaction nộp.
2. U09 đọc lượt qua `ISubmissionQuery.getSubmission` và bài qua `IAssignmentQuery.get`; bài `PRACTICE` thì bỏ qua.
3. Tạo đánh giá `PENDING` (lượt, bài, lớp, người học, `submissionHash`, tổng điểm từ `IRubric.maxPoints`).
4. Đánh giá cũ chưa công bố của cùng (bài, người học) chuyển `SUPERSEDED` (BR-U09-10).

##### F2 - Danh sách bài nộp và tiến độ (UC 42)
1. Giảng viên bấm "Bài nộp" trên dòng bài ở tab Evals (U09 gắn qua slot `AssignmentRowAction` của U07); frontend gọi `GET /api/v1/classes/{classId}/assignments/{assignmentId}/submissions`.
2. Kiểm giảng viên chính của lớp (BR-U09-01); bài là `GRADED` của lớp hoặc của môn chứa lớp.
3. Lấy tiến độ từng sinh viên qua `ISubmissionQuery.listSubmissions` (bài nhóm qua `IGroupSubmissionSource`), ghép với đánh giá của lớp.
4. Trả số đã nộp, đang làm, chưa nộp, nộp trễ, thời gian còn lại và bảng bài nộp (trạng thái chấm, điểm, trễ); lọc theo trạng thái, trễ.
5. Từ đây: mở một bài (F3), AI chấm hàng loạt (F6), chốt (F8), công bố (F9).

##### F3 - Mở bài để chấm (UC 42)
1. Bấm một bài; frontend gọi `GET /api/v1/evaluations/{id}`.
2. Kiểm giảng viên chính của lớp ghi trên đánh giá.
3. Trả bài nộp (nội dung qua `ISubmissionQuery.getSubmission` hoặc `IGroupSubmissionSource`), rubric đã khóa từng phần (`IRubric.listRubrics`), đánh giá, các đề xuất AI, lịch sử điểm.
4. Bài có theo dõi hoạt động: hiện `ActivityTimeline` với dữ liệu từ `IActivityQuery.summary` (bài nhóm theo từng thành viên) (BR-U09-13).
5. Nút ‹ › chuyển bài theo bộ lọc của Submission Detail hoặc theo lô vừa chấm AI.

##### F4 - Chấm tay (UC 44)
1. Giảng viên tích mục đạt của từng phần; điểm phần và điểm bài tự tính qua `IRubric.score`.
2. Tùy chọn nhập điều chỉnh (kèm lý do) và nhận xét.
3. Lưu: `PUT /api/v1/evaluations/{id}` kèm `version`; kiểm quyền, đánh giá không `SUPERSEDED`, mục thuộc rubric, điều chỉnh có lý do, điểm cuối trong 0 tới tổng.
4. Đánh giá `PENDING` hoặc `DRAFT` thành `DRAFT`; ghi người chấm, thời điểm; audit `GRADE_SAVED`. Đánh giá đã chốt hoặc đã công bố đi theo F10.
5. Tự mở bài kế.

##### F5 - AI đề xuất một bài (UC 43)
1. Trong Grading Workspace bấm "AI đề xuất"; `AiSourcePicker` của U06 hiện API chung (kèm quota còn lại) và kết nối cá nhân; giảng viên chọn.
2. Frontend gọi `POST /api/v1/evaluations/{id}/ai-proposals` kèm nguồn, `requestRef`.
3. Kiểm quyền, đánh giá chưa `SUPERSEDED`; dựng prompt theo BR-U09-31 (`IAssignmentQuery.getContent`, `IRubric.listRubrics`, `IDocumentModel.toPlainText`, `compactDiagramXml`) kèm JSON schema.
4. Gọi `IAiTask.submit` (loại `GRADING_PROPOSAL`, đích là đánh giá, `inputVersion` là `submissionHash`); tạo đề xuất `PENDING` version kế tiếp; audit `AI_GRADING_REQUESTED`.
5. U06 gọi `onReady`: kiểm quyền người yêu cầu, hash còn khớp, schema, id mục thuộc rubric; đạt thì lưu đề xuất `READY` kèm provenance, không thì `STALE` hoặc `REJECTED`. `onFailed` chuyển `FAILED`.
6. Giảng viên bấm "Dùng đề xuất": điền sẵn checklist và nhận xét; sửa nếu muốn rồi lưu như F4 bước 3–4; ghi đề xuất đã dùng.

##### F6 - AI chấm hàng loạt (UC 43)
1. Trong Submission Detail chọn nhiều bài `PENDING` hoặc `DRAFT`, bấm "Chấm hàng loạt bằng AI", chọn một nguồn.
2. Frontend gọi `POST /api/v1/evaluations:ai-proposals` kèm danh sách và nguồn.
3. Với từng bài chạy F5 bước 3–4 (`requestRef` riêng); hết quota ở bài nào thì bài đó và các bài sau trả "hết quota".
4. Trả kết quả từng mục; danh sách theo dõi trạng thái các task.
5. Giảng viên mở Grading Workspace theo lô, xác nhận từng bài như F5 bước 6.

##### F7 - Đề xuất qua agent MCP (UC 43)
1. Agent của giảng viên đọc bài qua `read_authorized_submission` (U08) hoặc `read_authorized_group_submission` (U12, bài nhóm) và rubric qua `read_assignment_context` (U07).
2. Agent gọi `record_grading_proposal(submissionRef, submissionHash, proposal)` với credential `READ_WRITE`.
3. U09 tìm đánh giá theo lượt hoặc tài liệu nhóm; kiểm giảng viên chính của lớp, hash khớp, schema và id mục; lưu đề xuất `READY` nguồn MCP (nhãn agent, model nếu agent báo).
4. Giảng viên dùng đề xuất như F5 bước 6.

##### F8 - Chốt (UC 43, 44)
1. Chốt một bài trong Grading Workspace, hoặc chọn nhiều bài trong Submission Detail rồi xác nhận; frontend gọi `POST /api/v1/evaluations:finalize` kèm id và `version`.
2. Với từng mục: kiểm quyền, `DRAFT`, điểm hợp lệ, `version` khớp; đạt thì `FINALIZED`, audit `GRADE_FINALIZED`.
3. Trả kết quả từng mục; mục lỗi không ảnh hưởng mục khác.

##### F9 - Công bố (UC 43, 44)
1. Công bố một bài, hoặc chọn nhiều bài đã chốt ("chọn tất cả đã chốt"); frontend gọi `POST /api/v1/evaluations:publish`.
2. Với từng mục: kiểm quyền, `FINALIZED`, `version`; đạt thì `PUBLISHED`, ghi `publishedAt`; audit `GRADE_PUBLISHED`.
3. Sau commit phát `grade.published` (bài, lớp, người học vừa có điểm; bài nhóm là các thành viên) cho U14.
4. Trả kết quả từng mục.

##### F10 - Sửa điểm đã chốt hoặc đã công bố (UC 44)
1. Trong Grading Workspace sửa checklist, điều chỉnh, nhận xét của đánh giá `FINALIZED` hoặc `PUBLISHED`, nhập lý do.
2. `PUT /api/v1/evaluations/{id}` kèm `version` và lý do; thiếu lý do thì từ chối.
3. Giữ trạng thái, thêm một mục lịch sử; audit `GRADE_CHANGED`.
4. Đánh giá đã công bố: người học thấy điểm mới kèm nhãn "đã cập nhật".

##### F11 - Sổ điểm (UC 45)
1. Giảng viên mở phần Sổ điểm trong tab Evals (U09 gắn qua `TeacherClassTab`); frontend gọi `GET /api/v1/classes/{classId}/gradebook`.
2. Kiểm giảng viên chính của lớp.
3. Lấy bài `GRADED` của lớp và của môn chứa lớp (`IAssignmentQuery.listForClass`), sinh viên đang ghi danh (`IClassAccess.listActiveStudents`), đánh giá của lớp và tiến độ nộp (`ISubmissionQuery`).
4. Trả mỗi sinh viên một mục đóng mở chứa từng bài: "x / tổng" của lượt được chấm và trạng thái (BR-U09-60); không có điểm tổng.

##### F12 - Xuất bảng điểm (UC 45)
1. Bấm Xuất trên Sổ điểm, chọn CSV hoặc XLSX, tùy chọn một bài; frontend gọi `GET /api/v1/classes/{classId}/gradebook/export`.
2. Kiểm quyền và bộ lọc trước khi tạo tệp; ngoài quyền trả `404`.
3. Tạo tệp theo BR-U09-61 dạng luồng; chặn công thức trong CSV; tên tệp gồm mã lớp, mã bài, ngày.
4. Ghi audit `GRADEBOOK_EXPORTED` (phạm vi, định dạng); không lưu tệp.

##### F13 - Người học xem điểm và phân bố
1. Student Assignments (U08) nhúng `StudentGradeCell` qua slot `StudentAssignmentResult`; component gọi `GET /api/v1/me/grades` và `GET /api/v1/me/grade-distributions` kèm lớp.
2. U09 kiểm người học đang ghi danh lớp; trả điểm `PUBLISHED` của chính mình theo từng bài.
3. Phân bố: kiểm BR-U09-62 với từng bài; đủ điều kiện thì trả khoảng điểm, ẩn khoảng dưới 5 người; không đủ thì không trả.
4. Submission History (U08) nhúng `StudentGradePanel` qua slot `SubmissionResultSection`; gọi `GET /api/v1/me/grades/attempts/{attemptId}`: điểm, nhận xét, mục rubric đạt, nhãn "đã cập nhật" nếu có; lượt chưa công bố thì trả "chưa có điểm".

##### F14 - Bài nhóm (khi U12 có)
1. U12 gọi `IGroupEvaluation.onGroupSubmitted` trong transaction nộp: tạo hoặc đưa về `PENDING` đánh giá tài liệu nhóm và đánh giá từng thành viên (BR-U09-50).
2. Submission Detail hiện nhóm thay cho sinh viên; Grading Workspace đọc tài liệu nhóm qua `IGroupSubmissionSource`, hiện từng phần kèm người làm.
3. Chấm tài liệu nhóm như F4, F5; điểm thành viên mặc định bằng điểm tài liệu chung; giảng viên sửa từng người qua `PUT /api/v1/evaluations/{id}/members/{accountId}` (BR-U09-51).
4. Chốt, công bố cả đánh giá tài liệu và đánh giá thành viên như F8, F9.

### U10 Quiz Authoring

Iter 2, Người 1, UC 46–48. Thiết kế: [u10-quiz-authoring](u10-quiz-authoring/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U10 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IQuizQuery` | `listForClass(classId)` → Quiz `OPEN` của lớp và của môn; `getStudentView(quizId)` → câu, lựa chọn, cài đặt, không có đáp án; `listByMaterial(materialId, classId)`; `get(quizId)` | U11, U13 | Hiện Quiz cho người học; Quiz gắn tài liệu trên Learning Material |
| `IQuizScoring` | `score(quizId, answers)` → đúng sai và điểm từng câu, tổng; `answerReview(quizId)` → đáp án, giải thích | U11 | Chấm theo đáp án ghim mà đáp án không rời U10; hiện đáp án sau khi nộp theo cài đặt |
| Sự kiện `quiz.published` | - | U14 | Báo Quiz mới trong app |
| `TeacherClassTab`, `DashboardNavEntry` (contract của U05, U10 cài) | - | U05 | Phần Quiz trong tab Evals; lối vào Question List, Quiz List trên Manager Dashboard |
| `IAiTaskResultHandler` cho `QUIZ_QUESTION_DRAFT`, `IMcpToolProvider` (contract của U06, U10 cài) | - | U06 | Nhận câu đề xuất; tool `read_quiz_context`, `save_quiz_draft` |
| Component `McqEditor` | - | Frontend U10 | Soạn câu trắc nghiệm, dùng ở Question Detail và câu riêng của Quiz |

##### Contract U10 khai báo (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `IQuizLifecycleListener` | `onRetired(quizId)` | U11 | Tự nộp lượt đang làm trong cùng transaction ngưng Quiz hoặc phát hành version mới |
| Slot frontend `QuizDetailSection` | - | U11 | Lượt làm và dữ liệu hoạt động của lượt trên Quiz Detail cho giảng viên |

Chưa có U11 thì listener rỗng và Quiz Detail không có phần lượt làm.

#### Business flow

##### F1 - Xem ngân hàng (UC 46)
1. Chủ nhiệm môn bấm Question List trên Manager Dashboard (U10 đăng ký `DashboardNavEntry`), chọn môn mình quản lý; frontend gọi `GET /api/v1/subjects/{subjectId}/questions` kèm bộ lọc.
2. Kiểm R2 (BR-U10-01).
3. Trả bản `ACTIVE` mới nhất mỗi lineage kèm cờ "có bản nháp" và các câu mới chỉ có bản nháp (BR-U10-06).
4. Bấm câu mở Question Detail: `GET /api/v1/questions/{id}` gồm nội dung, đáp án, giải thích và lịch sử phiên bản.

##### F2 - Tạo, sửa, kích hoạt, xóa câu (UC 47)
1. Tạo: trên Question List bấm "Tạo câu", nhập loại, nội dung, lựa chọn, đáp án, giải thích, điểm, độ khó, tag; `POST /api/v1/subjects/{subjectId}/questions` tạo bản `DRAFT`.
2. Sửa bản `DRAFT`: `PATCH /api/v1/questions/{id}` kèm `version`; kiểm BR-U10-02, 03.
3. Sửa bản `ACTIVE`: `POST /api/v1/questions/{id}/versions` tạo bản `DRAFT` mới cùng lineage (đã có bản nháp thì mở bản đó) (BR-U10-04).
4. Kích hoạt: `POST /api/v1/questions/{id}/activate`; kiểm nội dung hợp lệ; bản `ACTIVE` cũ của lineage giữ nguyên cho Quiz đang ghim; audit `QUESTION_ACTIVATED`.
5. Ngưng: `POST .../retire` (không chọn vào Quiz mới được nữa). Xóa: `DELETE .../{id}` chỉ với bản `DRAFT` chưa từng kích hoạt, còn lại trả gợi ý ngưng dùng (BR-U10-05). Audit.

##### F3 - Nhập câu từ tệp (UC 47)
1. Trên Question List bấm Nhập, tải tệp mẫu, chọn tệp ≤ 5 MB; frontend gọi `POST /api/v1/subjects/{subjectId}/questions:import`.
2. Kiểm R2, định dạng, ≤ 500 dòng (BR-U10-07).
3. Kiểm từng dòng như F2 bước 1; dòng hợp lệ tạo câu `DRAFT`, dòng lỗi không tạo.
4. Trả bảng kết quả từng dòng; audit `QUESTIONS_IMPORTED` một lần.

##### F4 - AI soạn câu cho ngân hàng (UC 47)
1. Trên Question List bấm "Soạn bằng AI": nhập số câu, loại, độ khó, chủ đề; chọn tài liệu tham khảo qua `IReferenceMaterial` (chưa có U13 thì bỏ qua); chọn nguồn trong `AiSourcePicker` của U06.
2. Frontend gọi `POST /api/v1/subjects/{subjectId}/question-ai-drafts`; kiểm R2.
3. Lấy chữ tài liệu (`extractText`), dựng prompt kèm JSON schema; gọi `IAiTask.submit` (loại `QUIZ_QUESTION_DRAFT`, đích là ngân hàng môn); tạo đề xuất `PENDING`.
4. U06 gọi `onReady`: kiểm schema và quy tắc câu; lưu đề xuất `READY` kèm provenance.
5. Chủ nhiệm môn chọn, sửa, chấp nhận câu: mỗi câu thành câu `DRAFT` của ngân hàng; audit `QUIZ_AI_DRAFT_ACCEPTED`. Kích hoạt như F2 bước 4.

##### F5 - Danh sách Quiz (UC 48)
1. Chủ nhiệm môn mở Quiz List từ Manager Dashboard, chọn môn: `GET /api/v1/subjects/{subjectId}/quizzes`.
2. Giảng viên mở tab Evals; phần Quiz do U10 gắn qua `TeacherClassTab`: `GET /api/v1/classes/{classId}/quizzes` trả Quiz của lớp và Quiz của môn chứa lớp (nhãn "Của môn", chỉ đọc).
3. Mỗi dòng: tiêu đề, số câu, tổng điểm, trạng thái, version, tài liệu gắn kèm; mặc định ẩn `RETIRED`; bấm Quiz mở Quiz Detail; nút "Tạo Quiz".

##### F6 - Tạo Quiz (UC 48)
1. Bấm "Tạo Quiz" ở Quiz List (Quiz của môn) hoặc tab Evals (Quiz của lớp; có thể chọn "Copy Quiz lớp khác", F11).
2. Kiểm quyền (R2 với môn, R3/R4 với lớp) và lớp, môn chưa lưu trữ.
3. Tạo Quiz `DRAFT`, `versionNo` 1, cài đặt mặc định (không trộn, không giới hạn giờ, 1 lượt, hiện điểm, không hiện đáp án, theo dõi tắt); audit `QUIZ_CREATED`; mở Quiz Detail.

##### F7 - Soạn câu và cài đặt Quiz (UC 48)
1. Quiz Detail gọi `GET /api/v1/quizzes/{id}`: thông tin, cài đặt, danh sách câu kèm điểm, tổng điểm.
2. Thêm câu ngân hàng: chọn tay (`GET /api/v1/subjects/{subjectId}/questions?status=ACTIVE`) hoặc bốc ngẫu nhiên (`POST /api/v1/quizzes/{id}/items:random-preview`); xem lại, bỏ hoặc bốc lại; lưu `PUT /api/v1/quizzes/{id}/items` kèm `version` (BR-U10-13, 14).
3. Viết câu riêng: `POST /api/v1/quizzes/{id}/inline-questions` theo BR-U10-02, 03.
4. Sắp thứ tự, đặt điểm từng câu; lưu kèm `version`.
5. Sửa tiêu đề, hướng dẫn, cài đặt, theo dõi hoạt động, tài liệu gắn kèm: `PATCH /api/v1/quizzes/{id}` kèm `version`; bật theo dõi cần `activity.retentionDays` (đọc qua `ISettings`) (BR-U10-15).
6. Quiz đang `REVIEWED` thì về `DRAFT`.

##### F8 - AI soạn câu cho Quiz (UC 48)
1. Trên Quiz Detail của Quiz `DRAFT` bấm "Soạn bằng AI", nhập yêu cầu, chọn tài liệu và nguồn như F4 bước 1.
2. Frontend gọi `POST /api/v1/quizzes/{id}/ai-drafts`; kiểm quyền và `DRAFT`.
3. Gọi `IAiTask.submit` (loại `QUIZ_QUESTION_DRAFT`, đích là Quiz, `inputVersion` là `version` hiện tại); tạo đề xuất `PENDING`.
4. `onReady`: Quiz còn `DRAFT` thì lưu đề xuất `READY`; không thì `REJECTED`.
5. Người soạn chọn, sửa, chấp nhận: câu thành câu riêng của Quiz, thêm vào cuối danh sách (không vượt 200 câu); kèm `version`; audit `QUIZ_AI_DRAFT_ACCEPTED`.

##### F9 - Xem trước, duyệt, phát hành (UC 48)
1. Xem trước như người học: câu, lựa chọn, không có đáp án.
2. Duyệt: `POST /api/v1/quizzes/{id}/review` kèm `version`; kiểm BR-U10-21; lỗi thì trả danh sách cần sửa; đạt thì `REVIEWED`; audit.
3. Phát hành: `POST /api/v1/quizzes/{id}/publish` kèm `version`; kiểm lớp `OPEN` (`IClassAccess`) hoặc môn `ACTIVE` (`ISubjectQuery`).
4. Một transaction: Quiz `OPEN`, `publishedAt`; có version cũ đang `OPEN` thì chuyển `RETIRED` và gọi `IQuizLifecycleListener.onRetired` của nó; audit `QUIZ_PUBLISHED`.
5. Sau commit phát `quiz.published` (Quiz, lớp hoặc môn) cho U14.

##### F10 - Ngưng Quiz (UC 48)
1. Bấm Ngưng, nhập lý do; `POST /api/v1/quizzes/{id}/retire`.
2. Kiểm quyền và `OPEN`.
3. Một transaction: `RETIRED`, gọi `onRetired` (U11 tự nộp lượt đang làm); audit `QUIZ_RETIRED`.

##### F11 - Nhân bản, version mới, copy (UC 48)
1. Nhân bản: `POST /api/v1/quizzes/{id}/clone`; version mới: `POST .../versions` (kiểm chưa có version đang mở); copy giữa lớp: `GET /api/v1/classes/{classId}/quiz-copy-sources`, `POST /api/v1/classes/{classId}/quizzes:copy` (kiểm dạy cả hai lớp).
2. Một transaction: tạo Quiz `DRAFT` theo BR-U10-31 (câu ngân hàng giữ phiên bản ghim, câu riêng sao mới); audit `QUIZ_CLONED`, `QUIZ_VERSION_CREATED` hoặc `QUIZ_COPIED`.
3. Mở Quiz Detail của Quiz mới.

##### F12 - Xóa Quiz nháp (UC 48)
1. Bấm Xóa, xác nhận; `DELETE /api/v1/quizzes/{id}` kèm `version`.
2. Kiểm quyền và Quiz chưa từng phát hành (BR-U10-24).
3. Xóa Quiz, câu riêng, đề xuất AI; audit `QUIZ_DELETED`.

##### F13 - Tool MCP
1. `read_quiz_context(quizId)`: kiểm actor là giảng viên hoặc Chủ nhiệm môn có quyền với Quiz; trả tiêu đề, cài đặt, câu, lựa chọn, đáp án, giải thích, `version`.
2. `save_quiz_draft(quizId, expectedVersion, questions)`: credential `READ_WRITE`, Quiz `DRAFT`, `expectedVersion` khớp; kiểm câu theo BR-U10-02, 03; lưu thành đề xuất `READY` nguồn MCP; người soạn chấp nhận như F8 bước 5.
3. Người học gọi thì bị từ chối (BR-U10-42).

##### F14 - Interface cho unit khác
1. U11 lấy Quiz người học được thấy (`IQuizQuery.listForClass`), đề không có đáp án và cài đặt (`getStudentView`), chấm bài (`IQuizScoring.score`) và đáp án để hiện sau khi nộp theo cài đặt (`answerReview`).
2. U13 lấy Quiz gắn một tài liệu (`listByMaterial`) để hiện trên Learning Material; U11 cài `IQuizLifecycleListener` và slot `QuizDetailSection`.
3. U14 nhận `quiz.published` để báo trong app.

## Iter 3: phần thêm

| Unit | Người | UC | LOC (nghìn) | Làm gì |
|---|---|---|---|---|
| U11 Quiz Practice | Người 1 | 49–52 | 2 | Lịch sử luyện tập, làm Quiz có đồng hồ, chấm tự động theo đáp án, xem kết quả theo cài đặt; giảng viên xem lượt làm |
| U12 Group Assignment | Người 2 | 53–57 | 5 | Chia nhóm (tay hoặc ngẫu nhiên), đổi trưởng nhóm, tài liệu nhóm chia mục theo khung đề, nhận hoặc giao mục, realtime qua SSE, trưởng nhóm nộp |
| U13 Reference Materials | Người 4 | 58–62 | 2 | Tài liệu tệp và link YouTube của môn hoặc lớp, có phiên bản, trích chữ cho AI |
| U14 Announcements & Notifications | Người 3 | 63–65 | 2 | Thông báo lớp, thông báo trong app (chuông), email có trần ngày, nhắc hạn 24 giờ trước hạn |
| U15 Admin Dashboard | Người 5 | 66 | 1 | Số liệu hệ thống, trạng thái và lịch sử backup |

### U11 Quiz Practice

Iter 3, Người 1, UC 49–52. Thiết kế: [u11-quiz-practice](u11-quiz-practice/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U11 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IQuizLifecycleListener` (contract của U10, U11 cài) | `onRetired(quizId)` | U10 | Tự nộp và chấm lượt đang làm khi Quiz bị ngưng hoặc có version mới |
| Slot `QuizDetailSection` (của U10, U11 cài) | - | U10 | Phần Lượt làm và hoạt động cho giảng viên trên Quiz Detail |
| Slot `LearningMaterialSection` (của U13, U11 cài) | - | U13 | Danh sách Quiz gắn tài liệu kèm nút Làm Quiz trên Learning Material |
| `DashboardNavEntry` (contract của U05, U11 cài) | - | U05 | Lối vào Quiz Practice History trên Class Dashboard |
| `IScheduledScanner` (contract của U03, U11 cài) | - | U03 | Tự nộp khi hết giờ |

##### Contract U11 khai báo (unit khác cài)

| Contract | Unit cài | Dùng để |
|---|---|---|
| Slot frontend `QuizMaterialSection` | U13 | Tài liệu gắn kèm Quiz trên Quiz Practice Detail |

#### Business flow

##### F1 - Lịch sử luyện tập (UC 49)
1. Student mở Quiz Practice History từ thanh bên Class Dashboard (U11 đăng ký `DashboardNavEntry`); frontend gọi `GET /api/v1/me/quizzes` kèm lớp.
2. Lấy các lớp `OPEN` đang học (`IClassAccess.listOpenClassesOf`), Quiz đang mở của từng lớp (`IQuizQuery.listForClass`) và các lượt của mình.
3. Ghép thành danh sách Quiz: số lượt, điểm cao nhất, điểm lượt gần nhất (chỉ khi Quiz cho hiện điểm), nhãn "chưa làm"; Quiz đã ngưng mà có lượt thì hiện để xem lại (BR-U11-02).
4. Bấm Quiz mở Quiz Practice Detail; bấm lượt đã nộp mở Quiz Result; lượt đang làm mở Quiz Taking.

##### F2 - Chi tiết Quiz (UC 50)
1. Frontend gọi `GET /api/v1/me/quizzes/{quizId}` kèm `classId`.
2. Kiểm R5 với lớp và Quiz (BR-U11-01); không đạt thì "không tìm thấy".
3. Trả thông tin theo BR-U11-03 (`IQuizQuery.get`) và các lượt của mình.
4. Nút Làm Quiz hoặc Tiếp tục khi Quiz `OPEN` và còn lượt; không thì nêu lý do (đã ngưng, hết lượt).
5. Learning Material (U13) nhúng `MaterialQuizList` của U11 qua slot `LearningMaterialSection`: Quiz gắn tài liệu đó (`IQuizQuery.listByMaterial`) kèm lượt đã dùng, còn lại và nút Làm Quiz.

##### F3 - Bắt đầu hoặc tiếp tục lượt (UC 51)
1. Bấm Làm Quiz (hoặc Tiếp tục); frontend gọi `POST /api/v1/quizzes/{quizId}/attempts` kèm `classId`.
2. Đã có lượt `IN_PROGRESS` thì trả lượt đó.
3. Khóa hàng theo (Quiz, người học); kiểm ghi danh, Quiz `OPEN` của lớp hoặc môn chứa lớp, còn lượt (BR-U11-10).
4. Tạo lượt: `attemptNo` kế tiếp, chụp theo BR-U11-11, sinh seed, `deadlineAt`.
5. Mở Quiz Taking.

##### F4 - Làm Quiz và tự lưu (UC 51)
1. Quiz Taking gọi `GET /api/v1/quiz-attempts/{attemptId}`: đề không có đáp án (`getStudentView`), thứ tự theo seed, câu trả lời đã lưu, giờ còn lại.
2. Có đồng hồ khi Quiz giới hạn giờ; thanh điều hướng câu đánh dấu câu đã trả lời.
3. 2 giây sau lần chọn cuối hoặc khi rời trang: `PUT /api/v1/quiz-attempts/{attemptId}/answers` kèm `answerVersion`.
4. Kiểm chủ lượt, `IN_PROGRESS`, chưa quá `deadlineAt` cộng 30 giây, câu và lựa chọn thuộc đề; lệch `answerVersion` thì `409`; đạt thì lưu (BR-U11-12).

##### F5 - Nộp và chấm (UC 51)
1. Bấm Nộp, xác nhận (báo số câu chưa trả lời); frontend lưu lần cuối rồi gọi `POST /api/v1/quiz-attempts/{attemptId}/submit`.
2. Lượt đã nộp thì trả kết quả cũ.
3. Kiểm chủ lượt, `IN_PROGRESS`, trước `deadlineAt` cộng 30 giây; quá giờ thì audit `QUIZ_SUBMIT_REJECTED` (scanner sẽ tự nộp).
4. Một transaction: `SUBMITTED`, `submittedAt`, `submitMode = MANUAL`; gọi `IQuizScoring.score` và lưu điểm, đúng sai từng câu (BR-U11-15); audit `QUIZ_ATTEMPT_SUBMITTED`.
5. Mở Quiz Result.

##### F6 - Tự nộp
1. `IScheduledScanner` của U11 chạy mỗi phút: lượt `IN_PROGRESS` có `deadlineAt` cộng 30 giây đã qua được nộp bằng câu trả lời đã lưu, `AUTO_TIME_LIMIT`, chấm như F5 bước 4.
2. U10 ngưng Quiz hoặc phát hành version mới thì gọi `onRetired` của U11: trong cùng transaction nộp và chấm mọi lượt `IN_PROGRESS` của Quiz đó với `AUTO_RETIRED`.
3. Câu lệnh nộp có điều kiện trạng thái nên chạy lại không nộp hai lần.

##### F7 - Xem kết quả (UC 52)
1. Frontend gọi `GET /api/v1/quiz-attempts/{attemptId}/result`.
2. Kiểm chủ lượt và lượt đã nộp.
3. Trả câu trả lời của mình, số câu đúng; điểm nếu Quiz cho hiện điểm; đáp án đúng và giải thích qua `answerReview` nếu Quiz đặt `AFTER_SUBMIT` (BR-U11-20).
4. Nút quay lại Quiz Practice Detail và Làm lại khi còn lượt.

##### F8 - Ghi dữ liệu hoạt động
1. Lượt có chính sách bật thì `useActivityTracker` (U08) trên Quiz Taking gửi lô sự kiện tới `POST /api/v1/quiz-attempts/{attemptId}/activity`.
2. U11 kiểm chủ lượt, `IN_PROGRESS`, chính sách đã chụp bật.
3. Gọi `IActivityWrite.record` với loại công việc `QUIZ_ATTEMPT`; U08 bỏ trùng, giới hạn tần suất, ghi giờ server.

##### F9 - Giảng viên xem lượt (UC 42)
1. Trên Quiz Detail (U10), phần Lượt làm do U11 gắn qua `QuizDetailSection`; frontend gọi `GET /api/v1/classes/{classId}/quizzes/{quizId}/attempts`.
2. Kiểm giảng viên chính của lớp; Quiz là Quiz của lớp hoặc của môn chứa lớp.
3. Trả mỗi sinh viên: số lượt, điểm cao nhất, điểm lượt gần nhất, thời điểm nộp.
4. Mở một lượt: câu trả lời chỉ đọc, điểm, và `ActivityTimeline` (U08) với dữ liệu từ `IActivityQuery.summary` (BR-U11-31).

### U12 Group Assignment

Iter 3, Người 2, UC 53–57. Thiết kế: [u12-group-assignment](u12-group-assignment/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U12 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `ISseHub` | `publish(channelKey, event)`, `subscribe(channelKey)` | U12, U14 | Kênh realtime qua SSE cho mọi instance backend; U14 dùng cho chuông thông báo |
| Sự kiện `group.membership-changed`, `group.leader-change-requested`, `group.leader-changed`, `group.leader-request-rejected`, `group.submitted` | - | U14 | Báo trong app cho đúng người (sự kiện mang danh sách người nhận) |
| `IGroupAssignmentSupport`, `IAssignmentLifecycleListener` (contract của U07, U12 cài) | `checkReadiness(classId)`; `onOpened`, `onRetired` | U07 | Bật bài nhóm, kiểm sẵn sàng; tạo tài liệu khi mở, tự nộp khi ngưng giao |
| Slot `AssignmentFormSection` (của U07, U12 cài) | - | U07 | Phần Nhóm trên Assignment Form |
| `IGroupWorkStatus` (contract của U08, U12 cài), slot `GroupWorkEntry` | `statusFor(accountId, assignmentIds)` | U08 | Trạng thái bài nhóm trên Student Assignments; nút mở bài nhóm trên Assignment Detail |
| `IGroupSubmissionSource` (contract của U09, U12 cài) | `getGroupSubmission`, `listGroupSubmissions` | U09 | Hiện, chấm bài nhóm và tiến độ theo nhóm |
| `StudentClassSection`, `TeacherClassTab`, `StudentDetailSection` (contract của U05, U12 cài) | - | U05 | My Group; phần Nhóm trong tab Students; nhóm của sinh viên trong popup Student Detail |
| `IMcpToolProvider` (contract của U06, U12 cài) | tool `read_authorized_group_submission` | U06 | Agent của giảng viên đọc bản nộp nhóm để đề xuất chấm |
| `IJobHandler` cho `GROUP_DOC_CREATE`, `IPendingSweeper`, `IScheduledScanner` (contract của U03, U12 cài) | - | U03 | Tạo tài liệu nhóm khi bài mở; tự nộp khi hết hạn |

##### Contract U12 khai báo

Không có.

#### Business flow

##### F1 - Quản lý nhóm (UC 57)
1. Giảng viên mở tab Students; phần Nhóm do U12 gắn qua `TeacherClassTab`; frontend gọi `GET /api/v1/classes/{classId}/groups`.
2. Kiểm giảng viên chính của lớp (BR-U12-01); trả nhóm, thành viên (người đã rời lớp có nhãn), trưởng nhóm, sinh viên chưa có nhóm, yêu cầu đang chờ, `version`.
3. Giảng viên tạo nhóm, thêm, chuyển, gỡ thành viên, chọn trưởng nhóm trên bản sửa ở trình duyệt.
4. Lưu: `PUT /api/v1/classes/{classId}/groups` kèm cả bộ nhóm và `version`.
5. Kiểm lớp `DRAFT` hoặc `OPEN`, thành viên ghi danh `ACTIVE` (`IClassAccess`), mỗi người một nhóm, mỗi nhóm có trưởng nhóm là thành viên, nhóm bị xóa chưa có tài liệu có nội dung (BR-U12-02, 05, 06); lỗi thì trả từng lỗi, không lưu gì.
6. Một transaction: ghi nhóm; với thay đổi khi có bài nhóm đang mở chạy F14; audit; sau commit phát `group.membership-changed` và `group.leader-changed` (kèm người nhận).

##### F2 - Chia ngẫu nhiên (UC 57)
1. Bấm Chia ngẫu nhiên, nhập sĩ số tối đa 2–20; frontend gọi `POST /api/v1/classes/{classId}/groups:random-preview`.
2. Lấy sinh viên đang học chưa có nhóm; chia thành nhóm chênh nhau tối đa một người; chọn ngẫu nhiên trưởng nhóm (BR-U12-03).
3. Trả bản xem trước gộp với nhóm hiện có; giảng viên sửa rồi lưu như F1 bước 4–6.

##### F3 - Duyệt yêu cầu đổi trưởng nhóm (UC 57)
1. Tab Students hiện yêu cầu đang chờ; giảng viên bấm Duyệt hoặc Từ chối.
2. Duyệt: chọn trưởng nhóm mới (mặc định người được đề cử); `POST /api/v1/leader-requests/{id}:approve`. Từ chối: nhập lý do; `POST .../{id}:reject`.
3. Kiểm giảng viên chính của lớp, yêu cầu còn chờ, người mới là thành viên hiệu lực (BR-U12-11).
4. Một transaction: cập nhật yêu cầu, đổi trưởng nhóm nếu duyệt (không đổi mục ai đang giữ, BR-U12-13); audit.
5. Sau commit phát `group.leader-changed` hoặc `group.leader-request-rejected` cho U14.

##### F4 - Nhóm của tôi (UC 54)
1. Student Class Detail có phần My Group do U12 gắn qua `StudentClassSection`; frontend gọi `GET /api/v1/me/classes/{classId}/group`.
2. Kiểm R5 với lớp; không có nhóm thì báo "chưa có nhóm".
3. Trả theo BR-U12-50: nhóm, thành viên, trưởng nhóm, tài liệu nhóm của các bài nhóm đã mở kèm tiến độ, trạng thái nộp, yêu cầu gần nhất của mình.
4. Bấm tài liệu mở Assignment Detail của bài nhóm (U08).

##### F5 - Gửi, hủy yêu cầu đổi trưởng nhóm (UC 55)
1. Trên My Group bấm "Đề nghị đổi trưởng nhóm", nhập lý do, tùy chọn đề cử; frontend gọi `POST /api/v1/groups/{groupId}/leader-requests`.
2. Kiểm người gửi là thành viên hiệu lực, lớp `OPEN`, lý do 10–1.000 ký tự, người được đề cử là thành viên khác trưởng nhóm, nhóm chưa có yêu cầu đang chờ (BR-U12-10).
3. Tạo yêu cầu `PENDING`; audit; sau commit phát `group.leader-change-requested` cho giảng viên của lớp.
4. Hủy khi còn chờ: `POST /api/v1/leader-requests/{id}:cancel` (chỉ người gửi).

##### F6 - Soạn và phát hành bài nhóm (UC 53)
1. Giảng viên tạo bài trên tab Evals (U07), chọn `GROUP` (chỉ hiện khi U12 có), bật hay tắt sơ đồ; chế độ khóa `GRADED`.
2. Soạn đề, khung, rubric trên Assignment Form của U07; phần Nhóm của U12 (slot `AssignmentFormSection`) hiện số nhóm, kết quả kiểm sẵn sàng, lối vào tab Students.
3. Khi duyệt và phát hành, U07 gọi `IGroupAssignmentSupport.checkReadiness(classId)`; U12 trả lỗi chặn và cảnh báo theo BR-U12-22.
4. Có cảnh báo `STUDENT_WITHOUT_GROUP` thì giảng viên xác nhận để phát hành.

##### F7 - Tạo tài liệu nhóm
1. U07 mở bài nhóm và gọi `onOpened(assignmentId)` trong transaction mở bài; U12 gửi job `GROUP_DOC_CREATE` qua `IJob` sau commit.
2. Worker lấy khung và phần (`IAssignmentQuery.getContent`), tạo một tài liệu cho mỗi nhóm hợp lệ của lớp: mục theo `partId`, phần chung chỉ đọc (BR-U12-30); làm lại không tạo trùng.
3. Người học mở trước khi job chạy xong thì tài liệu được tạo ngay khi truy cập.
4. Nhóm mới tạo khi bài đang mở cũng được tạo tài liệu (F1 bước 6).

##### F8 - Mở workspace nhóm (UC 56)
1. Trên Assignment Detail bấm "Mở bài nhóm" (U12 gắn qua `GroupWorkEntry`); frontend gọi `GET /api/v1/assignments/{assignmentId}/my-group-doc` kèm `classId`.
2. Kiểm thành viên hiệu lực của một nhóm trong lớp; trả tài liệu (phần chung, mục kèm trạng thái, người giữ, nội dung đã đăng), quyền của mình (thành viên hay trưởng nhóm), `version`.
3. Mở kênh SSE `GET /api/v1/group-docs/{id}/events`; bài bật theo dõi thì `useActivityTracker` gửi sự kiện tới `POST /api/v1/group-docs/{id}/activity` (F15).
4. Workspace hiện toàn bộ tài liệu và danh sách mục với thao tác hợp lệ cho từng người.

##### F9 - Giao, nhận mục (UC 56)
1. Thành viên bấm Nhận ở mục `OPEN` hoặc `DONE`: `POST /api/v1/group-docs/{id}/sections/{sectionId}:claim`.
2. Trưởng nhóm bấm Giao, chọn thành viên: `POST .../sections/{sectionId}:assign`; mục đang do người khác giữ thì cảnh báo và nhả trước (F11).
3. Kiểm quyền, tài liệu `IN_PROGRESS`, bài còn nhận bài (BR-U12-32); khóa dòng mục; cập nhật `CLAIMED`, người giữ, người giao; audit với giao mục.
4. Đẩy sự kiện SSE cho mọi người đang mở tài liệu.

##### F10 - Làm mục và Xong (UC 56)
1. Người giữ bấm Làm, mở popup làm mục (BR-U12-34) dùng `DocumentEditor` của U07.
2. 10 giây sau lần sửa cuối: `PUT /api/v1/group-docs/{id}/sections/{sectionId}/draft` kèm `version` của mục; kiểm người giữ, còn hạn (cộng 30 giây), `validateForSave`; lệch `version` thì `409`.
3. Bấm Xong: `POST .../sections/{sectionId}:done`; kiểm block; nội dung nháp thành nội dung đã đăng; thêm bản lịch sử; mục `DONE`, bỏ khóa (BR-U12-35).
4. Đóng popup; đẩy sự kiện SSE; workspace của mọi người tải nội dung mới của mục.

##### F11 - Nhả khóa (UC 56)
1. Người giữ, trưởng nhóm (trên workspace) hoặc giảng viên (trên tab Students) bấm Nhả, xác nhận cảnh báo; frontend gọi `POST .../sections/{sectionId}:release`.
2. Kiểm quyền; xóa bản nháp; mục về `DONE` nếu đã có nội dung đăng, không thì `OPEN` (BR-U12-36); audit khi trưởng nhóm hoặc giảng viên nhả.
3. Đẩy sự kiện SSE.

##### F12 - Nộp bài nhóm (UC 56)
1. Trưởng nhóm bấm Nộp; hệ thống báo mục chưa Xong; frontend gọi `POST /api/v1/group-docs/{id}/submit` kèm `version`.
2. Kiểm trưởng nhóm hiện tại, tài liệu `IN_PROGRESS`, bài còn nhận bài; `validateForSubmit`; lỗi thì trả danh sách cần sửa (BR-U12-40).
3. Một transaction: chụp bản nộp (phần chung, nội dung đã đăng, tác giả từng mục), `receiptHash`, `late`, `MANUAL`; gọi `IGroupEvaluation.onGroupSubmitted` (U09); audit.
4. Sau commit phát `group.submitted`; đẩy SSE; trả biên nhận. Trưởng nhóm nộp lại được trước hạn (BR-U12-42).

##### F13 - Tự nộp
1. `IScheduledScanner` của U12 chạy mỗi phút: tài liệu `IN_PROGRESS` của bài đã quá hạn cuối 30 giây.
2. U07 ngưng giao bài thì gọi `onRetired`: U12 đánh dấu các tài liệu của bài để tự nộp ngay trong cùng transaction.
3. Với từng tài liệu có thay đổi sau lần nộp cuối: chốt mục `CLAIMED` bằng bản nháp đã lưu, chụp bản nộp `AUTO_DEADLINE` hoặc `AUTO_RETIRED`, gọi `onGroupSubmitted`; tài liệu `CLOSED` (BR-U12-43).
4. Đẩy SSE: client khóa trang "Đang nộp…" rồi chuyển sang thẻ Bản nộp; câu lệnh có điều kiện nên chạy lại không nộp hai lần.

##### F14 - Đổi thành viên khi có bài đang mở
1. Trong transaction lưu nhóm (F1 bước 6), với người bị gỡ hoặc chuyển nhóm: nhả mục họ đang giữ (bỏ bản nháp), đóng kênh SSE của họ với tài liệu cũ.
2. Nhóm mới: tạo tài liệu cho mọi bài nhóm đang mở của lớp.
3. Nội dung đã Xong giữ tên tác giả; bản nộp đã có không đổi (BR-U12-05).

##### F15 - Interface cho unit khác, realtime
1. U08 hỏi `IGroupWorkStatus.statusFor` cho Student Assignments; Assignment Detail nhúng nút của U12 qua `GroupWorkEntry`.
2. U09 đọc bản nộp và tiến độ qua `IGroupSubmissionSource`; agent của giảng viên đọc bản nộp qua tool `read_authorized_group_submission` (kiểm giảng viên chính của lớp).
3. Ghi dữ liệu hoạt động: U12 kiểm thành viên hiệu lực, tài liệu `IN_PROGRESS`, chính sách đã chụp khi bài mở, rồi gọi `IActivityWrite.record` loại `GROUP_DOCUMENT`.
4. `ISseHub` gửi sự kiện tới mọi instance backend qua exchange `platform.realtime`; U14 dùng `ISseHub` cho chuông thông báo.

### U13 Reference Materials

Iter 3, Người 4, UC 58–62. Thiết kế: [u13-reference-materials](u13-reference-materials/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U13 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IReferenceMaterial` (contract của U06, U13 cài) | `listSelectable(actor, scopeRef)`, `extractText(actor, materialIds, maxChars)` | U07, U10 | Người soạn chọn tài liệu và lấy chữ đưa vào prompt AI |
| `IMcpToolProvider` (contract của U06, U13 cài) | tool `read_authorized_material` | U06 | Agent đọc tài liệu đúng quyền |
| `StudentClassSection`, `TeacherClassTab`, `DashboardNavEntry` (contract của U05, U13 cài) | - | U05 | Phần Học liệu trên Student Class Detail; tab Materials; lối vào Material List |
| Slot `AssignmentMaterialSection` (của U08), `QuizMaterialSection` (của U11), U13 cài bằng `MaterialLinkList` | - | U08, U11 | Hiện tài liệu gắn kèm Assignment, Quiz đúng quyền |
| `IJobHandler` cho `MATERIAL_TEXT_EXTRACT`, `IPendingSweeper` (contract của U03, U13 cài) | - | U03 | Trích chữ nền, gửi lại message bị mất |

##### Contract U13 khai báo (unit khác cài)

| Contract | Unit cài | Dùng để |
|---|---|---|
| Slot frontend `LearningMaterialSection` | U11 | Danh sách Quiz gắn tài liệu kèm nút Làm Quiz trên Learning Material |

#### Business flow

##### F1 - Tài liệu của môn (UC 58)
1. Chủ nhiệm môn bấm Material List trên Manager Dashboard (U13 đăng ký `DashboardNavEntry`), chọn môn mình quản lý; frontend gọi `GET /api/v1/subjects/{subjectId}/materials`.
2. Kiểm R2 (BR-U13-01); trả tài liệu theo thứ tự, loại, phiên bản hiện tại, trạng thái chữ; lọc bỏ hoặc hiện tài liệu đã lưu trữ.
3. Bấm tài liệu mở Material Detail: `GET /api/v1/materials/{id}` gồm thông tin và lịch sử phiên bản.

##### F2 - Thêm, sửa, lưu trữ tài liệu của môn (UC 59)
1. Thêm tệp: chọn tệp, tải qua U03 (`IArtifact.store`, purpose `MATERIAL`) nhận `FileRef`; nhập tiêu đề, mô tả; `POST /api/v1/subjects/{subjectId}/materials` loại `FILE`.
2. Thêm link: nhập URL YouTube, tiêu đề, mô tả; `POST` loại `YOUTUBE_LINK`; kiểm dạng URL một video (BR-U13-10).
3. Kiểm R2, môn `ACTIVE`; `attach` tệp; tạo tài liệu `ACTIVE` với phiên bản 1; gửi job trích chữ với tệp (F6); audit `MATERIAL_CREATED`.
4. Sửa thông tin hoặc thứ tự: `PATCH /api/v1/materials/{id}` kèm `version`; audit `MATERIAL_UPDATED`.
5. Thay tệp hoặc link: `POST /api/v1/materials/{id}/versions` kèm `version`; tạo phiên bản mới, giữ phiên bản cũ (BR-U13-12); gửi job trích chữ; audit `MATERIAL_VERSION_CREATED`.
6. Xóa: xác nhận, `POST /api/v1/materials/{id}/archive`; tài liệu `ARCHIVED` (BR-U13-13); audit `MATERIAL_ARCHIVED`.

##### F3 - Tài liệu của lớp (UC 60)
1. Giảng viên mở tab Materials (U13 gắn qua `TeacherClassTab`); frontend gọi `GET /api/v1/classes/{classId}/materials`.
2. Kiểm giảng viên chính của lớp; trả tài liệu của lớp (quản lý được) và tài liệu của môn chứa lớp (nhãn "Của môn", chỉ đọc).
3. Bấm tài liệu mở Material Detail (tài liệu của môn chỉ xem).

##### F4 - Thêm, sửa, lưu trữ tài liệu của lớp (UC 61)
1. Như F2 với `POST /api/v1/classes/{classId}/materials`; kiểm giảng viên chính của lớp và lớp chưa lưu trữ.
2. Sửa, thay phiên bản, lưu trữ dùng chung API của F2 bước 4–6; quyền kiểm theo phạm vi của tài liệu.

##### F5 - Xem tài liệu (UC 62)
1. Student Class Detail có phần Học liệu do U13 gắn qua `StudentClassSection`: `GET /api/v1/me/classes/{classId}/materials` trả tài liệu `ACTIVE` của môn và của lớp.
2. Bấm tài liệu mở Learning Material: `GET /api/v1/materials/{id}/view` kèm `classId`; kiểm quyền xem (BR-U13-02).
3. Tệp: frontend gọi `POST /api/v1/materials/{id}/download-token`; U13 kiểm quyền rồi gọi `IArtifact.issueDownloadToken`; PDF hiện ngay trên trang, tệp khác tải về.
4. Link YouTube: trang nhúng `youtube-nocookie.com` theo id video đã lưu (BR-U13-20, 21).
5. Vùng `LearningMaterialSection` hiện phần do unit khác gắn (U11: Quiz gắn tài liệu này).
6. Giảng viên, Chủ nhiệm môn cũng mở được Learning Material của tài liệu mình có quyền xem.
7. Assignment Detail (U08) và Quiz Practice Detail (U11) nhúng `MaterialLinkList` qua slot `AssignmentMaterialSection`, `QuizMaterialSection`: component gọi `GET /api/v1/me/classes/{classId}/materials?ids=`; U13 chỉ trả tài liệu `ACTIVE` người học được xem (BR-U13-23).

##### F6 - Trích chữ
1. Sau khi lưu phiên bản tệp, U13 gửi job `MATERIAL_TEXT_EXTRACT` qua `IJob` (queue `jobs.triggered`); phiên bản ở `PENDING`.
2. Worker đọc tệp qua `IArtifact.open`, trích chữ PDF, DOCX, PPTX bằng thư viện, không OCR.
3. Ghi chữ (cắt ở 1.000.000 ký tự) và `EXTRACTED`; ít chữ thì `NO_TEXT`; lỗi sau các lần thử của U03 thì `FAILED`. Phiên bản bị thay trước khi job chạy thì bỏ qua.

##### F7 - Nguồn cho AI và MCP
1. U07, U10 gọi `IReferenceMaterial.listSelectable(actor, scopeRef)` để hiện danh sách tài liệu người soạn chọn được (BR-U13-31).
2. Khi gửi task AI, U07, U10 gọi `extractText(actor, materialIds, maxChars)`: U13 kiểm lại quyền từng tài liệu, trả chữ phiên bản hiện tại kèm id, phiên bản, tiêu đề; tài liệu chưa có chữ được đánh dấu để người soạn biết.
3. Agent gọi `read_authorized_material(materialId)` qua U06: U13 kiểm quyền xem của actor, trả chữ (cắt theo trần của tool) và thông tin tài liệu.

### U14 Announcements & Notifications

Iter 3, Người 3, UC 63–65. Thiết kế: [u14-announcements-and-notifications](u14-announcements-and-notifications/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U14 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| Sự kiện `class.announcement-posted` | - | U14 | Tạo thông báo lớp sau commit |
| `StudentClassSection`, `DashboardNavEntry` (contract của U05, U14 cài) | - | U05 | Phần Thông báo trên Student Class Detail; lối vào Class Announcements trên Class Dashboard |
| Slot `HeaderWidget` (của U01, U14 cài) | - | U01 | Chuông và popup thông báo cho mọi vai trò |
| `IJobHandler` cho `EMAIL_SEND`, `IPendingSweeper`, `IScheduledScanner` (contract của U03, U14 cài) | - | U03 | Gửi email; gửi lại message bị mất; nhắc hạn, gửi email đã hoãn, dọn thông báo cũ |

##### Contract U14 khai báo

Không có.

#### Business flow

##### F1 - Xem thông báo lớp (UC 63)
1. Người dùng mở Class Announcements từ thanh bên Class Dashboard (U14 đăng ký `DashboardNavEntry`) hoặc từ phần Thông báo trên Student Class Detail (`StudentClassSection`, lọc sẵn lớp); frontend gọi `GET /api/v1/me/announcements` kèm lớp.
2. Lấy các lớp `OPEN` người dùng đang học hoặc đang dạy (`IClassAccess`); trả thông báo chưa xóa của các lớp đó, mới nhất trước, lọc theo lớp (BR-U14-06).
3. Thông báo của lớp ngoài quyền trả "không tìm thấy".

##### F2 - Tạo thông báo lớp (UC 64)
1. Giảng viên bấm Tạo trên Class Announcements, chọn lớp mình dạy, nhập tiêu đề, nội dung, đánh dấu quan trọng và chọn gửi email nếu muốn; frontend gọi `POST /api/v1/classes/{classId}/announcements`.
2. Kiểm giảng viên chính của lớp, lớp `OPEN`, giới hạn nội dung (BR-U14-01, 02).
3. Lưu thông báo; audit `ANNOUNCEMENT_CREATED`; sau commit phát `class.announcement-posted` (lớp, thông báo, người đăng, cờ quan trọng, chọn email).

##### F3 - Sửa, xóa thông báo lớp (UC 64)
1. Sửa: `PATCH /api/v1/announcements/{id}` kèm `version`; kiểm giảng viên chính của lớp; lưu người sửa, thời điểm; audit trước, sau; không phát sự kiện (BR-U14-04).
2. Xóa: xác nhận, `DELETE /api/v1/announcements/{id}`; chuyển `DELETED`, ghi người xóa, thời điểm; audit (BR-U14-05).

##### F4 - Tạo thông báo từ sự kiện
1. Listener của U14 nhận sự kiện trên queue `jobs.notification` (bind `platform.events` của U03); bỏ qua phiên bản sự kiện không hỗ trợ.
2. Xác định người nhận theo BR-U14-11: người học đang ghi danh qua `IClassAccess.listActiveStudents` (bài, Quiz của môn thì qua `listOpenClassesOfSubject`), giảng viên qua `getClassRef`, Admin qua `IAccountLookup`; người nhận đi kèm sự kiện thì dùng luôn.
3. Với mỗi người nhận: chèn thông báo, trùng (sự kiện, người nhận, loại) thì bỏ qua (BR-U14-10).
4. Loại có email: tạo bản ghi email (F6) theo tùy chọn (BR-U14-21).
5. Sau commit đẩy sự kiện qua `ISseHub` tới kênh của từng người nhận.

##### F5 - Xem, đánh dấu đã đọc (UC 65)
1. `NotificationBell` trên thanh điều hướng (U14 gắn qua slot `HeaderWidget` của U01) gọi `GET /api/v1/me/notifications/unread-count` và mở kênh `GET /api/v1/me/notifications/events`.
2. Mở popup: `GET /api/v1/me/notifications` (phân trang, mới nhất trước).
3. Bấm một thông báo: `POST /api/v1/me/notifications/{id}:read` rồi mở đường dẫn; màn đích kiểm quyền hiện hành (BR-U14-12).
4. "Đánh dấu tất cả đã đọc": `POST /api/v1/me/notifications:read-all`.

##### F6 - Gửi email
1. Bản ghi email tạo cùng transaction với thông báo: người nhận tắt loại đó thì `SKIPPED`; đã chạm trần trong ngày thì `DEFERRED`; còn lại `PENDING` và gửi job `EMAIL_SEND` sau commit (BR-U14-22).
2. Worker nhận job, đọc bản ghi; đã `SENT` thì bỏ qua; lấy địa chỉ qua `IAccountLookup.getContact`; dựng nội dung tối thiểu kèm link; gửi qua `IMail` của U01.
3. Thành công thì `SENT`; lỗi tạm thì U03 thử lại; hết lượt thì `onFailed` ghi `FAILED` với lỗi đã làm sạch.

##### F7 - Nhắc hạn nộp
1. `IScheduledScanner` của U14 chạy mỗi phút: lấy bài có `closesAt` trong 24 giờ tới qua `IAssignmentQuery.listClosingBetween` của U07.
2. Với mỗi (bài, lớp, `closesAt`) chưa xử lý: bỏ qua nếu bài không còn `OPEN` hoặc mở sau thời điểm nhắc (BR-U14-32).
3. Lớp của bài cá nhân hoặc nhóm: lớp của bài; bài của môn: mọi lớp `OPEN` thuộc môn (`listOpenClassesOfSubject`).
4. Lấy người chưa nộp qua `ISubmissionQuery.listStudentsWithoutSubmission(assignmentId, classId)` của U08 (bài nhóm: U08 dựa vào trạng thái nhóm từ U12).
5. Tạo thông báo `DEADLINE_REMINDER` và email như F4 bước 3–5; đánh dấu (bài, lớp, `closesAt`) đã nhắc.

##### F8 - Tùy chọn email (UC 65)
1. Người học mở phần Cài đặt email trong popup: `GET /api/v1/me/notification-preferences`.
2. Bật, tắt từng loại học tập: `PUT /api/v1/me/notification-preferences`; áp cho email tạo sau đó (BR-U14-21).
3. Giảng viên, Admin không thấy phần này vì không có loại email học tập gửi cho họ.

##### F9 - Dọn dữ liệu, gửi email đã hoãn
1. Scanner mỗi phút: sang ngày mới (giờ Việt Nam) thì chuyển email `DEFERRED` sang `PENDING` theo thứ tự ưu tiên tới trần ngày; nhắc hạn đã qua hạn thì `SKIPPED` (BR-U14-24).
2. Xóa theo lô thông báo cũ hơn 180 ngày (BR-U14-14).

### U15 Admin Dashboard

Iter 3, Người 5, UC 66. Thiết kế: [u15-admin-dashboard](u15-admin-dashboard/functional-design/business-rules.md).

#### Interface và contract unit cung cấp

##### Interface U15 cung cấp

Không có; U15 chỉ có màn và API cho Admin.

##### Contract U15 khai báo

Không có.

#### Business flow

##### F1 - Mở Admin Dashboard (UC 66)
1. Admin đăng nhập, Post-Login chuyển tới `/admin`; frontend gọi `GET /api/v1/admin/dashboard`.
2. Kiểm `ADMIN` đang `ACTIVE` qua `IAuthorization`; vai trò khác trả `403` và ghi audit (BR-U15-01).
3. Gọi song song: `IAccountLookup.countByRoleAndStatus` (U01), `ISubjectQuery.countSubjectsByStatus` (U04), `IClassAccess.countClassesByStatus` và `countActiveEnrollments` (U05), `IBackupStatus.latest` (U03); mỗi lời gọi có timeout 2 giây.
4. Ghép kết quả; nguồn lỗi hoặc hết giờ thì phần đó là "không khả dụng" (BR-U15-12, 21).
5. Trả số liệu và trạng thái backup; frontend hiện các thẻ, tổng số tài khoản, và lối vào bốn màn quản trị (BR-U15-30).

##### F2 - Xem lịch sử backup (UC 66)
1. Trong mục Backup bấm Lịch sử; frontend gọi `GET /api/v1/admin/dashboard/backups` kèm trang.
2. Kiểm `ADMIN`; gọi `IBackupStatus.list(page)`.
3. Trả các lần chạy (trạng thái, thời điểm, dung lượng, lỗi đã làm sạch); không có link tải.
4. Nút "Cài đặt backup" mở Setting Detail của U03.


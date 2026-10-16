# U03 Job and Settings — domain-entities

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3. Viết lại từ thiết kế chi tiết 2026-10-09 theo phạm vi 66 UC: bỏ avatar và việc nền của Code Lab, PayOS, tóm tắt học liệu; thêm backup và các nhóm cài đặt mới. Chỉ sửa tài liệu; unit chưa có code (khung code tạo lại 2026-10-16).

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

Thiết kế độc lập công nghệ. U03 là unit hạ tầng: tệp cho U07, U08, U12, U13; việc nền và sự kiện thông báo cho mọi unit; cài đặt hệ thống UC 14, 15; backup database.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `StoredFile` | Value object | Byte và thuộc tính trên Google Shared Drive; bảng của unit sở hữu giữ `file_id` | U03 tải lên; unit sở hữu lưu `file_id` |
| `FileRef` | Tham chiếu tạm có chữ ký | Trả cho frontend sau khi upload | U03 |
| `ArtifactPurpose` | Danh mục cố định | Code | U03 |
| `DownloadToken` | Entity tạm thời | Redis | U03 |
| `JobMessage` | Message việc nền | RabbitMQ | U03 gửi, unit sở hữu xử lý |
| `DomainEvent` | Message thông báo | RabbitMQ | Unit nguồn phát qua U03 |
| `SystemSetting` | Entity | `system_settings` | U03 (Admin sửa qua UC 15) |
| `SettingDefinition` | Khai báo trong code | Code | Unit sở hữu mục: U03, U08 |
| `BackupRun` | Entity | `backup_runs` | U03 |

U03 không sở hữu nội dung nghiệp vụ, quyết định ai được xem tệp, quota và kết nối AI (U06).

## 2. `StoredFile` và `FileRef`

| Thuộc tính | Ở đâu | Ràng buộc |
|---|---|---|
| `fileId` | Mã tệp Drive; unit sở hữu lưu vào cột của mình | Không trả cho frontend |
| `ownerAccountId` | Thuộc tính tệp | Người tải lên |
| `purpose` | Thuộc tính tệp | Một giá trị của `ArtifactPurpose` |
| `originalFileName` | Thuộc tính tệp | Đã làm sạch |
| `mediaType` | Thuộc tính tệp | Xác định từ nội dung |
| `byteSize` | Drive | ≤ trần của `purpose` |
| `sha256` | Thuộc tính tệp | Tính khi nhận |
| `FileRef` | Chuỗi ký HMAC gồm `fileId`, `ownerAccountId`, `purpose`, hạn 1 giờ | Unit sở hữu đổi `FileRef` lấy `fileId` qua `attach` |

## 3. `ArtifactPurpose`

| Giá trị | Loại tệp | Trần mặc định và tối đa | Unit dùng |
|---|---|---|---|
| `MATERIAL` | PDF, DOCX, PPTX | 50 MB | U13 (học liệu môn và lớp) |
| `DOCUMENT_IMAGE` | PNG, JPEG, GIF, SVG | 5 MB | U07 (khung đề), U08 (bài làm cá nhân), U12 (tài liệu nhóm) |

`AVATAR` của bản cũ bị bỏ. Không có purpose cho Draw.io: XML nằm trong khối sơ đồ của tài liệu.

## 4. `DownloadToken`

| Thuộc tính | Ràng buộc |
|---|---|
| `token` | Ngẫu nhiên 256 bit; Redis lưu băm |
| `fileId`, `accountId` | Chỉ người được cấp dùng được |
| `expiresAt` | 5 phút; dùng lại nhiều lần trong hạn để trình xem tải từng phần |
| `disposition` | `inline` (ảnh, PDF) hoặc `attachment` |

## 5. `JobMessage` và loại việc

`{ schemaVersion, jobType, idempotencyKey, payload, attempt, correlationId }`; payload chỉ chứa ID và tham chiếu.

| `jobType` | Unit sở hữu handler | Queue |
|---|---|---|
| `OTP_DELIVERY` | U01 | `jobs.email` (ưu tiên cao) |
| `EMAIL_SEND` | U14 | `jobs.email` |
| `AI_TASK` | U06 | `jobs.ai` |
| `GROUP_DOC_CREATE` | U12 | `jobs.triggered` |
| `MATERIAL_TEXT_EXTRACT` | U13 | `jobs.triggered` |
| `DRIVE_CLEANUP` | U03 | `jobs.drive` |
| `BACKUP_RUN` | U03 | `jobs.backup` |

Bỏ so với bản cũ: `LESSON_SCAN`, `YOUTUBE_CAPTION` (xử lý học liệu là phần mở rộng), `PAYOS_CHECK` (bỏ mua credit), `CODE_RUN` (bỏ Code Lab).

## 6. `DomainEvent` và loại sự kiện

`{ schemaVersion, eventId, eventType, sourceUnit, occurredAt, correlationId, payload }`, gửi lên `platform.events`, chỉ cho thông báo; U14 nghe.

| `eventType` | Unit phát |
|---|---|
| `enrollment.activated` | U05 |
| `assignment.opened` | U07 |
| `quiz.published` | U10 |
| `grade.published` | U09 |
| `group.membership-changed`, `group.leader-changed`, `group.leader-change-requested`, `group.leader-request-rejected`, `group.submitted` | U12 |
| `class.announcement-posted` | U14 |
| `backup.failed` | U03 |

Bỏ so với bản cũ: `payment.paid`.

## 7. `SystemSetting` (bảng `system_settings`)

| Cột | Ràng buộc |
|---|---|
| `key` | Khóa chính, ví dụ `files.material.maxSizeMb` |
| `group` | `FILES`, `BACKUP`, `ACTIVITY` |
| `value` | JSON theo kiểu của khai báo; rỗng là chưa cấu hình |
| `version` | Tăng mỗi lần sửa; khóa lạc quan |
| `updated_by` | FK `accounts`; rỗng khi là giá trị khởi tạo |
| `updated_at` | Thời điểm sửa gần nhất |

Không có thao tác xóa; không chứa bí mật.

## 8. `SettingDefinition`

Khai báo trong code của unit sở hữu: `key`, `group`, tên hiển thị, mô tả, kiểu (`INTEGER`, `BOOLEAN`, `TIME_OF_DAY`, `ENUM`, `ENUM_SET`), giá trị mặc định (có thể không có), giới hạn, ràng buộc giữa các mục trong nhóm.

| Nhóm | Mục | Unit khai báo |
|---|---|---|
| Tệp | `files.material.maxSizeMb`, `files.material.allowedTypes`, `files.documentImage.maxSizeMb`, `files.documentImage.allowedTypes` (BR-U03-87) | U03 |
| Backup | `backup.enabled`, `backup.dailyAt` (BR-U03-88); giữ bản 7 ngày cố định | U03 |
| Hoạt động làm bài | `activity.retentionDays` (không có mặc định, chốt trước khi bật theo dõi) | U08 |

## 9. `BackupRun` (bảng `backup_runs`)

| Cột | Ràng buộc |
|---|---|
| `id` | Định danh |
| `scheduled_for` | Ngày chạy theo lịch; duy nhất |
| `state` | `CREATED`, `RUNNING`, `SUCCESS`, `FAILED` |
| `snapshot_at` | Thời điểm dump |
| `schema_version`, `app_version` | Phiên bản lúc dump |
| `archive_ref` | Mã tệp archive dump đã mã hóa trong thư mục backup |
| `archive_sha256`, `byte_size` | Kiểm toàn vẹn và hiển thị |
| `started_at`, `completed_at`, `purged_at` | Mốc thời gian |
| `safe_error` | Lỗi đã che, không có đường dẫn nội bộ hay bí mật |

## 10. Contract

### Interface U03 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IArtifact` | `store(actor, purpose, file)` → `FileRef`; `attach(fileRef, actor, purpose)` → `StoredFileInfo`; `issueDownloadToken(fileId, accountId)` → `DownloadToken`; `open(fileId)` → `InputStream` | U07, U08, U12 (ảnh trong tài liệu, nhúng ảnh khi xuất DOCX), U13 (học liệu) | Lưu tệp riêng tư, gắn tệp vào đối tượng, cấp link tải ngắn hạn sau khi unit sở hữu đã kiểm quyền, đọc tệp phía server |
| `IJob` | `enqueue(jobType, payload, idempotencyKey)` | U01, U03, U06, U12, U13, U14 | Gửi việc nền sau commit, không ghi database |
| `IEventPublisher` | `publish(DomainEvent)` | U03, U05, U07, U09, U10, U12, U14 | Phát sự kiện thông báo sau commit, không đảm bảo giao hàng |
| `ISettings` | `get(key)` (có kiểu) | U03, U08 | Đọc giá trị cài đặt hiện hành, cache 30 giây |
| `IBackupStatus` | `latest()`, `list(page)` | U15 | Trạng thái và lịch sử backup cho Admin Dashboard, không có dữ liệu tải về |

### Contract U03 cung cấp (unit khác cài)

| Contract | Phương thức | Unit cài | Dùng để |
|---|---|---|---|
| `IJobHandler` | `jobType()`, `queue()`, `handle(message)`, `onFailed(message, error)`, `timeout()` | U01, U03, U06, U12, U13, U14 | Xử lý một loại việc nền trong worker và chuyển dòng nghiệp vụ sang lỗi khi hết lượt |
| `IPendingSweeper` | `jobType()`, `findStalePending(olderThan)` | U03, U06, U12, U13, U14 | Trả các dòng còn chờ quá 5 phút để U03 gửi lại message bị mất |
| `IScheduledScanner` | `name()`, `scan(now)` | U03 (lịch backup), U06 (giải phóng quota giữ quá hạn), U07 (mở, đóng bài theo lịch), U08 (tự nộp khi hết giờ, dọn dữ liệu hoạt động), U12 (tự nộp bài nhóm), U14 (nhắc hạn, gửi lại email hoãn) | Việc hẹn giờ chạy mỗi phút, cập nhật idempotent |
| `SettingDefinition` | khai báo `key`, nhóm, kiểu, mặc định, giới hạn | U03, U08 | Thêm mục cài đặt khóa–giá trị vào Settings |
| `ISettingsSectionProvider` | `sectionKey()`, `title()`, `summary()` | U06 | Thêm phần cài đặt do unit khác lưu (API AI chung, quota giảng viên) vào Setting List mà U03 không phụ thuộc ngược vào U06 |

### Interface U03 dùng

| Interface | Unit | Cạnh |
|---|---|---|
| `IAuthorization` | U01 | `C` - kiểm role upload và quyền `ADMIN` cho Settings; tới khi U01 xong, bản tạm trong khung luôn cho phép |
| `IAudit` | U02 | `C` - audit; tới khi U02 xong, bản tạm trong khung chỉ ghi ra log |

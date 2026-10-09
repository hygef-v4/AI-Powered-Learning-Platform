# U03 File, Job & Event - Domain Entities

**Bản tài liệu 2026-10-09**: UC 70, 71 (Settings, người dùng chốt U03 giữ ngày 2026-10-09) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-SET-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Thiết kế độc lập công nghệ. Unit hạ tầng: tệp cho U05 (học liệu), U06/U09/U11/U14 (ảnh trong tài liệu); việc nền, worker và sự kiện thông báo cho mọi unit (chuyển từ U02, 2026-10-04); cài đặt hệ thống UC 70–71 cho U03, U07, U13 (người dùng chốt 2026-10-09). Việc nền không có bảng: trạng thái nằm ở dòng nghiệp vụ của unit sở hữu (ví dụ `lessons.scan_status`, `ai_suggestions.status`, `payments.status`).

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `StoredFile` | Value object | Byte và metadata trên Google Shared Drive (thuộc tính tệp `appProperties`); bảng sở hữu giữ `file_id` | U03 tải lên; unit sở hữu lưu `file_id` |
| `FileRef` | Tham chiếu tạm có chữ ký | Trả cho frontend sau khi tải lên | U03 |
| `ArtifactPurpose` | Value object (danh mục cố định) | Code | U03 |
| `DownloadToken` | Entity tạm thời | Redis | U03 |
| `JobMessage` | Message việc nền | RabbitMQ | U03 gửi, unit sở hữu xử lý trong worker |
| `DomainEventMessage` | Message (chỉ cho thông báo) | RabbitMQ | U03 (unit nguồn phát qua `EventPublisherPort`) |
| `SystemSetting` | Entity | `system_settings` | U03 (Admin sửa qua UC 71) |
| `SettingDefinition` | Khai báo trong code | Code | Unit sở hữu mục: U03 (Tệp), U07 (Credit), U13 (AI) |

U03 chỉ có một bảng PostgreSQL là `system_settings` (thêm 2026-10-09 cho UC 70–71; ERD cần bổ sung bảng này). Tệp và việc nền vẫn không có bảng. U03 **không** sở hữu: nội dung nghiệp vụ, bài nộp, quyết định ai được xem file (unit sở hữu đối tượng tự kiểm quyền rồi mới xin token).

## 2. `StoredFile` và `FileRef`

| Thuộc tính | Ở đâu | Ràng buộc |
|---|---|---|
| `fileId` | Mã tệp Drive; unit sở hữu lưu vào cột của mình (`lessons.file_id`, ảnh trong JSON tài liệu) | Không trả cho frontend; frontend chỉ thấy `FileRef` khi vừa tải lên và URL tải có token |
| `ownerAccountId` | `appProperties` của tệp | Người tải lên |
| `purpose` | `appProperties` | Một giá trị của `ArtifactPurpose` |
| `originalFileName` | `appProperties` | Đã làm sạch ký tự điều khiển và đường dẫn |
| `mediaType` | `appProperties` | Xác định từ nội dung file, không tin header trình duyệt |
| `byteSize` | Drive | ≤ trần của `purpose` |
| `sha256` | `appProperties` | SHA-256 khi nhận |
| `FileRef` | Chuỗi có chữ ký HMAC: `fileId`, `ownerAccountId`, `purpose`, hạn 1 giờ | Unit sở hữu đổi `FileRef` lấy `fileId` qua `attach` |

Byte file không đổi sau khi tạo. Không có file dẫn xuất và không xóa file đã gắn.

## 3. `ArtifactPurpose`

| Giá trị | Loại file hỗ trợ | Trần mặc định và tối đa | Dùng bởi |
|---|---|---|---|
| `MATERIAL` | PDF, DOCX, PPTX | 50 MB | U05 |
| `DOCUMENT_IMAGE` | PNG, JPEG, GIF, SVG | 5 MB | U06, U09, U11, U14 (ảnh trong khung đề, bài làm, tài liệu nhóm) |

Admin chỉnh trần dung lượng (không vượt mức tối đa trên) và thu hẹp loại file cho phép trên Settings (BR-U03-87).

Không có loại file cho Draw.io: XML sơ đồ nằm trong block `DIAGRAM` của tài liệu (U09). Caption YouTube lưu ở U05; tài liệu xuất DOCX tại chỗ (U09); U16 xuất bảng điểm CSV/XLSX theo yêu cầu, không lưu tệp xuất.

## 4. `DownloadToken`

| Thuộc tính | Ràng buộc |
|---|---|
| `token` | Ngẫu nhiên 256 bit; Redis lưu băm |
| `fileId`, `accountId` | Token chỉ dùng được bởi đúng người được cấp |
| `expiresAt` | 5 phút |
| `disposition` | `inline` hoặc `attachment` |

Token dùng lại nhiều lần trong 5 phút (để ảnh và PDF viewer tải từng phần).

## 5. `JobMessage`

`{ schemaVersion, jobType, idempotencyKey, payload, attempt, correlationId }`, gửi tới exchange `jobs` với routing key = `jobType`, vào queue đã đăng ký (BR-U03-63). `payload` chỉ chứa ID/tham chiếu. Handler đọc dòng nghiệp vụ theo ID, không tin nội dung message.

## 6. `DomainEventMessage`

`{ schemaVersion, eventId, eventType, sourceUnit, occurredAt, correlationId, payload }`, gửi sau commit lên `platform.events`, **chỉ cho thông báo** (U16 nghe bằng queue `jobs.notification`). Audit không đi qua message. Payload không chứa bí mật.

## 6a. `SystemSetting` (bảng `system_settings`)

| Cột | Ràng buộc |
|---|---|
| `key` | Khóa chính, ví dụ `files.material.maxSizeMb` |
| `group` | `FILES`, `CREDIT`, `AI` |
| `value` | JSON theo kiểu của khai báo |
| `version` | Số nguyên, tăng mỗi lần sửa; dùng cho khóa lạc quan |
| `updated_by` | FK `accounts`; rỗng khi là giá trị mặc định lúc khởi tạo |
| `updated_at` | Thời điểm sửa gần nhất |

Không có thao tác xóa. Không chứa bí mật (BR-U03-85).

## 6b. `SettingDefinition`

Khai báo trong code của unit sở hữu: `key`, `group`, tên hiển thị, mô tả, kiểu (`INTEGER`, `DECIMAL`, `BOOLEAN`, `ENUM`, `ENUM_SET`, `STRING`), giá trị mặc định, giới hạn (min, max, tập cho phép). Nhóm Tệp của U03 có 4 mục theo BR-U03-87; nhóm Credit (U07) và AI (U13) do hai unit đó khai báo.

## 7. Contract

### Port U03 cung cấp

| Port | Dùng bởi |
|---|---|
| `ArtifactPort.store(actor, purpose, file)` → `FileRef` | API upload chung; U09 lưu ảnh khi nhập DOCX |
| `ArtifactPort.attach(fileRef, actor, purpose)` → `{fileId, mediaType, byteSize, originalFileName}` | Unit sở hữu khi gắn file vào đối tượng; unit lưu `fileId` vào dòng của mình |
| `ArtifactPort.issueDownloadToken(fileId, accountId)` | Unit sở hữu, **sau khi** tự kiểm quyền |
| `ArtifactPort.open(fileId)` | Worker U05 đọc học liệu để quét; U09 nhúng ảnh khi xuất DOCX |
| `JobPort.enqueue(jobType, payload, idempotencyKey)` | Mọi unit có việc nền; trong transaction thì gửi sau commit, ngoài transaction thì gửi ngay; không ghi DB |
| `JobHandler` + `JobHandlerRegistry` | Unit sở hữu đăng ký handler và `onFailed` cho `jobType`, chọn 1 trong 7 queue |
| `PendingSweeper` | Unit sở hữu đăng ký truy vấn dòng còn chờ để gửi lại message bị mất |
| `ScheduledScanner` | Unit sở hữu đăng ký việc hẹn giờ đọc mốc thời gian trên bảng của mình |
| `EventPublisherPort.publish(event)` | Unit phát sự kiện cho thông báo U16 |
| `SettingsPort` | U03 (giới hạn tệp), U07 (credit tặng định kỳ), U13 (AI); đọc giá trị hiện hành |
| `SettingDefinition` | U03, U07, U13 cài để khai báo mục cài đặt của mình |

### Port U03 dùng

| Port | Unit | Cạnh |
|---|---|---|
| `AuthorizationPort` | U01 | `C` - kiểm role được upload theo `purpose` và quyền `ADMIN` cho Settings; U03 code trước U01 nên đang dùng `AuthorizationService` khung của U01 (luôn từ chối) |
| `AuditPort` | U02 | `C` - audit; U02 chưa cài thật nên `AuditStore` khung của U02 chỉ ghi ra log |

## Tương thích code đã sinh
U03 đã sinh hỗ trợ avatar theo baseline trước; code summary/checkbox đã hoàn thành giữ nguyên làm bằng chứng. Hợp đồng mục tiêu chỉ MATERIAL/DOCUMENT_IMAGE; lúc triển khai revision rà purpose cũ, bỏ call site U01 và xử lý tương thích trước khi bỏ port. Không khẳng định mã hiện tại đã đổi.

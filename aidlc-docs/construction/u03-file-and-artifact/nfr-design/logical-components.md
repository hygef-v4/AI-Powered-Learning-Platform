# U03 File, Job & Event - Logical Components

**Bản tài liệu 2026-10-09**: UC 70, 71 (Settings, người dùng chốt U03 giữ ngày 2026-10-09) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-SET-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt
   | POST /api/v1/files (multipart)         GET /api/v1/files/download/{token}
   v                                          v
 +------------------------------ backend ------------------------------+
 | FileUploadController --> UploadService --> ContentInspector         |
 |                              |   (Semaphore 5, file tạm)            |
 |                              v                                      |
 |                         StoragePort --------> GoogleDriveStorage /  |
 |                              |                LocalFolderStorage    |
 |                                                                     |
 | DownloadController --> DownloadTokenService (Redis) --> StoragePort |
 | ArtifactPort (cho unit khác): attach, issueDownloadToken, open      |
 +---------------------------------------------------------------------+
                          | việc DRIVE_CLEANUP
                          v
                 worker --> DriveJobHandler --> StoragePort
```

**Text alternative**: Trình duyệt upload qua `FileUploadController`; `UploadService` giới hạn 5 upload cùng lúc, ghi file tạm, cho `ContentInspector` kiểm loại file, đẩy file và metadata qua `StoragePort` (Google Drive, hoặc thư mục local khi không có key); không có bảng PostgreSQL. Tải về đi qua `DownloadController`, kiểm token trong Redis rồi stream từ `StoragePort`. Các unit khác dùng `ArtifactPort`. Dọn file Drive còn sót khi upload lỗi chạy bằng việc nền (mục 4), do `DriveJobHandler` trong worker xử lý.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `FileUploadController` | backend | Nhận multipart, gọi `UploadService` |
| `UploadService` | backend | P1 |
| `ContentInspector` | backend | P3 |
| `StoragePort` + 2 adapter | backend, worker | P5, P6 |
| `ArtifactService` (`ArtifactPort`) | backend, worker | `attach` (kiểm `FileRef` có chữ ký), `open`, `issueDownloadToken` |
| `DownloadTokenService` | backend | P4 |
| `DownloadController` | backend | P2 |
| `DriveJobHandler` | worker | Việc `DRIVE_CLEANUP` |
| `FilePolicyService` | backend | Đọc trần và loại tệp hiện hành từ `SettingsPort` cho upload và cho `GET /api/v1/files/policies` |
| `SettingsController` | backend | `GET /api/v1/admin/settings`, `GET`, `PATCH /api/v1/admin/settings/{key}` |
| `SettingsService` (`SettingsPort`) | backend, worker | P14: đọc có cache 30 s, sửa kèm version, kiểm theo khai báo, audit |
| `SettingDefinitionRegistry` | backend | Gom `SettingDefinition` của U03, U07, U13; tạo mục còn thiếu lúc khởi động |
| `FileSettingDefinitions` | backend | Khai báo 4 mục nhóm Tệp (BR-U03-87) |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` | Rỗng → dùng thư mục local |
| `GOOGLE_SHARED_DRIVE_ID` | - |
| `U03_MAX_FILE_SIZE` | 50MB: trần cứng của multipart; trần đang dùng theo `purpose` lấy từ Settings |
| `U03_SETTINGS_CACHE_TTL` | 30s |
| `U03_MAX_CONCURRENT_UPLOADS` | 5 |
| `U03_UPLOAD_TMP_DIR` | `/tmp/uploads` |
| `U03_DOWNLOAD_TOKEN_TTL` | 5m |
| `U03_FILEREF_SECRET` | Khóa HMAC ký `FileRef` (bí mật) |
| `U03_FILEREF_TTL` | 1h |
| `U03_LOCAL_STORAGE_DIR` | `./data/files` (chỉ local) |

## 4. Việc nền và sự kiện (chuyển từ U02)

```
 Backend (unit bất kỳ)                         Worker (profile worker)
 +------------------------------+              +--------------------------------+
 | service nghiệp vụ            |              | JobListener (mỗi queue một cái)|
 |   -> ghi dòng nghiệp vụ      |              |   -> handler của unit sở hữu   |
 |   -> JobPort.enqueue         |              |   -> JobRetryPublisher         |
 |   -> EventPublisherPort      |              | PendingSweepRunner (mỗi phút)  |
 | afterCommit/ngay -> AmqpPublisher --+       | ScheduledScanRunner (mỗi phút) |
 +------------------------------+      |       +--------------------------------+
                                       v                ^
                                   RabbitMQ ------------+
                     7 queue jobs.*, jobs.retry.*, platform.events
```

**Text alternative**: Service nghiệp vụ ghi dòng nghiệp vụ và gọi `JobPort.enqueue` hoặc `EventPublisherPort`. `AmqpPublisher` gửi message sau commit (hoặc ngay khi không có transaction) sang RabbitMQ. Worker có một `JobListener` cho mỗi queue, gọi handler của unit sở hữu; lỗi tạm thì `JobRetryPublisher` gửi vào queue thử lại có TTL. `PendingSweepRunner` gửi lại việc bị mất; `ScheduledScanRunner` chạy việc hẹn giờ của các unit.

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `JobPort` / `JobPublisher` | Backend, worker | P8: gửi sau commit hoặc ngay, kiểm payload bằng `ForbiddenKeyGuard` |
| `EventPublisherPort` | Backend, worker | Gửi `platform.events` sau commit |
| `AmqpPublisher` | Backend, worker | Gửi message, publisher confirm, log lỗi |
| `JobListener` | Worker | Mỗi queue một listener, số luồng cấu hình |
| `JobHandlerRegistry` | Worker | Ánh xạ `jobType` → handler, `onFailed`, queue |
| `JobRetryPublisher` | Worker | P9 |
| `PendingSweepRunner` | Worker | P10: gọi các `PendingSweeper` đã đăng ký |
| `ScheduledScanRunner` | Worker | P10: gọi các `ScheduledScanner` đã đăng ký |

### RabbitMQ

| Thành phần | Kiểu | Ghi chú |
|---|---|---|
| `jobs` exchange | direct | Routing key = `jobType` |
| `jobs.triggered` | queue durable | Việc nội bộ sau thao tác: `GROUP_DOC_CREATE` |
| `jobs.email` | queue durable, priority | SMTP: `OTP_DELIVERY` (ưu tiên 9), `EMAIL_SEND` (ưu tiên 1) |
| `jobs.gemini` | queue durable | Gemini: `LESSON_SCAN`, `AI_TASK` |
| `jobs.youtube` | queue durable | `YOUTUBE_CAPTION` |
| `jobs.code` | queue durable | Judge0: `CODE_RUN` |
| `jobs.drive` | queue durable | Google Drive: `DRIVE_CLEANUP` |
| `jobs.payos` | queue durable | PayOS: `PAYOS_CHECK` |
| `jobs.retry` exchange | direct | `JobRetryPublisher` gửi bản sao vào đúng queue thử lại theo lượt |
| `jobs.retry.30s` … `jobs.retry.8m` | queue durable, TTL cố định, không consumer | Dead-letter về `jobs` giữ routing key gốc |
| `platform.events` | topic exchange | Chỉ cho thông báo; U16 bind queue `jobs.notification` |

Không có DLQ; việc hết lượt chỉ để dòng nghiệp vụ ở trạng thái lỗi và log ERROR.

### Cấu hình việc nền

| Khóa | Mặc định |
|---|---|
| `U03_JOB_MAX_ATTEMPTS` | 5 |
| `U03_JOB_BACKOFF` | 30s,1m,2m,4m,8m |
| `U03_SWEEP_INTERVAL` / `U03_SWEEP_REPUBLISH_AFTER` | 1m / 5m |
| `U03_SCAN_INTERVAL` | 1m |
| `U03_JOB_HANDLER_TIMEOUT` | 60s |
| `U03_WORKER_MAX_CONCURRENCY` | 4 |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Mật khẩu là bí mật |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log file ID, key, token |
| SECURITY-04 | Compliant | P2 header |
| SECURITY-05 | Compliant | P3 |
| SECURITY-08 | Compliant | P4, quyền do unit sở hữu |
| SECURITY-09 | Compliant | Key từ `.env` |
| SECURITY-15 | Compliant | P1 dọn bù trừ, P7 |
| RESILIENCY-06 | Compliant | P7 |
| RESILIENCY-10 | Compliant | P6 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

# U03 - Business Logic Summary

Code: `backend/src/main/java/edu/aiplatform/files/` và `.../jobs/`.

## Tệp

| Thành phần | Vai trò | Rule |
|---|---|---|
| `domain/PurposePolicy` | Allowlist loại tệp, trần (AVATAR, MATERIAL 50 MB; DOCUMENT_IMAGE 5 MB), vai trò được upload, ảnh/PDF `inline` | BR-U03-02, 03, 04, 23 |
| `domain/FileNameSanitizer` | Bỏ đường dẫn, ký tự điều khiển, cắt 255 ký tự | BR-U03-08 |
| `domain/StoredFile`, `FileRefClaims` | Metadata tệp đi kèm tệp; nội dung có chữ ký của `FileRef` | domain-entities §2 |
| `application/ContentInspector` | Tika trên 8 KB đầu; đuôi tệp chỉ là gợi ý cho kiểu con tương thích | BR-U03-03, P3 |
| `application/UploadService` | Semaphore 5 (hết → 503), kiểm vai trò + `AuthorizationPort`, kiểm trần cả khi kích thước khai báo sai, SHA-256, ghi qua `StoragePort`, ghi dở thì xóa thử 3 lần rồi gửi `DRIVE_CLEANUP`, luôn xóa tệp tạm | BR-U03-01…07, 41, P1 |
| `application/ArtifactService` | `ArtifactPort` + `AvatarPort`: `attach` kiểm chữ ký, hạn, chủ, `purpose`; không có hàm xóa | BR-U03-30…32, F2, F4, F5 |
| `application/DownloadTokenService` | Token 32 byte, Redis lưu băm TTL 5 phút, gắn `accountId`; sai người → 404 + audit | BR-U03-20, 21, 40, P4 |
| `application/FileAudit` | Audit upload bị từ chối, token sai người, Drive chặn vì abuse; không ghi `fileId` | BR-U03-40 |
| `worker/DriveJobHandler` | `DRIVE_CLEANUP` trên `jobs.drive`, idempotent | BR-U03-06 |

## Việc nền và sự kiện

| Thành phần | Vai trò | Rule |
|---|---|---|
| `application/JobPublisher` (`JobPort`) | Kiểm `jobType`, `idempotencyKey`, khóa cấm; gửi sau commit hoặc ngay | BR-U03-50, 52, P8 |
| `application/EventPublisher` (`EventPublisherPort`) | Kiểm `schemaVersion`, khóa cấm; gửi `platform.events` sau commit | BR-U03-70, 71 |
| `domain/JobRouting` | `jobType` → 1 trong 7 queue, độ ưu tiên email, số luồng mỗi queue | BR-U03-63, P11 |
| `domain/BackoffPolicy` | 5 mức 30 s…8 m, mỗi mức một queue `jobs.retry.*` | BR-U03-56, P9 |
| `worker/JobListener` | Ack thủ công; timeout handler; lỗi tạm → queue thử lại; vĩnh viễn/hết lượt → `onFailed`; sai `schemaVersion` → WARN + ack | BR-U03-51, 53, 56, 71 |
| `worker/JobHandlerRegistry` | Tự đăng ký bean handler/sweeper/scanner, kiểm queue đúng bảng định tuyến | BR-U03-63 |
| `worker/PendingSweepRunner`, `ScheduledScanRunner`, `WorkerConfig` | Chạy mỗi phút; mỗi queue một listener container | BR-U03-58, 60, P10, P11 |

## Quyết định khi code

- `attempt` là số lần đã thử lại, lần đầu `0`; hết 5 lần thử lại thì gọi `onFailed` (khớp 5 mức backoff). Đã sửa `business-logic-model.md` J1.
- Queue thử lại dead-letter về `jobs` không đặt routing key; bản sao được gửi kèm header `CC = [jobType]` nên quay về đúng queue gốc.
- Bean `JobHandler`/`PendingSweeper`/`ScheduledScanner` tự được đăng ký; `register*` vẫn dùng được cho đối tượng không phải bean.
- `JobHandler.timeout()` (mặc định rỗng → `U03_JOB_HANDLER_TIMEOUT`) cho phép từng loại việc khai báo timeout riêng (NFR-U03-46).
- Gửi việc nền/event chạy trên luồng riêng để RabbitMQ chậm không chặn request (NFR-U03-40); mất message khi backend dừng đột ngột được sweeper gửi lại.
- Production đặt `U03_REQUIRE_DRIVE=true`: thiếu key Drive thì báo không khả dụng thay vì lưu tệp vào container (NFR-U03-14).
- Link tải `/api/v1/files/download/{token}` yêu cầu đăng nhập vì token gắn với tài khoản (trước đây OpenAPI để công khai; đã sửa).
- `AuthorizationPort` dùng `AuthorizationService` khung của U01 (luôn từ chối) nên upload thật bị từ chối tới khi U01 xong; `AuditPort` dùng `AuditStore` khung của U02 (ghi ra log).

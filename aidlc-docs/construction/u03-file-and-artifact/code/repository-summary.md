# U03 - Repository & Storage Summary

U03 không có bảng PostgreSQL và không có migration (Bước 13).

| Thành phần | Vai trò |
|---|---|
| `files/port/StoragePort` | `put(Path, metadata)`, `get`, `readMetadata`, `delete`; nhận `Path` để adapter gửi lại khi retry. Lỗi: `StorageUnavailableException`, `StorageWriteException` (kèm `orphanFileId`), `FileUnavailableException` (abuse), `StoredFileNotFoundException` |
| `infrastructure/GoogleDriveStorageAdapter` | Bật khi có `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`. Thư mục theo `purpose` tạo khi khởi động (luồng nền, lỗi không chặn khởi động) hoặc khi upload; tệp + metadata tạo trong **một** request; tên gốc ở `name`, các metadata khác ở `appProperties` (giới hạn 124 byte/cặp nên tên dài không vừa); kiểm kích thước sau upload, lệch thì báo ghi dở. Timeout 5/60/120 s; 429/5xx retry 3 lần 1, 2, 4 s có jitter; 404 → không có tệp; `cannotDownloadAbusiveFile` → `FileUnavailableException`. Key sai: backend vẫn khởi động, thao tác tệp báo không khả dụng |
| `infrastructure/LocalFolderStorageAdapter` | Bật khi không có key và `U03_REQUIRE_DRIVE=false` (overlay local), cảnh báo khi khởi động. `{uuid}.bin` + `{uuid}.properties`; chỉ nhận ID dạng UUID (chặn path traversal) |
| `infrastructure/UnconfiguredStorageAdapter` | Production (`U03_REQUIRE_DRIVE=true`) thiếu key: backend vẫn chạy, mọi thao tác tệp báo không khả dụng (NFR-U03-14) |
| `infrastructure/FileRefSigner` | HMAC-SHA256 với `U03_FILEREF_SECRET`; xoay khóa bằng triển khai lại; không có khóa thì sinh ngẫu nhiên và cảnh báo |
| `infrastructure/DriveHealthIndicator` | `drives.get` cache 60 s; lỗi → `DEGRADED` (HTTP 200), không làm backend `DOWN` |
| Redis `file:download-token:{sha256}` | Token tải về, TTL 5 phút |
| `jobs/infrastructure/AmqpTopology` | Exchange `jobs`, `jobs.retry`, `platform.events`; 7 queue (`jobs.email` priority 10) + 5 queue thử lại (TTL, DLX `jobs`) |
| `jobs/infrastructure/AmqpPublisher` | Message persistent, publisher confirm; việc nền và event gửi trên một luồng riêng có hàng đợi 1 000 (không chặn request, NFR-U03-40), bản thử lại gửi đồng bộ chờ confirm 2 s; lỗi chỉ log |

Cấu hình mới: `platform.files.*` (gồm `require-drive`, `U03_MAX_FILE_SIZE`) và `platform.jobs.*` trong `application.yml` (biến `U03_*`, `GOOGLE_*`); Compose truyền `U03_FILEREF_SECRET`, `GOOGLE_*` cho `backend` và `worker`, local gắn volume `files-local`.

Integration test (Bước 18, J9) không viết: tester riêng đảm nhận theo kịch bản trong plan.

# U03 File & Artifact - Logical Components

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
 |                              v                                      |
 |                       ArtifactRepository (PostgreSQL)               |
 |                                                                     |
 | DownloadController --> DownloadTokenService (Redis) --> StoragePort |
 | ArtifactPort (cho unit khác): attach, issueDownloadToken, open      |
 +---------------------------------------------------------------------+
                          | job U03_DRIVE_CLEANUP
                          v
                 worker (U02) --> DriveJobHandler --> StoragePort
```

**Text alternative**: Trình duyệt upload qua `FileUploadController`; `UploadService` giới hạn 5 upload cùng lúc, ghi file tạm, cho `ContentInspector` kiểm loại file, đẩy file qua `StoragePort` (Google Drive, hoặc thư mục local khi không có key) và lưu metadata vào PostgreSQL. Tải về đi qua `DownloadController`, kiểm token trong Redis rồi stream từ `StoragePort`. Các unit khác dùng `ArtifactPort`. Dọn file Drive còn sót khi upload lỗi chạy bằng job của U02, do `DriveJobHandler` trong worker xử lý.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `FileUploadController` | backend | Nhận multipart, gọi `UploadService` |
| `UploadService` | backend | P1 |
| `ContentInspector` | backend | P3 |
| `StoragePort` + 2 adapter | backend, worker | P5, P6 |
| `ArtifactRepository` | backend, worker | Bảng `artifacts` |
| `ArtifactService` (`ArtifactPort`) | backend, worker | `attach`, `open`, `issueDownloadToken` |
| `DownloadTokenService` | backend | P4 |
| `DownloadController` | backend | P2 |
| `DriveJobHandler` | worker | Job `U03_DRIVE_CLEANUP` |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` | Rỗng → dùng thư mục local |
| `GOOGLE_SHARED_DRIVE_ID` | - |
| `U03_MAX_FILE_SIZE` | 50MB (`DOCUMENT_IMAGE` 5MB) |
| `U03_MAX_CONCURRENT_UPLOADS` | 5 |
| `U03_UPLOAD_TMP_DIR` | `/tmp/uploads` |
| `U03_DOWNLOAD_TOKEN_TTL` | 5m |
| `U03_LOCAL_STORAGE_DIR` | `./data/files` (chỉ local) |

## 4. Compliance

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

# U03 File & Artifact - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `FileUploadController`, `UploadService`, `ContentInspector`, `DownloadController`, `DownloadTokenService`, `ArtifactService` | `backend` |
| `DriveJobHandler` | `worker` |
| Bảng `artifacts` | `postgres` |
| Download token | `redis`, tiền tố `u03:dl:` |
| Byte file | Google Shared Drive (ngoài VPS); local dùng volume `files-local` |

## 2. Google Drive

| Mục | Giá trị |
|---|---|
| Xác thực | JSON key service account trong `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` (base64) |
| Shared Drive | `GOOGLE_SHARED_DRIVE_ID`; service account là Content manager |
| Thư mục | Mỗi `purpose` một thư mục, tạo lúc khởi động nếu chưa có |
| Kết nối ra ngoài | Backend và worker cần đi được tới `www.googleapis.com:443`; firewall VPS không chặn chiều ra |
| Chi phí | Dùng quota miễn phí của Drive API và dung lượng Shared Drive của trường |

## 3. Container

| Mục | Giá trị |
|---|---|
| Volume `upload-tmp` | Gắn vào `backend` tại `/tmp/uploads`, giới hạn 1 GB (5 × 50 MB có dư) |
| Volume `files-local` | Chỉ khi chạy local không có key; gắn tại `./data/files` |
| Nginx | `client_max_body_size 50m`; `proxy_request_buffering off` cho `/api/v1/files` để không đệm hai lần; `proxy_read_timeout 120s` |
| Backend | `spring.servlet.multipart.max-file-size=50MB`, `max-request-size=51MB`, `file-size-threshold=0`, `location=/tmp/uploads` |

## 4. Migration

`V20260925_1000__u03_artifacts.sql`: bảng `artifacts` theo `domain-entities.md` (không có `scan_status`, có `status`, `deleted_at`); index `(scope_type, scope_id)`, `source_artifact_id`, unique `provider_file_id`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | Compliant | Key từ `.env`, không commit; Drive không chia sẻ công khai |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| RESILIENCY-06 | Compliant | Healthcheck có Drive |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |

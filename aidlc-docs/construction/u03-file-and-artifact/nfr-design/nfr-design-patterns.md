# U03 File & Artifact - NFR Design Patterns

## P1 - Upload an toàn giao dịch với dọn dẹp bù trừ
1. `Semaphore.tryAcquire()` (5 permit); không lấy được → `503` (NFR-U03-01).
2. Spring ghi multipart ra thư mục tạm `/tmp/uploads` (NFR-U03-02).
3. Kiểm kích thước theo `purpose`, magic bytes (P3), tính SHA-256 trên file tạm.
4. Upload lên Drive → nhận `providerFileId`.
5. INSERT `artifacts` và commit.
6. Bước 4-5 lỗi → xóa file Drive nếu đã có (retry 3 lần; vẫn lỗi thì tạo job U02 `DRIVE_CLEANUP` để dọn sau) (BR-U03-06).
7. `finally`: xóa file tạm, trả permit.

## P2 - Stream tải về
- `StreamingResponseBody` đọc từ `InputStream` của Drive, bộ đệm 64 KB (NFR-U03-05).
- Header: `Content-Type` đã lưu, `Content-Disposition` (`inline` cho ảnh/PDF, còn lại `attachment`, tên file mã hóa RFC 5987), `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store` (NFR-U03-23). SVG thêm `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; sandbox` (BR-U03-10).
- Drive trả `cannotDownloadAbusiveFile` → cập nhật `BLOCKED`, ghi audit, trả `410` "file không khả dụng" (BR-U03-24).

## P3 - Kiểm nội dung file
- Tika `detect()` trên 8 KB đầu → so allowlist của `purpose` (NFR-U03-21).
- Không có parser XML và không nhận ZIP ở U03 (XML Draw.io do U09 kiểm).

## P4 - Download token
- Sinh 32 byte `SecureRandom`, mã hóa base64url; Redis `file:download-token:{sha256(token)}` = `{artifactId, accountId, disposition}` TTL 5 phút (NFR-U03-22).
- Endpoint tải kiểm `accountId` token khớp phiên; sai → `404` và audit (BR-U03-21, 40).

## P5 - Adapter lưu trữ thay được
- `StoragePort` với hai implementation: `GoogleDriveStorageAdapter` (production/demo) và `LocalFolderStorageAdapter` (test, local không có key).
- Chọn bằng cấu hình: có `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` → Drive, không có → local kèm cảnh báo khi khởi động (NFR-U03-31).

## P6 - Gọi Drive có timeout và retry
- Kết nối 5 s, đọc 60 s, upload 120 s (NFR-U03-12).
- 429/5xx: retry 3 lần, backoff 1, 2, 4 s có jitter; 401/403/404/abuse: không retry (NFR-U03-13).

## P7 - Health
- `/health` kiểm Drive bằng một lời gọi `drives.get(sharedDriveId)` có cache 60 s; lỗi → Drive `DOWN` nhưng backend vẫn `UP` để các chức năng khác chạy (NFR-U03-14).

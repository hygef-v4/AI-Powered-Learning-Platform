# U03 File & Artifact - NFR Design Patterns

## P1 - Upload an toàn giao dịch với dọn dẹp bù trừ
1. `Semaphore.tryAcquire()` (5 permit); không lấy được → `503` (NFR-U03-01).
2. Spring ghi multipart ra thư mục tạm `/tmp/uploads` (NFR-U03-02).
3. Kiểm kích thước, magic bytes, XML (P3), tính SHA-256 trên file tạm.
4. Upload lên Drive → nhận `providerFileId`.
5. INSERT `artifacts` và commit.
6. Bước 4-5 lỗi → xóa file Drive nếu đã có (retry 3 lần; vẫn lỗi thì tạo job U02 `U03_DRIVE_CLEANUP` để dọn sau) (BR-U03-06).
7. `finally`: xóa file tạm, trả permit.

## P2 - Stream tải về
- `StreamingResponseBody` đọc từ `InputStream` của Drive, bộ đệm 64 KB (NFR-U03-05).
- Header: `Content-Type` đã lưu, `Content-Disposition` (`inline` cho ảnh/PDF, còn lại `attachment`, tên file mã hóa RFC 5987), `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store` (NFR-U03-23).
- Drive trả `cannotDownloadAbusiveFile` → cập nhật `BLOCKED`, ghi audit, trả `410` "file không khả dụng" (BR-U03-24).

## P3 - Kiểm nội dung file
- Tika `detect()` trên 8 KB đầu → so allowlist của `purpose` (NFR-U03-21).
- XML: `DocumentBuilderFactory` với `disallow-doctype-decl = true`, `external-general-entities = false`, `external-parameter-entities = false`, `XIncludeAware = false`, `ExpandEntityReferences = false`; gốc phải `mxfile` hoặc `mxGraphModel` (NFR-U03-20).
- ZIP (file bài nộp): chỉ kiểm magic bytes, **không giải nén** trên server để tránh zip bomb.

## P4 - Download token
- Sinh 32 byte `SecureRandom`, mã hóa base64url; Redis `u03:dl:{sha256(token)}` = `{artifactId, accountId, disposition}` TTL 5 phút (NFR-U03-22).
- Endpoint tải kiểm `accountId` token khớp phiên; sai → `404` và audit (BR-U03-21, 40).

## P5 - Adapter lưu trữ thay được
- `StoragePort` với hai implementation: `GoogleDriveStorageAdapter` (production/demo) và `LocalFolderStorageAdapter` (test, local không có key).
- Chọn bằng cấu hình: có `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` → Drive, không có → local kèm cảnh báo khi khởi động (NFR-U03-31).

## P6 - Gọi Drive có timeout và retry
- Kết nối 5 s, đọc 60 s, upload 120 s (NFR-U03-12).
- 429/5xx: retry 3 lần, backoff 1, 2, 4 s có jitter; 401/403/404/abuse: không retry (NFR-U03-13).

## P7 - Xóa file dẫn xuất qua job
- `deleteDerived` đánh dấu `DELETED` ngay trong DB rồi tạo job U02 `U03_DRIVE_DELETE`; worker xóa file Drive, retry theo cơ chế U02 (BR-U03-32).
- File `DELETED` không cấp token được kể cả khi Drive chưa xóa xong.

## P8 - Health
- `/health` kiểm Drive bằng một lời gọi `drives.get(sharedDriveId)` có cache 60 s; lỗi → Drive `DOWN` nhưng backend vẫn `UP` để các chức năng khác chạy (NFR-U03-14).

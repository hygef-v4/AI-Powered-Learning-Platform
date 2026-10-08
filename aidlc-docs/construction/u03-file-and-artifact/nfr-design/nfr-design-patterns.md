# U03 File, Job & Event - NFR Design Patterns

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: không primary story. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Upload an toàn giao dịch với dọn dẹp bù trừ
1. `Semaphore.tryAcquire()` (5 permit); không lấy được → `503` (NFR-U03-01).
2. Spring ghi multipart ra thư mục tạm `/tmp/uploads` (NFR-U03-02).
3. Kiểm kích thước theo `purpose`, magic bytes (P3), tính SHA-256 trên file tạm.
4. Upload lên Drive → nhận `providerFileId`.
5. Ghi `appProperties` lên tệp Drive, ký `FileRef` (HMAC, hạn 1 giờ); không ghi PostgreSQL.
6. Bước 4-5 lỗi → xóa file Drive nếu đã có (retry 3 lần; vẫn lỗi thì gửi việc `DRIVE_CLEANUP` qua `JobPort` (queue `jobs.drive`, P8) để worker dọn sau) (BR-U03-06).
7. `finally`: xóa file tạm, trả permit.

## P2 - Stream tải về
- `StreamingResponseBody` đọc từ `InputStream` của Drive, bộ đệm 64 KB (NFR-U03-05).
- Header: `Content-Type` lấy từ `appProperties`, `Content-Disposition` (`inline` cho ảnh/PDF, còn lại `attachment`, tên file mã hóa RFC 5987), `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store` (NFR-U03-23). SVG thêm `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; sandbox` (BR-U03-10).
- Drive trả `cannotDownloadAbusiveFile` → ghi audit, trả `410` "file không khả dụng" (BR-U03-24).

## P3 - Kiểm nội dung file
- Tika `detect()` trên 8 KB đầu → so allowlist của `purpose` (NFR-U03-21).
- Không có parser XML và không nhận ZIP ở U03 (XML Draw.io do U09 kiểm).

## P4 - Download token
- Sinh 32 byte `SecureRandom`, mã hóa base64url; Redis `file:download-token:{sha256(token)}` = `{fileId, accountId, disposition}` TTL 5 phút (NFR-U03-22).
- Endpoint tải kiểm `accountId` token khớp phiên; sai → `404` và audit (BR-U03-21, 40).

## P5 - Adapter lưu trữ thay được
- `StoragePort` với hai implementation: `GoogleDriveStorageAdapter` (production/demo) và `LocalFolderStorageAdapter` (test, local không có key).
- Chọn bằng cấu hình: có `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` → Drive, không có → local kèm cảnh báo khi khởi động (NFR-U03-31).

## P6 - Gọi Drive có timeout và retry
- Kết nối 5 s, đọc 60 s, upload 120 s (NFR-U03-12).
- 429/5xx: retry 3 lần, backoff 1, 2, 4 s có jitter; 401/403/404/abuse: không retry (NFR-U03-13).

## P7 - Health
- `/health` kiểm Drive bằng một lời gọi `drives.get(sharedDriveId)` có cache 60 s; lỗi → Drive `DOWN` nhưng backend vẫn `UP` để các chức năng khác chạy (NFR-U03-14).

## Việc nền (chuyển từ U02, 2026-10-04)

## P8 - Gửi việc nền sau commit
- Unit gọi ghi dòng nghiệp vụ ở trạng thái chờ trong transaction của mình; `JobPort.enqueue` có transaction đang mở thì đăng ký `TransactionSynchronization.afterCommit` để gửi `JobMessage` (rollback thì không gửi); không có transaction (yêu cầu không ghi database như xin OTP của U01, dọn file của U03) thì gửi ngay.
- Gửi lỗi → log WARN, không ném lỗi ra ngoài (NFR-U03-40); sweeper ở P10 sẽ gửi lại.

## P9 - Thử lại qua queue có TTL
- Handler lỗi tạm và `attempt` < 5: gửi bản sao với `attempt + 1` vào `jobs.retry.{30s|1m|2m|4m|8m}` theo lượt rồi ack bản gốc. Queue thử lại có `x-message-ttl` cố định và `x-dead-letter-exchange = jobs`, nên hết TTL message quay về queue gốc theo routing key cũ.
- Mỗi mức backoff một queue để message không chặn nhau ở đầu hàng.
- Lỗi vĩnh viễn hoặc hết lượt: gọi `onFailed` của unit sở hữu, log ERROR, ack (BR-U03-56).

## P10 - Sweeper và scanner
- `PendingSweepRunner` mỗi phút gọi từng `PendingSweeper`: unit sở hữu trả danh sách ID còn ở trạng thái chờ và cập nhật lần cuối quá 5 phút; runner gửi lại message (BR-U03-58).
- `ScheduledScanRunner` mỗi phút gọi từng `ScheduledScanner`; scanner cập nhật bằng câu lệnh có điều kiện (ví dụ `UPDATE ... WHERE status = 'SCHEDULED' AND opens_at <= now()`) nên chạy lại không gây trùng (BR-U03-60).
- Chỉ có một worker nên không cần khóa phân tán.

## P11 - Cách ly theo loại việc
- 7 queue theo tính chất (BR-U03-63); mỗi queue một container listener với số luồng = prefetch: `jobs.triggered` 2, `jobs.email` 1 (priority queue `x-max-priority = 10`), `jobs.gemini` 4 (U05 và U13 tự giới hạn thêm bằng semaphore `U05_SCAN_CONCURRENCY`, `U13_AI_CONCURRENCY`), `jobs.youtube` 1, `jobs.code` 2, `jobs.drive` 1, `jobs.payos` 1 (NFR-U03-43, 49).
- Việc hẹn giờ chạy bằng scanner nên AI hoặc chạy code chậm không làm trễ tự nộp hay mở/đóng bài; OTP luôn gửi trước email thông báo.

## P12 - Timeout và kết nối lại
- RabbitMQ: connection timeout 5 s, tự kết nối lại của Spring AMQP; publisher confirm bật, chờ tối đa 2 s.
- Mỗi handler có timeout theo loại việc, mặc định 60 s; quá hạn thì coi như lỗi tạm (NFR-U03-46).

## P13 - Health worker
- Worker `/health` kiểm PostgreSQL và RabbitMQ (NFR-U03-54).

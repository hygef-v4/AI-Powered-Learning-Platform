# U03 Job and Settings — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

## P1 - Upload với dọn dẹp bù trừ
1. `Semaphore.tryAcquire()` (5 permit); không lấy được thì `503`.
2. Spring ghi multipart ra `/tmp/uploads`.
3. Kiểm kích thước và loại theo giới hạn từ `ISettings`, tính SHA-256 trên tệp tạm.
4. Upload lên Drive kèm thuộc tính tệp, ký `FileRef` (HMAC, hạn 1 giờ); không ghi PostgreSQL.
5. Lỗi thì xóa tệp Drive (3 lần), vẫn lỗi thì gửi `DRIVE_CLEANUP`.
6. `finally`: xóa tệp tạm, trả permit.

## P2 - Stream tải về
- `StreamingResponseBody`, bộ đệm 64 KB; header theo BR-U03-23; SVG thêm CSP sandbox; abuse thì audit và `410`.

## P3 - Kiểm nội dung tệp
- Tika `detect()` trên 8 KB đầu, so danh sách cho phép của `purpose`.

## P4 - Download token
- 32 byte `SecureRandom`, base64url; Redis `file:download-token:{sha256(token)}` = `{fileId, accountId}`, TTL 5 phút; `inline` hay `attachment` tính khi tải theo loại tệp.

## P5 - Adapter lưu trữ thay được
- `IStorage` với `GoogleDriveStorageAdapter`, `LocalFolderStorageAdapter`, `UnconfiguredStorageAdapter`; chọn theo cấu hình.

## P6 - Gọi Drive có timeout và thử lại
- Kết nối 5 giây, đọc 60 giây, upload 120 giây; 429/5xx thử lại 3 lần backoff 1, 2, 4 giây có jitter.

## P7 - Health
- Health kiểm Drive bằng `drives.get` có cache 60 giây; lỗi thì Drive `DEGRADED` nhưng backend vẫn chạy.

## P8 - Gửi việc nền sau commit
- Có transaction thì đăng ký `afterCommit`; không có thì gửi ngay; gửi trên luồng riêng để RabbitMQ chậm không chặn request; lỗi chỉ log.

## P9 - Thử lại qua queue có TTL
- Bản sao `attempt + 1` gửi tới exchange fanout `jobs.retry.{30s|1m|2m|4m|8m}` (routing key là loại việc) rồi vào queue cùng tên có TTL cố định; hết hạn thì dead-letter về `jobs` giữ routing key.

## P10 - Sweeper và scanner
- `PendingSweepRunner` và `ScheduledScanRunner` chạy mỗi phút; scanner cập nhật có điều kiện nên chạy lại không trùng.

## P11 - Cách ly theo loại việc
- 5 queue, mỗi queue một listener: `jobs.triggered` 2 luồng, `jobs.email` 1 luồng (priority, OTP trước), `jobs.ai` 4 luồng (U06 tự giới hạn thêm theo provider), `jobs.drive` 1 luồng, `jobs.backup` 1 luồng.

## P12 - Timeout và kết nối lại RabbitMQ
- Connection timeout 5 giây, tự kết nối lại; publisher confirm 2 giây; timeout handler theo loại việc.

## P13 - Health worker
- Kiểm PostgreSQL và RabbitMQ.

## P14 - Cài đặt có cache ngắn và khóa lạc quan
- Cache theo khóa, hết hạn 30 giây; sửa xong xóa cache ở instance hiện tại.
- `UPDATE system_settings SET value = ?, version = version + 1 ... WHERE key = ? AND version = ?`; 0 dòng thì `409`.
- Khởi động: `INSERT ... ON CONFLICT (key) DO NOTHING`.

## P15 - Phần cài đặt ủy quyền
- `SettingsSectionRegistry` gom các `ISettingsSectionProvider`; mỗi `summary()` gọi có timeout 1 giây và bắt lỗi riêng, nên phần của U06 lỗi không làm hỏng Setting List.

## P16 - Pipeline backup
1. Scanner tạo `backup_runs` với `scheduled_for` duy nhất rồi gửi `BACKUP_RUN`.
2. Handler chuyển `CREATED` → `RUNNING` bằng cập nhật có điều kiện.
3. `pg_dump --format=custom` bằng role `backup_reader` ra volume tạm.
4. Mã hóa dump (P17), upload vào `backup/{runId}`, kiểm kích thước và checksum; không chép tệp Drive.
5. Ghi `SUCCESS`, audit, rồi chạy retention (P18).
6. Lỗi: thử lại theo P9; hết lượt thì `FAILED`, dọn thư mục của lần chạy qua `DRIVE_CLEANUP`, phát `backup.failed`.

## P17 - Mã hóa lai
- Sinh khóa AES-256 và nonce ngẫu nhiên cho mỗi lần chạy; mã hóa luồng bằng AES-GCM.
- Bọc khóa AES bằng public key RSA-OAEP-256 lấy từ `U03_BACKUP_PUBLIC_KEY`; ghi khóa đã bọc vào đầu archive.
- Giải mã chỉ làm được với private key của người vận hành, giữ ngoài VPS.

## P18 - Giữ bản trong 7 ngày
- Chọn các lần `SUCCESS` có `snapshot_at` cũ hơn 7 ngày, trừ lần thành công mới nhất; xóa thư mục `backup/{runId}` của các lần đó, ghi `purged_at`.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

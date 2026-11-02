# U03 Job and Settings — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3. Viết lại từ thiết kế chi tiết 2026-10-09 theo phạm vi 66 UC: bỏ avatar và việc nền của Code Lab, PayOS, tóm tắt học liệu; thêm backup và các nhóm cài đặt mới. Chỉ sửa tài liệu; unit chưa có code (khung code tạo lại 2026-10-16).

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

## 1. Hiệu năng và tài nguyên

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-01 | Tối đa 5 upload xử lý cùng lúc trên backend; upload thứ 6 nhận `503` "hệ thống đang bận". | Thiết kế |
| NFR-U03-02 | Tệp nhận vào ghi ra thư mục tạm trên đĩa, không giữ toàn bộ trong RAM; xóa ngay khi xong, kể cả khi lỗi. | Thiết kế |
| NFR-U03-03 | Nginx `client_max_body_size 50m`; backend từ chối sớm khi `Content-Length` > 50 MB. | BR-U03-02 |
| NFR-U03-04 | Upload 50 MB hoàn tất trong ≤ 60 giây trên mạng VPS thông thường; timeout request upload 120 giây. | NFR-003 |
| NFR-U03-05 | Tải về stream từng phần, bộ đệm ≤ 64 KB. | NFR-003 |
| NFR-U03-06 | Cấp download token p95 ≤ 100 ms. | NFR-003 |

## 2. Google Drive

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-10 | Backend dùng JSON key của service account trong `.env` (`GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`, `GOOGLE_SHARED_DRIVE_ID`); service account là Content manager của Shared Drive. Không commit `.env`. | SEC-006 |
| NFR-U03-11 | Mỗi `purpose` một thư mục; ID thư mục lấy từ cấu hình hoặc tạo lúc khởi động. | Thiết kế |
| NFR-U03-12 | Lời gọi Drive có timeout: kết nối 5 giây, upload 120 giây, đọc 60 giây. | REL-003 |
| NFR-U03-13 | Lỗi tạm của Drive (429, 5xx) thử lại tối đa 3 lần với backoff 1, 2, 4 giây; lỗi quyền hoặc abuse không thử lại. | REL-003 |
| NFR-U03-14 | Thiếu hoặc sai cấu hình Drive: backend vẫn khởi động, upload và tải về trả "tạm thời không khả dụng", health báo Drive lỗi. | REL-002 |
| NFR-U03-16 | Thư mục backup (`GOOGLE_BACKUP_FOLDER_ID`) tách khỏi thư mục tệp đang dùng; nên ở Shared Drive khác. | FR-038 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-20 | Ảnh SVG trả kèm CSP sandbox; U03 không có parser XML. | BR-U03-10, 11 |
| NFR-U03-21 | Nhận dạng loại tệp bằng magic bytes (Apache Tika). | BR-U03-03 |
| NFR-U03-22 | Download token 256 bit, Redis lưu băm, TTL 5 phút, gắn `accountId`. | BR-U03-21 |
| NFR-U03-23 | Phản hồi tải về có `Content-Disposition` đúng, `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store`. | BR-U03-23 |
| NFR-U03-24 | Không log mã tệp Drive, thông tin đăng nhập Drive, token tải về, khóa backup. | SEC-005 |

## 4. Việc nền, RabbitMQ và worker

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-40 | `IJob.enqueue` không thêm câu SQL nào; gửi message và phát sự kiện không chặn request quá 50 ms. | NFR-003 |
| NFR-U03-41 | Frontend poll trạng thái việc nền mỗi 3 giây, dừng ở trạng thái cuối. | Thiết kế |
| NFR-U03-42 | Worker là container riêng, cùng mã nguồn backend với profile `worker`. | Thiết kế |
| NFR-U03-43 | Mỗi queue có luồng xử lý riêng; gọi AI hoặc backup chậm không chặn email và việc nội bộ. | Thiết kế |
| NFR-U03-44 | Thử lại tối đa 5 lượt với backoff 30 giây, 1, 2, 4, 8 phút qua các queue `jobs.retry.*`. | BR-U03-56 |
| NFR-U03-45 | Sweeper và scanner chạy mỗi phút trong worker; chỉ có một worker nên không cần khóa phân tán. | BR-U03-58, 60 |
| NFR-U03-46 | Mỗi loại việc có timeout do unit sở hữu khai báo, mặc định 60 giây; quá hạn coi như lỗi tạm. | REL-003 |
| NFR-U03-47 | Queue durable, message persistent; RabbitMQ khởi động lại không mất message đang chờ. | REL-003 |
| NFR-U03-48 | Ack thủ công, chỉ sau khi handler đã cập nhật PostgreSQL. | BR-U03-53 |
| NFR-U03-50 | Kết nối RabbitMQ timeout 5 giây, tự kết nối lại; publisher confirm chờ tối đa 2 giây. | REL-003 |
| NFR-U03-51 | Tài khoản RabbitMQ riêng cho ứng dụng; tắt `guest`. | SEC-006 |
| NFR-U03-52 | RabbitMQ không khả dụng: thao tác nghiệp vụ vẫn thành công; dòng nghiệp vụ ở trạng thái chờ đến khi sweeper gửi lại; sự kiện thông báo có thể mất. | BR-U03-58, 70 |
| NFR-U03-54 | Health của worker kiểm PostgreSQL và RabbitMQ. | REL-002 |

## 5. Cài đặt hệ thống

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-60 | `ISettings` dùng cache trong bộ nhớ hết hạn sau 30 giây. | BR-U03-86 |
| NFR-U03-61 | Xem và sửa cài đặt p95 ≤ 300 ms, không tính phần ủy quyền. | NFR-003 |
| NFR-U03-62 | API Settings chỉ cho `ADMIN`; không trả và không lưu bí mật. | BR-U03-80, 85 |
| NFR-U03-64 | Gọi `summary()` của phần ủy quyền có timeout 1 giây; lỗi chỉ ảnh hưởng phần đó. | BR-U03-89 |

## 6. Backup

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-70 | Backup chạy trong worker trên queue `jobs.backup` với 1 luồng; timeout của việc `BACKUP_RUN` là 2 giờ. | FR-038 |
| NFR-U03-71 | Dump dùng `pg_dump` (một snapshot MVCC, không khóa ghi); giờ chạy do Admin chọn ngoài giờ cao điểm. | FR-038 |
| NFR-U03-72 | Mã hóa lai: mỗi lần chạy một khóa AES-256-GCM ngẫu nhiên, khóa này được bọc bằng public key RSA-OAEP (3072 bit) của người vận hành; private key không nằm trên VPS. | BR-U03-93 |
| NFR-U03-73 | Thư mục tạm cho dump là volume riêng; kiểm dung lượng trống ≥ 2 lần kích thước database trước khi dump, không đủ thì thất bại sớm. | FR-038 |
| NFR-U03-74 | RPO bằng khoảng cách giữa hai lần chạy (tối đa 24 giờ với lịch hằng ngày) và RTO được đo trong lần thử khôi phục; không cam kết con số trước khi đo. | FR-038 |
| NFR-U03-75 | Trước demo phải thử khôi phục vào môi trường cô lập ít nhất một lần và lưu bằng chứng. | FR-038 |
| NFR-U03-76 | Dump chạy bằng role database chỉ đọc riêng (`backup_reader`). | SEC-006 |

## 7. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-30 | Người code viết unit test cho mọi `BR-U03-xx`, gồm SVG có script, tệp đổi đuôi, tệp vượt trần lấy từ Settings, token sai người, kiểm giới hạn cài đặt, version lệch, khởi tạo không ghi đè, trạng thái backup, giữ bản 7 ngày, mã hóa và giải mã bằng khóa test. | NFR-004 |
| NFR-U03-31 | Drive được thay bằng adapter giả lưu ra thư mục tạm trong test và khi chạy local không có credential. | NFR-004 |
| NFR-U03-55 | Tester viết integration test với PostgreSQL và RabbitMQ thật trong container: gửi sau commit, rollback không gửi, thử lại theo backoff, `onFailed` sau 5 lượt, sweeper gửi lại, scanner không trùng, backup thành công và thất bại, khôi phục cô lập. | NFR-004 |

## 8. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U03-24 |
| SECURITY-04 | Compliant | NFR-U03-23 |
| SECURITY-05 | Compliant | Kiểm loại, kích thước theo `purpose`; kiểm giới hạn cài đặt |
| SECURITY-08 | Compliant | Unit sở hữu kiểm quyền; token gắn người dùng; Settings chỉ cho `ADMIN` |
| SECURITY-09 | Compliant | Credential từ `.env`; archive backup mã hóa |
| SECURITY-12 | N/A | U03 không xác thực người dùng |
| SECURITY-15 | Compliant | Fail closed khi không kiểm được quyền; dọn tệp khi lỗi |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| RESILIENCY-06 | Compliant | Health gồm Drive, PostgreSQL, RabbitMQ |
| RESILIENCY-10 | Compliant | Timeout và thử lại hữu hạn |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

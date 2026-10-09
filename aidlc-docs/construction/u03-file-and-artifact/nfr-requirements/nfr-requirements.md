# U03 File, Job & Event - NFR Requirements

**Bản tài liệu 2026-10-09**: UC 70, 71 (Settings, người dùng chốt U03 giữ ngày 2026-10-09) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-SET-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Hiệu năng và tài nguyên

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-01 | Tối đa 5 upload xử lý cùng lúc trên backend; upload thứ 6 nhận `503` "hệ thống đang bận, thử lại sau". | Câu N2 |
| NFR-U03-02 | File nhận vào được ghi ra thư mục tạm trên đĩa, không giữ toàn bộ trong RAM; xóa ngay khi xong, kể cả khi lỗi. | Câu N2 |
| NFR-U03-03 | Nginx `client_max_body_size 50m`; backend từ chối sớm khi `Content-Length` > 50 MB. | BR-U03-02 |
| NFR-U03-04 | Upload 50 MB hoàn tất trong ≤ 60 giây trên mạng VPS thông thường; timeout request upload 120 giây. | NFR-003 |
| NFR-U03-05 | Tải về stream từng phần, bộ đệm ≤ 64 KB; không nạp file vào RAM. | NFR-003 |
| NFR-U03-06 | Cấp download token p95 ≤ 100 ms (chỉ ghi Redis). | NFR-003 |

## 2. Google Drive

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-10 | Backend upload bằng **JSON key của service account** đặt trong `.env`: `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` (nội dung JSON mã hóa base64) và `GOOGLE_SHARED_DRIVE_ID`. Service account được thêm làm Content manager của Shared Drive. Không dùng API key thường vì Drive không cho upload bằng loại key đó. Không commit `.env`. | Câu N1, SEC-006 |
| NFR-U03-11 | Mỗi `purpose` một thư mục trên Shared Drive; ID thư mục lấy từ cấu hình hoặc tạo lúc khởi động. | Thiết kế |
| NFR-U03-12 | Lời gọi Drive có timeout: kết nối 5 s, upload 120 s, tải về 60 s mỗi lần đọc. | REL-003 |
| NFR-U03-13 | Lỗi tạm của Drive (429, 5xx) retry tối đa 3 lần với backoff 1, 2, 4 s; lỗi quyền hoặc abuse không retry. | REL-003 |
| NFR-U03-14 | Thiếu hoặc sai cấu hình Drive: backend vẫn khởi động, upload/tải về trả "tạm thời không khả dụng", healthcheck báo Drive lỗi. | REL-002 |
| NFR-U03-15 | Quota API Drive miễn phí của Google đủ cho quy mô ≤ 100 người đồng thời; không cần gói trả phí. | REL-005 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-20 | Ảnh SVG trả kèm `Content-Security-Policy` sandbox; không có parser XML ở U03. | BR-U03-10, 11 |
| NFR-U03-21 | Nhận dạng loại file bằng magic bytes (Apache Tika). | BR-U03-03 |
| NFR-U03-22 | Download token 256 bit, Redis lưu băm, TTL 5 phút, gắn `accountId`. | BR-U03-21 |
| NFR-U03-23 | Phản hồi tải về có `Content-Disposition` đúng, `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store`. | BR-U03-23 |
| NFR-U03-24 | Không log `providerFileId`, thông tin đăng nhập Drive hay token tải về. | SEC-005 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-30 | Unit test mọi `BR-U03-xx`, gồm SVG có script (header CSP), file đổi đuôi, file vượt trần theo `purpose` lấy từ Settings, token dùng sai người. | NFR-004 |
| NFR-U03-31 | Drive được thay bằng adapter giả lưu ra thư mục tạm trong test và khi chạy local không có credential. | NFR-004, NFR-005 |

## 5. Việc nền, RabbitMQ và worker (chuyển từ U02, 2026-10-04)

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-40 | `JobPort.enqueue` không thêm câu SQL nào; gửi message không chặn request quá 50 ms. `EventPublisherPort.publish` cũng không chặn quá 50 ms, gửi lỗi thì trả ngay. | NFR-003 |
| NFR-U03-41 | Frontend poll trạng thái việc nền mỗi 3 giây, dừng ở trạng thái cuối. | frontend-components |
| NFR-U03-42 | Worker là container riêng, dùng cùng mã nguồn backend với profile `worker`. | Câu N1 (U02) |
| NFR-U03-43 | Mỗi queue có luồng xử lý riêng; việc chậm của Gemini/Judge0 không chặn email và việc nội bộ. | Câu N3 (U02) |
| NFR-U03-44 | Thử lại tối đa 5 lượt với backoff 30 s, 1, 2, 4, 8 phút qua các queue `jobs.retry.*`. | BR-U03-56 |
| NFR-U03-45 | Sweeper và scanner chạy mỗi phút trong worker; chỉ có một worker nên không cần khóa phân tán. | BR-U03-58, 60 |
| NFR-U03-46 | Mỗi loại việc có timeout xử lý do unit sở hữu khai báo; mặc định 60 giây, quá hạn coi như lỗi tạm. | REL-003 |
| NFR-U03-47 | Queue durable, message persistent; RabbitMQ khởi động lại không mất message đang chờ. | Câu N2 (U02) |
| NFR-U03-48 | Consumer ack thủ công, chỉ ack sau khi handler đã cập nhật PostgreSQL. | BR-U03-53 |
| NFR-U03-49 | Prefetch bằng số luồng xử lý của từng queue (P11). | Câu N3 (U02) |
| NFR-U03-50 | Kết nối RabbitMQ có timeout 5 s và tự kết nối lại; publisher confirm chờ tối đa 2 s. | REL-003 |
| NFR-U03-51 | Tài khoản RabbitMQ riêng cho ứng dụng, mật khẩu từ biến môi trường; tắt tài khoản `guest`. | SEC-006 |
| NFR-U03-52 | RabbitMQ không khả dụng: thao tác nghiệp vụ vẫn thành công; dòng nghiệp vụ nằm ở trạng thái chờ đến khi sweeper gửi lại; event thông báo có thể mất (chấp nhận). | BR-U03-58, 70 |
| NFR-U03-53 | Worker chết giữa chừng: message chưa ack được RabbitMQ giao lại; handler idempotent. | BR-U03-59 |
| NFR-U03-54 | Healthcheck worker kiểm PostgreSQL và RabbitMQ. | REL-002 |
| NFR-U03-55 | Integration test với PostgreSQL và RabbitMQ bằng Testcontainers: gửi sau commit, rollback không gửi, gửi ngay khi không có transaction, retry theo backoff, `onFailed` sau 5 lượt, sweeper gửi lại, scanner chạy lại không trùng. | NFR-004 |

## 6. Cài đặt hệ thống (UC 70–71)

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-60 | Đọc cài đặt qua `SettingsPort` dùng cache trong bộ nhớ, hết hạn sau 30 giây; giá trị mới có hiệu lực ở mọi instance trong tối đa 30 giây. | BR-U03-86 |
| NFR-U03-61 | Xem và sửa cài đặt p95 ≤ 300 ms. | NFR-003 |
| NFR-U03-62 | API Settings chỉ cho `ADMIN`; không trả và không lưu bí mật. | BR-U03-80, 85 |
| NFR-U03-63 | Unit test: kiểm giới hạn từng kiểu, version lệch trả `409`, khởi tạo không ghi đè giá trị đã sửa, người không phải Admin bị từ chối. | NFR-004 |

## 7. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U03-24 |
| SECURITY-04 | Compliant | NFR-U03-23 |
| SECURITY-05 | Compliant | Kiểm loại, kích thước theo `purpose` |
| SECURITY-08 | Compliant | Unit sở hữu kiểm quyền; token gắn người dùng; Settings chỉ cho `ADMIN` |
| SECURITY-09 | Compliant | Credential từ `.env`, không commit |
| SECURITY-12 | N/A | U03 không xác thực người dùng |
| SECURITY-15 | Compliant | NFR-U03-14, dọn file khi lỗi |
| RESILIENCY-06 | Compliant | Healthcheck gồm Drive |
| RESILIENCY-10 | Compliant | NFR-U03-12, 13 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

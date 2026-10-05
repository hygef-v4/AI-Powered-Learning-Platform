# U03 File, Job & Event - Business Logic Model

Luồng F1–F5 cho tệp; J1–J4 cho việc nền, E1 cho sự kiện thông báo.

## 1. F1 - Upload

**Vào**: phiên đăng nhập, `purpose`, file multipart.

1. Kiểm quyền upload theo `purpose` (BR-U03-04).
2. Đọc stream, dừng nếu vượt trần của `purpose` (BR-U03-02).
3. Xác định loại từ magic bytes, đối chiếu allowlist (BR-U03-03).
4. Tính SHA-256.
5. Tải lên Drive vào thư mục theo `purpose`, ghi `appProperties` (`ownerAccountId`, `purpose`, `mediaType`, `originalFileName`, `sha256`), nhận `fileId`.
6. Bước 5 lỗi → xóa file Drive nếu đã tạo, trả lỗi (BR-U03-06).
7. Trả `{ fileRef, mediaType, byteSize, originalFileName }`; không có dòng database nào.

## 2. F2 - Gắn vào đối tượng

1. Unit sở hữu gọi `attach(fileRef, actor, purpose)`.
2. Kiểm chữ ký, hạn, chủ sở hữu, `purpose` (BR-U03-30).
3. Trả `{ fileId, mediaType, byteSize, originalFileName }`; unit sở hữu lưu vào dòng của mình trong transaction nghiệp vụ.

## 3. F3 - Tải về

1. Frontend gọi API của **unit sở hữu** (ví dụ "tải học liệu"). Unit đó tự kiểm quyền nghiệp vụ.
2. Unit sở hữu gọi `issueDownloadToken(fileId, accountId)`. U03 sinh token, lưu băm Redis 5 phút.
3. Frontend nhận URL `/api/v1/files/download/{token}`.
4. Khi tải: tìm token, kiểm `accountId` khớp phiên (BR-U03-21), đọc `appProperties`, stream từ Drive với header an toàn; SVG kèm CSP sandbox (BR-U03-10, 22, 23).
5. Drive báo abuse → audit, trả lỗi (BR-U03-24).

Ảnh đại diện: U01 cấp token cho bất kỳ người đã đăng nhập (BR-U03-20).

## 4. F4 - Worker đọc file

1. Worker U05 gọi `open(fileId)` để quét học liệu; U09 gọi khi nhúng ảnh vào DOCX xuất.
2. Trả stream từ Drive; abuse → lỗi vĩnh viễn như F3.

## 5. F5 - Kiểm ảnh đại diện (cho U01)

`validateAvatar(fileRef, actor)`: như `attach` với `purpose = AVATAR`; trả `fileId` để U01 lưu `avatar_file_id`.

## 6. Việc nền (J1–J4, chuyển từ U02)

### J1 - Gửi việc nền
1. Unit gọi ghi dòng nghiệp vụ ở trạng thái chờ trong transaction của mình (ví dụ `lessons.scan_status = PENDING`).
2. Gọi `JobPort.enqueue(jobType, payload, idempotencyKey)`; U03 kiểm payload (BR-U03-52); có transaction thì đăng ký gửi sau commit, không có thì gửi ngay.
3. Sau commit: gửi `JobMessage` (`attempt = 0`, tức số lần đã thử lại) tới exchange `jobs`, routing key = `jobType`. Lỗi gửi → log WARN; J3 sẽ gửi lại.

### J2 - Worker xử lý việc nền
1. Nhận `JobMessage`, gọi handler của unit sở hữu với `payload`.
2. Handler đọc dòng nghiệp vụ; dòng không còn ở trạng thái chờ → ack và bỏ (BR-U03-51).
3. Thành công → handler cập nhật dòng nghiệp vụ; worker ack (BR-U03-53).
4. Lỗi:
   - Tạm thời, `attempt` < 5 → gửi bản sao (attempt + 1) vào hàng chờ thử lại theo backoff rồi ack bản gốc.
   - Vĩnh viễn hoặc hết lượt → gọi `onFailed`, unit sở hữu chuyển dòng sang trạng thái lỗi; log ERROR; ack (BR-U03-56, 27).

### J3 - Gửi lại việc bị mất (mỗi phút)
1. Với mỗi `PendingSweeper` đã đăng ký, lấy các dòng còn ở trạng thái chờ và cập nhật lần cuối quá 5 phút.
2. Gửi lại `JobMessage` cho từng dòng; handler idempotent nên gửi trùng không gây hại (BR-U03-58, 29).

### J4 - Việc hẹn giờ (mỗi phút)
1. Worker gọi lần lượt các `ScheduledScanner` đã đăng ký (BR-U03-60).
2. Mỗi scanner đọc mốc thời gian trên bảng của unit mình (ví dụ `assignments.opens_at`, `attempts.deadline_at`) và cập nhật có điều kiện để chạy lại không gây trùng.

## 7. Sự kiện thông báo (E1)

### E1 - Phát sự kiện
1. Sau commit, unit gọi `EventPublisherPort.publish(event)`.
2. Gửi RabbitMQ exchange `platform.events` với routing key `eventType`. Lỗi → log WARN, không retry (BR-U03-70).
3. Chỉ dùng cho thông báo; phản ứng bắt buộc giữa unit đi qua port (BR-U03-64).

## 8. Ảnh hưởng tới U01

- Gửi OTP dùng `JobPort.enqueue` (yêu cầu OTP không ghi database nên message gửi ngay) với `jobType = OTP_DELIVERY`, `idempotencyKey` = `accountId:purpose:phút hiện tại`. Mất message thì người dùng yêu cầu OTP lại; OTP không cần `PendingSweeper`.

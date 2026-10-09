# U03 File, Job & Event - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 70, 71 (Settings, người dùng chốt U03 giữ ngày 2026-10-09) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-SET-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Luồng F1–F4, F6 cho tệp; S1–S4 cho cài đặt hệ thống (UC 70–71); J1–J4 cho việc nền; E1 cho sự kiện thông báo.

## 1. F1 - Upload

**Vào**: phiên đăng nhập, `purpose`, file multipart.

1. Kiểm quyền upload theo `purpose` (BR-U03-04).
2. Đọc stream, dừng nếu vượt trần của `purpose` lấy từ Settings (BR-U03-02, 87).
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

## 4. F4 - Worker đọc file

1. Worker U05 gọi `open(fileId)` để quét học liệu; U09 gọi khi nhúng ảnh vào DOCX xuất.
2. Trả stream từ Drive; abuse → lỗi vĩnh viễn như F3.

## 5. F6 - Giới hạn tải lên cho frontend

1. Frontend gọi `GET /api/v1/files/policies` (người đã đăng nhập).
2. U03 đọc trần dung lượng và loại tệp cho phép của từng `purpose` qua `SettingsPort`.
3. `FileUploader` dùng kết quả để kiểm sơ bộ trước khi gửi; backend vẫn kiểm lại khi upload (F1).


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
   - Vĩnh viễn hoặc hết lượt → gọi `onFailed`, unit sở hữu chuyển dòng sang trạng thái lỗi; log ERROR; ack (BR-U03-56, 59).

### J3 - Gửi lại việc bị mất (mỗi phút)
1. Với mỗi `PendingSweeper` đã đăng ký, lấy các dòng còn ở trạng thái chờ và cập nhật lần cuối quá 5 phút.
2. Gửi lại `JobMessage` cho từng dòng; handler idempotent nên gửi trùng không gây hại (BR-U03-58, 59).

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

## 9. Cài đặt hệ thống (S1–S4, UC 70–71)

### S1 - Xem cài đặt (UC 70)
1. Admin mở Setting List từ Admin Dashboard; frontend gọi `GET /api/v1/admin/settings`.
2. Kiểm quyền qua U01: không phải `ADMIN` → `403`, audit bị từ chối (BR-U03-80).
3. Đọc `system_settings`, ghép với `SettingDefinition` để có tên, mô tả, giới hạn; nhóm theo Tệp, Credit, AI.
4. Trả danh sách: khóa, nhóm, tên, giá trị hiện hành, người sửa và thời điểm sửa gần nhất, `version`.
5. Bấm một mục → Setting Detail; frontend gọi `GET /api/v1/admin/settings/{key}` lấy thêm mô tả và giới hạn hợp lệ.

### S2 - Sửa cài đặt (UC 71)
1. Trên Setting Detail, Admin nhập giá trị mới và lưu; frontend gọi `PATCH /api/v1/admin/settings/{key}` với `value`, `version`.
2. Kiểm quyền `ADMIN` (BR-U03-80).
3. Tìm `SettingDefinition` của khóa; không có → `404`.
4. Kiểm giá trị theo khai báo (kiểu, khoảng min–max, tập cho phép); sai → `400` kèm lý do (BR-U03-82, 87).
5. Cập nhật có điều kiện `version` khớp; không khớp → `409`, frontend báo tải lại (BR-U03-82).
6. Ghi audit `SETTING_UPDATED` với giá trị trước và sau trong cùng transaction (BR-U03-83).
7. Xóa cache của khóa ở instance hiện tại; instance khác nhận giá trị mới trong tối đa 30 giây (BR-U03-86).
8. Trả giá trị và `version` mới.

### S3 - Unit đọc cài đặt
1. Unit gọi `SettingsPort` khi cần: U03 lúc upload, U07 lúc cấp credit định kỳ, U13 trước khi gọi AI.
2. U03 trả giá trị trong cache nếu chưa quá 30 giây; quá hạn thì đọc lại `system_settings`.
3. Không đọc được database và không còn cache → báo lỗi tạm thời như mọi thao tác cần database.

### S4 - Khởi tạo mục mặc định
1. Khi backend khởi động, gom mọi `SettingDefinition` đã khai báo (U03, U07, U13).
2. Mục chưa có trong `system_settings` → thêm với giá trị mặc định, `version` 1, chưa có người sửa.
3. Mục đã có → giữ nguyên giá trị Admin đã sửa (BR-U03-84).

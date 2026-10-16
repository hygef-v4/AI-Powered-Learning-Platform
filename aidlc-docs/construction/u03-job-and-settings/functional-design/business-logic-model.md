# U03 Job and Settings — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3. Viết lại từ thiết kế chi tiết 2026-10-09 theo phạm vi 66 UC: bỏ avatar và việc nền của Code Lab, PayOS, tóm tắt học liệu; thêm backup và các nhóm cài đặt mới. Chỉ sửa tài liệu; unit chưa có code (khung code tạo lại 2026-10-16).

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

Luồng F1–F5 cho tệp, J1–J4 cho việc nền, E1 cho sự kiện thông báo, S1–S5 cho cài đặt (UC 14, 15), B1–B5 cho backup.

## 1. Truy vết

| Luồng | UC / story | Màn |
|---|---|---|
| F1–F5 Tệp | FR-013; dùng bởi UC của U07, U08, U12, U13 | Màn của unit sở hữu |
| J1–J4 Việc nền, E1 Sự kiện | Hạ tầng cho mọi unit | - |
| S1 Xem cài đặt | UC 14, US-SET-001 | Setting List, Setting Detail |
| S2 Sửa cài đặt | UC 15, US-SET-001 | Setting Detail |
| S3–S5 | UC 14, 15 | - |
| B1–B5 Backup | US-OPS-001, FR-038; UC 66 (xem trạng thái) | Admin Dashboard (U15) |

## 2. Tệp

### F1 - Upload
**Vào**: phiên đăng nhập, `purpose`, tệp multipart.
1. Kiểm role được upload theo `purpose` qua `IAuthorization` (BR-U03-04).
2. Lấy trần dung lượng và loại cho phép của `purpose` từ `ISettings`.
3. Đọc stream ra thư mục tạm; vượt trần thì dừng ngay và trả `413` (BR-U03-02).
4. Xác định loại từ magic bytes, đối chiếu danh sách cho phép; không khớp thì trả `415` và ghi audit (BR-U03-03, 40).
5. Tính SHA-256 (BR-U03-05).
6. Tải lên Drive vào thư mục của `purpose`, ghi thuộc tính tệp (`ownerAccountId`, `purpose`, `mediaType`, `originalFileName` đã làm sạch, `sha256`), nhận `fileId`.
7. Bước 6 lỗi: xóa tệp Drive nếu đã tạo, thử 3 lần; vẫn lỗi thì gửi việc `DRIVE_CLEANUP`; trả lỗi (BR-U03-06).
8. Xóa tệp tạm trong mọi trường hợp.
9. Trả `{ fileRef, mediaType, byteSize, originalFileName, expiresAt }`; không trả `fileId`, không ghi database.

### F2 - Gắn vào đối tượng
1. Unit sở hữu (U13 với học liệu; U07, U08, U12 với ảnh trong tài liệu) gọi `IArtifact.attach(fileRef, actor, purpose)` trong transaction nghiệp vụ.
2. U03 kiểm chữ ký HMAC, hạn 1 giờ, người gắn là người đã tải lên, đúng `purpose` (BR-U03-30).
3. Trả `{ fileId, mediaType, byteSize, originalFileName }`; unit sở hữu lưu `fileId` vào dòng của mình.

### F3 - Tải về
1. Frontend gọi API của unit sở hữu (ví dụ "xem học liệu"); unit đó tự kiểm quyền nghiệp vụ (BR-U03-20).
2. Unit sở hữu gọi `issueDownloadToken(fileId, accountId)`; U03 sinh token ngẫu nhiên 256 bit, lưu bản băm vào Redis 5 phút.
3. Frontend nhận URL `/api/v1/files/download/{token}` và mở.
4. Khi tải: tìm token, kiểm `accountId` khớp phiên; sai người thì trả `404` và ghi audit (BR-U03-21, 40).
5. Đọc thuộc tính, stream từ Drive với header an toàn; SVG kèm CSP sandbox (BR-U03-10, 22, 23).
6. Drive báo abuse thì ghi audit và trả `410` (BR-U03-24).

### F4 - Đọc tệp phía server
1. Unit cần đọc nội dung tệp gọi `IArtifact.open(fileId)`: U07, U08, U12 nhúng ảnh khi xuất DOCX.
2. U03 trả stream từ Drive; abuse thì báo lỗi vĩnh viễn như F3.

### F5 - Giới hạn tải lên cho frontend
1. Frontend gọi `GET /api/v1/files/policies` (người đã đăng nhập).
2. U03 đọc trần và loại cho phép của từng `purpose` qua `ISettings`.
3. `FileUploader` dùng kết quả để kiểm sơ bộ; backend vẫn kiểm lại ở F1.

## 3. Việc nền

### J1 - Gửi việc nền
1. Unit gọi ghi dòng nghiệp vụ ở trạng thái chờ trong transaction của mình.
2. Gọi `IJob.enqueue(jobType, payload, idempotencyKey)`; U03 kiểm `jobType` đã đăng ký và payload không có khóa cấm (BR-U03-52).
3. Có transaction thì đăng ký gửi sau commit; không có thì gửi ngay (BR-U03-50).
4. Gửi `JobMessage` (`attempt = 0`) tới exchange `jobs`, routing key là `jobType`. Gửi lỗi thì log WARN; J3 gửi lại.

### J2 - Worker xử lý việc nền
1. Listener của queue nhận `JobMessage`, gọi handler của unit sở hữu.
2. Handler đọc dòng nghiệp vụ; không còn chờ thì ack và bỏ (BR-U03-51).
3. Thành công: handler cập nhật dòng; worker ack (BR-U03-53).
4. Lỗi tạm và `attempt` < 5: gửi bản sao `attempt + 1` vào queue thử lại theo backoff, ack bản gốc.
5. Lỗi vĩnh viễn hoặc hết lượt: gọi `onFailed`, unit chuyển dòng sang lỗi, log ERROR, ack (BR-U03-56).
6. Quá timeout của loại việc thì coi như lỗi tạm.

### J3 - Gửi lại việc bị mất (mỗi phút)
1. Với mỗi `IPendingSweeper` đã đăng ký, lấy các dòng còn chờ và cập nhật lần cuối quá 5 phút.
2. Gửi lại `JobMessage` cho từng dòng; handler idempotent nên trùng không gây hại (BR-U03-58, 59).

### J4 - Việc hẹn giờ (mỗi phút)
1. Worker gọi lần lượt các `IScheduledScanner` đã đăng ký (BR-U03-60).
2. Mỗi scanner đọc mốc thời gian trên bảng của unit mình và cập nhật có điều kiện, nên chạy lại không gây trùng.
3. Một scanner lỗi không chặn scanner khác.

## 4. Sự kiện thông báo

### E1 - Phát sự kiện
1. Sau commit, unit gọi `IEventPublisher.publish(event)` với `schemaVersion`, `eventId`, `eventType`, `sourceUnit`.
2. U03 kiểm payload không có khóa cấm, gửi lên exchange `platform.events` với routing key `eventType`. Lỗi thì log WARN, không thử lại (BR-U03-70).
3. U14 nghe qua queue `jobs.notification` để tạo thông báo trong app và email.

## 5. Cài đặt hệ thống

### S1 - Xem cài đặt (UC 14)
1. Admin mở Setting List từ Admin Dashboard; frontend gọi `GET /api/v1/admin/settings`.
2. Kiểm quyền qua `IAuthorization`; không phải `ADMIN` thì trả `403` và ghi audit bị từ chối (BR-U03-80).
3. Đọc `system_settings`, ghép với `SettingDefinition` để có tên, mô tả, giới hạn; nhóm theo Tệp, Backup, Hoạt động làm bài.
4. Với mỗi `ISettingsSectionProvider` đã đăng ký (API AI chung, quota giảng viên của U06), gọi `summary()` có timeout 1 giây; lỗi thì đánh dấu phần đó "không khả dụng" (BR-U03-89).
5. Trả danh sách nhóm: mỗi mục khóa–giá trị có giá trị hiện hành, người sửa và thời điểm sửa gần nhất, `version`; mỗi phần ủy quyền có tên và tóm tắt đã che.
6. Bấm một mục khóa–giá trị thì mở Setting Detail; frontend gọi `GET /api/v1/admin/settings/{key}` để lấy mô tả và giới hạn.
7. Bấm phần API AI chung hoặc quota thì mở panel do U06 cung cấp; panel gọi API của U06.

### S2 - Sửa cài đặt (UC 15)
1. Trên Setting Detail, Admin nhập giá trị mới và lưu; frontend gọi `PATCH /api/v1/admin/settings/{key}` kèm `value`, `version`.
2. Kiểm quyền `ADMIN` (BR-U03-80).
3. Tìm `SettingDefinition`; không có thì trả `404`.
4. Kiểm kiểu, khoảng giá trị, tập cho phép; kiểm ràng buộc giữa các mục (ví dụ bật backup cần đã đặt giờ); sai thì trả `400` kèm lý do (BR-U03-82, 88).
5. Cập nhật có điều kiện `version` khớp; không khớp thì trả `409`, frontend báo tải lại.
6. Ghi audit `SETTING_UPDATED` với giá trị trước và sau trong cùng transaction (BR-U03-83).
7. Xóa cache của khóa ở instance hiện tại; instance khác nhận giá trị mới trong tối đa 30 giây (BR-U03-86).
8. Trả giá trị và `version` mới.
9. Sửa API AI chung hoặc quota đi qua API của U06; U03 không tham gia transaction đó (BR-U03-89).

### S3 - Unit đọc cài đặt
1. Unit gọi `ISettings.get(key)` khi cần: U03 lúc upload và lúc chạy backup, U08 lúc dọn dữ liệu hoạt động.
2. Trả giá trị trong cache nếu chưa quá 30 giây; quá hạn thì đọc lại bảng.
3. Không đọc được database và không còn cache thì báo lỗi tạm thời.

### S4 - Khởi tạo mục khi khởi động
1. Gom mọi `SettingDefinition` đã khai báo.
2. Mục chưa có trong `system_settings` thì thêm với giá trị mặc định (hoặc rỗng), `version` 1, chưa có người sửa.
3. Mục đã có thì giữ nguyên (BR-U03-84).

### S5 - Đăng ký phần cài đặt ủy quyền
1. Unit có cài đặt không dạng khóa–giá trị cài `ISettingsSectionProvider` (`sectionKey`, `title`, `summary`) và đăng ký với U03 khi khởi động.
2. Frontend của unit đó đăng ký panel cho `sectionKey` vào registry của màn Settings.
3. Chưa có unit nào đăng ký thì Setting List không hiện phần đó (trước khi U06 hoàn tất ở Iter 2).

## 6. Backup

### B1 - Lên lịch
1. `BackupScheduleScanner` chạy mỗi phút trong worker.
2. Đọc `backup.enabled`, `backup.dailyAt` qua `ISettings`; tắt hoặc chưa cấu hình thì bỏ qua.
3. Đến hoặc qua giờ chạy của hôm nay và chưa có lần chạy cho ngày đó thì thêm `backup_runs` trạng thái `CREATED` với khóa duy nhất là ngày chạy (chạy lại không tạo trùng).
4. Trong cùng transaction, gửi việc `BACKUP_RUN` qua `IJob` (queue `jobs.backup`).

### B2 - Chạy backup
1. Handler nhận `BACKUP_RUN`, chuyển `CREATED` → `RUNNING` có điều kiện; dòng không còn `CREATED` thì bỏ (BR-U03-51).
2. Kiểm dung lượng đĩa tạm; không đủ thì `FAILED` ngay.
3. Chạy `pg_dump` ra tệp tạm (một snapshot nhất quán), ghi phiên bản schema (Flyway) và phiên bản ứng dụng.
4. Mã hóa dump (BR-U03-93), upload vào thư mục backup riêng, kiểm kích thước và checksum sau upload. Không chép tệp trên Drive (BR-U03-91).
5. Ghi `SUCCESS` với tham chiếu archive, dung lượng, thời điểm xong; ghi audit `BACKUP_COMPLETED` (BR-U03-92).
6. Chạy B3.
7. Xóa tệp tạm trong mọi trường hợp.

### B3 - Giữ bản trong 7 ngày
1. Lấy các lần `SUCCESS` có `snapshot_at` cũ hơn 7 ngày; luôn chừa bản thành công mới nhất.
2. Xóa archive của các lần đó trên Drive; đánh dấu `purgedAt`, giữ dòng lịch sử (BR-U03-96).

### B4 - Lỗi backup
1. Lỗi tạm (Drive 429/5xx, mạng): handler thử lại theo J2.
2. Lỗi vĩnh viễn hoặc hết lượt: `onFailed` chuyển `FAILED` kèm lỗi đã che, gửi `DRIVE_CLEANUP` để xóa phần upload dở của lần đó.
3. Ghi audit `BACKUP_FAILED`, phát sự kiện `backup.failed` sau commit; U14 gửi thông báo trong app và email cho Admin.
4. Bản thành công trước đó giữ nguyên (BR-U03-95).

### B5 - Xem trạng thái và khôi phục
1. U15 gọi `IBackupStatus.latest()` và `list(page)` để hiện trên Admin Dashboard: lần thành công gần nhất, lịch sử, dung lượng, lỗi; không có nút tải (BR-U03-97).
2. Khôi phục do người vận hành làm theo runbook trong môi trường cô lập: tải archive, giải mã bằng khóa giữ ngoài VPS, `pg_restore` vào database mới.
3. Tệp trên Drive không được khôi phục: database trỏ tới tệp đang có trên Drive; tệp đã bị xóa sau thời điểm backup thì thiếu (BR-U03-91).
4. Kiểm theo danh sách: số dòng các bảng chính, quyền, tài liệu và XML sơ đồ, bài nộp và điểm, quota và usage, audit; đo RPO/RTO và lưu bằng chứng (BR-U03-98).

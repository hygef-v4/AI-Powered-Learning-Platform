# U03 Job and Settings — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3. Viết lại từ thiết kế chi tiết 2026-10-09 theo phạm vi 66 UC: bỏ avatar và việc nền của Code Lab, PayOS, tóm tắt học liệu; thêm backup và các nhóm cài đặt mới. Chỉ sửa tài liệu; unit chưa có code (khung code tạo lại 2026-10-16).

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

Mỗi rule có mã `BR-U03-xx` để truy vết sang test. Mã của phần tệp và việc nền giữ như bản 2026-10-09.

## 1. Upload

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-01 | Upload đi qua backend trong một request multipart. | Thiết kế |
| BR-U03-02 | Trần dung lượng theo `purpose` lấy từ Settings (BR-U03-87): `MATERIAL` mặc định và tối đa 50 MB, `DOCUMENT_IMAGE` mặc định và tối đa 5 MB. Vượt thì từ chối trước khi đọc hết. Không còn purpose `AVATAR` (hồ sơ không có ảnh đại diện, BR-U01-52). | FR-013, FR-033 |
| BR-U03-03 | Loại tệp xác định bằng nội dung (magic bytes) và phải thuộc danh sách loại cho phép của `purpose` lấy từ Settings. Đuôi tệp và `Content-Type` của trình duyệt chỉ để tham khảo. | FR-033, SEC-003 |
| BR-U03-04 | `MATERIAL`: Teacher hoặc Subject Manager, U13 kiểm thêm phân công môn/lớp. `DOCUMENT_IMAGE`: Student, Teacher hoặc Subject Manager đang được sửa tài liệu đó; U07, U08, U12 kiểm thêm quyền sửa. Admin không upload. U03 chỉ kiểm role và `purpose`; unit sở hữu kiểm phạm vi. | FR-002, SEC-002 |
| BR-U03-05 | Tính SHA-256 khi nhận và lưu vào thuộc tính tệp trên Drive. | Thiết kế |
| BR-U03-06 | Chỉ khi tệp và thuộc tính đã lên Drive mới trả `FileRef`. Lỗi ở bất kỳ bước nào thì xóa tệp Drive nếu đã tạo; xóa lỗi thì gửi việc `DRIVE_CLEANUP`. | REL-003 |
| BR-U03-07 | Upload thành công được giữ kể cả khi chưa gắn vào đối tượng nào; không có việc dọn tệp chưa gắn. | Thiết kế |
| BR-U03-08 | Tên tệp gốc được làm sạch: bỏ đường dẫn, ký tự điều khiển, cắt còn 255 ký tự. | SEC-003 |

## 2. Ảnh SVG và Draw.io

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-10 | Ảnh SVG khi tải luôn kèm header `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; sandbox` để script trong SVG không chạy. | SEC-004 |
| BR-U03-11 | U03 không lưu và không kiểm XML Draw.io: XML nằm trong khối sơ đồ của tài liệu do U07, U08, U12 lưu và kiểm. | Thiết kế |

## 3. Tải về

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-20 | Unit sở hữu đối tượng kiểm quyền nghiệp vụ rồi mới gọi `issueDownloadToken`; U03 không tự cấp quyền xem tệp. | FR-002 |
| BR-U03-21 | Token hạn 5 phút, gắn đúng `accountId`; người khác dùng token thì bị từ chối. | SEC-002 |
| BR-U03-22 | Backend stream tệp từ Drive; không bao giờ trả `fileId` hay link Drive cho frontend. | SEC-002 |
| BR-U03-23 | Header tải về: `Content-Type` đã xác định, `X-Content-Type-Options: nosniff`; tệp không phải ảnh hoặc PDF luôn `attachment`. | SEC-004 |
| BR-U03-24 | Drive từ chối trả tệp vì abuse thì trả "tệp không khả dụng" và ghi audit; không dùng `acknowledgeAbuse`. | Thiết kế |

## 4. Gắn và lưu giữ

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-30 | `attach` kiểm chữ ký và hạn của `FileRef`, người gắn là người đã tải lên, đúng `purpose`; unit sở hữu lưu `fileId` vào dòng của mình. | Thiết kế |
| BR-U03-31 | Không có tệp dẫn xuất; mọi tệp do người dùng tải lên. | Thiết kế |
| BR-U03-32 | Tệp đã upload thành công không bị xóa bởi nghiệp vụ; U03 không có chức năng xóa tệp đang dùng. | Thiết kế |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-40 | Audit: upload bị từ chối, Drive từ chối trả tệp vì abuse, dùng token sai người, sửa cài đặt, backup thành công và thất bại. Không audit từng lần tải thành công. | FR-014, SEC-005 |
| BR-U03-41 | Drive không khả dụng thì upload và tải về trả "tạm thời không khả dụng", không để lại dữ liệu dở dang. | REL-003 |

## 6. Việc nền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-50 | `IJob.enqueue` không ghi database: trong transaction của unit gọi, nó đăng ký gửi `JobMessage` sau commit, rollback thì không gửi; gọi ngoài transaction thì gửi ngay. Trạng thái việc nằm ở dòng nghiệp vụ của unit gọi. | Thiết kế |
| BR-U03-51 | Chống xử lý trùng dựa vào trạng thái dòng nghiệp vụ: handler gặp dòng đã xử lý hoặc không còn chờ thì ack và bỏ. `idempotencyKey` đi trong message để log và đối chiếu. | Thiết kế |
| BR-U03-52 | Payload chỉ chứa ID và tham chiếu; U03 từ chối payload có khóa cấm (`password`, `otp`, `token`, `secret`, `phone`) bằng `ForbiddenKeyGuard`. | SEC-005 |
| BR-U03-53 | Worker ack thủ công, chỉ sau khi handler đã cập nhật dòng nghiệp vụ. | Thiết kế |
| BR-U03-56 | Lỗi tạm và `attempt` < 5: gửi lại vào hàng chờ thử lại có TTL theo lượt (30 giây, 1, 2, 4, 8 phút), hết TTL thì quay về queue gốc. Lỗi vĩnh viễn hoặc hết lượt: gọi `onFailed` của unit sở hữu để chuyển dòng nghiệp vụ sang trạng thái lỗi, log ERROR. | REL-003 |
| BR-U03-57 | Không có màn hình hay nút chạy lại việc nền; người dùng thao tác lại từ màn nghiệp vụ nếu màn đó cho phép. | Thiết kế |
| BR-U03-58 | Message có thể mất nếu backend dừng giữa commit và gửi. Mỗi unit có việc nền đăng ký `IPendingSweeper`: mỗi phút, dòng còn chờ quá 5 phút được gửi lại message. | REL-003 |
| BR-U03-59 | Handler của unit sở hữu phải idempotent vì một việc có thể được gửi hơn một lần. | Thiết kế |
| BR-U03-60 | Việc hẹn giờ không dùng message hẹn giờ: mỗi unit đăng ký `IScheduledScanner` chạy mỗi phút trong worker, đọc mốc thời gian trên bảng của mình và cập nhật idempotent. | Thiết kế |
| BR-U03-61 | Frontend xem trạng thái việc nền qua API của unit sở hữu; U03 không có API trạng thái job. | SEC-006 |
| BR-U03-63 | Mỗi `jobType` thuộc đúng một trong 5 queue: `jobs.triggered` (việc nội bộ sau thao tác người dùng), `jobs.email` (SMTP, OTP ưu tiên hơn email thông báo), `jobs.ai` (gọi API AI), `jobs.drive` (dọn tệp Drive), `jobs.backup` (chạy backup). Các queue `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.payos` của bản cũ bị bỏ. | Phạm vi 66 UC |
| BR-U03-64 | Phản ứng nghiệp vụ bắt buộc giữa các unit (tạo đánh giá khi nộp, tự nộp khi ngưng giao) không dùng event: unit nguồn gọi port do unit nhận cài, trong cùng transaction. | Thiết kế |

## 7. Sự kiện thông báo

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-70 | `IEventPublisher.publish` gửi sau commit, không đảm bảo giao hàng; chỉ dùng cho thông báo của U14, nên mất sự kiện được chấp nhận. | Thiết kế |
| BR-U03-71 | Mọi message có `schemaVersion`; consumer bỏ qua và log WARN nếu gặp phiên bản không hỗ trợ. | Thiết kế |

## 8. Cài đặt hệ thống (UC 14, 15)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-80 | Chỉ `ADMIN` đang `ACTIVE` xem (UC 14) và sửa (UC 15) cài đặt; role khác bị từ chối và ghi audit; không kiểm được quyền thì từ chối. | FR-033, SEC-002 |
| BR-U03-81 | Cài đặt dạng khóa–giá trị do unit sở hữu khai báo trong code (`SettingDefinition`): khóa, nhóm, tên, mô tả, kiểu, giá trị mặc định (có thể không có), giới hạn hợp lệ. U03 lưu, hiển thị và kiểm theo khai báo; unit sở hữu đọc giá trị qua `ISettings`. Nhóm hiện có: Tệp và Backup (U03), Hoạt động làm bài (U08). | FR-033 |
| BR-U03-82 | Sửa phải gửi kèm `version` hiện tại; lệch thì trả `409`. Giá trị sai kiểu, ngoài giới hạn hoặc vi phạm ràng buộc giữa các mục thì trả `400` kèm lý do, không ghi gì. | FR-033 |
| BR-U03-83 | Mỗi lần sửa ghi audit `SETTING_UPDATED` (khóa, giá trị trước và sau, người sửa, thời điểm) trong cùng transaction. | FR-014 |
| BR-U03-84 | Không thêm hay xóa mục từ giao diện. Khi khởi động, mục đã khai báo mà chưa có trong bảng được tạo với giá trị mặc định (hoặc rỗng là "chưa cấu hình"); mục đã có giữ nguyên giá trị Admin đã sửa. | FR-033 |
| BR-U03-85 | Bí mật (API key, mật khẩu SMTP, khóa Google Drive, khóa ký, khóa backup) không phải cài đặt khóa–giá trị: hoặc là biến môi trường, hoặc do unit sở hữu lưu dạng bảo vệ (API AI chung của U06). | SEC-006 |
| BR-U03-86 | Giá trị mới có hiệu lực trong tối đa 30 giây ở mọi instance backend và worker; thao tác đang chạy giữ giá trị cũ; dữ liệu đã tạo không bị ảnh hưởng. | FR-033 |
| BR-U03-87 | Nhóm Tệp: `files.material.maxSizeMb` (1–50, mặc định 50), `files.material.allowedTypes` (tập con khác rỗng của PDF, DOCX, PPTX), `files.documentImage.maxSizeMb` (1–5, mặc định 5), `files.documentImage.allowedTypes` (tập con khác rỗng của PNG, JPEG, GIF, SVG). | FR-013, FR-033 |
| BR-U03-88 | Nhóm Backup: `backup.enabled` (mặc định tắt), `backup.dailyAt` (giờ:phút theo Asia/Ho_Chi_Minh, không có mặc định). Chỉ bật được khi đã đặt giờ; giờ chạy phải được xác nhận trước triển khai, hệ thống không tự gán. Thời gian giữ bản cố định 7 ngày (BR-U03-96), không phải cài đặt. | FR-038, council features, chốt 2026-10-15 |
| BR-U03-89 | Cài đặt không dạng khóa–giá trị (API AI chung, quota giảng viên) do unit sở hữu (U06) cung cấp qua `ISettingsSectionProvider`: U03 chỉ hiện tên và bản tóm tắt đã che trên Setting List; xem chi tiết, sửa, thử kết nối, thu hồi đi qua API của U06, U06 tự kiểm quyền, version và audit. U03 không đọc hay ghi bảng của U06, không giữ key rõ. Phần của U06 không trả lời thì hiện "không khả dụng", không bao giờ báo đã lưu. | FR-033, FR-034 |

## 9. Backup và khôi phục

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-90 | Backup chạy theo lịch trong Settings, mỗi ngày tối đa một lần chạy theo lịch; không có nút backup thủ công trên giao diện. | FR-038 |
| BR-U03-91 | Chỉ backup database: một lần backup gồm bản dump PostgreSQL nhất quán tại một thời điểm, phiên bản schema và ứng dụng. Tệp người dùng trên Drive (học liệu, ảnh trong tài liệu) không được backup; tệp bị mất hoặc xóa trên Drive thì không khôi phục được từ backup. | FR-038, chốt 2026-10-15 |
| BR-U03-92 | Trạng thái lần chạy: `CREATED` → `RUNNING` → `SUCCESS` hoặc `FAILED`. Chỉ `SUCCESS` sau khi dump, upload archive và kiểm checksum đều xong. | FR-038 |
| BR-U03-93 | Archive dump được mã hóa trước khi upload; khóa giải mã không nằm trên VPS và không nằm trong archive. Không xuất API key hay mật khẩu dạng rõ; bản ghi credential trong database vẫn là ciphertext. | FR-038, SEC-006 |
| BR-U03-94 | Bản backup nằm trong thư mục Drive riêng tư tách khỏi thư mục tệp đang dùng. | FR-038 |
| BR-U03-95 | Lỗi tạm (Drive, mạng) được thử lại hữu hạn; lỗi vĩnh viễn hoặc hết lượt thì `FAILED` kèm lỗi đã che, dọn phần upload dở, ghi audit và phát sự kiện `backup.failed` để U14 báo Admin. Lỗi gửi email không đổi kết quả backup. | FR-038 |
| BR-U03-96 | Giữ các bản `SUCCESS` trong 7 ngày gần nhất; bản cũ hơn bị xóa khỏi thư mục backup. Không bao giờ xóa bản thành công gần nhất, kể cả khi đã quá 7 ngày vì các lần sau đều lỗi; bản lỗi không thay bản tốt. | FR-038, chốt 2026-10-15 |
| BR-U03-97 | Admin chỉ xem trạng thái, lịch sử, dung lượng và lỗi (qua Admin Dashboard của U15); quyền xem không cho tải archive hay dữ liệu học vụ. | FR-038, UC 66 |
| BR-U03-98 | Khôi phục do người vận hành làm theo runbook, chỉ vào môi trường cô lập trước; không có nút khôi phục production hay tool MCP khôi phục. Sau khôi phục phải kiểm quyền, tài liệu và XML sơ đồ, bài nộp, điểm, quota và usage, số dòng audit; ghi bằng chứng và RPO/RTO đo được. Tệp trên Drive không được khôi phục (BR-U03-91). Không cam kết không mất dữ liệu. | FR-038 |

## 10. Quy tắc chung

- Quyền kiểm phía server ở mọi request web, API và MCP, mặc định từ chối; route đổi trạng thái có chống CSRF; không tin actor do client gửi.
- Sửa có `version`; lệnh lặp được có `idempotencyKey`; xung đột không ghi đè trạng thái hiện tại.
- Repository của unit là riêng; unit khác đọc ghi qua public port.

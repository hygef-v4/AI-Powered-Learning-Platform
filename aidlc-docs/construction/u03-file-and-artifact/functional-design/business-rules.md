# U03 File, Job & Event - Business Rules

**Bản tài liệu 2026-10-09**: UC 70, 71 (Settings, người dùng chốt U03 giữ ngày 2026-10-09) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-SET-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Upload

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-01 | Upload đi qua backend trong một request multipart. | Câu 2 |
| BR-U03-02 | Trần dung lượng theo `purpose` lấy từ Settings (BR-U03-87): `MATERIAL` mặc định và tối đa 50 MB; `DOCUMENT_IMAGE` mặc định và tối đa 5 MB. Vượt thì từ chối trước khi đọc hết. Không còn purpose `AVATAR`. | FR-013, FR-033, BR-U09-34 |
| BR-U03-03 | Loại file xác định bằng nội dung (magic bytes), phải khớp danh sách loại tệp cho phép của `purpose` lấy từ Settings (BR-U03-87). Đuôi file và `Content-Type` của trình duyệt chỉ để tham khảo. | Câu 5, FR-033 |
| BR-U03-04 | `MATERIAL`: Teacher hoặc Subject Manager; unit nghiệp vụ kiểm thêm phân công R2/R3/R4. `DOCUMENT_IMAGE`: Student, Teacher hoặc Subject Manager đang được sửa tài liệu đó (R3/R4/R5). Admin không upload vì không dạy và không quản lý môn (người dùng chốt 2026-10-09). U03 kiểm role và `purpose`, unit gọi kiểm phạm vi. | FR-002, FR-004, SEC-002 |
| BR-U03-05 | Tính SHA-256 khi nhận; lưu vào thuộc tính tệp trên Drive. | services.md |
| BR-U03-06 | Chỉ khi file và thuộc tính đã lên Drive mới trả về `FileRef`. Lỗi ở bất kỳ bước nào → xóa file trên Drive nếu đã tạo (lỗi xóa thì gửi việc `DRIVE_CLEANUP`). | Câu 7 |
| BR-U03-07 | Upload thành công được giữ kể cả khi chưa gắn vào đối tượng nào; không có việc dọn file chưa gắn. | Câu 7 |
| BR-U03-08 | Tên file gốc được làm sạch: bỏ đường dẫn, ký tự điều khiển, cắt còn 255 ký tự. | SEC-003 |

## 2. Ảnh SVG và Draw.io

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-10 | Ảnh SVG (`DOCUMENT_IMAGE`) khi tải luôn kèm header `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; sandbox` để script trong SVG không chạy. | SEC-004 |
| BR-U03-11 | U03 không lưu và không kiểm XML Draw.io: XML đầy đủ nằm trong block `DIAGRAM` của tài liệu, do U09 kiểm (BR-U09-35); bản rút gọn cho AI do U09 tạo trong bộ nhớ, không lưu. | U09, Invariant #5 |

## 3. Tải về

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-20 | Unit sở hữu kiểm quyền nghiệp vụ rồi gọi issueDownloadToken; U03 không tự cấp quyền xem file. Token gắn đúng accountId/TTL; không có ngoại lệ xem avatar cho mọi tài khoản. | FR-001/002 |
| BR-U03-21 | Token hạn 5 phút, gắn với đúng `accountId`; người khác dùng token → từ chối. | Câu 3 |
| BR-U03-22 | Backend stream file từ Drive; không bao giờ trả `fileId` hay link Drive cho frontend. | SEC-002 |
| BR-U03-23 | Header tải về: `Content-Type` đã xác định, `X-Content-Type-Options: nosniff`; file không phải ảnh/PDF luôn `attachment`. | SEC-004 |
| BR-U03-24 | Drive từ chối trả file vì abuse → trả "file không khả dụng" và audit; không dùng `acknowledgeAbuse`. | components.md §5 |

## 4. Gắn và lưu giữ

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-30 | `attach` kiểm chữ ký và hạn của `FileRef`, `ownerAccountId` = người gắn, đúng `purpose`; unit sở hữu lưu `fileId` vào dòng của mình. | Thiết kế |
| BR-U03-31 | Không có file dẫn xuất; mọi file do người dùng tải lên. | Rút gọn U03 (2026-09-25) |
| BR-U03-32 | File đã upload thành công không bị xóa; U03 không có chức năng xóa file. | Invariant #4 |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-40 | Audit: upload bị từ chối, Drive từ chối trả file vì abuse, dùng token sai người. Không audit từng lần tải thành công. | SEC-005 |
| BR-U03-41 | Drive không khả dụng: upload và tải về trả "tạm thời không khả dụng"; không để lại dữ liệu dở dang. | REL-003 |

## 6. Việc nền (chuyển từ U02, 2026-10-04)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-50 | `JobPort.enqueue` không ghi database: trong transaction của unit gọi, nó đăng ký gửi `JobMessage` **sau commit**; rollback thì không gửi. Gọi ngoài transaction (thao tác không ghi database) thì gửi ngay. Trạng thái việc nằm ở dòng nghiệp vụ mà unit gọi đã ghi trong cùng transaction (ví dụ `lessons.scan_status = PENDING`). | Quyết định 2026-10-03 |
| BR-U03-51 | Chống xử lý trùng dựa vào trạng thái dòng nghiệp vụ: handler gặp dòng đã xử lý hoặc không còn ở trạng thái chờ thì ack và bỏ. `idempotencyKey` đi trong message để log và đối chiếu. | Thiết kế |
| BR-U03-52 | Payload chỉ chứa ID/tham chiếu; U03 từ chối payload có khóa thuộc danh sách cấm ở BR-U02-05 (dùng `ForbiddenKeyGuard` trong `shared/`). | Worker payload rule |
| BR-U03-53 | Worker ack thủ công, chỉ sau khi handler đã cập nhật dòng nghiệp vụ. | Thiết kế |
| BR-U03-56 | Handler lỗi tạm và `attempt` < 5: ack message gốc, gửi lại vào hàng chờ thử lại có TTL theo lượt (30 s, 1, 2, 4, 8 phút); hết TTL message quay về queue gốc. Lỗi vĩnh viễn hoặc hết lượt: gọi `onFailed` của unit sở hữu để chuyển dòng nghiệp vụ sang trạng thái lỗi, log ERROR. | REL-003 |
| BR-U03-57 | Không có màn hình hay nút chạy lại việc nền; người dùng thao tác lại từ màn nghiệp vụ khi màn đó cho phép (ví dụ tạo yêu cầu AI mới sau lỗi nếu nghiệp vụ cho phép; học liệu không có quét lại thủ công). | Câu 3 |
| BR-U03-58 | Message có thể mất nếu backend dừng giữa commit và gửi, hoặc RabbitMQ không nhận. Mỗi unit có việc nền đăng ký một `PendingSweeper`: mỗi phút, dòng còn ở trạng thái chờ quá 5 phút được gửi lại message. | Câu 8 |
| BR-U03-59 | Handler của unit sở hữu phải idempotent vì một việc có thể được gửi hơn một lần. | Thiết kế |
| BR-U03-60 | Việc hẹn giờ (mở/đóng bài, tự nộp khi hết giờ, nhắc hạn, trả credit giữ quá hạn, đối soát PayOS) không dùng message hẹn giờ: mỗi unit đăng ký `ScheduledScanner` chạy mỗi phút trong worker, đọc mốc thời gian trên bảng của mình và cập nhật idempotent. | Quyết định 2026-10-03 |
| BR-U03-61 | Frontend xem trạng thái việc nền qua API của unit sở hữu (cột trạng thái của dòng nghiệp vụ); U03 không có API trạng thái job. | SEC-006 |
| BR-U03-63 | Mỗi `jobType` thuộc đúng một trong 7 queue: `jobs.triggered` (việc nội bộ phát sinh sau thao tác người dùng: tạo tài liệu nhóm, LESSON_SCAN trích chữ không AI), `jobs.email` (SMTP; priority: OTP trước email thông báo), `jobs.gemini` (gọi Gemini: MATERIAL_SUMMARY sau nút View Material, việc AI), `jobs.youtube` (YouTube Data API và phụ đề), `jobs.code` (Judge0), `jobs.drive` (dọn tệp Google Drive), `jobs.payos` (tra PayOS). | Câu N3 |
| BR-U03-64 | Phản ứng nghiệp vụ bắt buộc giữa các unit (tạo đánh giá khi nộp, tự nộp khi ngưng giao, nhận điểm Code Lab) **không** dùng event: unit nguồn gọi port do unit nhận cài, trong cùng transaction; cài đặt port ghi dòng của unit nhận hoặc gọi `JobPort.enqueue`. | Quyết định 2026-09-26 |

## 7. Sự kiện thông báo

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-70 | `EventPublisherPort.publish` gửi sau commit, không đảm bảo giao hàng; chỉ dùng cho thông báo (U16), nên mất sự kiện được chấp nhận. | Câu 2 |
| BR-U03-71 | Mọi message có `schemaVersion`; consumer bỏ qua và log WARN nếu gặp phiên bản không hỗ trợ. | Contract rule |

## 8. Cài đặt hệ thống (UC 70–71, thêm 2026-10-09)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-80 | Chỉ `ADMIN` đang `ACTIVE` xem (UC 70) và sửa (UC 71) cài đặt; vai trò khác bị từ chối và ghi audit; không kiểm được quyền thì từ chối. | FR-033, SEC-002 |
| BR-U03-81 | Cài đặt chia 3 nhóm: Tệp (U03), Credit tặng định kỳ (U07), AI (U13). Mỗi mục do unit sở hữu khai báo trong code (`SettingDefinition`): khóa, nhóm, tên hiển thị, mô tả, kiểu, giá trị mặc định, giới hạn hợp lệ. U03 lưu, hiển thị và kiểm theo khai báo; unit sở hữu dùng giá trị. Nhóm Credit: `credit.monthlyFreeCredits` (U07, 0–10 000, mặc định 100). Nhóm AI (U13): `ai.enabled`, `ai.dailyCostCapUsd`, `ai.ratePerMinute`, `ai.{task}.model`, `ai.{task}.enabled`. | FR-033 |
| BR-U03-82 | Sửa phải gửi kèm `version` hiện tại; version lệch thì trả `409`. Giá trị sai kiểu hoặc ngoài giới hạn thì trả `400` kèm lý do, không ghi gì. | FR-033 |
| BR-U03-83 | Mỗi lần sửa ghi audit `SETTING_UPDATED`: khóa, giá trị trước và sau, người sửa, thời điểm; ghi trong cùng transaction với lần sửa. | FR-014, FR-033 |
| BR-U03-84 | Không thêm hay xóa mục từ giao diện. Khi khởi động, mục đã khai báo mà chưa có trong bảng được tạo với giá trị mặc định; mục đã có giữ nguyên giá trị Admin đã sửa. | FR-033 |
| BR-U03-85 | Bí mật (API key, mật khẩu SMTP, khóa Google Drive, khóa ký) không phải cài đặt, vẫn là biến môi trường. | SEC-006 |
| BR-U03-86 | Giá trị mới có hiệu lực trong tối đa 30 giây ở mọi instance backend và worker; thao tác đang chạy giữ giá trị cũ; dữ liệu đã tạo (tệp đã tải, giao dịch cũ) không bị ảnh hưởng. | FR-033 |
| BR-U03-87 | Mục nhóm Tệp: `files.material.maxSizeMb` (1–50, mặc định 50), `files.material.allowedTypes` (tập con khác rỗng của PDF, DOCX, PPTX), `files.documentImage.maxSizeMb` (1–5, mặc định 5), `files.documentImage.allowedTypes` (tập con khác rỗng của PNG, JPEG, GIF, SVG). Trần 50 MB do Nginx và thư mục tạm; trần 5 MB theo BR-U09-34. | FR-013, FR-033 |

## Upload material và nút tóm tắt (revision 2026-10-09)

U03 upload MATERIAL chỉ lưu tệp và trả FileRef; không tóm tắt, không gọi Gemini, không quote/hold/reserve credit. U05 gắn tệp/tạo lesson và trích chữ/phụ đề không AI. Student, Teacher và Subject Manager yêu cầu từ nút Tóm tắt tài liệu trên View Material theo quyền xem U05/U04; U05 gọi U13 giữ credit của người bấm rồi enqueue MATERIAL_SUMMARY qua JobPort U03 (jobs.gemini). Quyền upload vẫn chỉ Teacher/Subject Manager; Student có nút tóm tắt không được upload MATERIAL. Generic worker retry không tự tạo yêu cầu AI cho upload.

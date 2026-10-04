# U03 File, Job & Event - Business Rules

## 1. Upload

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-01 | Upload đi qua backend trong một request multipart. | Câu 2 |
| BR-U03-02 | Trần dung lượng theo `purpose`: `AVATAR`, `MATERIAL` ≤ 50 MB; `DOCUMENT_IMAGE` ≤ 5 MB. Vượt thì từ chối trước khi đọc hết. | Câu 4, BR-U09-34 |
| BR-U03-03 | Loại file xác định bằng nội dung (magic bytes), phải khớp allowlist của `purpose`. Đuôi file và `Content-Type` của trình duyệt chỉ để tham khảo. | Câu 5 |
| BR-U03-04 | Quyền upload theo mục đích: `AVATAR` mọi người đã đăng nhập; `MATERIAL` giảng viên, chủ nhiệm môn (UC 11); `DOCUMENT_IMAGE` (ảnh trong khung đề, bài làm, tài liệu nhóm) giảng viên, chủ nhiệm môn và người học. U03 không có file do hệ thống tạo. | SEC-002 |
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
| BR-U03-20 | U03 **không tự quyết** ai được xem file. Unit sở hữu kiểm quyền nghiệp vụ rồi gọi `issueDownloadToken`. Riêng `AVATAR`: mọi người đã đăng nhập được xem. | components.md |
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
| BR-U03-57 | Không có màn hình hay nút chạy lại việc nền; người dùng thao tác lại từ màn nghiệp vụ khi màn đó cho phép (ví dụ quét lại học liệu). | Câu 3 |
| BR-U03-58 | Message có thể mất nếu backend dừng giữa commit và gửi, hoặc RabbitMQ không nhận. Mỗi unit có việc nền đăng ký một `PendingSweeper`: mỗi phút, dòng còn ở trạng thái chờ quá 5 phút được gửi lại message. | Câu 8 |
| BR-U03-59 | Handler của unit sở hữu phải idempotent vì một việc có thể được gửi hơn một lần. | Thiết kế |
| BR-U03-60 | Việc hẹn giờ (mở/đóng bài, tự nộp khi hết giờ, nhắc hạn, trả credit giữ quá hạn, đối soát PayOS) không dùng message hẹn giờ: mỗi unit đăng ký `ScheduledScanner` chạy mỗi phút trong worker, đọc mốc thời gian trên bảng của mình và cập nhật idempotent. | Quyết định 2026-10-03 |
| BR-U03-61 | Frontend xem trạng thái việc nền qua API của unit sở hữu (cột trạng thái của dòng nghiệp vụ); U03 không có API trạng thái job. | SEC-006 |
| BR-U03-63 | Mỗi `jobType` thuộc đúng một trong 7 queue: `jobs.triggered` (việc nội bộ phát sinh sau thao tác người dùng, ví dụ tạo tài liệu nhóm cho nhóm mới), `jobs.email` (SMTP; priority: OTP trước email thông báo), `jobs.gemini` (gọi Gemini: quét học liệu, việc AI), `jobs.youtube` (YouTube Data API và phụ đề), `jobs.code` (Judge0), `jobs.drive` (dọn tệp Google Drive), `jobs.payos` (tra PayOS). | Câu N3 |
| BR-U03-64 | Phản ứng nghiệp vụ bắt buộc giữa các unit (tạo đánh giá khi nộp, tự nộp khi ngưng giao, nhận điểm Code Lab) **không** dùng event: unit nguồn gọi port do unit nhận cài, trong cùng transaction; cài đặt port ghi dòng của unit nhận hoặc gọi `JobPort.enqueue`. | Quyết định 2026-09-26 |

## 7. Sự kiện thông báo

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-70 | `EventPublisherPort.publish` gửi sau commit, không đảm bảo giao hàng; chỉ dùng cho thông báo (U16), nên mất sự kiện được chấp nhận. | Câu 2 |
| BR-U03-71 | Mọi message có `schemaVersion`; consumer bỏ qua và log WARN nếu gặp phiên bản không hỗ trợ. | Contract rule |

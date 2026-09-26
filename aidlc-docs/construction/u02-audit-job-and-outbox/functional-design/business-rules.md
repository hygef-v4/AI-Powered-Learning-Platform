# U02 Audit, Job & Event - Business Rules

## 1. Audit

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-01 | Audit chỉ được thêm, không có API hay thao tác ứng dụng nào sửa hoặc xóa. | US-AUD-001 S2 |
| BR-U02-02 | `AuditPort.record` ghi thẳng vào bảng audit **trong transaction của unit gọi**: thao tác nghiệp vụ commit thì audit có, rollback thì audit không có. Sự kiện bị từ chối (`DENIED`) và lỗi (`FAILURE`) được ghi trong transaction riêng để không mất khi nghiệp vụ rollback. Không đi qua RabbitMQ. | Quyết định 2026-09-26 |
| BR-U02-03 | `eventId` duy nhất; ghi lại cùng `eventId` không tạo bản ghi thứ hai. | Thiết kế |
| BR-U02-04 | Ghi audit lỗi (ví dụ DB lỗi) thì thao tác nghiệp vụ cùng transaction cũng rollback; audit bắt buộc không bị mất âm thầm. | Quyết định 2026-09-26 |
| BR-U02-05 | `beforeData`/`afterData` không chứa mật khẩu, OTP, token, số điện thoại; unit gọi chịu trách nhiệm che, U02 từ chối lưu nếu phát hiện khóa thuộc danh sách cấm (`password`, `otp`, `token`, `secret`, `phone`). | SEC-005 |
| BR-U02-06 | Audit giữ vĩnh viễn. | Câu 6 |
| BR-U02-07 | Chỉ `ADMIN` được tra cứu audit; mọi lần tra cứu cũng được audit. | Câu 7, UC-OPS-02 |
| BR-U02-08 | Tra cứu lọc theo actor, action, resource, result, khoảng thời gian; sắp xếp mới nhất trước; mỗi trang tối đa 100 bản ghi. | US-AUD-001 S1 |

## 2. Job

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-20 | `enqueue` ghi dòng `jobs` ở `PENDING` **trong transaction của unit gọi**; sau khi commit mới gửi `JobMessage` sang RabbitMQ. | Câu 1, 2 |
| BR-U02-21 | Cùng `jobType` + `idempotencyKey` thì trả job đã có, không tạo mới. | Thiết kế |
| BR-U02-22 | Payload chỉ chứa ID/tham chiếu; U02 từ chối payload có khóa thuộc danh sách cấm ở BR-U02-05. | Worker payload rule |
| BR-U02-23 | Worker `claim` chỉ thành công khi job đang `PENDING` và `nextAttemptAt` đã tới; claim chuyển `RUNNING`, đặt lease 5 phút. Hai worker không thể giữ cùng một job. | Thiết kế |
| BR-U02-24 | Message tới cho job không còn `PENDING` thì bỏ qua (chống xử lý trùng). | Thiết kế |
| BR-U02-25 | `complete` chuyển `SUCCEEDED`, lưu `resultRef`. | component-methods |
| BR-U02-26 | `fail` với lỗi tạm và còn lượt: về `PENDING`, tăng `attempts`, `nextAttemptAt` theo backoff 30 s, 1, 2, 4, 8 phút. Lỗi vĩnh viễn hoặc hết lượt: `FAILED`. | REL-003 |
| BR-U02-27 | Job `FAILED` chỉ ghi log ERROR; **không** có màn hình hay nút chạy lại. | Câu 3 |
| BR-U02-28 | Tác vụ quét chạy mỗi phút: job `PENDING` có `nextAttemptAt` đã qua **5 phút** mà chưa được nhận thì gửi lại message. Job `RUNNING` quá hạn lease thì về `PENDING`. | Câu 8 |
| BR-U02-29 | Handler của unit sở hữu phải idempotent vì một job có thể được gửi hơn một lần. | Thiết kế |
| BR-U02-30 | Chỉ người tạo job và `ADMIN` xem được trạng thái; người khác nhận "không tìm thấy". | Câu 4 |
| BR-U02-31 | Trạng thái trả ra gồm `status`, `attempts`, `safeMessage`, thời gian; không trả payload hay `leaseOwner`. | SEC-006 |
| BR-U02-32 | Job ở trạng thái cuối được giữ; không có dọn dẹp trong MVP. | Thiết kế |
| BR-U02-33 | Mỗi `jobType` thuộc đúng một trong 8 queue: `jobs.scheduled` (việc nội bộ hẹn giờ, phải chạy đúng giờ: mở/đóng bài, tự nộp, nhắc hạn, chia email, trả credit quá hạn), `jobs.triggered` (việc nội bộ phát sinh sau thao tác người dùng, có thể dồn cục: tạo điểm khi nộp, tạo tài liệu nhóm), `jobs.email` (SMTP), `jobs.gemini` (Gemini), `jobs.youtube` (YouTube Data API), `jobs.code` (Judge0), `jobs.drive` (Google Drive), `jobs.payos` (PayOS). Queue có hệ thống ngoài giới hạn số việc chạy cùng lúc theo giới hạn của hệ thống đó. | Quyết định 2026-09-26 |
| BR-U02-34 | Phản ứng nghiệp vụ bắt buộc giữa các unit (tạo điểm khi nộp, tự nộp khi ngưng giao, nhận điểm Code Lab) **không** dùng event: unit nguồn gọi port do unit nhận cài, trong cùng transaction; cài đặt port chỉ tạo job của unit nhận qua `JobPort.enqueue`. | Quyết định 2026-09-26 |

## 3. Sự kiện nghiệp vụ

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-40 | `EventPublisherPort.publish` gửi sau commit, không đảm bảo giao hàng; chỉ dùng cho thông báo (U16), nên mất sự kiện được chấp nhận. | Câu 2 |
| BR-U02-41 | Mọi message có `schemaVersion`; consumer bỏ qua và log WARN nếu gặp phiên bản không hỗ trợ. | Contract rule |

## 4. Lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U02-50 | Không kiểm được quyền khi đọc audit/job thì từ chối. | SEC-002 |
| BR-U02-51 | Lỗi trả client an toàn, không lộ chi tiết queue hay database. | SEC-006 |

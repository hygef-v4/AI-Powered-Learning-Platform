# U02 Audit, Job & Outbox - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-01 | `AuditPort.record` và `EventPublisherPort.publish` không chặn request quá 50 ms; gửi RabbitMQ lỗi thì trả ngay. | NFR-003, BR-U02-04 |
| NFR-U02-02 | `JobPort.enqueue` chỉ thêm một câu INSERT vào transaction của unit gọi. | NFR-003 |
| NFR-U02-03 | Tra cứu audit và `getJobStatus`: p95 ≤ 500 ms với tới 1 triệu bản ghi audit. Có index theo `occurred_at`, `(actor_id, occurred_at)`, `(resource_type, resource_id)`, `action`. | NFR-003, BR-U02-08 |
| NFR-U02-04 | Frontend poll trạng thái job mỗi 3 giây, dừng ở trạng thái cuối. | frontend-components |

## 2. Xử lý job

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-10 | Worker là container riêng, dùng cùng mã nguồn backend với profile `worker`. | Câu N1 |
| NFR-U02-11 | Mỗi worker xử lý tối đa 4 job cùng lúc; consumer audit có luồng riêng, không bị job chậm chặn. | Câu N3 |
| NFR-U02-12 | Lease 5 phút; handler chạy lâu hơn phải gia hạn lease. | BR-U02-23 |
| NFR-U02-13 | Lượt quét job kẹt chạy mỗi phút trong worker; chỉ có một worker nên không cần khóa phân tán. | BR-U02-28 |
| NFR-U02-14 | Mỗi loại job có timeout xử lý do unit sở hữu khai báo; mặc định 60 giây. | REL-003 |

## 3. RabbitMQ

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-20 | Queue durable, message persistent; RabbitMQ khởi động lại không mất message đang chờ. | Câu N2 |
| NFR-U02-21 | Consumer ack thủ công, chỉ ack sau khi đã cập nhật PostgreSQL. | BR-U02-24 |
| NFR-U02-22 | Prefetch bằng số luồng xử lý (4 cho job, 10 cho audit). | Câu N3 |
| NFR-U02-23 | Kết nối RabbitMQ có timeout 5 s và tự kết nối lại. | REL-003 |

## 4. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-30 | User database của ứng dụng không có quyền `UPDATE`/`DELETE` trên `audit_events`. | BR-U02-01, SEC-005 |
| NFR-U02-31 | Tra cứu audit chỉ cho `ADMIN`; `getJobStatus` chỉ cho người tạo và `ADMIN`. | BR-U02-07, 30 |
| NFR-U02-32 | Tài khoản RabbitMQ riêng cho ứng dụng, mật khẩu từ biến môi trường; tắt tài khoản `guest`. | SEC-006 |

## 5. Khả dụng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-40 | RabbitMQ không khả dụng: thao tác nghiệp vụ vẫn thành công; job nằm ở `PENDING` chờ lượt quét; audit có thể mất (chấp nhận). | BR-U02-04, 40 |
| NFR-U02-41 | Worker chết: job `RUNNING` về `PENDING` khi hết lease; worker khởi động lại tự tiếp tục. | BR-U02-28 |
| NFR-U02-42 | Healthcheck worker kiểm PostgreSQL và RabbitMQ. | REL-002 |

## 6. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U02-50 | Unit test cho mọi `BR-U02-xx`. Integration test với PostgreSQL và RabbitMQ bằng Testcontainers: tạo job, claim trùng, retry, hết lease, quét gửi lại, audit trùng `eventId`. | NFR-004 |

## 7. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Danh sách khóa cấm, không log payload |
| SECURITY-04 | N/A | U02 không phục vụ HTML riêng; header ở Nginx chung |
| SECURITY-05 | Compliant | Validate bộ lọc, giới hạn trang, kiểm payload |
| SECURITY-08 | Compliant | NFR-U02-31 |
| SECURITY-09 | Compliant | NFR-U02-32 |
| SECURITY-12 | N/A | U02 không xác thực người dùng |
| SECURITY-15 | Compliant | Lỗi RabbitMQ không làm hỏng nghiệp vụ, lỗi trả an toàn |
| RESILIENCY-04 | Compliant | Worker deploy cùng Compose, rollback theo tag |
| RESILIENCY-06 | Compliant | NFR-U02-42 |
| RESILIENCY-10 | Compliant | Timeout RabbitMQ, timeout job, retry có giới hạn |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

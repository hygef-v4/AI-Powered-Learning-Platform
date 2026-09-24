# U02 Audit, Job & Outbox - Business Logic Model

## 1. Truy vết

| Luồng | Nguồn |
|---|---|
| A1 Ghi audit | US-AUD-001 S2, S3 |
| A2 Tra cứu audit | US-AUD-001 S1, UC-OPS-02 |
| J1 Tạo job | Mọi unit có tác vụ nền |
| J2 Worker xử lý job | component-methods `claim/complete/fail` |
| J3 Quét job kẹt | Câu 8 |
| J4 Xem trạng thái job | `getJobStatus` |
| E1 Phát sự kiện nghiệp vụ | services.md |

## 2. Audit

### A1 - Ghi audit
1. Unit gọi thực hiện thao tác, commit transaction.
2. Sau commit, gọi `AuditPort.record(event)` với `eventId` mới, `occurredAt` là thời điểm thao tác.
3. U02 kiểm danh sách khóa cấm trong `beforeData`/`afterData` (BR-U02-05); vi phạm thì log ERROR và bỏ.
4. Gửi `DomainEventMessage` loại `AUDIT` sang RabbitMQ. Lỗi → log ERROR, trả về bình thường (BR-U02-04).
5. Consumer U02 lưu vào `audit_events`; `eventId` đã tồn tại thì bỏ qua (BR-U02-03).

### A2 - Tra cứu audit
1. Gọi U01 `authorize(actor, AUDIT_READ)`. Không phải `ADMIN` hoặc U01 lỗi → từ chối (BR-U02-07, 50).
2. Kiểm bộ lọc hợp lệ, trang ≤ 100.
3. Truy vấn, sắp xếp `occurredAt` giảm dần.
4. Ghi audit `AUDIT_QUERIED` với bộ lọc đã dùng.

## 3. Job

### J1 - Tạo job
1. Trong transaction của unit gọi: tìm job cùng `jobType` + `idempotencyKey`; có thì trả job đó (BR-U02-21).
2. Kiểm payload (BR-U02-22). Ghi `jobs` ở `PENDING`, `nextAttemptAt` = hiện tại.
3. Trả `JobReference` cho unit gọi.
4. Sau commit: gửi `JobMessage`, cập nhật `lastPublishedAt`. Lỗi gửi → log WARN; J3 sẽ gửi lại.

### J2 - Worker xử lý job
1. Nhận `JobMessage`, gọi `claim(jobId, workerId)`. Không claim được → ack và bỏ (BR-U02-24).
2. Gọi handler của unit sở hữu với `payloadRef`.
3. Thành công → `complete(jobId, resultRef)`.
4. Lỗi → `fail(jobId, failureClass, safeMessage)`:
   - Tạm thời, còn lượt → `PENDING`, `nextAttemptAt` theo backoff; tác vụ quét sẽ gửi lại khi tới hạn.
   - Vĩnh viễn hoặc hết lượt → `FAILED`, log ERROR (BR-U02-27).
5. Ack message sau khi đã cập nhật bảng.

### J3 - Quét job kẹt (mỗi phút)
1. Job `PENDING` có `nextAttemptAt` ≤ hiện tại và (`lastPublishedAt` rỗng hoặc cách ≥ 5 phút) → gửi lại message, cập nhật `lastPublishedAt`.
2. Job `PENDING` đang chờ backoff mà `nextAttemptAt` vừa tới → gửi message.
3. Job `RUNNING` có `leaseExpiresAt` đã qua → về `PENDING`, `nextAttemptAt` = hiện tại.

### J4 - Xem trạng thái job
1. Tìm job. Không có, hoặc actor không phải người tạo và không phải `ADMIN` → "không tìm thấy" (BR-U02-30).
2. Trả `status`, `attempts`, `safeMessage`, `createdAt`, `updatedAt` (BR-U02-31).

## 4. Sự kiện nghiệp vụ

### E1 - Phát sự kiện
1. Sau commit, unit gọi `EventPublisherPort.publish(event)`.
2. Gửi RabbitMQ exchange `platform.events` với routing key `eventType`. Lỗi → log WARN, không retry (BR-U02-40).

## 5. Ảnh hưởng tới U01

- `OutboxPort` của U01 đổi thành `JobPort.enqueue` với `jobType = U01_OTP_DELIVERY`, `idempotencyKey` = `accountId:purpose:phút hiện tại`.
- Gửi mail OTP chạy qua J2; hết lượt thì chỉ log.

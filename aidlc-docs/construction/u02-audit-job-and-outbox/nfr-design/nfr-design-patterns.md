# U02 Audit, Job & Event - NFR Design Patterns

## P1 - Ghi job cùng transaction, gửi sau commit
- `JobPort.enqueue` INSERT vào `jobs` bằng transaction hiện tại của unit gọi (Spring `@Transactional` propagation `MANDATORY`).
- Đăng ký `TransactionSynchronization.afterCommit` để gửi `JobMessage`. Rollback thì không gửi.
- Gửi lỗi → log WARN, không ném lỗi ra ngoài (NFR-U02-01).

## P2 - Nhận job bằng UPDATE có điều kiện
```sql
UPDATE jobs
   SET status = 'RUNNING', lease_owner = :worker, lease_expires_at = now() + interval '5 minutes',
       attempts = attempts + 1, updated_at = now()
 WHERE job_id = :id AND status = 'PENDING' AND next_attempt_at <= now()
```
- Một dòng bị cập nhật → worker giữ job. Không dòng nào → message trùng hoặc chưa tới hạn, ack và bỏ (BR-U02-23, 24).
- Không dùng khóa phân tán vì chỉ có một worker và PostgreSQL đã đảm bảo.

## P3 - Retry do PostgreSQL điều khiển
- `fail` lỗi tạm: `status = PENDING`, `next_attempt_at = now() + backoff(attempts)` với backoff 30 s, 1, 2, 4, 8 phút. **Không** gửi message ngay.
- Lượt quét mỗi phút gửi message cho job `PENDING` có `next_attempt_at <= now()` và (`last_published_at` rỗng hoặc `last_published_at < next_attempt_at` hoặc `last_published_at < now() - 5 phút`).
- Backoff thực tế lệch tối đa 1 phút; chấp nhận được với quy mô đồ án.
- RabbitMQ không có queue trễ hay DLQ; job `FAILED` chỉ nằm trong bảng và log ERROR (BR-U02-27).

## P4 - Hết lease
- Lượt quét đưa job `RUNNING` có `lease_expires_at < now()` về `PENDING`, `next_attempt_at = now()`.
- Handler chạy lâu gọi `extendLease(jobId)` trước khi hết hạn (NFR-U02-12).

## P5 - Audit ghi trong transaction
- `AuditPort.record` INSERT vào `audit_events` bằng transaction hiện tại của unit gọi (propagation `REQUIRED`): nghiệp vụ commit thì có audit, rollback thì không (BR-U02-02).
- `recordDenied`/`recordFailure` dùng propagation `REQUIRES_NEW` để sự kiện bị từ chối hoặc lỗi còn lại dù nghiệp vụ rollback.
- `INSERT ... ON CONFLICT (event_id) DO NOTHING` (BR-U02-03). Kiểm khóa cấm trong JSON trước khi ghi (BR-U02-05).

## P6 - Chặn sửa audit ở tầng database
- Ứng dụng kết nối bằng user `app`; `REVOKE UPDATE, DELETE, TRUNCATE ON audit_events FROM app` trong migration (NFR-U02-30).

## P7 - Cách ly theo loại job
- 8 queue theo tính chất (BR-U02-33); mỗi queue một container listener với số luồng = prefetch: `jobs.scheduled` 2, `jobs.triggered` 2, `jobs.email` 1 (priority queue `x-max-priority = 10`), `jobs.gemini` 4 (U05 và U13 tự giới hạn thêm bằng semaphore `U05_INGEST_CONCURRENCY`, `U13_AI_CONCURRENCY`), `jobs.youtube` 1, `jobs.code` 2, `jobs.drive` 1, `jobs.payos` 1 (NFR-U02-11, 22).
- Job AI hoặc chạy code chậm không làm OTP, tự nộp hay mở/đóng bài phải xếp hàng; cả lớp nộp sát hạn (nhiều `GRADE_INIT`) không làm trễ tự nộp/đóng bài; OTP luôn gửi trước email thông báo.

## P8 - Timeout và kết nối lại
- RabbitMQ: connection timeout 5 s, tự kết nối lại của Spring AMQP; publisher confirm bật, chờ tối đa 2 s.
- Mỗi handler có timeout theo loại job, mặc định 60 s; quá hạn thì coi như lỗi tạm (NFR-U02-14).

## P9 - Health
- Worker `/health` kiểm PostgreSQL và RabbitMQ (NFR-U02-42).

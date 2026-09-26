# U02 Audit, Job & Event - Logical Components

## 1. Sơ đồ

```
 Backend (unit bất kỳ)                         Worker (profile worker)
 +------------------------------+              +--------------------------------+
 | service nghiệp vụ            |              | JobListener (mỗi queue một cái)|
 |   -> JobPort.enqueue --------+--INSERT----> |   -> JobClaimService (P2)      |
 |   -> AuditPort.record -------+--INSERT----> |   -> handler của unit sở hữu   |
 |   -> EventPublisherPort      |  jobs, audit |   -> JobCompletionService      |
 | afterCommit -> AmqpPublisher-+--message-+   | StuckJobSweeper (@Scheduled)   |
 +------------------------------+          |   +--------------------------------+
 | AuditQueryService (API admin)|          |                ^
 | JobStatusService (API)       |          v                |
 +------------------------------+     RabbitMQ -------------+
                |                     8 queue jobs.*, platform.events
                v
           PostgreSQL: jobs, audit_events
```

**Text alternative**: Service nghiệp vụ trong backend gọi `JobPort.enqueue` (INSERT vào `jobs`), `AuditPort.record` (INSERT vào `audit_events` trong cùng transaction) và `EventPublisherPort`. Sau commit, `AmqpPublisher` gửi message job và event sang RabbitMQ. Worker có một `JobListener` cho mỗi queue job: nhận job bằng `JobClaimService`, gọi handler của unit sở hữu, rồi ghi kết quả bằng `JobCompletionService`. `StuckJobSweeper` chạy mỗi phút để gửi lại job đến hạn và trả job hết lease. API admin tra audit qua `AuditQueryService`; API trạng thái job qua `JobStatusService`.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `JobPort` / `JobEnqueueService` | Backend | P1 |
| `AuditPort` / `AuditStore` | Backend, worker | P5: INSERT trong transaction của unit gọi, kiểm khóa cấm |
| `EventPublisherPort` | Backend | Gửi `platform.events` sau commit |
| `AmqpPublisher` | Backend, worker | Gửi message, publisher confirm, log lỗi |
| `JobStatusService` | Backend | `getJobStatus`, kiểm người tạo/ADMIN |
| `AuditQueryService` | Backend | Tra cứu audit cho ADMIN, tự ghi `AUDIT_QUERIED` |
| `JobListener` | Worker | Mỗi queue một listener, số luồng cấu hình |
| `JobClaimService` | Worker | P2 |
| `JobCompletionService` | Worker | `complete`, `fail`, `extendLease` |
| `JobHandlerRegistry` | Worker | Ánh xạ `jobType` → handler của unit sở hữu |
| `StuckJobSweeper` | Worker | P3, P4 |

## 3. RabbitMQ

| Thành phần | Kiểu | Ghi chú |
|---|---|---|
| `jobs` exchange | direct | Routing key = `jobType` |
| `jobs.scheduled` | queue durable | Việc nội bộ hẹn giờ: `PUBLICATION_OPEN`, `PUBLICATION_CLOSE`, `ATTEMPT_AUTO_SUBMIT`, `GROUP_AUTO_SUBMIT`, `DEADLINE_REMINDER`, `EMAIL_DISPATCH`, `CREDIT_RESERVATION_SWEEP` |
| `jobs.triggered` | queue durable | Việc nội bộ phát sinh sau thao tác: `GRADE_INIT`, `GROUP_DOC_CREATE`; tách riêng để lượt nộp dồn cục không làm trễ việc hẹn giờ |
| `jobs.email` | queue durable, priority | SMTP: `OTP_DELIVERY` (ưu tiên 9), `EMAIL_SEND` (ưu tiên 1) |
| `jobs.gemini` | queue durable | Gemini: `RAG_INGEST`, `AI_TASK` |
| `jobs.youtube` | queue durable | YouTube Data API: `YOUTUBE_RESOLVE` |
| `jobs.code` | queue durable | Judge0: `CODE_RUN` |
| `jobs.drive` | queue durable | Google Drive: `DRIVE_CLEANUP` |
| `jobs.payos` | queue durable | PayOS: `PAYOS_RECONCILE` |
| `platform.events` | topic exchange | Chỉ cho thông báo; U16 bind queue `jobs.notification` |

Không có queue trễ, không có DLQ.

## 4. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U02_JOB_LEASE` | 5m |
| `U02_JOB_MAX_ATTEMPTS` | 5 |
| `U02_JOB_BACKOFF` | 30s,1m,2m,4m,8m |
| `U02_SWEEP_INTERVAL` / `U02_SWEEP_REPUBLISH_AFTER` | 1m / 5m |
| `U02_JOB_HANDLER_TIMEOUT` | 60s |
| `U02_WORKER_MAX_CONCURRENCY` | 4 |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Mật khẩu là bí mật |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | P5 kiểm khóa cấm |
| SECURITY-05 | Compliant | Validate bộ lọc, payload |
| SECURITY-08 | Compliant | `JobStatusService`, `AuditQueryService` kiểm quyền |
| SECURITY-09 | Compliant | P6, tài khoản RabbitMQ riêng |
| SECURITY-15 | Compliant | Lỗi RabbitMQ không làm hỏng nghiệp vụ |
| RESILIENCY-06 | Compliant | P9 |
| RESILIENCY-10 | Compliant | P8 |
| Rule còn lại | N/A | Không áp dụng cho U02 hoặc ngoài phạm vi đồ án |

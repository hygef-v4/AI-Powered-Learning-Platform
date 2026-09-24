# U02 Audit, Job & Outbox - Logical Components

## 1. Sơ đồ

```
 Backend (unit bất kỳ)                         Worker (profile worker)
 +------------------------------+              +--------------------------------+
 | service nghiệp vụ            |              | JobListener (mỗi queue một cái)|
 |   -> JobPort.enqueue --------+--INSERT----> |   -> JobClaimService (P2)      |
 |   -> AuditPort.record        |  jobs        |   -> handler của unit sở hữu   |
 |   -> EventPublisherPort      |              |   -> JobCompletionService      |
 | afterCommit -> AmqpPublisher-+--message-+   | AuditListener -> AuditStore    |
 +------------------------------+          |   | StuckJobSweeper (@Scheduled)   |
 | AuditQueryService (API admin)|          |   +--------------------------------+
 | JobStatusService (API)       |          v                ^
 +------------------------------+     RabbitMQ -------------+
                |                     jobs.<unit>.<type>, audit.events, platform.events
                v
           PostgreSQL: jobs, audit_events
```

**Text alternative**: Service nghiệp vụ trong backend gọi `JobPort.enqueue` (INSERT vào `jobs`), `AuditPort.record` và `EventPublisherPort`. Sau commit, `AmqpPublisher` gửi message sang RabbitMQ. Worker có một `JobListener` cho mỗi queue job: nhận job bằng `JobClaimService`, gọi handler của unit sở hữu, rồi ghi kết quả bằng `JobCompletionService`. `AuditListener` lưu audit vào `audit_events`. `StuckJobSweeper` chạy mỗi phút để gửi lại job đến hạn và trả job hết lease. API admin tra audit qua `AuditQueryService`; API trạng thái job qua `JobStatusService`.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `JobPort` / `JobEnqueueService` | Backend | P1 |
| `AuditPort` / `AuditPublisher` | Backend | P5, kiểm khóa cấm |
| `EventPublisherPort` | Backend | Gửi `platform.events` sau commit |
| `AmqpPublisher` | Backend, worker | Gửi message, publisher confirm, log lỗi |
| `JobStatusService` | Backend | `getJobStatus`, kiểm người tạo/ADMIN |
| `AuditQueryService` | Backend | Tra cứu audit cho ADMIN, tự ghi `AUDIT_QUERIED` |
| `JobListener` | Worker | Mỗi queue một listener, số luồng cấu hình |
| `JobClaimService` | Worker | P2 |
| `JobCompletionService` | Worker | `complete`, `fail`, `extendLease` |
| `JobHandlerRegistry` | Worker | Ánh xạ `jobType` → handler của unit sở hữu |
| `AuditListener` + `AuditStore` | Worker | P5 |
| `StuckJobSweeper` | Worker | P3, P4 |

## 3. RabbitMQ

| Thành phần | Kiểu | Ghi chú |
|---|---|---|
| `jobs` exchange | direct | Routing key = `jobType` |
| `jobs.<unit>.<type>` | queue durable | Ví dụ `jobs.u01.otp-delivery` |
| `audit` exchange / `audit.events` | direct / queue durable | |
| `platform.events` | topic exchange | U16 và unit khác tự bind queue riêng |

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

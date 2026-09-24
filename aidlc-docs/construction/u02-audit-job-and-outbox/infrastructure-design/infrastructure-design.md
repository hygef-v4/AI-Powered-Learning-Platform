# U02 Audit, Job & Outbox - Infrastructure Design

Hạ tầng chung ở `construction/shared-infrastructure.md`.

## 1. Ánh xạ thành phần

| Thành phần | Container |
|---|---|
| `JobEnqueueService`, `AuditPublisher`, `EventPublisherPort`, `JobStatusService`, `AuditQueryService` | `backend` |
| `JobListener`, `JobClaimService`, `JobCompletionService`, `AuditListener`, `StuckJobSweeper` | `worker` |
| Bảng `jobs`, `audit_events` | `postgres` |
| Exchange, queue | `rabbitmq` |

## 2. PostgreSQL

| Mục | Giá trị |
|---|---|
| User migration | `migrator`, chủ sở hữu schema, chỉ Flyway dùng lúc khởi động backend |
| User ứng dụng | `app`, quyền `SELECT, INSERT, UPDATE, DELETE` trên bảng nghiệp vụ; riêng `audit_events` chỉ `SELECT, INSERT` |
| Migration U02 | `V20260925_0900__u02_jobs_audit.sql`: bảng `jobs` (unique `(job_type, idempotency_key)`, index `(status, next_attempt_at)`, `lease_expires_at`), bảng `audit_events` + 4 index, `REVOKE UPDATE, DELETE, TRUNCATE ON audit_events FROM app` |

## 3. RabbitMQ

| Mục | Giá trị |
|---|---|
| vhost | `/platform` |
| User | `app` với quyền trên `/platform`; tắt `guest` |
| Exchange | `jobs` (direct), `audit` (direct), `platform.events` (topic), tất cả durable |
| Queue | `audit.events` và `jobs.<unit>.<type>`, durable; mỗi unit tự khai báo queue của mình khi thêm loại job |
| Khai báo | Spring AMQP khai báo lúc khởi động bằng `Declarables`; không cấu hình tay |
| Management UI | Cổng 15672 chỉ trên mạng `internal`, vào qua SSH tunnel |

## 4. Worker

| Mục | Giá trị |
|---|---|
| Image | Cùng image backend, biến `SPRING_PROFILES_ACTIVE=worker` |
| Giới hạn | 0.5 CPU, 768 MB (như shared-infrastructure) |
| Mạng | Chỉ `internal` |
| Healthcheck | `/health` kiểm PostgreSQL, RabbitMQ |
| Số instance | 1 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | Compliant | Tắt `guest`, user riêng, `app` không sửa được audit |
| RESILIENCY-04 | Compliant | Worker deploy cùng Compose, rollback theo tag |
| RESILIENCY-06 | Compliant | Healthcheck worker |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |

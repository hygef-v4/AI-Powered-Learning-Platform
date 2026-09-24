# U02 Audit, Job & Outbox - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U02. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-AUD-001. **Use case**: UC-OPS-02.
- **Thiết kế nguồn**: `construction/u02-audit-job-and-outbox/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án (Maven, cấu hình, `shared/`, frontend, Docker Compose, CI) là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm các bước đó; unit sau đánh dấu `[x]` vì đã có. Bước 0 dưới đây kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` (đọc audit, xem job) | U01 (`C`) | Nếu U01 chưa code: adapter giả **luôn từ chối** (fail closed), test dùng mock. Thay bằng U01 thật khi có |

U02 không phụ thuộc U03, U04.

### Dữ liệu U02 sở hữu

PostgreSQL `jobs`, `audit_events`; RabbitMQ exchange `jobs`, `audit`, `platform.events`, queue `audit.events` và `jobs.<unit>.<type>`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u02/
    api/                AuditController, JobStatusController, DTO
    application/        JobEnqueueService, JobStatusService, AuditQueryService,
                        AuditPublisher, EventPublisher
    domain/             Job, JobStatus, AuditEvent, BackoffPolicy, ForbiddenKeyGuard
    infrastructure/     JPA repository, AmqpPublisher, AmqpTopology
    worker/             JobListener, JobClaimService, JobCompletionService,
                        JobHandlerRegistry, AuditListener, StuckJobSweeper
    port/               JobPort, AuditPort, EventPublisherPort, JobWorkerPort,
                        JobHandler, AuthorizationPort
    adapter/fake/       FakeAuthorizationPort (từ chối)
/backend/src/main/resources/db/migration/u02/
/frontend/src/app/admin/audit/
/frontend/src/components/jobs/
/contracts/openapi/u02-audit-job.yaml
/contracts/messages/u02-*.json   (schema message)
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - Thêm vào `pom.xml`: Spring AMQP, Testcontainers RabbitMQ. Thêm profile `worker` vào `application.yml` và biến cấu hình U02 theo `logical-components.md` §4.
- [ ] **Bước 2** - Thêm service `worker` (cùng image backend, profile `worker`, chỉ mạng `internal`, healthcheck) vào `docker-compose.yml`; RabbitMQ vhost `/platform`, user `app`, tắt `guest`; PostgreSQL hai user `migrator` và `app`.

### Nhóm B - Domain và logic

- [ ] **Bước 3** - Domain: `Job` với 4 trạng thái và chuyển trạng thái hợp lệ, `BackoffPolicy` (30 s, 1, 2, 4, 8 phút), `AuditEvent`, `ForbiddenKeyGuard` (khóa `password`, `otp`, `token`, `secret`, `phone`).
- [ ] **Bước 4** - Port: `JobPort`, `AuditPort`, `EventPublisherPort`, `JobWorkerPort`, `JobHandler`, `AuthorizationPort`; `FakeAuthorizationPort`.
- [ ] **Bước 5** - `JobEnqueueService`: INSERT trong transaction bắt buộc, idempotent theo `(jobType, idempotencyKey)`, kiểm payload, gửi sau commit (BR-U02-20…22, P1).
- [ ] **Bước 6** - `AuditPublisher` và `EventPublisher`: kiểm khóa cấm, gửi sau commit, lỗi chỉ log (BR-U02-02…05, 40, 41).
- [ ] **Bước 7** - Worker: `JobClaimService` (UPDATE có điều kiện, lease 5 phút), `JobCompletionService` (`complete`, `fail`, `extendLease`), `JobHandlerRegistry`, `JobListener` mỗi queue (BR-U02-23…27, P2-P4, P7).
- [ ] **Bước 8** - `AuditListener`: `INSERT ... ON CONFLICT (event_id) DO NOTHING` (BR-U02-03, P5).
- [ ] **Bước 9** - `StuckJobSweeper` mỗi phút: gửi lại job đến hạn hoặc chưa gửi quá 5 phút, trả job hết lease (BR-U02-28, P3, P4).
- [ ] **Bước 10** - `JobStatusService` (người tạo hoặc ADMIN, còn lại "không tìm thấy") và `AuditQueryService` (chỉ ADMIN, lọc, trang ≤ 100, tự ghi `AUDIT_QUERIED`) (BR-U02-07, 08, 30, 31).
- [ ] **Bước 11** - Unit test cho mọi `BR-U02-xx`.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u02-audit-job-and-outbox/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và messaging

- [ ] **Bước 13** - Flyway `V20260925_0900__u02_jobs_audit.sql`: bảng `jobs` (unique `(job_type, idempotency_key)`, index `(status, next_attempt_at)`, `lease_expires_at`), bảng `audit_events` + 4 index, `REVOKE UPDATE, DELETE, TRUNCATE ON audit_events FROM app`.
- [ ] **Bước 14** - JPA repository; `AmqpTopology` khai báo exchange/queue bằng `Declarables`; `AmqpPublisher` với publisher confirm 2 s.
- [ ] **Bước 15** - Integration test Testcontainers (PostgreSQL, RabbitMQ): tạo job, claim trùng, retry theo backoff, hết lease, quét gửi lại, audit trùng `eventId`, `app` không UPDATE/DELETE được `audit_events`.
- [ ] **Bước 16** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 17** - `/contracts/openapi/u02-audit-job.yaml` (`GET /api/v1/admin/audit-events`, `GET /api/v1/jobs/{jobId}`) và JSON schema message.
- [ ] **Bước 18** - Controller + DTO + validation bộ lọc.
- [ ] **Bước 19** - Test MockMvc, gồm từ chối người không phải ADMIN, xem job của người khác trả 404, không có endpoint sửa/xóa audit.
- [ ] **Bước 20** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 21** - `AuditLogPage` với `AuditFilters`, `AuditTable`, `AuditDetailDrawer` (chỉ đọc).
- [ ] **Bước 22** - `useJobStatus` (poll 3 giây, dừng ở trạng thái cuối) và `JobStatusBadge`.
- [ ] **Bước 23** - Test frontend cho bộ lọc, phân trang, hook dừng poll.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: chạy worker, xem RabbitMQ qua SSH tunnel, cách một unit thêm loại job mới (khai báo queue + đăng ký handler).
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-AUD-001 S1 (tra cứu) | 10, 13, 17-19, 21 |
| US-AUD-001 S2 (không sửa/xóa) | 8, 13, 15, 19 |
| US-AUD-001 S3 (sự kiện bắt buộc) | 6, 8 (unit khác gọi `AuditPort`) |
| UC-OPS-02 | 10, 18, 21 |
| Job platform (không có story) | 3, 5, 7, 9, 13-15, 22 |

## 5. Ngoài phạm vi

- Handler nghiệp vụ của từng loại job (thuộc unit sở hữu).
- Adapter `AuthorizationPort` thật (khi U01 được code).

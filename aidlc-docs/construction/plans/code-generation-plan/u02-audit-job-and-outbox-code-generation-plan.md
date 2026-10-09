# U02 Audit - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 73 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U02. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Cập nhật 2026-10-04: việc nền, worker, RabbitMQ và sự kiện thông báo chuyển sang U03; U02 chỉ còn audit. Đánh số bước lại.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-AUD-001.
- **Primary UC hiện hành**: UC 73 View Audit Log (bản 73 UC), màn Audit Log mở từ Admin Dashboard.
- **Thiết kế nguồn**: `construction/u02-audit-job-and-outbox/` (functional-design, nfr-requirements, nfr-design, infrastructure-design), `construction/shared-infrastructure.md` và [mô hình dữ liệu của unit](../../u02-audit-job-and-outbox/functional-design/domain-entities.md).
- **Stack**: Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Thứ tự**: wave 1, song song với U03 (U03 dựng khung); U01 code sau.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (gồm hai user PostgreSQL `migrator`, `app` và `ForbiddenKeyGuard` trong `shared/`). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` (đọc audit) | U01 (`C`) | U01 code sau: dùng `AuthorizationService` khung đã có của U01, **luôn từ chối** (fail closed), không tạo adapter giả; test dùng mock; U01 Bước 15 cài thật |
| `AccountLookupPort.getContact` (email người thực hiện) | U01 (`C`) | U01 code sau: khung trả rỗng nên `actorEmail` để trống; U01 Bước 18 cài thật |

### Dữ liệu U02 sở hữu

PostgreSQL `audit_logs`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  audit/
    api/                AuditController, DTO
    application/        AuditStore (AuditPort), AuditQueryService
    domain/             AuditLog
    infrastructure/     JPA repository
    port/               AuditPort, AuditQueryPort, AuditEvent, AuditFilter, AuditResult (đã có)
/backend/src/main/resources/db/migration/audit/
/frontend/src/app/admin/audit/
/contracts/openapi/audit.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6). Chưa có thì phối hợp với người code U03.

### Nhóm B - Domain và logic

- [ ] **Bước 1** - Domain `AuditLog` (`id`, `actor_id`, `action`, `object_type`, `object_id`, `result` `SUCCESS`/`DENIED`/`FAILURE`, `reason`, `details`, `occurred_at`).
- [ ] **Bước 2** - Port `AuditPort`, `AuditQueryPort` và kiểu `AuditEvent`, `AuditFilter`, `AuditResult` đã có trong code: giữ nguyên, sửa chú thích "UC 39" thành UC 73. Không tạo `FakeAuthorizationPort`; `AuthorizationPort` là của U01.
- [ ] **Bước 3** - `AuditStore`: INSERT `audit_logs` trong transaction của unit gọi; `recordDenied`/`recordFailure` dùng `REQUIRES_NEW`; kiểm khóa cấm bằng `ForbiddenKeyGuard`; `ON CONFLICT (id) DO NOTHING` (BR-U02-02…05, P1).
- [ ] **Bước 4** - Test audit: thao tác rollback thì không có audit thường nhưng vẫn có audit `DENIED`/`FAILURE`; ghi trùng `id` không tạo dòng thứ hai (BR-U02-02, 03).
- [ ] **Bước 5** - `AuditQueryService` (chỉ ADMIN, lọc, trang ≤ 100, mới nhất trước, lấy email người thực hiện qua `AccountLookupPort`, tự ghi `AUDIT_QUERIED`) (BR-U02-07, 08, 09).
- [ ] **Bước 6** - Unit test cho mọi `BR-U02-xx`.
- [ ] **Bước 7** - Tóm tắt: `aidlc-docs/construction/u02-audit-job-and-outbox/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 8** - Flyway `db/migration/audit/V20260925_0900__create_audit_logs.sql`: bảng `audit_logs` + 4 index (`occurred_at`, `(actor_id, occurred_at)`, `(object_type, object_id)`, `action`), `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app`. U02 code trước U01 nên chưa tạo FK `actor_id`; migration `accounts` của U01 (`V20260925_0930`) thêm FK `actor_id` → `accounts`.
- [ ] **Bước 9** - JPA repository.
- [ ] **Bước 10** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers (PostgreSQL): audit trùng `id`, rollback vẫn còn `DENIED`/`FAILURE`, `app` không UPDATE/DELETE được `audit_logs` (NFR-U02-50).
- [ ] **Bước 11** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 12** - `/contracts/openapi/audit.yaml` (`GET /api/v1/admin/audit-logs`).
- [ ] **Bước 13** - Controller + DTO + validation bộ lọc.
- [ ] **Bước 14** - Test MockMvc, gồm từ chối người không phải ADMIN, không có endpoint sửa/xóa audit.
- [ ] **Bước 15** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 16** - `AuditLogPage` (màn Audit Log, mở từ Admin Dashboard) với `AuditFilters` (nhập email, đổi sang `actorId` qua API danh sách tài khoản của U01), `AuditTable`, `AuditDetailDrawer` (chỉ đọc).
- [ ] **Bước 17** - Test frontend cho bộ lọc, phân trang.
- [ ] **Bước 18** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 19** - Cập nhật `README.md`: cách unit khác ghi audit qua `AuditPort` (trong transaction, không đưa dữ liệu nhạy cảm vào `details`).
- [ ] **Bước 20** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-AUD-001 S1 (tra cứu) | 5, 8, 12-14, 16 |
| US-AUD-001 S2 (không sửa/xóa) | 4, 8, 10, 14 |
| US-AUD-001 S3 (sự kiện bắt buộc) | 3, 4 (unit khác gọi `AuditPort` trong transaction) |
| UC 73 | 5, 13, 16 |

## 5. Ngoài phạm vi

- Việc nền, worker, RabbitMQ, sự kiện thông báo (U03).
- Cài thật `AuthorizationPort` (U01 Bước 15) và `AccountLookupPort` (U01 Bước 18).

## 6. Revision theo bản 73 UC - 2026-10-09

- [ ] Sửa `contracts/openapi/audit.yaml`: mô tả "UC 39" thành UC 73; giữ `actorId` làm bộ lọc và `actorEmail` trong kết quả.
- [ ] Sửa chú thích "UC 39" thành UC 73 trong `AuditQueryPort`.
- [ ] Màn Audit Log mở từ Admin Dashboard (không còn Admin Sidebar).

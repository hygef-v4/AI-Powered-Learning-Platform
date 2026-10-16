# U02 Audit — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Thiết kế: [business-logic-model](../../u02-audit-job-and-outbox/functional-design/business-logic-model.md), [business-rules](../../u02-audit-job-and-outbox/functional-design/business-rules.md), [domain-entities](../../u02-audit-job-and-outbox/functional-design/domain-entities.md), [frontend-components](../../u02-audit-job-and-outbox/functional-design/frontend-components.md), [NFR](../../u02-audit-job-and-outbox/nfr-requirements/nfr-requirements.md), [NFR design](../../u02-audit-job-and-outbox/nfr-design/nfr-design-patterns.md), [infrastructure](../../u02-audit-job-and-outbox/infrastructure-design/infrastructure-design.md). Phụ thuộc và thứ tự: [unit-of-work](../../../inception/application-design/unit-of-work.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/audit/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../audit/contract/` | interface `IAudit`, `IAuditQuery`; kiểu dữ liệu `AuditEvent`, `AuditFilter`, `AuditLogView`, `AuditResult` |
| `backend/.../audit/contract/stub/AuditContractStubs` | Bản tạm trả dữ liệu mẫu cho `IAudit`, `IAuditQuery` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/audit/` | Chỉ có `index.ts` trống |
| `backend/.../shared/security/ForbiddenKeyGuard` | Có, kèm unit test; dùng để chặn khóa cấm trong `details` |

## Checklist

- [ ] **Bước 1 — Migration:** tạo `db/migration/audit/V20260925_0900__create_audit_logs.sql`: bảng `audit_logs` theo domain-entities mục 2 (chưa có FK; U01 thêm FK `actor_id` → `accounts`), index `occurred_at`, `(actor_id, occurred_at)`, `(object_type, object_id)`, `action`; `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app`.
- [ ] **Bước 2 — Ghi audit (A1):** `AuditStore` kiểm `details` bằng `ForbiddenKeyGuard` (BR-U02-05), `INSERT ... ON CONFLICT (id) DO NOTHING` (BR-U02-03); `record` chạy trong transaction hiện tại khi `result = SUCCESS`, chạy `REQUIRES_NEW` qua `TransactionTemplate` khi `DENIED` hoặc `FAILURE` (BR-U02-02).
- [ ] **Bước 3 — Tra cứu (A2):** `AuditQueryService` gọi `IAuthorization.authorize(actor, AUDIT_READ)` và từ chối khi lỗi (BR-U02-07, 50); kiểm bộ lọc (`from` ≤ `to`, `result` hợp lệ, trang ≤ 100); sắp `occurred_at` giảm dần; lấy email qua `IAccountLookup.getContact` (BR-U02-09); ghi audit `AUDIT_QUERIED`.
- [ ] **Bước 4 — API:** `AuditController` cho `GET /api/v1/admin/audit-logs`, lỗi trả dạng an toàn (BR-U02-51).
- [ ] **Bước 5 — Frontend:** `AuditLogPage` với `AuditFilters` (mặc định 7 ngày, đổi email sang `actorId` qua API tài khoản của U01), `AuditTable` (50 dòng/trang), `AuditDetailDrawer` chỉ đọc.
- [ ] **Bước 6 — Unit test (người code viết):** khóa cấm bị từ chối, ghi lại cùng `id` không tạo dòng mới, kiểm bộ lọc sai, người không phải Admin bị từ chối, `actor_id` rỗng hiện "Hệ thống"; test component frontend. Integration test do tester viết.
- [ ] **Bước 7 — Review:** đối chiếu UC 13, US-AUD-001, BR-U02 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Ghi lại cùng `id` không tạo bản ghi thứ hai; nghiệp vụ rollback thì audit `SUCCESS` mất nhưng `DENIED`/`FAILURE` vẫn còn.
- User `app` không `UPDATE`/`DELETE` được `audit_logs`.
- `details` có khóa `password`, `otp`, `token`, `secret`, `phone` bị từ chối; người không phải Admin gọi API tra cứu bị từ chối.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.

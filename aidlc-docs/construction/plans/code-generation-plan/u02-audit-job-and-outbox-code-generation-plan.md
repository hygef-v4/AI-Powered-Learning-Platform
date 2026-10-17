# U02 Audit — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Thiết kế: [business-logic-model](../../u02-audit-job-and-outbox/functional-design/business-logic-model.md), [business-rules](../../u02-audit-job-and-outbox/functional-design/business-rules.md), [domain-entities](../../u02-audit-job-and-outbox/functional-design/domain-entities.md), [frontend-components](../../u02-audit-job-and-outbox/functional-design/frontend-components.md), [NFR](../../u02-audit-job-and-outbox/nfr-requirements/nfr-requirements.md), [NFR design](../../u02-audit-job-and-outbox/nfr-design/nfr-design-patterns.md), [infrastructure](../../u02-audit-job-and-outbox/infrastructure-design/infrastructure-design.md). Phụ thuộc và thứ tự: [unit-of-work](../../../inception/application-design/unit-of-work.md).

## Trạng thái

Plan theo unit 2026-10-15, chi tiết hóa ngày 2026-10-18 trước khi code (Part 1); người dùng duyệt ngày 2026-10-18. Code và unit test viết ngày 2026-10-18 theo plan này (bước 1–8), chưa được người dùng duyệt (bước 9). Tóm tắt: [code-summary](../../u02-audit-job-and-outbox/code/code-summary.md). Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng. Nhánh git: `audit`, mỗi bước một commit Conventional Commits scope `u02`, gộp vào `main` qua PR (CI: `mvn -B verify`; `npm run lint`, `typecheck`, `test`, `build`).

## Ngữ cảnh unit

| Mục | Nội dung |
|---|---|
| Story | US-AUD-001 (S1 tra cứu, S2 append-only, S3 ghi từ mọi unit) |
| Interface cung cấp | `IAudit.record(AuditEvent)` cho mọi unit và worker; `IAuditQuery.query(Actor, AuditFilter, Pageable)` cho API của chính U02 |
| Interface dùng | `IAuthorization.authorize` (U01, cạnh `C`; bản tạm luôn cho phép), `IAccountLookup.getContact` (U01, cạnh `C`; bản tạm trả email mẫu) |
| Dữ liệu sở hữu | Bảng `audit_logs` (U02 duy nhất ghi) |
| Ràng buộc kiến trúc | `ArchitectureTest`: unit khác chỉ dùng `audit.contract`; U02 (số 2) chỉ phụ thuộc `identity.contract` (U01) và `shared` |
| Frontend | Màn #14 Audit Log theo design G21 (artboard `a13`, `a13b`, `a13c`; xem `docs/ui-reference/`), khung `AppShell` và `@/shared/ui` có sẵn; `NavEntry` đã đăng ký trong `features/audit/register.ts` |
| Extension | Security Baseline (SECURITY-03, 04, 05, 08, 09, 12, 15), Resiliency Baseline (RESILIENCY-04, 06, 10) theo phạm vi rút gọn trong `aidlc-state.md` |

## Khác biệt phát hiện khi chi tiết hóa

- API tài khoản của U01 (nhánh `cuong`, chưa vào `main`) lọc bằng tham số `q`, không phải `email=` như frontend-components ghi; frontend U02 gọi `GET /api/v1/admin/accounts?q=<email>` rồi chọn đúng tài khoản có email trùng. Khi U01 chưa gộp, API trả lỗi thì hiện "Không tìm thấy người thực hiện với email này." và không tra cứu.
- Môi trường không có role `app` (ví dụ Testcontainers của tester) thì `REVOKE ... FROM app` lỗi; migration chỉ chạy `REVOKE` khi role `app` tồn tại.
- `shared/ui` chưa có phân trang; thêm `Pagination` vào `shared/ui` (việc của người phụ trách frontend) để unit khác dùng lại.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/audit/` | `domain` (`AuditLogRepository`, `AuditRow`), `application` (`AuditStore`, `AuditQueryService`), `web` (`AuditController`); `infrastructure` để trống |
| `backend/.../audit/contract/` | interface `IAudit`, `IAuditQuery`; kiểu dữ liệu `AuditEvent`, `AuditFilter`, `AuditLogView`, `AuditResult` |
| `backend/.../audit/contract/stub/AuditContractStubs` | Bản tạm, tự tắt vì `AuditStore`, `AuditQueryService` đã cài thật |
| `backend/src/main/resources/db/migration/audit/` | `V20260925_0900__create_audit_logs.sql` |
| `frontend/src/features/audit/`, `frontend/src/app/admin/audit/` | Màn #14 Audit Log: `AuditLogPage`, `AuditFilters`, `AuditTable`, `AuditDetailDialog`; `shared/ui/Pagination` |
| `backend/.../shared/security/ForbiddenKeyGuard` | Có, kèm unit test; dùng để chặn khóa cấm trong `details` |

## Checklist

Unit test viết cùng bước tạo code (người code viết); mỗi bước xong chạy `mvn -B verify` hoặc bộ lệnh npm như CI rồi mới commit.

- [x] **Bước 1 — Migration (NFR-U02-03, 30; P2):** tạo `backend/src/main/resources/db/migration/audit/V20260925_0900__create_audit_logs.sql`: bảng `audit_logs` (`id uuid` khóa chính, `actor_id uuid` rỗng được, chưa có FK, U01 thêm FK → `accounts`; `action varchar(100)`, `object_type varchar(50)`, `object_id varchar(200)`, `result varchar(10)` có `check` 3 giá trị, `reason varchar(2000)`, `details jsonb not null default '{}'`, `occurred_at timestamptz not null`, `recorded_at timestamptz not null default now()`); index `occurred_at desc`, `(actor_id, occurred_at desc)`, `(object_type, object_id)`, `action`; khối `DO` chỉ `REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM app` khi role `app` tồn tại. Commit `feat(u02): add audit_logs table with append-only grants`.
- [x] **Bước 2 — Repository (A1, A2):** `backend/src/main/java/edu/aiplatform/audit/domain/AuditLogRepository.java` dùng `JdbcClient` như U03: `insertIfAbsent` (`INSERT ... ON CONFLICT (id) DO NOTHING`, BR-U02-03), `count(filter)`, `find(filter, offset, limit)` sắp `occurred_at desc, id desc`, chỉ có hàm thêm và đọc (BR-U02-01); `AuditRow` (record) nếu cần. Unit test dựng SQL lọc với từng tổ hợp bộ lọc.
- [x] **Bước 3 — Ghi audit `IAudit` (A1; BR-U02-02, 03, 04, 05; NFR-U02-01, 40; P1):** `audit/application/AuditStore.java` (`@Service`, thay bản tạm nhờ `@ConditionalOnMissingBean`): kiểm trường bắt buộc (`id`, `action`, `result`, `occurredAt`); `ForbiddenKeyGuard.check(details)` sai thì `IllegalArgumentException` (lỗi lập trình, nghiệp vụ rollback); `details` chuyển JSON bằng `ObjectMapper`; `SUCCESS` gọi repository ngay trong transaction hiện tại của unit gọi, không mở transaction mới; `DENIED`, `FAILURE` chạy qua `TransactionTemplate` `PROPAGATION_REQUIRES_NEW`; lỗi DB ném tiếp, không nuốt (BR-U02-04). Unit test: khóa cấm bị từ chối kể cả lồng nhau, `SUCCESS` không mở transaction mới, `DENIED`/`FAILURE` dùng `REQUIRES_NEW`, `id` lặp chỉ gọi insert-if-absent, lỗi DB ném ra. Commit `feat(u02): record audit events in the caller transaction`.
- [x] **Bước 4 — Tra cứu `IAuditQuery` (A2; BR-U02-07, 08, 09, 50):** `audit/application/AuditQueryService.java`: `actor` rỗng, không phải `ADMIN`, hoặc `IAuthorization.authorize(actor, "AUDIT_READ", ResourceRef.none())` từ chối hay ném lỗi → ghi `ACCESS_DENIED` (`DENIED`) rồi `ApiException.forbidden("AUDIT_FORBIDDEN")`; kiểm bộ lọc (`from` ≤ `to`, chuỗi rỗng coi như không lọc, độ dài tối đa như cột) sai → `400 AUDIT_FILTER_INVALID`; trang mặc định 50, tối đa 100, số trang âm → 400; đọc `audit_logs`, lấy email từng `actorId` khác nhau trong trang qua `IAccountLookup.getContact` (lỗi hay không có thì để trống, không làm hỏng trang); `actor_id` rỗng → `actorEmail` rỗng (frontend hiện "Hệ thống"); ghi `AUDIT_QUERIED` (`SUCCESS`, `details` = bộ lọc đã dùng, không chứa email người được lọc) trong transaction của truy vấn. Unit test: không phải Admin bị từ chối và có audit `DENIED`, U01 lỗi thì từ chối (fail closed), bộ lọc sai trả 400, trang > 100 bị chặn, email lấy một lần mỗi người, actor rỗng không gọi U01, `AUDIT_QUERIED` được ghi. Commit `feat(u02): add admin audit query with access check`.
- [x] **Bước 5 — API (BR-U02-51; SECURITY-08):** `audit/web/AuditController.java`: `GET /api/v1/admin/audit-logs` nhận `actorId`, `action`, `objectType`, `objectId`, `result`, `from`, `to` (ISO-8601), `page`, `size`; `Actor` lấy từ `@AuthenticationPrincipal` (không lấy từ request); trả `{ items: [{ id, occurredAt, actorId, actorEmail, action, objectType, objectId, result, reason, details }], page, size, totalElements, totalPages }`; tham số sai kiểu trả Problem Detail `400` qua `GlobalExceptionHandler`, không lộ chi tiết DB. Unit test tầng web (MockMvc standalone) cho tham số sai và ánh xạ kết quả. Commit `feat(u02): expose GET /api/v1/admin/audit-logs`.
- [x] **Bước 6 — Thành phần dùng chung frontend:** `frontend/src/shared/ui/Pagination.tsx` ("Hiển thị a–b trong N …", nút trang, trước/sau, theo design) xuất trong `shared/ui/index.ts`, kèm test. Commit `feat(ui): add shared Pagination component`.
- [x] **Bước 7 — Frontend màn #14 Audit Log (UC 13; artboard `a13`, `a13b`, `a13c`):** `frontend/src/app/admin/audit/page.tsx`; `frontend/src/features/audit/`: `types.ts`, `api.ts` (`getAuditLogs`, `findActorIdByEmail` qua `GET /api/v1/admin/accounts?q=`), `labels.ts` (nhãn tiếng Việt cho kết quả, hành động thường gặp, loại đối tượng; mã lạ hiện nguyên mã), `AuditLogPage.tsx` (`PageHeader` "Nhật ký audit", phụ đề theo design; đọc `objectType`, `objectId` từ URL khi mở từ Account Detail), `AuditFilters.tsx` (email người thực hiện, hành động, loại đối tượng, mã đối tượng, kết quả, khoảng thời gian mặc định 7 ngày; Xóa lọc, Lọc; email không khớp → lỗi dưới ô và không tra cứu), `AuditTable.tsx` (cột Thời gian, Người thực hiện hoặc "Hệ thống", Hành động, Đối tượng loại · mã rút gọn, Kết quả bằng `Pill`, Lý do; 50 dòng/trang; trạng thái đang tải, rỗng "Không có bản ghi nào khớp bộ lọc.", lỗi tải, 403), `AuditDetailDialog.tsx` (`Dialog` chỉ đọc: mã, thời điểm, người thực hiện, hành động, đối tượng đầy đủ, kết quả, lý do, "Chi tiết (JSON, chỉ đọc)", "Bản ghi không sửa hoặc xóa được", nút Đóng); mọi phần tử tương tác có `data-testid` dạng `audit-<thành phần>-<vai trò>`. Test component (Vitest + Testing Library): mặc định 7 ngày, email không khớp không gọi API audit, actor rỗng hiện "Hệ thống", mở và đóng chi tiết, trạng thái rỗng và lỗi. Commit `feat(u02): add audit log screen with filters and detail dialog`.
- [x] **Bước 8 — Tài liệu:** `aidlc-docs/construction/u02-audit-job-and-outbox/code/code-summary.md` theo mẫu U03 (luồng → class, API, migration, frontend, unit test, khác với thiết kế); sửa `frontend-components.md` ghi tham số `q`; đánh dấu các bước trong plan này và cập nhật `aidlc-state.md` (U02: code viết, chờ review). Commit `docs(u02): add code summary and record U02 progress`.
- [ ] **Bước 9 — Review:** đối chiếu UC 13, US-AUD-001, BR-U02 với code; trình bày kết quả và tóm tắt tuân thủ extension; người dùng duyệt trước khi đẩy nhánh và mở PR.

## Integration test cases (tester viết)

- Ghi lại cùng `id` không tạo bản ghi thứ hai; nghiệp vụ rollback thì audit `SUCCESS` mất nhưng `DENIED`/`FAILURE` vẫn còn.
- User `app` không `UPDATE`/`DELETE` được `audit_logs`.
- `details` có khóa `password`, `otp`, `token`, `secret`, `phone` bị từ chối; người không phải Admin gọi API tra cứu bị từ chối.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.

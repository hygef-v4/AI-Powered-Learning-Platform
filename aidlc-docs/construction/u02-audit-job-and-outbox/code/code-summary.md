# U02 Audit — code summary

Code viết ngày 2026-10-18 theo [plan](../../plans/code-generation-plan/u02-audit-job-and-outbox-code-generation-plan.md), nhánh `audit`. Chưa chạy với PostgreSQL thật (migration và transaction riêng chưa kiểm ở runtime); integration test do tester viết.

## Business logic

| Luồng | Class chính |
|---|---|
| A1 Ghi audit | `AuditStore` (cài `IAudit`, thay bản tạm): kiểm `id`, `action`, `result`, `occurredAt`; `ForbiddenKeyGuard` chặn khóa cấm kể cả lồng nhau (BR-U02-05); `SUCCESS` ghi trong transaction của unit gọi, `DENIED`/`FAILURE` qua `TransactionTemplate` `REQUIRES_NEW` (BR-U02-02); lỗi DB ném tiếp để nghiệp vụ rollback (BR-U02-04) |
| A2 Tra cứu | `AuditQueryService` (cài `IAuditQuery`): chỉ `ADMIN` và `IAuthorization.authorize(actor, "AUDIT_READ")`; U01 lỗi thì từ chối (BR-U02-50) và ghi `ACCESS_DENIED`; chuỗi lọc rỗng bỏ qua, quá độ dài cột hoặc `from` sau `to` trả `400 AUDIT_FILTER_INVALID`; trang 1–100 dòng; email lấy qua `IAccountLookup.getContact` mỗi người một lần, lỗi thì để trống (BR-U02-09); ghi `AUDIT_QUERIED` kèm bộ lọc (chỉ id, không email) |

## API

| Endpoint | Ghi chú |
|---|---|
| `GET /api/v1/admin/audit-logs` | UC 13. Tham số `actorId`, `action`, `objectType`, `objectId`, `result`, `from`, `to` (ISO-8601), `page` (mặc định 0), `size` (mặc định 50, tối đa 100). Trả `items` (`id`, `occurredAt`, `actorId`, `actorEmail`, `action`, `objectType`, `objectId`, `result`, `reason`, `details`), `page`, `size`, `totalElements`, `totalPages`. Tham số sai kiểu trả Problem Detail `400 AUDIT_FILTER_INVALID`; không phải Admin trả `403 AUDIT_FORBIDDEN`. Không có API sửa hay xóa |

## Repository và migration

- `AuditLogRepository` dùng `JdbcClient`, chỉ có `insertIfAbsent` (`ON CONFLICT (id) DO NOTHING`, BR-U02-03), `count`, `find` (mới nhất trước, cùng thời điểm theo `id`); điều kiện lọc dựng bằng tham số gắn tên, không nối chuỗi.
- `db/migration/audit/V20260925_0900__create_audit_logs.sql`: bảng `audit_logs` (thêm `recorded_at` lúc lưu, `check` cho `result`), 4 index theo NFR-U02-03; `REVOKE UPDATE, DELETE, TRUNCATE` khỏi `app` chỉ khi role `app` tồn tại (NFR-U02-30).

## Frontend

- `app/admin/audit` (màn #14 Audit Log, artboard `a13`, `a13b`, `a13c`), bọc `Suspense` vì đọc tham số URL.
- `features/audit`: `AuditLogPage` (mặc định 7 ngày theo giờ Việt Nam; mở từ Account Detail với `?objectType=&objectId=` thì lọc sẵn), `AuditFilters`, `AuditTable` ("Hệ thống" khi actor rỗng, nhãn kết quả, trạng thái rỗng), `AuditDetailDialog` (chỉ đọc, JSON `details`), `labels.ts` (nhãn tiếng Việt cho kết quả, hành động, loại đối tượng; mã lạ hiện nguyên mã), `api.ts`. Lỗi 403 và lỗi tải có thông báo riêng. Phần tử tương tác có `data-testid` dạng `audit-<thành phần>-<vai trò>`.
- `shared/ui`: thêm `Pagination` ("Hiển thị a–b trong N …", số trang rút gọn bằng "…") và 5 icon `calendar_month`, `chevron_left`, `filter_alt_off`, `receipt_long`, `search` lấy từ `@material-symbols/svg-400` 0.48.0 như các icon sẵn có.
- Mục menu "Nhật ký audit" đã có sẵn trong `features/audit/register.ts`.

## Unit test

- Backend 26 test: điều kiện lọc và tham số gắn tên (`AuditLogRepositoryWhereTest`); `SUCCESS` không mở transaction mới, `DENIED`/`FAILURE` dùng `REQUIRES_NEW`, `id` lặp đi qua insert-if-absent, khóa cấm lồng nhau, thiếu trường, lỗi DB không bị nuốt (`AuditStoreTest`); không phải Admin, thiếu actor, U01 từ chối hoặc lỗi đều bị từ chối và có audit `DENIED`, email lấy một lần mỗi người, U01 lỗi để trống email, phân trang, `AUDIT_QUERIED`, bộ lọc rỗng và sai, trang > 100 (`AuditQueryServiceTest`); ánh xạ tham số và kết quả, mặc định trang 50, tham số sai trả 400, 403 thành Problem Detail (`AuditControllerTest`).
- Frontend 11 test: mặc định 7 ngày và 50 dòng, "Hệ thống", lọc sẵn từ URL, email không khớp không tra cứu, email khớp gửi `actorId`, popup chi tiết chỉ đọc, thông báo 403 và lỗi tải, chuyển trang giữ bộ lọc (`AuditLogPage.test.tsx`); rút gọn số trang, dòng đếm, nút trước/sau (`Pagination.test.tsx`).
- Chạy như CI: `mvn -B verify` (backend 120 test), `npm run lint`, `typecheck`, `test` (46 test), `build`. Đã xem màn trên trình duyệt ở khổ 1280 px với API giả trả dữ liệu mẫu.

## Khác với thiết kế

- Đổi email người thực hiện thành `actorId` gọi `GET /api/v1/admin/accounts?q=<email>` (tham số của U01) rồi chọn tài khoản có email trùng, thay cho `?email=` (đã sửa frontend-components). Khi U01 chưa gộp vào `main`, API lỗi thì màn báo "Không tìm thấy người thực hiện với email này.".
- Bảng có thêm `recorded_at` (thời điểm lưu) bên cạnh `occurred_at` do unit gọi cung cấp.
- `REVOKE` trong migration có điều kiện để migration chạy được ở môi trường không có role `app` (ví dụ Testcontainers).

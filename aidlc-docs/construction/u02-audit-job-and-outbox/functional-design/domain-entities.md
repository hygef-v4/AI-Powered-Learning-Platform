# U02 Audit - Domain Entities

**Bản tài liệu 2026-10-09**: UC 73 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Thiết kế độc lập công nghệ. Truy vết: `US-AUD-001`, UC 73. Tên thư mục `u02-audit-job-and-outbox` giữ từ bản cũ; việc nền, worker và sự kiện thông báo đã chuyển sang U03 (quyết định 2026-10-04).

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `AuditLog` (thực thể `AUDIT_LOG`) | Entity bất biến | `audit_logs` | U02 (mọi unit gửi qua `AuditPort`) |

U02 **không** sở hữu quyền đọc audit (U01) và việc nền (U03).

## 2. `AuditLog`

| Cột | Ràng buộc |
|---|---|
| `id` | Định danh do unit gọi tạo; khóa chính nên ghi lại cùng `id` không tạo bản ghi thứ hai |
| `actor_id` | FK `accounts`; NULL khi actor là hệ thống hoặc worker |
| `action` | Mã hành động ổn định, ví dụ `ROLE_CHANGED` |
| `object_type`, `object_id` | Đối tượng bị tác động |
| `result` | `SUCCESS`, `DENIED`, `FAILURE` |
| `reason` | Lý do do người dùng nhập khi có (ví dụ sửa điểm), có thể rỗng |
| `details` | JSON đã che dữ liệu nhạy cảm: dữ liệu trước/sau, loại actor (`USER`, `WORKER`, `SYSTEM`), `correlationId` |
| `occurred_at` | Thời điểm xảy ra do unit gọi cung cấp, không phải lúc lưu |

Không có thao tác sửa hoặc xóa. Giữ **vĩnh viễn**.

## 3. Contract

### Port U02 cung cấp

| Port | Dùng bởi |
|---|---|
| `AuditPort.record(event)` | Mọi unit; ghi trong transaction của unit gọi |
| `AuditPort.recordDenied(event)`, `AuditPort.recordFailure(event)` | Mọi unit; ghi sự kiện bị từ chối hoặc lỗi bằng transaction riêng |
| `AuditQueryPort.query(actor, filter, page)` | `AuditController` của màn Audit Log (UC 73) |

### Port U02 dùng

| Port | Unit | Cạnh |
|---|---|---|
| `AuthorizationPort.authorize` | U01 | `C` - chỉ cho API đọc audit; U01 code sau U02 nên dùng `AuthorizationService` khung của U01 (luôn từ chối) |
| `AccountLookupPort.getContact` | U01 | `C` - lấy email người thực hiện để hiện trên Audit Log; khi U01 chưa cài thì để trống email |

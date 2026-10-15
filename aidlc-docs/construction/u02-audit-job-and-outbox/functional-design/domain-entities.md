# U02 Audit — domain-entities

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u02-audit-job-and-outbox` không quyết định chức năng hiện hành.

Thiết kế độc lập công nghệ. Truy vết: `US-AUD-001`, UC 13. Tên thư mục `u02-audit-job-and-outbox` giữ từ bản cũ; việc nền, worker và sự kiện thông báo đã chuyển sang U03 (quyết định 2026-10-04).

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
| `AuditQueryPort.query(actor, filter, page)` | `AuditController` của màn Audit Log (UC 13) |

### Port U02 dùng

| Port | Unit | Cạnh |
|---|---|---|
| `AuthorizationPort.authorize` | U01 | `C` - chỉ cho API đọc audit; U01 code sau U02 nên dùng `AuthorizationService` khung của U01 (luôn từ chối) |
| `AccountLookupPort.getContact` | U01 | `C` - lấy email người thực hiện để hiện trên Audit Log; khi U01 chưa cài thì để trống email |

## Quy tắc tích hợp revision hiện hành

- Audit append-only; chỉ Admin query, không sửa/xóa. Ghi actor/action/object/result/occurredAt/correlationId; payload sanitize.
- Success ghi cùng transaction owner; denied/failure dùng transaction riêng khi nghiệp vụ rollback. Audit id do caller cung cấp, dedup khóa chính.
- Thêm actions AI_CONNECTION_CHANGED, QUOTA_CHANGED, MCP_REVOKED, BACKUP_COMPLETED/FAILED, RESTORE_VERIFIED và activity policy changed; không log secrets/raw prompt/nội dung nhạy cảm.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Id lặp không trùng; rollback denied vẫn ghi; field secrets/prompt bị loại; filter malformed từ chối; không Admin denied.

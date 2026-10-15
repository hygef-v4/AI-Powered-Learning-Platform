# U02 Audit — domain-entities

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u02-audit-job-and-outbox` giữ tên cũ để không phá liên kết, là thiết kế của U02 Audit hiện hành.

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

| Port | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `AuditPort` | `record(AuditEvent)` | Mọi unit, cả handler ở worker | Ghi audit, chọn transaction theo `result`: `SUCCESS` ghi trong transaction của unit gọi (nghiệp vụ rollback thì mất theo); `DENIED`, `FAILURE` ghi bằng transaction riêng để còn lại khi nghiệp vụ rollback (BR-U02-02) |
| `AuditQueryPort` | `query(actor, AuditFilter, PageRequest)` → `Page<AuditLogView>` | API `GET /api/v1/admin/audit-logs` của chính U02 (màn Audit Log, UC 13) | Tra cứu cho Admin; mỗi dòng kèm email người thực hiện lấy lúc đọc |

Kiểu dữ liệu đi kèm: `AuditEvent` (`id` do unit gọi tạo, `actorId`, `action`, `objectType`, `objectId`, `result`, `reason`, `details` đã che, `occurredAt`), `AuditResult` (`SUCCESS`, `DENIED`, `FAILURE`), `AuditFilter`, `AuditLogView` (`AuditEvent` + `actorEmail`, rỗng nghĩa là hệ thống).

### Port U02 dùng

| Port | Unit | Cạnh |
|---|---|---|
| `AuthorizationPort.authorize` | U01 | `C` - chỉ cho API đọc audit. U02 code trước U01 trong Iter 1 nên tới khi U01 xong, `AuthorizationService` khung của U01 luôn từ chối |
| `AccountLookupPort.getContact` | U01 | `C` - lấy email người thực hiện để hiện trên Audit Log; khi U01 chưa cài thì để trống email |

## Quy tắc tích hợp revision hiện hành

- Audit append-only; chỉ Admin query, không sửa/xóa. Ghi actor/action/object/result/occurredAt/correlationId; payload sanitize.
- Success ghi cùng transaction owner; denied/failure dùng transaction riêng khi nghiệp vụ rollback. Audit id do caller cung cấp, dedup khóa chính.
- Thêm actions AI_CONNECTION_CHANGED, QUOTA_CHANGED, MCP_REVOKED, BACKUP_COMPLETED/FAILED, RESTORE_VERIFIED và activity policy changed; không log secrets/raw prompt/nội dung nhạy cảm.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Id lặp không trùng; rollback denied vẫn ghi; field secrets/prompt bị loại; filter malformed từ chối; không Admin denied.

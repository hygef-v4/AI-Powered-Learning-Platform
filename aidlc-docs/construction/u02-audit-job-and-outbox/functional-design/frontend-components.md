# U02 Audit — frontend-components

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 73. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u02-audit-job-and-outbox` không quyết định chức năng hiện hành.

## 1. Cây component

```
app/admin/audit/
  AuditLogPage             màn Audit Log, mở từ Admin Dashboard (UC 73)
    AuditFilters
    AuditTable
    AuditDetailDrawer
```

Component trạng thái việc nền (`usePollStatus`, `StatusBadge`) thuộc U03.

## 2. AuditLogPage

| Component | State | API | Hành vi |
|---|---|---|---|
| `AuditFilters` | `actorEmail`, `action`, `objectType`, `objectId`, `result`, `from`, `to` | `GET /api/v1/admin/accounts?email=` (U01) để đổi email thành `actorId` | Mặc định 7 ngày gần nhất; nút Áp dụng, Xóa lọc; email không khớp tài khoản nào thì báo "không tìm thấy người thực hiện" |
| `AuditTable` | `items`, `page`, `loading` | `GET /api/v1/admin/audit-logs` | Cột: thời gian, người thực hiện (email, hoặc "Hệ thống"), hành động, đối tượng, kết quả, lý do; 50 dòng/trang, tối đa 100 |
| `AuditDetailDrawer` | `log` | - | Hiện `details` dạng JSON chỉ đọc; không có nút sửa/xóa |

Route nằm dưới `/admin` nên `RoleGuard` của U01 chỉ cho `ADMIN` vào; API vẫn tự kiểm quyền.

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

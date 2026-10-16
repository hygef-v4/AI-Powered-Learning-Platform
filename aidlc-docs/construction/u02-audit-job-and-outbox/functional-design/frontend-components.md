# U02 Audit — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U02 Audit thuộc Iter 1, người code: Người 2; phạm vi UC 13 giữ như U02 cũ (chỉ audit; việc nền đã thuộc U03). 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 13. **Primary stories:** US-AUD-001. **Requirements:** FR-014.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u02-audit-job-and-outbox` giữ tên cũ để không phá liên kết, là thiết kế của U02 Audit hiện hành.

## 1. Cây component

```
app/admin/audit/
  AuditLogPage             màn Audit Log, mở từ Admin Dashboard (UC 13)
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
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Id lặp không trùng; rollback denied vẫn ghi; field secrets/prompt bị loại; filter malformed từ chối; không Admin denied.

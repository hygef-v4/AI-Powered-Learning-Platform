# U02 Audit - Frontend Components

**Bản tài liệu 2026-10-09**: UC 73 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

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

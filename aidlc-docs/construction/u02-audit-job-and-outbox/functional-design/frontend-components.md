# U02 Audit - Frontend Components

**Bản tài liệu 2026-10-08**: UC 70; primary stories: US-AUD-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Cây component

```
app/admin/audit/
  AuditLogPage             màn Audit Log trên Admin Sidebar (UC 70)
    AuditFilters
    AuditTable
    AuditDetailDrawer
```

Component trạng thái việc nền (`usePollStatus`, `StatusBadge`) thuộc U03.

## 2. AuditLogPage

| Component | State | API | Hành vi |
|---|---|---|---|
| `AuditFilters` | `actor`, `action`, `objectType`, `objectId`, `result`, `from`, `to` | - | Mặc định 7 ngày gần nhất; nút Áp dụng, Xóa lọc |
| `AuditTable` | `items`, `page`, `loading` | `GET /api/v1/admin/audit-logs` | Cột: thời gian, actor, hành động, đối tượng, kết quả, lý do; 50 dòng/trang, tối đa 100 |
| `AuditDetailDrawer` | `log` | - | Hiện `details` dạng JSON chỉ đọc; không có nút sửa/xóa |

Route chỉ hiện cho `ADMIN`; API vẫn tự kiểm quyền.

# U02 Audit - Frontend Components

## 1. Cây component

```
app/admin/audit/
  AuditLogPage             màn Audit Log trên Admin Menu (UC 39)
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

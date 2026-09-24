# U02 Audit, Job & Outbox - Frontend Components

## 1. Cây component

```
admin-console/audit/
  AuditLogPage
    AuditFilters
    AuditTable
    AuditDetailDrawer
shared/jobs/
  JobStatusBadge
  useJobStatus            (hook poll trạng thái)
```

Không có màn quản lý job lỗi (BR-U02-27).

## 2. AuditLogPage

| Component | State | API | Hành vi |
|---|---|---|---|
| `AuditFilters` | `actor`, `action`, `resourceType`, `resourceId`, `result`, `from`, `to` | - | Mặc định 7 ngày gần nhất; nút Áp dụng, Xóa lọc |
| `AuditTable` | `items`, `page`, `loading` | `queryAudit` | Cột: thời gian, actor, hành động, đối tượng, kết quả, correlation ID; 50 dòng/trang, tối đa 100 |
| `AuditDetailDrawer` | `event` | - | Hiện `beforeData`/`afterData` dạng JSON chỉ đọc; không có nút sửa/xóa |

Route chỉ hiện cho `ADMIN`; API vẫn tự kiểm quyền.

## 3. Trạng thái job dùng chung

| Component | Props | Hành vi |
|---|---|---|
| `useJobStatus(jobId)` | `jobId`, `intervalMs = 3000` | Poll `getJobStatus`; dừng khi `SUCCEEDED` hoặc `FAILED`; dừng khi rời trang |
| `JobStatusBadge` | `status`, `safeMessage` | Nhãn: Đang chờ, Đang xử lý, Hoàn tất, Lỗi; lỗi thì hiện `safeMessage` |

Unit khác (U05, U13, U14, U16...) dùng hai component này cho màn hình của mình.

# U15 Admin Dashboard — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Admin Dashboard | `AdminDashboardPage` | 66 | Admin |

## 2. Cây component

```
app/admin/                        AdminDashboardPage   Admin Dashboard
  CountCard                                            tài khoản, môn, lớp, ghi danh; "không khả dụng" khi lỗi
  BackupStatusCard                                     lần gần nhất, thành công gần nhất, nút Cài đặt backup
    BackupHistoryDialog                                lịch sử phân trang
  AdminShortcuts                                       Account List, Subject List, Setting List, Audit Log
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `AdminDashboardPage`, `CountCard` | `GET /api/v1/admin/dashboard` | Tải lại khi mở trang; từng thẻ hiện riêng trạng thái lỗi |
| `BackupStatusCard`, `BackupHistoryDialog` | `GET /api/v1/admin/dashboard`, `GET .../backups` | Không có nút tải hay khôi phục; lỗi nguồn không hiện thành công |
| `AdminShortcuts` | - | Liên kết tới `/admin/accounts`, `/admin/subjects`, `/admin/settings`, `/admin/audit` |

Route `/admin` chỉ cho `ADMIN` (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.

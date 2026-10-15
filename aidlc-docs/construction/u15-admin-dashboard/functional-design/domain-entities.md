# U15 Admin Dashboard — domain-entities

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Thiết kế độc lập công nghệ. U15 không có bảng; mọi số liệu tính khi đọc.

## 1. Kết quả trả về

| Thành phần | Nội dung |
|---|---|
| `AdminDashboardView` | `accounts` (số theo vai trò × trạng thái, tổng), `subjects` (theo trạng thái), `classes` (theo trạng thái), `activeEnrollments`, `backup` (lần gần nhất, thành công gần nhất); mỗi phần có `availability` (`AVAILABLE`, `UNAVAILABLE`) |
| `BackupHistoryPage` | Danh sách lần chạy từ U03, phân trang |

## 2. Contract

### Port U15 cung cấp

Không có; U15 chỉ có màn và API cho Admin.

### Contract U15 khai báo

Không có.

### Port U15 dùng

| Port | Unit | Dùng để |
|---|---|---|
| `AuthorizationPort`, `AccountLookupPort.countByRoleAndStatus` | U01 | Kiểm `ADMIN`; số tài khoản |
| `AuditPort` | U02 | Ghi truy cập bị từ chối |
| `BackupStatusPort` (`latest`, `list`) | U03 | Trạng thái, lịch sử backup |
| `SubjectQueryPort.countSubjectsByStatus` | U04 | Số môn |
| `ClassAccessPort.countClassesByStatus`, `countActiveEnrollments` | U05 | Số lớp, số ghi danh |

## 3. API

| API | UC |
|---|---|
| `GET /api/v1/admin/dashboard` | 66 |
| `GET /api/v1/admin/dashboard/backups` | 66 |

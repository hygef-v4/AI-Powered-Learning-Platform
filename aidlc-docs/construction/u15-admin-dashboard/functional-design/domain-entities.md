# U15 Admin Dashboard — domain-entities

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Thiết kế độc lập công nghệ. U15 không có bảng; mọi số liệu tính khi đọc.

## 1. Kết quả trả về

| Thành phần | Nội dung |
|---|---|
| `AdminDashboardView` | `accounts` (số theo vai trò × trạng thái, tổng), `subjects` (theo trạng thái), `classes` (theo trạng thái), `activeEnrollments`, `backup` (lần gần nhất, thành công gần nhất); mỗi phần có `availability` (`AVAILABLE`, `UNAVAILABLE`) |
| `BackupHistoryPage` | Danh sách lần chạy từ U03, phân trang |

## 2. Contract

### Interface U15 cung cấp

Không có; U15 chỉ có màn và API cho Admin. Phía frontend U15 cài `NavEntry` của U01 (mục Tổng quan trên menu bên của Admin).

### Contract U15 khai báo

Không có.

### Interface U15 dùng

| Interface | Unit | Dùng để |
|---|---|---|
| `IAuthorization`, `IAccountLookup.countByRoleAndStatus` | U01 | Kiểm `ADMIN`; số tài khoản |
| `IAudit` | U02 | Ghi truy cập bị từ chối |
| `IBackupStatus` (`latest`, `list`) | U03 | Trạng thái, lịch sử backup |
| `ISettings` (`backup.enabled`, `backup.dailyAt`) | U03 | Lịch backup và lần chạy tiếp theo trên Admin Dashboard |
| `ISubjectQuery.countSubjectsByStatus` | U04 | Số môn |
| `IClassAccess.countClassesByStatus`, `countActiveEnrollments` | U05 | Số lớp, số ghi danh |

## 3. API

| API | UC |
|---|---|
| `GET /api/v1/admin/dashboard` | 66 |
| `GET /api/v1/admin/dashboard/backups` | 66 |

## Source behavior delta — 2026-10-30

Admin Dashboard có counts, backup metadata/history và GET /activity. Activity gồm biến động ACTIVE accounts hôm nay, số Assignment 7 ngày (individual/group/practice), tối đa 50 security events trong 30 ngày; ngày Asia/Ho_Chi_Minh. Các nguồn lỗi/timeout trả từng Section UNAVAILABLE/data null; no-store, chỉ ADMIN, query lạ bị từ chối. Không lộ archive/secrets hoặc cấp quyền học vụ.

[Source/API/schema/test inventory](../code/as-built-baseline.md). Các rule và test cũ trái delta này là thiết kế trước baseline; không tiếp tục áp điều kiện cũ vào AS-BUILT.

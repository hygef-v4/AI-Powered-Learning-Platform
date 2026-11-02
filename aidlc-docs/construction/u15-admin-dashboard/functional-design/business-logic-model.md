# U15 Admin Dashboard — business-logic-model

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Mở Admin Dashboard | UC 66 | Admin Dashboard |
| F2 Xem lịch sử backup | UC 66 | Admin Dashboard (mục Backup) |

### F1 - Mở Admin Dashboard (UC 66)
1. Admin đăng nhập, Post-Login chuyển tới `/admin`; frontend gọi `GET /api/v1/admin/dashboard`.
2. Kiểm `ADMIN` đang `ACTIVE` qua `IAuthorization`; vai trò khác trả `403` và ghi audit (BR-U15-01).
3. Gọi song song: `IAccountLookup.countByRoleAndStatus` (U01), `ISubjectQuery.countSubjectsByStatus` (U04), `IClassAccess.countClassesByStatus` và `countActiveEnrollments` (U05), `IBackupStatus.latest` (U03); mỗi lời gọi có timeout 2 giây.
4. Ghép kết quả; nguồn lỗi hoặc hết giờ thì phần đó là "không khả dụng" (BR-U15-12, 21).
5. Trả số liệu và trạng thái backup; frontend hiện các thẻ, tổng số tài khoản, và lối vào bốn màn quản trị (BR-U15-30).

### F2 - Xem lịch sử backup (UC 66)
1. Trong mục Backup bấm Lịch sử; frontend gọi `GET /api/v1/admin/dashboard/backups` kèm trang.
2. Kiểm `ADMIN`; gọi `IBackupStatus.list(page)`.
3. Trả các lần chạy (trạng thái, thời điểm, dung lượng, lỗi đã làm sạch); không có link tải.
4. Nút "Cài đặt backup" mở Setting Detail của U03.

## Source behavior delta — 2026-10-30

Admin Dashboard có counts, backup metadata/history và GET /activity. Activity gồm biến động ACTIVE accounts hôm nay, số Assignment 7 ngày (individual/group/practice), tối đa 50 security events trong 30 ngày; ngày Asia/Ho_Chi_Minh. Các nguồn lỗi/timeout trả từng Section UNAVAILABLE/data null; no-store, chỉ ADMIN, query lạ bị từ chối. Không lộ archive/secrets hoặc cấp quyền học vụ.

[Source/API/schema/test inventory](../code/as-built-baseline.md). Các rule và test cũ trái delta này là thiết kế trước baseline; không tiếp tục áp điều kiện cũ vào AS-BUILT.

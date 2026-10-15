# U15 Admin Dashboard — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Mở Admin Dashboard | UC 66 | Admin Dashboard |
| F2 Xem lịch sử backup | UC 66 | Admin Dashboard (mục Backup) |

### F1 - Mở Admin Dashboard (UC 66)
1. Admin đăng nhập, Post-Login chuyển tới `/admin`; frontend gọi `GET /api/v1/admin/dashboard`.
2. Kiểm `ADMIN` đang `ACTIVE` qua `AuthorizationPort`; vai trò khác trả `403` và ghi audit (BR-U15-01).
3. Gọi song song: `AccountLookupPort.countByRoleAndStatus` (U01), `SubjectQueryPort.countSubjectsByStatus` (U04), `ClassAccessPort.countClassesByStatus` và `countActiveEnrollments` (U05), `BackupStatusPort.latest` (U03); mỗi lời gọi có timeout 2 giây.
4. Ghép kết quả; nguồn lỗi hoặc hết giờ thì phần đó là "không khả dụng" (BR-U15-12, 21).
5. Trả số liệu và trạng thái backup; frontend hiện các thẻ, tổng số tài khoản, và lối vào bốn màn quản trị (BR-U15-30).

### F2 - Xem lịch sử backup (UC 66)
1. Trong mục Backup bấm Lịch sử; frontend gọi `GET /api/v1/admin/dashboard/backups` kèm trang.
2. Kiểm `ADMIN`; gọi `BackupStatusPort.list(page)`.
3. Trả các lần chạy (trạng thái, thời điểm, dung lượng, lỗi đã làm sạch); không có link tải.
4. Nút "Cài đặt backup" mở Setting Detail của U03.

# U15 Admin Dashboard — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U15 Admin Dashboard thuộc Iter 3, người code: Người 5; UC 66. Lấy phần Admin Dashboard của U16 cũ; thêm trạng thái backup của U03. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U15-01 | Admin Dashboard là trang đích `/admin` của `ADMIN`. Vai trò khác bị từ chối (`403`), không trả số liệu nào, kể cả khi gọi API trực tiếp; ghi audit `ACCESS_DENIED`. | US-RPT-002 S2 |
| BR-U15-02 | Trang không cho vào nội dung học vụ (môn, lớp, bài, điểm) và không có nút khôi phục dữ liệu. | UC 66 |

## 2. Số liệu hệ thống

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U15-10 | Hiện số tài khoản theo vai trò và trạng thái (`PENDING`, `ACTIVE`, `DISABLED`) kèm tổng; số môn theo trạng thái (`ACTIVE`, `ARCHIVED`); số lớp theo trạng thái (`DRAFT`, `OPEN`, `ARCHIVED`); số ghi danh `ACTIVE`. | US-RPT-002 S1, FR-024 |
| BR-U15-11 | Số liệu đếm mỗi lần mở trang, không lưu, không cache; chỉ trả số đếm, không trả tên, email hay ID. | US-RPT-002 S1 |
| BR-U15-12 | Mỗi nhóm số liệu lấy từ unit sở hữu (U01, U04, U05); nguồn nào lỗi thì thẻ đó hiện "không khả dụng", các thẻ khác vẫn hiện. | Thiết kế |

## 3. Backup

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U15-20 | Mục Backup đọc `BackupStatusPort` của U03: lần chạy gần nhất (trạng thái, thời điểm bắt đầu, kết thúc, dung lượng, lỗi đã làm sạch), thời điểm backup thành công gần nhất, lịch sử phân trang. | US-RPT-002 S3, FR-038 |
| BR-U15-21 | U03 lỗi thì mục Backup hiện "không khả dụng", không bao giờ hiện như thành công. Backup chưa bật thì ghi rõ "chưa bật" kèm lối sang Setting Detail. | US-RPT-002 S3 |
| BR-U15-22 | Không tải bản backup, không khôi phục từ giao diện. Sửa lịch, số bản giữ qua Setting Detail (UC 15, U03). | FR-038 |

## 4. Lối vào màn quản trị

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U15-30 | Admin Dashboard có lối vào Account List (U01), Subject List (U04), Setting List (U03), Audit Log (U02). Không có lối vào Manager Dashboard, Class Dashboard hay nội dung lớp. | UC 66, screen flow |
| BR-U15-31 | Khi U15 chưa có (Iter 1, 2), `homePath` của Admin tạm là Account List. | Thiết kế |

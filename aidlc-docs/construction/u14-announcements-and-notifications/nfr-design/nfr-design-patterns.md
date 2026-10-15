# U14 Announcements & Notifications — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Tiêu thụ sự kiện idempotent
- `INSERT ... ON CONFLICT (source_event_id, recipient_id, type) DO NOTHING`; tạo thông báo và bản ghi email trong một transaction; SSE sau commit.

## P2 - Trần email theo ngày
- Đếm email `PENDING`, `SENT` của ngày (giờ Việt Nam) trong cùng transaction tạo bản ghi; vượt thì `DEFERRED`; scanner đầu ngày đưa lại theo ưu tiên.

## P3 - Nhắc hạn theo dấu
- Dấu (bài, lớp, `closesAt`) chèn trước khi tạo thông báo; đổi hạn tạo khóa mới nên nhắc theo hạn mới; chạy lại không nhắc hai lần.

## P4 - Dọn theo lô
- Xóa tối đa 10.000 thông báo cũ mỗi lần chạy.

# U14 Announcements & Notifications — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Tiêu thụ sự kiện idempotent
- `INSERT ... ON CONFLICT (source_event_id, recipient_id, type) DO NOTHING`; tạo thông báo và bản ghi email trong một transaction; SSE sau commit.

## P2 - Trần email theo ngày
- Đếm email `PENDING`, `SENT` của ngày (giờ Việt Nam) trong cùng transaction tạo bản ghi; vượt thì `DEFERRED`; scanner đầu ngày đưa lại theo ưu tiên.

## P3 - Nhắc hạn theo dấu
- Dấu (bài, lớp, `closesAt`) chèn trước khi tạo thông báo; đổi hạn tạo khóa mới nên nhắc theo hạn mới; chạy lại không nhắc hai lần.

## P4 - Dọn theo lô
- Xóa tối đa 10.000 thông báo cũ mỗi lần chạy.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

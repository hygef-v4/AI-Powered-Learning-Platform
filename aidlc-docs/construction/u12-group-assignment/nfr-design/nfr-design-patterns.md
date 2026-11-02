# U12 Group Assignment — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Khóa theo mục
- Nhận, giao, nhả, lưu nháp, Xong: `SELECT ... FOR UPDATE` dòng mục, kiểm người giữ và `version`; tài liệu chỉ khóa khi nộp.

## P2 - Lưu nhóm một lần
- Bộ nhóm của lớp có `version`; lưu thì kiểm toàn bộ trước, ghi trong một transaction, kèm xử lý mục của người rời nhóm.

## P3 - Tạo tài liệu idempotent
- `INSERT ... ON CONFLICT (group_id, assignment_id) DO NOTHING`; job và truy cập đầu tiên dùng chung hàm.

## P4 - Realtime qua fanout
- Sau commit, gửi sự kiện tới exchange `platform.realtime`; mỗi instance có queue tạm, đẩy tới `SseEmitter` của kênh; client mất kết nối thì tải lại toàn bộ.

## P5 - Tự nộp có điều kiện
- Chỉ tài liệu `IN_PROGRESS` có `lastChangedAt` sau lần nộp cuối mới chụp lại; câu lệnh chuyển `CLOSED` có điều kiện trạng thái.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

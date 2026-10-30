# U08 Assignment Work — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Lưu nháp có phiên bản
- `UPDATE assignment_attempts SET content = ?, content_version = content_version + 1 WHERE id = ? AND status = 'IN_PROGRESS' AND content_version = ?`; 0 dòng thì `409`.

## P2 - Nộp một lần
- `UPDATE ... SET status = 'SUBMITTED', ... WHERE id = ? AND status = 'IN_PROGRESS'`; 0 dòng mà lượt đã nộp thì trả biên nhận cũ. Scanner và `onRetired` dùng cùng câu lệnh.

## P3 - Phản hồi gắn bài nộp
- Lưu `receiptHash` vào yêu cầu; khi nhận kết quả so lại với lượt; lệch thì bỏ.

## P4 - Sự kiện chỉ thêm, tính khi đọc
- Ghi `INSERT ... ON CONFLICT DO NOTHING`; tóm tắt tính khi giảng viên mở, không lưu kết luận.

## P5 - Dọn dữ liệu theo lô
- Scanner xóa tối đa 10.000 sự kiện mỗi lần chạy, theo index thời gian.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

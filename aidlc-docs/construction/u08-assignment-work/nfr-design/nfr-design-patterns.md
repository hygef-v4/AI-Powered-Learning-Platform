# U08 Assignment Work — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

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

# U12 Group Assignment — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

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

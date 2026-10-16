# U07 Assignment Authoring — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Tính phần và đồng bộ rubric khi lưu
- Mỗi lần lưu khung: dựng cây heading, lấy lá của mỗi nhánh làm phần; so với phần cũ theo `partId`; tạo rubric trống cho phần mới, xóa rubric của phần mất, trong cùng transaction với khung.

## P2 - Khóa khi phát hành
- `UPDATE assignments SET status = 'SCHEDULED', ... WHERE id = ? AND status = 'REVIEWED' AND version = ?`; trong cùng transaction kiểm lại và đặt `locked_at` cho mọi rubric; một bước lỗi thì rollback cả.

## P3 - Scanner theo lịch
- `UPDATE ... SET status = 'OPEN' WHERE status = 'SCHEDULED' AND opens_at <= now() RETURNING id`; với từng bài gọi listener trong cùng transaction; phát sự kiện sau commit.
- `UPDATE ... SET status = 'CLOSED' WHERE status = 'OPEN' AND COALESCE(late_until, closes_at) <= now()`.

## P4 - Mã sơ đồ trong DOCX
- Alt text "mô tả [drawio:assignmentId/blockId]"; khi nhập chỉ đọc phần trong ngoặc vuông, kiểm quyền đọc bài nguồn rồi sao XML; nhiều lớp dự phòng như BR-U07-61.

## P5 - Đề xuất AI tách khỏi bài
- Kết quả AI và MCP luôn vào `assignment_ai_drafts`; chỉ lệnh Áp dụng có `version` mới sửa bài.

## P6 - Làm sạch đầu vào tài liệu
- XML sơ đồ, SVG, DOCX đều kiểm kích thước trước khi đọc hết; lỗi thì từ chối cả thao tác, không lưu một phần.

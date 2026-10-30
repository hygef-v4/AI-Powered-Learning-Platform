# U07 Assignment Authoring — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-22 theo code hiện tại (các yêu cầu của người dùng 2026-10-21, 2026-10-22; chi tiết từng vòng trong code-summary).

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Tính phần và đồng bộ rubric khi lưu
- Mỗi lần lưu khung: dựng cây heading, lấy lá của mỗi nhánh làm phần; so với phần cũ theo `partId`; tạo rubric trống cho phần mới, xóa rubric của phần mất, trong cùng transaction với khung.

## P2 - Khóa khi phát hành
- Phát hành: khóa dòng bài (`SELECT ... FOR UPDATE`, `version` khớp, bài còn nháp `DRAFT` hoặc `REVIEWED` cũ), kiểm luật duyệt và lịch, ghi lịch và `SCHEDULED`, đặt `locked_at` cho mọi rubric trong cùng transaction; một bước lỗi thì rollback cả. Không còn bước `REVIEWED` riêng (yêu cầu 2026-10-22).

## P3 - Scanner theo lịch
- `UPDATE ... SET status = 'OPEN' WHERE status = 'SCHEDULED' AND opens_at <= now() RETURNING id`; với từng bài gọi listener trong cùng transaction; phát sự kiện sau commit.
- `UPDATE ... SET status = 'CLOSED' WHERE status = 'OPEN' AND COALESCE(late_until, closes_at) <= now()`.

## P4 - Mã sơ đồ trong DOCX
- Alt text "mô tả [drawio:assignmentId/blockId]"; khi nhập chỉ đọc phần trong ngoặc vuông, kiểm quyền đọc bài nguồn rồi sao XML; nhiều lớp dự phòng như BR-U07-61.

## P5 - Đề xuất AI tách khỏi bài
- Kết quả AI và MCP luôn vào `assignment_ai_drafts`; chỉ lệnh Áp dụng có `version` mới sửa bài.

## P6 - Làm sạch đầu vào tài liệu
- XML sơ đồ, SVG, DOCX đều kiểm kích thước trước khi đọc hết; lỗi thì từ chối cả thao tác, không lưu một phần.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

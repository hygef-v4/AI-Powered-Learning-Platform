# U10 Quiz Authoring — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Phiên bản câu bất biến
- Sửa bản `ACTIVE` luôn tạo dòng mới; Quiz lưu id phiên bản; không cập nhật dây chuyền.

## P2 - Đáp án không rời U10
- DTO cho người học tách khỏi DTO cho người soạn; U11 chấm bằng `IQuizScoring.score`, chỉ nhận đúng sai và điểm.

## P3 - Phát hành thay version
- Một transaction: Quiz mới `OPEN`, Quiz cũ `RETIRED` có điều kiện, gọi `onRetired`; lỗi thì rollback cả.

## P4 - Đề xuất tách khỏi Quiz
- Câu AI và MCP luôn vào `quiz_ai_drafts`; chỉ lệnh chấp nhận kèm `version` mới thêm vào Quiz hoặc ngân hàng.

## P5 - Nhập từng dòng
- Mỗi dòng kiểm riêng, ghi các dòng hợp lệ trong một transaction; trả kết quả từng dòng.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

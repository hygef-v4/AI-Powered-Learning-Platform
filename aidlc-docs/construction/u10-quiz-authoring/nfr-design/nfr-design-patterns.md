# U10 Quiz Authoring — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Lấy ngân hàng câu hỏi của U06 cũ, phần Quiz của U08 cũ và cài đặt Quiz của U09 cũ. Ngân hàng chỉ còn câu trắc nghiệm theo UC 46, 47. Làm Quiz thuộc U11. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Phiên bản câu bất biến
- Sửa bản `ACTIVE` luôn tạo dòng mới; Quiz lưu id phiên bản; không cập nhật dây chuyền.

## P2 - Đáp án không rời U10
- DTO cho người học tách khỏi DTO cho người soạn; U11 chấm bằng `QuizScoringPort.score`, chỉ nhận đúng sai và điểm.

## P3 - Phát hành thay version
- Một transaction: Quiz mới `OPEN`, Quiz cũ `RETIRED` có điều kiện, gọi `onRetired`; lỗi thì rollback cả.

## P4 - Đề xuất tách khỏi Quiz
- Câu AI và MCP luôn vào `quiz_ai_drafts`; chỉ lệnh chấp nhận kèm `version` mới thêm vào Quiz hoặc ngân hàng.

## P5 - Nhập từng dòng
- Mỗi dòng kiểm riêng, ghi các dòng hợp lệ trong một transaction; trả kết quả từng dòng.

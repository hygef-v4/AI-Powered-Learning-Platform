# U11 Quiz Practice — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Lưu có phiên bản
- `UPDATE quiz_attempts SET answers = ?, answer_version = answer_version + 1 WHERE id = ? AND status = 'IN_PROGRESS' AND answer_version = ?`; 0 dòng thì `409`.

## P2 - Nộp và chấm một lần
- `UPDATE ... SET status = 'SUBMITTED' ... WHERE id = ? AND status = 'IN_PROGRESS'`, rồi chấm trong cùng transaction; nộp lặp đọc lại kết quả đã lưu.

## P3 - Đáp án ở U10
- U11 chỉ nhận đúng sai và điểm từ `QuizScoringPort`; đáp án chỉ lấy qua `answerReview` khi trả Quiz Result đúng cài đặt.

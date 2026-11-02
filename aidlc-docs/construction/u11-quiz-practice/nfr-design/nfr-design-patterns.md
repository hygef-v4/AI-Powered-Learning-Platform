# U11 Quiz Practice — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Lưu có phiên bản
- `UPDATE quiz_attempts SET answers = ?, answer_version = answer_version + 1 WHERE id = ? AND status = 'IN_PROGRESS' AND answer_version = ?`; 0 dòng thì `409`.

## P2 - Nộp và chấm một lần
- `UPDATE ... SET status = 'SUBMITTED' ... WHERE id = ? AND status = 'IN_PROGRESS'`, rồi chấm trong cùng transaction; nộp lặp đọc lại kết quả đã lưu.

## P3 - Đáp án ở U10
- U11 chỉ nhận đúng sai và điểm từ `IQuizScoring`; đáp án chỉ lấy qua `answerReview` khi trả Quiz Result đúng cài đặt.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

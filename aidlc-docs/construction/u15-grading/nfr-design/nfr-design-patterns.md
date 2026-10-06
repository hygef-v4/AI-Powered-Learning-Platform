# U15 Grading - NFR Design Patterns

## P1 - Một đường ghi điểm
- `GradeWriter.apply(evaluationId, change, actor, reason?)`: khóa theo `version` → kiểm luật (lý do bắt buộc khi sửa điểm tự chấm hoặc điểm đã chốt; khác đề xuất AI không cần lý do; `0 ≤ score ≤ max`) → UPDATE kèm thêm phần tử `history` → audit → event sau commit nếu `PUBLISHED`. Mọi luồng (tự chấm, chấm tay, dùng đề xuất, chốt, công bố, sửa) đi qua đây (NFR-U15-11, 12).

## P2 - Tạo đánh giá idempotent
- Unique một phần trên `evaluations` (xem infrastructure-design §2); port nộp dùng `INSERT ... ON CONFLICT DO NOTHING` rồi cập nhật qua P1 (NFR-U15-13).
- Bản nộp nhóm mới ghi đè cùng `group_document_id` (không tạo lượt mới), đánh giá `GROUP_DOCUMENT` và `MEMBER` về `PENDING` cho bản mới, điểm cũ thêm vào `history` (BR-U15-44).

## P3 - Chấm xác định
- `QuizScorer` thuần: nhận câu (góc nhìn đầy đủ từ U06/câu riêng U08) và đáp án; trả `items` + tổng `BigDecimal`.
- Code: nhận `score`, `results` qua `CodeGradedPort.onGraded`.
- Practice: `PracticeResultPort` ghi `kind = PRACTICE`, `PUBLISHED` một lần cho mỗi lượt.

## P4 - Hàng loạt theo từng mục
- Chốt/công bố hàng loạt: mỗi mục một transaction con qua P1 (`TransactionTemplate`), gom kết quả `{evaluationId, ok, error}`; công bố hàng loạt cũng chạy từng mục qua P1 với điều kiện `status = 'FINALIZED'` + lịch sử (NFR-U15-02).

## P5 - Góc nhìn tách biệt
- `StudentGradeView` chỉ gồm điểm/phản hồi `PUBLISHED`; `TeacherGradeView` đủ (tự chấm, đề xuất, lịch sử). Controller người học chỉ dùng `StudentGradeView` (NFR-U15-20).

## P6 - Sổ điểm
- Một query: người học đang ghi danh (U04) × bài của lớp (U08) LEFT JOIN điểm của lượt được chấm (U11) → map trạng thái; không có cột tổng.

# U15 Grading - NFR Design Patterns

## P1 - Một đường ghi điểm
- `GradeWriter.apply(gradeId, change, actor, reason?)`: khóa theo `version` → kiểm luật (lý do khi sửa điểm tự chấm/đã chốt/khác đề xuất; `0 ≤ score ≤ max`) → UPDATE → INSERT `grade_history` → event sau commit nếu `PUBLISHED`. Mọi luồng (tự chấm, chấm tay, dùng đề xuất, chốt, công bố, sửa) đi qua đây (NFR-U15-11, 12).

## P2 - Tiêu thụ event idempotent
- Unique `(target_kind, target_id, learner_id)`; listener dùng `INSERT ... ON CONFLICT DO NOTHING` rồi cập nhật qua P1 (NFR-U15-13).
- Bản nộp nhóm mới: điểm chưa chốt chuyển `target_id` sang bản mới; điểm đã chốt giữ và gắn cờ "có bản nộp mới".

## P3 - Chấm xác định
- `QuizScorer` thuần: nhận câu (góc nhìn đầy đủ từ U06/câu riêng U08) và đáp án; trả `items` + tổng `BigDecimal`.
- Code: nhận `score`, `results` từ `u13.code.graded`.

## P4 - Hàng loạt theo từng mục
- Chốt/công bố hàng loạt: mỗi mục một transaction con qua P1 (`TransactionTemplate`), gom kết quả `{gradeId, ok, error}`; công bố một lượt phát hành dùng một UPDATE `WHERE publication_id = ? AND status = 'FINALIZED'` + lịch sử hàng loạt (NFR-U15-02).

## P5 - Góc nhìn tách biệt
- `LearnerGradeView` chỉ gồm điểm/phản hồi `PUBLISHED`; `TeacherGradeView` đủ (tự chấm, đề xuất, lịch sử). Controller người học chỉ dùng `LearnerGradeView` (NFR-U15-20).

## P6 - Sổ điểm
- Một query: người học đang ghi danh (U04) × publication của lớp (U08) LEFT JOIN điểm của lượt được chấm (U11/U10) → map trạng thái; không có cột tổng.

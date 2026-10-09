# U15 Grading - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Một đường ghi điểm
- `GradeWriter.apply(evaluationId, change, actor, reason?)`: kiểm quyền theo `class_id` của đánh giá (`GradingScopeGuard`) → khóa theo `version` → kiểm luật (lý do bắt buộc khi sửa điểm tự chấm hoặc điểm đã chốt; khác đề xuất AI không cần lý do; `0 ≤ score ≤ max`) → UPDATE kèm thêm phần tử `history` → audit → event sau commit nếu `PUBLISHED`. Mọi luồng (tự chấm, chấm tay, dùng đề xuất, chốt, công bố, sửa) đi qua đây (NFR-U15-11, 12).

## P2 - Tạo đánh giá idempotent
- Unique một phần trên `evaluations` (xem infrastructure-design §2); port nộp dùng `INSERT ... ON CONFLICT DO NOTHING` rồi cập nhật qua P1 (NFR-U15-13).
- `assignment_id`, `class_id` ghi một lần khi tạo: lượt cá nhân lấy lớp của lượt từ `SubmissionQueryPort` (U11), bài nhóm lấy lớp của nhóm từ `GroupSubmissionQueryPort` (U14); không suy lớp từ ghi danh hiện tại.
- Bản nộp nhóm mới ghi đè cùng `group_document_id`: như lượt nộp mới của bài `DOCUMENT`, đánh giá `GROUP_DOCUMENT` và `MEMBER` về `PENDING` cho bản mới, điểm cũ thêm vào `history` (BR-U15-44).

## P3 - Chấm xác định
- `QuizScorer` thuần: nhận câu (version đã ghim từ U06, gồm câu riêng của quiz) và đáp án; trả `items` + tổng `BigDecimal`. Chỉ dùng cho quiz luyện tập.
- Code Lab: nhận `score`, `results` qua `CodeGradedPort.onGraded` (bài `GRADED`) hoặc `PracticeResultPort.record` (Practice).
- Practice: `PracticeResultPort` ghi `kind = PRACTICE`, `PUBLISHED` một lần cho mỗi lượt.

## P4 - Hàng loạt theo từng mục, công bố theo lớp
- Chốt/công bố hàng loạt: mỗi mục một transaction con qua P1 (`TransactionTemplate`), gom kết quả `{evaluationId, ok, error}`; mục ngoài lớp được dạy trả lỗi riêng, không ảnh hưởng mục khác (NFR-U15-02).
- Công bố: transaction con chỉ đổi đánh giá sang `PUBLISHED` và đặt `published_at`; sau commit gom người học vừa có điểm theo (bài, lớp) và phát một `grade.published` cho mỗi cặp.
- Thời điểm công bố của (bài, lớp) đọc bằng `MIN/MAX(published_at)` trên `evaluations` theo `assignment_id`, `class_id` (index `(assignment_id, class_id, status)`); không có bảng công bố riêng.

## P5 - Góc nhìn tách biệt
- `StudentGradeView` chỉ gồm điểm/phản hồi `PUBLISHED` (và kết quả Practice của chính mình); `TeacherGradeView` đủ (tự chấm, đề xuất, lịch sử). Controller của Student chỉ dùng `StudentGradeView` (NFR-U15-20).

## P6 - Sổ điểm
- Một query: sinh viên đang ghi danh lớp (U04) × bài `GRADED` của lớp và của môn chứa lớp (U08 `listForClass`) LEFT JOIN `evaluations` cùng `class_id` của lượt được chấm (U11) hoặc đánh giá `MEMBER` → map trạng thái; không có cột tổng.

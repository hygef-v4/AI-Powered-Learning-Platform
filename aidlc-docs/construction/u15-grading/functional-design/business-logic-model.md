# U15 Grading - Business Logic Model

## F1 - Nhận bài nộp
1. Chỉ bài `GRADED`: U11 gọi `SubmissionSubmittedPort.onSubmitted` (U15 cài) trong transaction nộp → tạo `evaluations` `kind = ATTEMPT`, `PENDING`, `max_score` = tổng điểm bài. `PRACTICE` không gọi port này; kết quả luyện tập vào `PracticeResultPort` (F8).
2. Trắc nghiệm → `QuizScorer` chấm ngay trong cùng transaction (BR-U15-10), áp BR-U15-12. Code Lab → gọi `CodeRunPort.grade` (U13 gửi việc sau commit).
3. U14 nộp gọi `GroupSubmittedPort.onGroupSubmitted` → `evaluations` `GROUP_DOCUMENT` và một `MEMBER` cho mỗi thành viên, `PENDING` (`INSERT ... ON CONFLICT DO NOTHING`). Nộp lại cùng `group_document_id`: như lượt nộp mới của bài `DOCUMENT`, đánh giá `GROUP_DOCUMENT` và `MEMBER` quay về `PENDING` cho bản mới, điểm cũ giữ trong `history` (BR-U15-44).
4. U13 chấm xong gọi `CodeGradedPort.onGraded` (U15 cài) → cập nhật `score`, `rubric_checks`, `DRAFT` (BR-U15-11).

## F2 - Chấm
1. Giảng viên mở bài: nội dung (U11/U14), đề, rubric, điểm tự chấm, đề xuất AI nếu có.
2. Chấm tay (BR-U15-21) hoặc nhờ AI (BR-U15-22, 23) → `DRAFT`. Nhờ AI: giảng viên gửi `POST /evaluations/{id}/ai-proposal`, U15 kiểm giảng viên của lớp rồi gọi `AiGradingPort.request`; popup Grade with AI poll `GET /ai-suggestions/{id}` của U13; bấm "Dùng đề xuất" thì điền sẵn checklist và giải thích, giảng viên lưu bằng `PUT /evaluations/{id}` như chấm tay.
3. Chấm hàng loạt: giảng viên chọn nhiều bài trong Grading Queue → `AiGradingPort.requestBatch` (U13 kiểm credit cả lô, xử lý nền); mỗi đề xuất xong ghi `ai_score`, `ai_feedback` và checklist đề xuất vào đánh giá, trạng thái vẫn `PENDING` tới khi giảng viên xác nhận (BR-U15-24).
4. Grading Workspace: ‹ › chuyển bài theo thứ tự hàng chờ hoặc lô; lưu xong tự sang bài kế (BR-U15-25).

## F3 - Chốt
1. Từng bài hoặc hàng loạt (BR-U15-31) → `FINALIZED`; lịch sử; audit.

## F4 - Công bố
1. Công bố một bài nộp trong Grading Workspace, hoặc chọn nhiều bài `FINALIZED` trong Grading Queue rồi bấm "Công bố hàng loạt" (BR-U15-32): mỗi bài một transaction con → `PUBLISHED`, kết quả từng mục; lần đầu ghi `grades_released_at`; event.

## F5 - Sửa điểm
1. Lý do bắt buộc (BR-U15-33); thêm phần tử `history`; event nếu đã công bố.

## F6 - Bài nhóm
1. Chấm tài liệu chung như bài `DOCUMENT`: chấm tay theo rubric của từng phần hoặc nhờ AI đề xuất; điểm tài liệu chung là tổng điểm các phần (BR-U15-40).
2. Điểm đóng góp từng thành viên: mặc định bằng điểm tài liệu chung cho mọi người; giảng viên chấm tay từng người nếu cần, lý do tùy chọn (BR-U15-41…43).

## F7 - Sổ điểm
1. Giảng viên/Chủ nhiệm môn của môn: danh sách theo sinh viên, mỗi sinh viên đóng/mở (BR-U15-50); người học: điểm của mình trên Assignment List (BR-U15-51); lịch sử (BR-U15-52).

## F8 - Kết quả Practice
1. Practice Quiz: U11 gọi `PracticeResultPort.scoreQuiz(attemptId)`, U15 chấm bằng `QuizScorer` (cùng hàm với bài `GRADED`). Practice Code Lab và AI chấm Text/Diagram Essay: U13 gọi `PracticeResultPort.record(attemptId, result)`. Cả hai ghi `evaluations` `kind = PRACTICE`, `PUBLISHED`, chỉ Student chủ lượt xem; không vào sổ điểm, không qua chốt/công bố (BR-U15-35).

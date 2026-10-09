# U15 Grading - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Màn theo screen flow: Teacher Class Detail (tab Evals) → Submission Detail → Grading Workspace; sổ điểm là phần "Sổ điểm" trên tab Evals (không có màn riêng).

## F1 - Nhận bài nộp
1. Chỉ bài `GRADED` (Text Essay, Diagram Essay, Code Lab): U11 gọi `SubmissionSubmittedPort.onSubmitted` (U15 cài) trong transaction nộp → U15 đọc lượt qua `SubmissionQueryPort` (bài, người học, lớp của lượt) rồi tạo `evaluations` `kind = ATTEMPT`, `PENDING`, `assignment_id`, `class_id`, `max_score` = tổng điểm bài. `PRACTICE` và quiz không gọi port này (F8).
2. Code Lab → gọi `CodeRunPort.grade` (U13 gửi việc sau commit).
3. U14 nộp gọi `GroupSubmittedPort.onGroupSubmitted` → `evaluations` `GROUP_DOCUMENT` và một `MEMBER` cho mỗi thành viên, `PENDING`, `class_id` là lớp của nhóm (`INSERT ... ON CONFLICT DO NOTHING`). Nộp lại cùng `group_document_id`: như lượt nộp mới của bài `DOCUMENT`, đánh giá `GROUP_DOCUMENT` và `MEMBER` quay về `PENDING` cho bản mới, điểm cũ giữ trong `history` (BR-U15-44).
4. U13 chấm xong gọi `CodeGradedPort.onGraded` (U15 cài) → cập nhật `score`, `rubric_checks` (kết quả test), `DRAFT` (BR-U15-11).

## F2 - Xem bài nộp (UC 37)
1. Giảng viên lớp mở tab Evals của Teacher Class Detail, bấm "Bài nộp" ở một bài (bài của lớp hoặc bài của môn) → Submission Detail của (bài, lớp).
2. Submission Detail liệt kê bài nộp của sinh viên hoặc nhóm trong lớp: người học/nhóm, lượt, thời điểm nộp, trễ, điểm "x / tổng", trạng thái chấm; lọc theo trạng thái và trễ; số đã nộp trên số sinh viên. Bấm một dòng xem nhanh bài nộp chỉ đọc; nút "Chấm" mở Grading Workspace (BR-U15-01, 03, 20).
3. Bài nhóm: thêm `GroupDocsOverviewPanel` (U14) để xem tiến độ phần và mở bản nộp nhóm.

## F3 - Chấm (UC 38, 39)
1. Grading Workspace mở một đánh giá: nội dung (U11/U14), đề, rubric của bài (U06), điểm tự chấm, đề xuất AI nếu có.
2. Chấm tay (UC 39, BR-U15-21) hoặc nhờ AI (UC 38, BR-U15-22, 23) → `DRAFT`. Nhờ AI: trong panel AI đề xuất của Grading Workspace (U15 tự làm), giảng viên gửi `POST /evaluations/{id}/ai-proposal`, U15 kiểm giảng viên của lớp rồi gọi `AiGradingPort.request`; panel poll `GET /ai-suggestions/{id}` của U13; bấm "Dùng đề xuất" thì điền sẵn checklist và giải thích, giảng viên lưu bằng `PUT /evaluations/{id}` như chấm tay.
3. Chấm hàng loạt bằng AI: giảng viên chọn nhiều bài trong Submission Detail → `AiGradingPort.requestBatch` (U13 kiểm credit cả lô, xử lý nền); mỗi đề xuất xong ghi `ai_score`, `ai_feedback` và checklist đề xuất vào đánh giá, trạng thái vẫn `PENDING` tới khi giảng viên xác nhận (BR-U15-24).
4. ‹ › chuyển bài theo thứ tự của Submission Detail hoặc lô; lưu xong tự sang bài kế (BR-U15-25).

## F4 - Chốt
1. Từng bài (Grading Workspace) hoặc hàng loạt (Submission Detail, hộp xác nhận) (BR-U15-31) → `FINALIZED`; lịch sử; audit.

## F5 - Công bố
1. Công bố một bài nộp trong Grading Workspace, hoặc chọn nhiều bài `FINALIZED` trong Submission Detail rồi bấm "Công bố hàng loạt" (BR-U15-32): mỗi bài một transaction con → `PUBLISHED`, kết quả từng mục.
2. Không lưu bản ghi công bố riêng: thời điểm công bố của (bài, lớp) suy ra từ `published_at` của các đánh giá đã công bố cùng `assignment_id`, `class_id` (lần đầu = MIN, gần nhất = MAX). Sau commit phát `grade.published` (bài, lớp, người học vừa có điểm). Bài của môn: mỗi lớp công bố riêng.

## F6 - Sửa điểm
1. Lý do bắt buộc (BR-U15-13, 33); thêm phần tử `history`; event nếu đã công bố.

## F7 - Bài nhóm
1. Chấm tài liệu chung như bài `DOCUMENT`: chấm tay theo rubric của từng phần hoặc nhờ AI đề xuất; điểm tài liệu chung là tổng điểm các phần (BR-U15-40).
2. Điểm đóng góp từng thành viên: mặc định bằng điểm tài liệu chung cho mọi người; giảng viên chấm tay từng người nếu cần, lý do tùy chọn (BR-U15-41…43).

## F8 - Sổ điểm (UC 40) và điểm của Student
1. Giảng viên lớp bấm "Sổ điểm" trên tab Evals: danh sách theo sinh viên đang ghi danh, mỗi sinh viên đóng/mở; gồm bài `GRADED` của lớp và của môn (BR-U15-50); lịch sử sửa điểm mở từ ô điểm (BR-U15-52); nút xuất CSV/XLSX do U16 gắn (BR-U15-55).
2. Student: điểm đã công bố của mình hiện cạnh bài trên Student Assignments, trong Assignment Detail và Submission History (BR-U15-51).

## F9 - Kết quả Practice
1. Quiz luyện tập: U11 gọi `PracticeResultPort.scoreQuiz(attemptId)`, U15 chấm bằng `QuizScorer`. Practice Code Lab và AI chấm Practice Text/Diagram Essay (UC 29): U13 gọi `PracticeResultPort.record(attemptId, result)`. Cả hai ghi `evaluations` `kind = PRACTICE`, `PUBLISHED`, chỉ Student chủ lượt xem; không vào sổ điểm, không qua chốt/công bố (BR-U15-10, 35).

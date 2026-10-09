# U11 Attempt & Submission - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Mọi luồng chỉ dành cho Student (R5: ghi danh `ACTIVE` lớp `OPEN`). Teacher, Subject Manager không có ghi danh nên không làm bài; Admin không vào `/classes/*` (BR-U11-05).

## F1 - Student Assignments (UC 22)
1. Từ Class Dashboard bấm Student Assignments; mở từ Student Class Detail thì lọc sẵn lớp đó (`classId`).
2. U11 lấy các lớp `OPEN` mình đang ghi danh (`ClassAccessPort.listOpenClassesOf`); lọc lớp ngoài phạm vi → "không tìm thấy".
3. Với từng lớp gọi `AssignmentQueryPort.listForClass(classId)`: bài của lớp và bài của môn chứa lớp theo BR-U08-03 (`OPEN`/`CLOSED`, `RETIRED` khi mình đã có điểm); không gồm quiz.
4. Ghép lượt gần nhất của mình cho từng bài (chưa làm, đang làm, đã nộp, trễ, hết hạn); bài nhóm lấy trạng thái nộp của nhóm qua `GroupSubmissionQueryPort` (U14, `C`).
5. Trả danh sách gộp, hạn gần nhất trước, lọc được theo lớp, dạng bài, trạng thái (BR-U11-36). Giao diện gắn điểm đã công bố (U15) và phân bố điểm (U16) cạnh từng bài.

## F2 - Assignment Detail (UC 23)
1. Kiểm BR-U11-05 với `classId` của bài (bài của lớp đó hoặc bài của môn chứa lớp).
2. Trả hướng dẫn, dạng, chế độ, nhãn "Của môn", lịch (mở, đóng, nộp trễ), số lượt đã dùng và còn lại, lượt gần nhất.
3. Text Essay, Code Lab: đề từng câu theo góc nhìn người học (`BankQueryPort.getStudentView`); Diagram Essay, bài nhóm: khung tài liệu (`TypeConfigPort`); tiêu chí chấm: rubric từng câu hoặc từng phần (`RubricPort.getRubric`), điểm test công khai của Code Lab (BR-U11-37).
4. Nút "Bắt đầu làm"/"Tiếp tục" → F4 rồi mở Text Essay Workspace, Codelab Workspace hoặc Diagram Essay Workspace; bài nhóm: nút "Mở bài nhóm" → Group Essay Workspace (U14, UC 27); nút "Lịch sử nộp" → Submission History (F9).

## F3 - Quiz của học liệu và Quiz Practice (UC 15, 18, 19)
1. Learning Material (U05) nhúng `LessonQuizList`: U11 gọi `AssignmentQueryPort.listQuizzesOfLesson(lessonId, classId)` (quiz `OPEN`, học liệu còn hiện trong lớp, BR-U08-63), kèm lượt đã dùng/còn lại và lượt gần nhất; nút "Làm quiz" → F4 rồi Quiz Taking (BR-U11-52).
2. Quiz Practice History (UC 18): từ Class Dashboard; danh sách gộp lượt quiz của mình ở các lớp `OPEN` đang học, nhóm theo quiz, mới nhất trước, lọc theo lớp; bấm quiz → Quiz Practice Detail; bấm lượt đã nộp → Quiz Result; lượt đang làm → Quiz Taking (BR-U11-50).
3. Quiz Practice Detail (UC 19): thông tin quiz, học liệu, lớp, số câu, tổng điểm, giới hạn giờ, lượt đã dùng/còn lại, cài đặt hiện điểm/đáp án; nút "Làm quiz"/"Tiếp tục" → F4 rồi Quiz Taking khi quiz `OPEN`, học liệu còn hiện và còn lượt (BR-U11-51).

## F4 - Bắt đầu lượt (UC 20, 24, 25, 26)
1. Kiểm BR-U11-01, 03, 04, 05; quiz kiểm thêm học liệu còn hiện trong lớp (`ContentRefPort.getLessonRef`).
2. Tạo `Attempt` `IN_PROGRESS` với `classId`, snapshot dạng/chế độ bài, cài đặt quiz, seed trộn và `deadlineAt` (BR-U11-02, 06).
3. Không tạo việc riêng: scanner tự nộp của U11 đọc `deadline_at` (quiz không giới hạn giờ thì `deadline_at` rỗng).
4. Trả đề theo góc nhìn người học (`BankQueryPort.getStudentView`), quiz trộn câu/đáp án theo seed khi cài đặt bật.

## F5 - Lưu nháp
1. Kiểm chủ lượt, `IN_PROGRESS`, thời hạn (BR-U11-14), `contentVersion` (BR-U11-11).
2. Kiểm cấu trúc (BR-U11-12); ghi nội dung (kèm thời điểm lưu), `content_version + 1`.

## F5a - Nhập DOCX vào bản nháp Diagram Essay
1. Kiểm chủ lượt, `IN_PROGRESS`, hạn còn hiệu lực và dạng bài `DIAGRAM_ESSAY` (mô hình DOCUMENT, BR-U09-45).
2. Gọi `DocxStudentImportPort` (U09) trả xem trước và báo cáo, không thay đổi nội dung lượt.
3. Khi xác nhận, thêm block `STUDENT` bằng cùng kiểm `contentVersion` và `validateForSave` như F5; xung đột trả `409`, bản nháp cũ giữ nguyên.

## F6 - Nộp tay
1. Kiểm BR-U11-20.
2. Chuyển `SUBMITTED` theo BR-U11-21: bài `GRADED` gọi `SubmissionSubmittedPort` (U15); quiz gọi `PracticeResultPort.scoreQuiz` (U15 chấm, ghi `evaluations` `kind = PRACTICE`); Practice Code Lab gọi `CodeRunPort.grade` (U13 chạy test sau commit rồi ghi qua U15); Practice Text/Diagram Essay không gọi AI (chờ F10); trả biên nhận; audit.
3. Quiz nộp xong mở Quiz Result (F8); bài tập hiện biên nhận và lối sang Submission History.

## F7 - Tự nộp
1. `AttemptDeadlineScanner` (scanner U03, mỗi phút): lượt còn `IN_PROGRESS` có `deadline_at + 30 s` đã qua → nộp nội dung hiện có với `submit_mode` tương ứng (BR-U11-23, 24).
2. U08 ngưng giao bài, ngưng quiz hoặc phát hành version mới của quiz gọi `AssignmentLifecyclePort.onRetired` (U11 cài) → trong cùng transaction đặt `deadline_at = now() - 30 s`, `submit_mode = AUTO_RETIRED` cho mọi lượt `IN_PROGRESS` của bài; scanner nộp ở lượt quét kế tiếp.
3. Client: đồng hồ về 0 → gửi lần lưu cuối rồi hiện "Đã tự nộp" (quiz chuyển Quiz Result).

## F8 - Quiz Result (UC 21)
1. Kiểm chủ lượt, lượt quiz đã `SUBMITTED`.
2. Trả lựa chọn của mình từng câu; điểm (U15, `GradeQueryPort`) khi `showScoreAfterSubmit`; đáp án đúng và giải thích (`BankQueryPort.getVersion` của phiên bản câu đã ghim) khi `showCorrectAnswers = AFTER_SUBMIT` (BR-U11-54).

## F9 - Submission History (UC 28)
1. Từ Assignment Detail; danh sách lượt của mình cho bài, đánh dấu lượt được chấm (BR-U11-31).
2. Xem lượt đã nộp (chỉ đọc): mặc định lượt gần nhất, nút ‹ › chuyển giữa các lượt; biên nhận; điểm/phản hồi theo BR-U11-33 (BR-U11-38).
3. Tải DOCX (BR-U11-34). Bài nhóm: hiện `GroupSubmissionView` của U14.

## F10 - Chấm AI bài Practice (UC 29)
1. Trên Submission History, Student chọn lượt `PRACTICE` Text/Diagram Essay đã nộp của mình, bấm "Chấm với AI" (`POST /api/v1/attempts/{id}/ai-grading`).
2. U11 kiểm chủ lượt, lượt `SUBMITTED`, dạng/chế độ rồi gọi `PracticeGradingPort` (U13); U13 giữ credit `PRACTICE_GRADING` tại lúc bấm (BR-U11-35, U13 F3a).
3. Thiếu credit → "Không đủ credit AI", không gọi AI; đủ → giao diện poll `GET /api/v1/attempts/{id}/practice-result` tới khi có điểm/phản hồi hoặc lỗi quá 5 phút (bấm lại được).

## F11 - Cho unit khác
- `SubmissionQueryPort`: lượt theo bài và lớp (U15, U16), nội dung (U13, U15), lượt được chấm.
- `AttemptRunResultPort`: U13 ghi kết quả chạy thử/chấm test Code Lab.
- `AssignmentLifecyclePort.onRetired`: U08 gọi khi ngưng giao/ngưng quiz.

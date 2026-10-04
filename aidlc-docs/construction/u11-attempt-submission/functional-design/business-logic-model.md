# U11 Attempt & Submission - Business Logic Model

## F1 - Danh sách bài của người học (UC 29)
1. Lấy bài của lớp đang ghi danh theo BR-U08-03 (U08): `OPEN`/`CLOSED`, và bài `RETIRED` mà người học đã có điểm; kèm lượt gần nhất của người học: chưa làm, đang làm, đã nộp (trễ, điểm nếu được hiện), hết hạn (BR-U11-36).

## F2 - Bắt đầu lượt
1. Kiểm BR-U11-01, 03, 04.
2. Tạo `Attempt` `IN_PROGRESS`, snapshot dạng/chế độ bài, seed và `deadlineAt` (BR-U11-02, 06).
3. Không tạo việc riêng: scanner tự nộp của U11 đọc `deadline_at`.
4. Trả đề theo góc nhìn người học (U06/U08 đã lọc đáp án), đã trộn theo seed.

## F3 - Lưu nháp
1. Kiểm quyền, `IN_PROGRESS`, thời hạn (BR-U11-14), `contentVersion` (BR-U11-11).
2. Kiểm cấu trúc (BR-U11-12); ghi nội dung (kèm thời điểm lưu), `content_version + 1`.

## F3a - Nhập DOCX vào bản nháp DOCUMENT
1. Kiểm chủ lượt, `IN_PROGRESS`, hạn còn hiệu lực và dạng bài `DIAGRAM_ESSAY` (mô hình DOCUMENT, BR-U09-45).
2. Gọi `DocxStudentImportPort` (U09) trả xem trước và báo cáo, không thay đổi nội dung lượt.
3. Khi xác nhận, thêm block `STUDENT` bằng cùng kiểm `contentVersion` và `validateForSave` như F3; xung đột trả `409`, bản nháp cũ giữ nguyên.

## F4 - Nộp tay
1. Kiểm BR-U11-20.
2. Chuyển `SUBMITTED` theo BR-U11-21; chỉ bài `GRADED` gọi `SubmissionSubmittedPort` (U15); Practice Quiz gọi `PracticeResultPort.scoreQuiz` (U15 chấm, ghi `evaluations` `kind = PRACTICE`); Practice Code Lab gọi `CodeRunPort.grade` (U13 chạy test sau commit rồi ghi qua U15); trả biên nhận; audit.
3. Với `PRACTICE` Text/Diagram Essay, nộp xong không gọi AI; lượt ở trạng thái chưa chấm AI. Student bấm "Chấm với AI" trên Submitted Assignment (`POST /api/v1/attempts/{id}/ai-grading`) thì U11 kiểm chủ lượt, lượt `SUBMITTED`, dạng/chế độ rồi gọi `PracticeGradingPort` của U13; kết quả đọc lại bằng `GET /api/v1/attempts/{id}/practice-result` (BR-U11-35, U13 F3a).

## F5 - Tự nộp
1. `AttemptDeadlineScanner` (scanner U03, mỗi phút): lượt còn `IN_PROGRESS` có `deadline_at + 30 s` đã qua → nộp nội dung hiện có với `submit_mode` tương ứng (BR-U11-23, 24).
2. U08 ngưng giao gọi `AssignmentLifecyclePort.onRetired` (U11 cài) → trong cùng transaction đặt `deadline_at = now()`, `submit_mode = AUTO_RETIRED` cho mọi lượt `IN_PROGRESS` của bài; scanner nộp ở lượt quét kế tiếp.
3. Client: đồng hồ về 0 → gửi lần lưu cuối rồi hiện "Đã tự nộp".

## F6 - Lịch sử, biên nhận, xuất DOCX
1. Danh sách lượt của mình, đánh dấu lượt được chấm (BR-U11-31).
2. Xem lượt đã nộp (chỉ đọc): mặc định lượt gần nhất, nút ‹ › chuyển giữa các lượt (BR-U11-36); điểm/đáp án theo BR-U11-33.
3. Tải DOCX (BR-U11-34).

## F7 - Cho giảng viên và unit khác
- `SubmissionQueryPort`: danh sách lượt theo bài (U15, U16), nội dung (U13, U15), lượt được chấm.

# U08 Assessment Core & Publication - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Tab Evals của Teacher Class Detail (Teacher, UC 41)
1. Teacher hoặc Subject Manager được giao dạy lớp mở tab Evals; U08 kiểm `isTeacherOf` (BR-U08-01).
2. Phần Bài tập: bài của lớp và bài của môn chứa lớp (nhãn "Của môn", chỉ đọc), lọc theo dạng/chế độ/trạng thái, mặc định ẩn `CLOSED`/`RETIRED` (BR-U08-42).
3. Phần Quiz: quiz của lớp và quiz của môn (chỉ đọc), kèm học liệu gắn.
4. Bấm bài → Assignment Form; bấm quiz → Quiz Detail; mỗi bài tập có lối vào bài nộp (Submission Detail, U15). Nút "Tạo bài" (F3) và "Tạo quiz" (F9).

## F2 - Assignment List và Quiz List của Subject Manager
1. Từ Manager Dashboard bấm Assignment List hoặc Quiz List, chọn môn mình quản lý; U08 kiểm R2 (BR-U08-01).
2. Assignment List: bài của môn (Text Essay, Diagram Essay, Code Lab), lọc dạng/chế độ/trạng thái; bấm bài → Assignment Form; "Tạo bài" (F3).
3. Quiz List: quiz của môn kèm học liệu; bấm quiz → Quiz Detail; "Tạo quiz" (F9).

## F3 - Tạo và soạn bài tập (UC 42–45)
1. Kiểm quyền: bài của lớp R3/R4, bài của môn R2 (BR-U08-01); chọn dạng và chế độ trước (BR-U08-10, 17); bài của môn không có `GROUP_ASSIGNMENT`.
2. Chọn nguồn: bài trống → tạo `assignments` `DRAFT` với `class_id` hoặc `subject_id`; bài của lớp có thể copy bài lớp khác (F11).
3. Assignment Form: tiêu đề, hướng dẫn, chế độ (U08); Text Essay, Code Lab thêm câu chọn tay từ ngân hàng, chọn ngẫu nhiên, viết câu riêng hoặc nhờ AI (F10), sắp xếp; thêm/bỏ câu Text Essay gọi `TypeConfigPort.syncQuestionRubrics`; khung Diagram Essay/bài nhóm, rubric và cấu hình riêng do U09 soạn trong `TypeConfigSlot` (BR-U08-11, 16, 18).
4. Sửa bài `REVIEWED` → về `DRAFT` (BR-U08-14). Xóa bài chưa từng phát hành (BR-U08-15).

## F4 - Xem trước và duyệt
1. Xem trước như người học.
2. Duyệt: kiểm BR-U08-20 (câu, học liệu của quiz, `TypeConfigPort` gồm rubric đã điền, `CodeLabCheckPort`) → `REVIEWED`; lỗi thì hiện danh sách mục cần sửa; audit.

## F5 - Phát hành bài tập
1. Kiểm `REVIEWED` (BR-U08-22) và quyền; bài của lớp: lớp `OPEN`; bài của môn: môn `ACTIVE`; bài `GROUP_ASSIGNMENT` hỏi `GroupReadinessPort` (U12) (BR-U08-02, 31).
2. Kiểm lịch, nộp trễ, số lượt (BR-U08-31, 32).
3. Một transaction: ghi `opens_at`, `closes_at`, `late_until`, `max_attempts`; `RubricPort.lockForAssignment` (rubric trống → từ chối, trả câu/phần cần điền); bài → `SCHEDULED` (BR-U08-33); audit.

## F6 - Mở/đóng theo lịch (scanner trong worker)
1. Mỗi phút: `UPDATE assignments SET status = 'OPEN' WHERE status = 'SCHEDULED' AND opens_at <= now() RETURNING id`; với từng bài vừa mở: gọi `AssignmentLifecyclePort.onOpened`, phát `assignment.opened` (kèm `classId` hoặc `subjectId`) sau commit (BR-U08-35, 36).
2. `UPDATE ... SET status = 'CLOSED' WHERE status = 'OPEN' AND type <> 'MULTIPLE_CHOICE_QUIZ' AND COALESCE(late_until, closes_at) <= now()`.
3. Câu lệnh có điều kiện nên chạy lại hoặc đổi lịch không gây trùng.

## F7 - Sửa lịch, ngưng giao, nhân bản, version mới
1. Sửa lịch theo BR-U08-34; scanner tự dùng lịch mới; audit.
2. Ngưng giao (BR-U08-40); nhân bản (BR-U08-41).
3. Sửa sau khi đóng/ngưng: kiểm BR-U08-43, 44 → tạo version `DRAFT` mới, sao chép câu, gọi `TypeConfigPort.copy`; audit.

## F8 - Truy vấn cho người học và unit khác
1. Người học: bài của lớp và bài của môn chứa lớp theo BR-U08-03 (U11 dùng cho Student Assignments, UC 22).
2. Quiz của một học liệu trong một lớp theo BR-U08-63 (U11 dùng cho Learning Material và Quiz Practice).
3. `isSubmissionOpen(assignmentId, now)` trả `ON_TIME`, `LATE`, `CLOSED` cho U11; quiz `OPEN` luôn `ON_TIME`.

## F9 - Tạo, soạn và phát hành quiz (UC 35)
1. Teacher bấm "Tạo quiz" trên tab Evals (chọn học liệu của lớp) hoặc Chủ nhiệm môn trên Quiz List (chọn học liệu của môn); U08 kiểm quyền và `ContentRefPort.getLessonRef` (BR-U08-60).
2. Tạo `assignments` `MULTIPLE_CHOICE_QUIZ`, `PRACTICE`, `DRAFT`, có `lesson_id`.
3. Quiz Detail: thêm câu chọn tay từ ngân hàng của môn (`BankQueryPort.search`), chọn ngẫu nhiên (`pickRandom`, BR-U08-18), viết câu riêng (`InlineQuestionPort.save`) hoặc nhờ AI (F10); sắp xếp, đặt điểm; cài đặt quiz do U09 soạn (BR-U08-62).
4. Duyệt như F4; phát hành → `OPEN` ngay, không lịch; version mới được phát hành thì version cũ `RETIRED` (BR-U08-62, 64); audit.
5. Ngưng: `OPEN` → `RETIRED`; sửa quiz đã phát hành tạo version mới (BR-U08-43).

## F10 - AI soạn câu (quiz, Text Essay, Code Lab)
1. Người soạn nhập yêu cầu (số câu, độ khó, module/học liệu) → `AiDraftPort.request` (U13 ghi `ai_suggestions`, giữ credit).
2. Nhận đề xuất kèm trích dẫn → chọn, sửa câu giữ lại → lưu thành câu riêng của bài qua `InlineQuestionPort` và thêm dòng `assignment_questions`; U13 đánh dấu đề xuất `ACCEPTED` (bỏ hết → `DISCARDED`) (BR-U08-21).

## F11 - Copy bài giữa lớp (luồng phụ của UC 42–45)
1. Trong bước chọn nguồn của "Tạo bài", giảng viên chọn "Copy bài lớp khác"; U08 liệt kê lớp mình dạy (trừ lớp đích) và bài của lớp đó cùng dạng, chế độ đã chọn (BR-U08-45).
2. Kiểm giảng viên dạy cả lớp nguồn và lớp đích; ngoài quyền → "không tìm thấy".
3. Một transaction: tạo bài `DRAFT` ở lớp đích (`source_assignment_id`, `config.origin = CLASS_COPY`); sao `assignment_questions` (câu ngân hàng giữ version, câu riêng qua `InlineQuestionPort.copyToAssignment`); `TypeConfigPort.copy` sao cấu hình, khung và nhân bản rubric (BR-U08-46).
4. Không copy lịch, lượt làm, bài nộp, điểm; audit; mở Assignment Form của bài mới.

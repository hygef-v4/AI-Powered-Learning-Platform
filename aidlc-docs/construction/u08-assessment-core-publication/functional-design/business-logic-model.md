# U08 Assessment Core & Publication - Business Logic Model

## F1 - Tạo và soạn bài
1. Kiểm quyền lớp (BR-U08-01); người soạn chọn dạng bài và chế độ trước (BR-U08-10, 17).
1a. Chọn nguồn: bài trống → tạo `assignments` `DRAFT` với `class_id`, dạng, chế độ; copy template hoặc copy bài lớp khác → U10 F2/F3, chỉ liệt kê nguồn cùng dạng và chế độ (BR-U08-17).
2. Thêm câu: chọn tay từ ngân hàng (`BankQueryPort.search`, chỉ `ACTIVE`), chọn ngẫu nhiên N câu (`BankQueryPort.pickRandom(scope, filter, n, excludeIds)`, BR-U08-18) hoặc tạo câu riêng (`InlineQuestionPort.save`, U06 kiểm và lưu) rồi thêm dòng `assignment_questions` (BR-U08-11).
3. Sắp xếp, đặt điểm (BR-U08-12).
3a. Rubric do U09 tạo trong `TypeConfigSlot`: Text Essay mỗi câu một rubric (điểm câu bằng tổng điểm rubric, BR-U08-12), Diagram Essay và bài nhóm mỗi phần một rubric, lưu ở `config` (BR-U08-16). Quiz, Code Lab: không có rubric. Diagram Essay và bài nhóm không thêm câu vào bài (khung ở `config`).
4. Sửa bài `REVIEWED` → về `DRAFT` (BR-U08-14).

## F2 - Tạo bằng AI
1. Giảng viên nhập yêu cầu (loại, số câu, độ khó, module/lesson) → `AiDraftPort.request` (U13 ghi `ai_suggestions`, giữ credit).
2. Nhận đề xuất kèm trích dẫn → giảng viên chọn, sửa câu giữ lại → lưu thành câu riêng của bài qua `InlineQuestionPort` và thêm dòng `assignment_questions`; U13 đánh dấu đề xuất `ACCEPTED` (bỏ hết → `DISCARDED`) (BR-U08-21).
3. Diagram Essay và bài nhóm không có câu: nút AI trong `TypeConfigSlot` nhờ AI soạn khung (U09 F2c).

## F3 - Xem trước và duyệt
1. Xem trước như người học (dùng `QuestionView` của U06).
2. Duyệt: kiểm BR-U08-20 (câu, rubric, `TypeConfigPort`, `CodeLabCheckPort`) → `REVIEWED`; audit.

## F4 - Phát hành
1. Kiểm `REVIEWED` (BR-U08-22), quyền và lớp `OPEN` (BR-U08-02, 31); bài `GROUP_ASSIGNMENT` hỏi `GroupReadinessPort` (U12).
2. Kiểm lịch, nộp trễ, số lượt (BR-U08-31, 32).
3. Ghi `opens_at`, `closes_at`, `late_until`, `max_attempts`; bài → `SCHEDULED` (BR-U08-33); audit.

## F5 - Mở/đóng theo lịch (scanner trong worker)
1. Mỗi phút: `UPDATE assignments SET status = 'OPEN' WHERE status = 'SCHEDULED' AND opens_at <= now() RETURNING id`; với từng bài vừa mở: gọi `AssignmentLifecyclePort.onOpened`, phát `assignment.opened` sau commit (BR-U08-35, 36).
2. `UPDATE ... SET status = 'CLOSED' WHERE status = 'OPEN' AND COALESCE(late_until, closes_at) <= now()`.
3. Câu lệnh có điều kiện nên chạy lại hoặc đổi lịch không gây trùng.

## F6 - Sửa lịch
1. Theo BR-U08-34; scanner tự dùng lịch mới; audit.

## F7 - Ngưng giao, nhân bản, version mới
1. Ngưng giao (BR-U08-40); nhân bản (BR-U08-41).
2. Sửa sau khi đóng/ngưng: kiểm BR-U08-43, 44 → tạo version `DRAFT` mới, sao chép câu, gọi `TypeConfigPort.copy`; audit.

## F8 - Truy vấn
1. Giảng viên: danh sách bài theo lớp/trạng thái, chi tiết, lịch (UC 28).
2. Người học: bài của lớp theo BR-U08-03 (dùng bởi Assignment List của U11).
3. `isSubmissionOpen(assignmentId, now)` trả `ON_TIME`, `LATE`, `CLOSED` cho U11.

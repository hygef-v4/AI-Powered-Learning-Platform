# U06 Rubric & Question Bank - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Question List (Subject Manager, UC 56)
1. Từ Manager Dashboard bấm Question List, chọn một môn mình quản lý; U06 kiểm actor là Chủ nhiệm môn của môn (BR-U06-01).
2. Hiện bản `ACTIVE` mới nhất mỗi câu kèm cờ "có bản nháp" và các câu chỉ mới có bản nháp; lọc theo dạng bài, loại câu, độ khó, tag, module/học liệu, trạng thái, từ khóa tiêu đề; trang ≤ 50 (BR-U06-13).
3. Bấm một câu → Question Detail (F2); bấm "Tạo câu hỏi" → Question Detail trống (F3); có thêm nút "Nhập từ file" (F6) và "Nhờ AI soạn câu hỏi" (F7).

## F2 - Question Detail (Subject Manager, UC 56)
1. Kiểm actor là Chủ nhiệm môn của môn chứa câu; ngoài phạm vi → "không tìm thấy" (BR-U06-04).
2. Hiện nội dung câu theo loại (lựa chọn và đáp án đúng; gợi ý đáp án; khung tài liệu; code mẫu, lời giải mẫu, test), điểm, phân loại, trạng thái và lịch sử phiên bản.
3. Xem trước như người học (ẩn đáp án), bật "hiện đáp án" khi cần.
4. Nút theo trạng thái: Sửa, Kích hoạt, Ngưng dùng, Xóa (F3–F5).

## F3 - Tạo và sửa câu hỏi (UC 57)
1. Kiểm quyền (BR-U06-01).
2. Tạo: `lineage_id` mới, `version = 1`, `DRAFT`, `scope_type = SUBJECT`; hoặc sửa bản `DRAFT` hiện có.
3. Sửa bản `ACTIVE` → sao chép thành `DRAFT` mới cùng `lineage_id`; đã có bản nháp → `409` "đã có bản nháp" (BR-U06-11).
4. Kiểm `definition` ở mức lưu nháp (định dạng, độ dài); khung `DOCUMENT` kiểm qua `DocumentModelPort` (U09); kiểm `lessonRefs` qua `ContentRefPort` của U05 (BR-U06-27).

## F4 - Kích hoạt (UC 57)
1. Kiểm đầy đủ theo loại (BR-U06-20…23, 25…27).
2. `DRAFT` → `ACTIVE`, `activatedAt`; audit. Câu sẵn sàng để chọn vào bài hoặc quiz khớp dạng.

## F5 - Ngưng dùng và xóa (UC 57)
1. Ngưng: `ACTIVE` → `RETIRED`; audit; bài đang dùng vẫn đọc được (BR-U06-14).
2. Xóa: chỉ bản `DRAFT` chưa từng kích hoạt; xóa hẳn dòng; audit. Bản khác chỉ ngưng dùng (BR-U06-15).

## F6 - Nhập từ file (luồng phụ của UC 57)
1. Trên Question List bấm "Nhập từ file", chọn loại câu, tải file mẫu của loại đó.
2. Chọn file `.xlsx` hoặc `.csv`; kiểm giới hạn (BR-U06-40).
3. Mỗi dòng: dựng `definition`, kiểm như F4 bước 1, tạo `DRAFT` (BR-U06-41, 42).
4. Trả bảng kết quả từng dòng; audit một sự kiện.

## F7 - Nhờ AI soạn câu hỏi (luồng phụ của UC 57)
1. Trên Question List bấm "Nhờ AI soạn câu hỏi", chọn loại câu, module/học liệu của môn, số câu, độ khó.
2. Gửi yêu cầu cho U13 (`AiDraftPort`); U13 lấy đoạn học liệu qua RAG của U05, kiểm và trừ credit của người yêu cầu (BR-U06-44).
3. Nhận đề xuất kèm trích dẫn; Chủ nhiệm môn chọn câu giữ lại, có thể sửa.
4. Câu giữ lại lưu thành `DRAFT` như F3; U13 đánh dấu đề xuất đã nhận.

## F8 - Rubric tự tạo và Rubric Detail (Teacher, Subject Manager, UC 46)
1. Tự tạo: khi người soạn thêm một câu Text Essay, hoặc khung Diagram Essay/bài nhóm được chia phần, U09 gọi `RubricPort.create(actor, assignmentId, scope, targetRef)` → U06 tạo rubric trống, chưa khóa; U09 ghi `rubricId` vào câu hoặc phần (BR-U06-34).
2. Tự xóa: khi câu hoặc phần bị bỏ khỏi bài nháp (xóa câu, khung chia lại phần), U09 gọi `RubricPort.delete(rubricId)` (BR-U06-35).
3. Trong Assignment Form, người soạn bấm rubric của một câu hoặc phần → popup Rubric Detail (U09 gắn `RubricEditor` của U06); câu hoặc phần có rubric trống hiện cảnh báo (BR-U06-37).
4. U09 kiểm quyền theo bài (R3/R4 với bài của lớp, R2 với bài cấp môn) và bài còn nháp.
5. Thêm, sửa, xóa tiêu chí và mục rồi Lưu: `RubricPort.update(rubricId, criteria)` ghi đè trực tiếp; kiểm định dạng, cho phép chưa đủ; rubric đã khóa → `409` (BR-U06-30, 33, 35).
6. Phát hành bài: U08 gọi `RubricPort.lockForAssignment(assignmentId)` trong cùng transaction; còn rubric trống hoặc chưa hợp lệ → từ chối phát hành, trả danh sách câu hoặc phần cần điền; đủ thì khóa, từ đó rubric chỉ đọc (BR-U06-33, 37).
7. Nhân bản, tạo version mới hoặc copy bài: U09 gọi `RubricPort.cloneForAssignment(rubricId, targetAssignmentId, targetScope)` → rubric mới chưa khóa cho bài đích (BR-U06-36).

## F9 - Contract cho unit khác
- `BankQueryPort`: U08 tìm và chọn ngẫu nhiên câu `ACTIVE` khớp dạng khi soạn bài/quiz (`search`, `pickRandom`, BR-U08-18); U09 đọc khung câu `DOCUMENT`; U13, U15 đọc câu đầy đủ để chấm (`getVersion`); U11 lấy câu đã bỏ đáp án, gợi ý, test ẩn, lời giải mẫu (`getStudentView`).
- `InlineQuestionPort`: U08 lưu và sao chép câu riêng của bài hoặc quiz (`scope_type = ASSIGNMENT`).
- `RubricPort`: U09 tự tạo/tự xóa theo câu hoặc phần, sửa và nhân bản rubric; U08 kiểm đủ và khóa rubric khi phát hành; U11, U13, U15 đọc rubric và tính điểm (`getRubric`, `score`, BR-U06-32).

# U09 Question Type Authoring - Business Logic Model

## F1 - Cấu hình bài
1. U08 tạo bài → U09 tạo `QuestionTypeConfig` mặc định theo loại.
2. Giảng viên sửa cấu hình khi bài `DRAFT` (BR-U09-01).

## F2 - Soạn khung tài liệu
1. Mở `DocumentEditor` chế độ khung; mọi block tạo ra là `TEACHER`.
2. Thêm/sửa/xóa/sắp xếp block; sơ đồ mở Draw.io nhúng, lưu XML + SVG (BR-U09-35, 38).
3. Lưu khung: kiểm `validateSkeleton`, tính `contentHash` từng block.
4. Lấy khung từ ngân hàng (UC 25, UC 27): chọn câu `DOCUMENT` `ACTIVE` khớp dạng (BR-U06-28), sao khung và `requiredDiagrams` vào `config` của bài làm điểm xuất phát (không thêm dòng `assignment_questions`); giảng viên sửa tiếp, bản ngân hàng không đổi. Heading của khung ngân hàng giữ nguyên nên các phần được tính lại như khung tự soạn.

## F2a - Chia phần và rubric từng phần (Diagram Essay, bài nhóm)
1. Mỗi lần lưu khung, hệ thống dựng cây heading (Tiêu đề 1–6) và lấy heading nhỏ nhất của mỗi nhánh làm phần (BR-U09-25); `config.parts[] = {partId, headingBlockId, ancestorHeadingIds[], title, rubricId}`. Người soạn không bật/tắt phần.
2. Với mỗi phần (heading nhỏ nhất), người soạn tạo rubric trong `PartRubricPanel` (dùng `RubricEditor` của U06) → `RubricPort.createForAssignment`; lưu `rubricId` vào phần (BR-U09-26).
3. Sửa rubric một phần: `RubricPort.revise` rồi ghi `rubricId` mới; U08 gọi `TypeConfigPort.repointRubric(old, new)` khi rubric được sửa ở Question Bank (BR-U09-28).

## F2b - Rubric từng câu (Text Essay)
1. Mỗi câu tự luận trong bài có một rubric: người soạn tạo trong `QuestionRubricPanel` (`RubricEditor` của U06) → `RubricPort.createForAssignment`; lưu `config.questionRubrics[] = {questionId, rubricId}` và đặt điểm của câu bằng tổng điểm rubric (`assignment_questions.points`, qua `AssignmentExtensionPort`) (BR-U09-23).
2. Sửa, nhân bản, chuyển phiên bản như rubric từng phần (BR-U09-27, 28).

## F2c - Khung do AI đề xuất (Diagram Essay, bài nhóm)
1. Người soạn bấm "Nhờ AI soạn khung" (mô tả, module/học liệu tùy chọn) → U09 gọi `AiDraftPort.request` loại `SKELETON_DRAFT` (U13 giữ credit, dùng RAG phạm vi lớp hoặc môn).
2. Poll đề xuất; hiện bản xem trước: cây heading, đoạn hướng dẫn, gợi ý rubric từng phần, trích dẫn (BR-U09-24).
3. Xác nhận: khung đề xuất thay khung hiện tại (cảnh báo nếu đã có nội dung), kiểm `validateSkeleton`, tính lại phần (F2a), U13 chuyển đề xuất `ACCEPTED`; gợi ý rubric điền sẵn vào `PartRubricPanel`, người soạn sửa rồi lưu rubric từng phần như F2a bước 2. Bỏ → `DISCARDED`, khung không đổi.

## F3 - Nhập khung từ DOCX
1. Upload DOCX (tạm, không lưu lâu) (BR-U09-40).
2. Duyệt tài liệu theo thứ tự, dựng block; mỗi ảnh chạy `DiagramDetector` (BR-U09-41, 42).
3. Trả bản xem trước + báo cáo (số sơ đồ nhận được, ảnh giữ nguyên, nội dung bị bỏ) (BR-U09-43).
4. Giảng viên chỉnh rồi lưu như F2 bước 3 (BR-U09-44).

## F3a - Người học nhập DOCX vào lượt DOCUMENT
1. U11 kiểm lượt thuộc người học, `IN_PROGRESS`, còn thời hạn; U09 kiểm tệp bằng `SafeZipGuard` rồi dùng `DocxImporter` hiện có, gắn `origin = STUDENT` cho block mới.
2. Trả xem trước và báo cáo phần bỏ qua, không ghi đè bản nháp. Người học xác nhận thì U11 thêm block qua luồng lưu nháp với `contentVersion` hiện tại; xung đột hoặc lỗi giữ nguyên bản nháp cũ.
3. Kiểm lại `validateForSave`; mọi block `TEACHER` và thứ tự tương đối của chúng không đổi.

## F4 - Kiểm duyệt (`TypeConfigPort`)
- `MULTIPLE_CHOICE_QUIZ`: BR-U09-14. `TEXT_ESSAY`: mọi câu có rubric `ACTIVE` (BR-U09-23). `DIAGRAM_ESSAY`: có khung (bắt buộc), `requiredDiagrams` hợp lệ và mọi phần có rubric `ACTIVE`. `GROUP_ASSIGNMENT`: khung có ít nhất một phần, mọi phần có rubric `ACTIVE` (BR-U09-25…27). `CODE_LAB`: cấu hình câu `CODE` đủ (lời giải mẫu do `CodeLabCheckPort` của U08 kiểm).

## F5 - Kiểm tài liệu của người học (U11 gọi)
- Lưu nháp: `validateForSave` (BR-U09-36).
- Nộp: `validateForSubmit` (BR-U09-37); trả danh sách lỗi theo `blockId`.

## F6 - Xuất DOCX
1. Duyệt block, dựng DOCX bằng POI.
2. Sơ đồ: SVG → PNG (rasterizer server), ghi chunk `tEXt` `mxfile` = XML đầy đủ đã URL-encode (BR-U09-51).

## F7 - Rút gọn XML (U13 gọi)
- `DiagramCompactor.compact(xml)` theo BR-U09-60.

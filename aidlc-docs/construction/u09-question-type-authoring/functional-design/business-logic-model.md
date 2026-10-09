# U09 Question Type Authoring - Business Logic Model

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Cấu hình mặc định và cài đặt quiz (UC 35)
1. U08 tạo bài hoặc quiz → U09 tạo `config` mặc định theo dạng.
2. Quiz Detail: người soạn sửa cài đặt quiz (trộn câu/đáp án, thời gian, hiện điểm, hiện đáp án) khi quiz `DRAFT` (BR-U09-01, 11…13).

## F2 - Rubric từng câu Text Essay (UC 42)
1. U08 thêm câu `ESSAY` (từ ngân hàng hoặc câu riêng) vào bài rồi gọi `TypeConfigPort.syncQuestionRubrics` trong cùng transaction.
2. U09 so danh sách câu: câu mới → `RubricPort.create` rubric trống, ghi `config.questionRubrics[]`; câu bị bỏ → `RubricPort.delete` (BR-U09-23).
3. Assignment Form hiện nút Rubric cạnh từng câu → popup Rubric Detail (F7); điểm câu bằng tổng điểm rubric.

## F3 - Code Lab (UC 43)
1. Câu `CODE` thêm vào bài qua U08 (từ ngân hàng hoặc câu riêng) (BR-U09-16).
2. U09 hiện trạng thái kiểm lời giải mẫu từng câu qua `CodeLabCheckPort` (U13); nút "Kiểm lời giải mẫu" (`VerifySolutionButton` của U13) chạy lời giải với mọi test trong Judge0 (BR-U09-18).
3. Câu riêng của bài sửa nội dung → phải kiểm lại.

## F4 - Soạn khung tài liệu (UC 44, 45)
1. Mở `DocumentEditor` chế độ khung; mọi block tạo ra là `TEACHER`.
2. Thêm/sửa/xóa/sắp xếp block; sơ đồ mở Draw.io nhúng, lưu XML + SVG (BR-U09-35, 38).
3. Lưu khung: kiểm `validateSkeleton`, tính `contentHash` từng block.
3a. Lấy khung từ ngân hàng: chọn câu `DOCUMENT` `ACTIVE` (`QuestionPicker`, `BankQueryPort`), sao khung và `requiredDiagrams` vào `config` làm điểm xuất phát; bản ngân hàng không đổi.
4. Dựng cây heading (Tiêu đề 1–6), lấy heading nhỏ nhất của mỗi nhánh làm phần (BR-U09-25); `config.parts[] = {partId, headingBlockId, ancestorHeadingIds[], title, rubricId}`.
5. So với danh sách phần cũ: phần mới → `RubricPort.create` rubric trống; phần không còn → `RubricPort.delete` (BR-U09-26, 27).
6. Bài nhóm: khung có ít nhất một phần; U14 dựng mỗi phần thành một mục khi bài mở.

## F5 - Nhập khung từ DOCX
1. Upload DOCX (tạm, không lưu lâu) (BR-U09-40).
2. Duyệt tài liệu theo thứ tự, dựng block; mỗi ảnh chạy `DiagramDetector` (BR-U09-41, 42).
3. Trả bản xem trước + báo cáo (số sơ đồ nhận được, ảnh giữ nguyên, nội dung bị bỏ) (BR-U09-43).
4. Người soạn chỉnh rồi lưu như F4 bước 3–5 (BR-U09-44).

## F6 - AI soạn khung (Diagram Essay, bài nhóm)
1. Bấm "Nhờ AI soạn khung" (mô tả, module/học liệu tùy chọn) → `AiDraftPort.request` loại `SKELETON_DRAFT` (U13 giữ credit, RAG phạm vi lớp hoặc môn); xem trước cây heading, đoạn hướng dẫn, gợi ý rubric, trích dẫn; xác nhận thì thay khung, tính lại phần như F4 bước 4–5, gợi ý rubric điền vào rubric của từng phần; bỏ thì khung không đổi (BR-U09-24).
2. AI soạn câu Text Essay, Code Lab, quiz do U08 xử lý (BR-U08-21).

## F7 - Rubric Detail (UC 46)
1. Trong Assignment Form, bấm rubric của một câu Text Essay hoặc một phần → popup Rubric Detail gắn `RubricEditor` của U06; rubric đã có sẵn (tự tạo).
2. Kiểm quyền theo bài và bài `DRAFT`; thêm, sửa, xóa tiêu chí và mục; Lưu → `RubricPort.update` (ghi đè) (BR-U06-35).
3. Câu hoặc phần có rubric trống hiện cảnh báo; bài đã phát hành thì popup chỉ đọc (rubric đã khóa).

## F8 - Kiểm duyệt (`TypeConfigPort.check`, U08 gọi)
- Quiz: cài đặt hợp lệ (BR-U09-14).
- Text Essay: mọi câu có rubric đã điền hợp lệ (BR-U09-23); số câu do U08 kiểm.
- Code Lab: không có cấu hình riêng; kết quả kiểm lời giải do U08 hỏi `CodeLabCheckPort` của U13 (BR-U09-18).
- Diagram Essay: có khung, `requiredDiagrams` hợp lệ, mọi rubric phần đã điền hợp lệ.
- Bài nhóm: khung có ít nhất một phần, mọi rubric phần đã điền hợp lệ (BR-U09-25…27).
- Trả danh sách lỗi theo câu, đề hoặc phần.

## F9 - Nhân bản, version, copy giữa lớp (`TypeConfigPort.copy`, U08 gọi)
1. Sao `config` (cài đặt quiz, `questionRubrics`, khung, phần) sang bài đích; câu do U08 sao.
2. Nhân bản rubric của từng câu/phần qua `RubricPort.cloneForAssignment`, ghi `rubricId` mới (BR-U09-28).

## F10 - Người học nhập DOCX vào lượt DOCUMENT (U11 gọi)
1. U11 kiểm lượt thuộc người học, `IN_PROGRESS`, còn thời hạn; U09 kiểm tệp bằng `SafeZipGuard` rồi dùng `DocxImporter`, gắn `origin = STUDENT` cho block mới.
2. Trả xem trước và báo cáo phần bỏ qua, không ghi đè bản nháp. Người học xác nhận thì U11 thêm block qua luồng lưu nháp với `contentVersion` hiện tại; xung đột hoặc lỗi giữ nguyên bản nháp cũ.
3. Kiểm lại `validateForSave`; mọi block `TEACHER` và thứ tự tương đối của chúng không đổi.

## F11 - Kiểm tài liệu của người học (U11 gọi)
- Lưu nháp: `validateForSave` (BR-U09-36).
- Nộp: `validateForSubmit` (BR-U09-37); trả danh sách lỗi theo `blockId`.

## F12 - Xuất DOCX
1. Duyệt block, dựng DOCX bằng POI.
2. Sơ đồ: SVG → PNG (rasterizer server), ghi chunk `tEXt` `mxfile` = XML đầy đủ đã URL-encode (BR-U09-51).

## F13 - Rút gọn XML (U13 gọi)
- `DiagramCompactor.compact(xml)` theo BR-U09-60.

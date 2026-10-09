# U09 Question Type Authoring - Business Rules

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Chung

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-01 | Nội dung dạng bài chỉ sửa khi bài `DRAFT`; người sửa là người có quyền với bài theo U08: Teacher hoặc Subject Manager được giao dạy lớp (R3/R4) với bài của lớp, Chủ nhiệm môn (R2) với bài của môn. Giảng viên lớp chỉ xem nội dung bài của môn. | BR-U08-01, 02 |
| BR-U09-02 | Bài của môn có Text Essay, Diagram Essay, Code Lab (không có bài nhóm); bài nhóm chỉ ở lớp. | BR-U08-10; người dùng chốt 2026-10-09 |
| BR-U09-03 | U09 soạn phần riêng của từng dạng trên Assignment Form (`TypeConfigSlot` của U08): rubric từng câu Text Essay, trạng thái kiểm lời giải mẫu Code Lab, khung Diagram Essay và bài nhóm, rubric từng phần (popup Rubric Detail); và cài đặt quiz trên Quiz Detail (`QuizSettingsSlot` của U08). Câu của quiz, Text Essay, Code Lab (từ ngân hàng của môn hoặc câu riêng của bài) do U08 và U06 quản lý. Danh sách, tạo, duyệt, phát hành, nhân bản, version, ngưng giao theo U08. U13 chạy kiểm lời giải mẫu Code Lab; U14 dựng tài liệu nhóm khi bài mở. | FR-017; người dùng chốt 2026-10-09 |

## 2. Cài đặt quiz luyện tập

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-10 | `MCQ_MULTI` chấm đúng hết mới có điểm (chọn đủ đáp án đúng và không chọn sai). | Câu 1 |
| BR-U09-11 | Trộn câu / trộn đáp án: mỗi lượt làm một thứ tự, lưu seed trên lượt làm (U11). | Câu 2 |
| BR-U09-12 | Giới hạn thời gian (tùy chọn, 1-300 phút) tính từ lúc bắt đầu lượt; hết giờ U11 tự nộp. Quiz không có hạn đóng. | Câu 2; BR-U08-61 |
| BR-U09-13 | `showScoreAfterSubmit` (bool) và `showCorrectAnswers` (`NEVER`, `AFTER_SUBMIT`). | Câu 2; người dùng chốt 2026-10-09 |
| BR-U09-14 | Duyệt quiz: U08 kiểm câu và điểm (BR-U08-20); U09 kiểm cài đặt hợp lệ. | US-ASM-006 S2 |

## 3. Text Essay

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-15 | Câu của Text Essay là câu `ESSAY` ở `assignment_questions` của U08 (từ ngân hàng của môn hoặc câu riêng của bài); 1-20 câu. | BR-U08-11, 13 |
| BR-U09-20 | Bài viết văn bản thường có định dạng cơ bản (đoạn, heading, danh sách, đậm/nghiêng); không bảng, ảnh, sơ đồ. | Câu 9 |
| BR-U09-21 | Không giới hạn số từ hay số dòng; chỉ có trần kỹ thuật 1 000 000 ký tự để bảo vệ hệ thống. | Câu 9; người dùng chốt 2026-10-04 |
| BR-U09-22 | Ngôn ngữ tự nhiên nào cũng được; không phải cấu hình riêng. | US-ASM-007 S2 |
| BR-U09-23 | Mỗi câu Text Essay của bài có đúng một rubric: khi U08 thêm hoặc bỏ câu, U08 gọi `TypeConfigPort.syncQuestionRubrics`; U09 gọi `RubricPort.create` tạo rubric trống cho câu mới, `RubricPort.delete` cho câu bị bỏ, ghi `config.questionRubrics[] = {questionId, rubricId}`. Điểm tối đa của câu bằng tổng điểm rubric của câu (tính khi đọc); điểm bài là tổng các câu. Quiz và Code Lab không có rubric. | BR-U06-34, 35; người dùng chốt 2026-10-09 |

## 3a. Code Lab

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-16 | Câu của Code Lab là câu `CODE` ở `assignment_questions` của U08 (từ ngân hàng của môn hoặc câu riêng của bài), theo BR-U06-23; 1-20 câu; điểm câu bằng tổng điểm test. U09 không có cấu hình riêng cho Code Lab ngoài việc hiện trạng thái kiểm lời giải mẫu từng câu. | BR-U06-23, BR-U08-11 |
| BR-U09-18 | Duyệt Code Lab cần mọi câu có lời giải mẫu đã được U13 chạy qua hết test; U13 lưu kết quả theo `contentHash` của phiên bản câu và trả lời qua `CodeLabCheckPort`; câu riêng của bài sửa nội dung thì phải kiểm lại. | BR-U08-20 |

## 4. Bài tài liệu (`DOCUMENT`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-30 | Diagram Essay luôn có khung của người soạn (bắt buộc, không có bài trang trắng); không giới hạn số từ. Khung lưu ở `assignments.config`; có thể bắt đầu bằng cách sao khung và `requiredDiagrams` từ câu `DOCUMENT` `ACTIVE` của ngân hàng (bản ngân hàng không đổi). Diagram Essay và bài nhóm không có dòng `assignment_questions`. | Câu 6, 9; người dùng chốt 2026-10-04 |
| BR-U09-31 | Toàn bộ khung của người soạn bị khóa: người học không sửa, không xóa, không di chuyển bất kỳ block `TEACHER` nào (chữ, ảnh, bảng, sơ đồ). Người học chỉ thêm và sửa block của mình (gồm bảng, sơ đồ của mình). Áp dụng cho Diagram Essay và bài nhóm. | Câu 6, 11; người dùng chốt 2026-10-04 |
| BR-U09-32 | Người học chèn block của mình ở bất kỳ vị trí nào. | Câu 11 |
| BR-U09-33 | `requiredDiagrams` (tùy chọn): mỗi loại 1-20 sơ đồ tối thiểu; kiểm lúc nộp. | demo_do_an |
| BR-U09-34 | Trần kỹ thuật: ≤ 2 000 block, ≤ 100 sơ đồ, mỗi XML sơ đồ ≤ 2 MB, mỗi ảnh ≤ 5 MB. | Thiết kế |
| BR-U09-35 | XML sơ đồ phải qua parser an toàn (không DOCTYPE/XXE/XInclude; gốc `mxfile` hoặc `mxGraphModel`). | SEC-003, US-ASM-004 S2 |
| BR-U09-36 | Lưu nháp: kiểm cấu trúc, id block duy nhất, block khung khóa không bị đổi (so `contentHash`). | US-ASM-004 S2 |
| BR-U09-37 | Nộp: thêm kiểm mọi block khung còn đủ, mọi sơ đồ không rỗng, đủ `requiredDiagrams`, tài liệu có nội dung của người học. | US-ASM-004 S1 |
| BR-U09-38 | Sơ đồ luôn hiển thị bằng SVG xem trước; không bao giờ hiện XML cho người dùng. Bấm vào mở Draw.io nhúng. | demo_do_an |

## 4a. Phần của khung và rubric (Diagram Essay, bài nhóm)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-25 | Heading của khung tạo cây cha–con theo cấp như Word: Tiêu đề 1 chứa các Tiêu đề 2 đứng sau nó tới Tiêu đề 1 kế tiếp, Tiêu đề 2 chứa các Tiêu đề 3, v.v. Phần là heading nhỏ nhất của mỗi nhánh (lá của cây), hệ thống tự tính, người soạn không bật/tắt. Ví dụ H1 có nhánh H2.1 chứa H3.1, H3.2 và nhánh H2.2 không có H3 → các phần là H3.1, H3.2 và H2.2. Block trước heading đầu tiên, Tiêu đề và Phụ đề là phần chung; khung không có heading được coi là một phần. Cây phần chỉ dùng cho rubric (BR-U09-26) và giao phần ở bài nhóm (U14). Mọi màn xem tài liệu khác hiện toàn bộ tài liệu. | người dùng chốt 2026-10-04 |
| BR-U09-26 | Mỗi phần có đúng một rubric, hệ thống tự tạo trống qua `RubricPort.create` khi phần xuất hiện; `rubricId` lưu trong `config.parts[]`. Điểm tối đa của phần là tổng điểm rubric của phần; điểm bài là tổng các phần. | BR-U06-34; người dùng chốt 2026-10-04, 2026-10-09 |
| BR-U09-27 | Khi bài còn `DRAFT`, mỗi lần lưu khung tính lại danh sách phần: phần mới (heading mới trở thành heading nhỏ nhất) được tự tạo rubric trống; phần không còn (heading bị xóa hoặc có thêm heading con) thì rubric của nó bị xóa (`RubricPort.delete`). Phần giữ `partId` theo id của block heading nên rubric đã điền được giữ khi heading còn. Duyệt (`TypeConfigPort.check`) cần mọi rubric đã điền hợp lệ. | BR-U06-35, 37; người dùng chốt 2026-10-09 |
| BR-U09-28 | Nhân bản, version mới hoặc copy giữa lớp: `TypeConfigPort.copy` sao cấu hình, khung, `questionRubrics`, `parts` và nhân bản rubric của từng câu hoặc phần (`RubricPort.cloneForAssignment`), ghi `rubricId` mới. | BR-U06-36 |
| BR-U09-29 | Kiểu đoạn trong trình soạn giống Word: Văn bản thường, Tiêu đề, Phụ đề, Tiêu đề 1–6. Chỉ Tiêu đề 1–6 tạo cấu trúc phần; Tiêu đề và Phụ đề là tiêu đề tài liệu, thuộc phần chung. | người dùng chốt 2026-10-04 |
| BR-U09-24 | AI soạn khung (Diagram Essay, bài nhóm): người soạn gửi yêu cầu qua `AiDraftPort` (U13); AI đề xuất cây heading Tiêu đề 1–6, đoạn hướng dẫn dưới mỗi heading và gợi ý rubric cho từng phần, kèm trích dẫn học liệu, không có sơ đồ. Xác nhận thì khung đề xuất thay toàn bộ khung hiện tại (cảnh báo nếu khung đã có nội dung), phần tính lại (BR-U09-27), gợi ý rubric điền vào rubric tự tạo của từng phần để người soạn sửa. Bỏ đề xuất thì khung không đổi. | người dùng chốt 2026-10-04 |

## 5. Nhập khung từ DOCX (người soạn)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-40 | Nhận `.docx` ≤ 20 MB; chuyển heading theo style của Word (Title → Tiêu đề, Subtitle → Phụ đề, Heading 1-6 → Tiêu đề 1-6), đoạn văn, danh sách, bảng, ảnh thành block `TEACHER`. | Câu 6, 7 |
| BR-U09-41 | Ảnh PNG có chunk `tEXt`/`zTXt`/`iTXt` khóa `mxfile` (hoặc `mxGraphModel` cũ), hoặc SVG có thuộc tính `content` chứa `mxfile` → giải mã (URL-decode, base64 + inflate nếu nén) → qua BR-U09-35 → thành block `DIAGRAM` (loại `OTHER`, người soạn đổi được). **Không dùng AI.** | Câu 6, 10 |
| BR-U09-42 | Ảnh không có dữ liệu Draw.io, hoặc giải mã/kiểm lỗi → giữ nguyên là block `IMAGE`. | Câu 6 |
| BR-U09-43 | Nội dung không hỗ trợ (textbox, SmartArt, công thức, header/footer) bỏ qua và liệt kê trong báo cáo nhập. | Thiết kế |
| BR-U09-44 | Kết quả nhập là bản xem trước; người soạn sửa/xóa block rồi mới lưu làm khung. | Thiết kế |

## 5a. Người học nhập DOCX vào lượt DOCUMENT

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-45 | Chỉ người học sở hữu lượt `DOCUMENT` đang `IN_PROGRESS` được nhập `.docx` ≤ 20 MB trên Diagram Essay Workspace; U11 kiểm quyền và thời hạn trước khi gọi U09. Không áp dụng cho Text Essay, quiz hoặc bài đã nộp. | US-ASM-004 S4 |
| BR-U09-46 | Dùng cùng bộ đọc DOCX an toàn BR-U09-40…43; block nhập có `origin = STUDENT`. Trả bản xem trước và báo cáo phần bỏ qua; không lưu bài chỉ vì tải file lên. | US-ASM-004 S4 |
| BR-U09-47 | Sau xác nhận, thêm block nhập vào bản nháp đang làm theo `contentVersion` của U11; không thay, xóa, đổi thứ tự hay đổi `contentHash` của block `TEACHER`. Xung đột phiên bản hoặc lỗi nhập giữ nguyên bản nháp. | US-ASM-004 S4, BR-U09-36 |
| BR-U09-48 | Nếu DOCX chứa sơ đồ Draw.io, chỉ lưu XML sau khi qua BR-U09-35; ảnh không nhận được XML giữ dạng `IMAGE`. Kết quả vẫn phải qua `validateForSave`, và khi nộp qua `validateForSubmit`. | BR-U09-35…37 |

## 6. Xuất DOCX

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-50 | Xuất tài liệu (khung + phần người học) ra `.docx` giữ heading, đoạn, danh sách, bảng, ảnh. | Câu 7 |
| BR-U09-51 | Sơ đồ xuất thành ảnh PNG dựng từ SVG, **nhúng lại XML Draw.io** vào chunk `tEXt` `mxfile` để mở lại được trong draw.io. | Câu 10 |
| BR-U09-52 | Người học xuất bài của mình; giảng viên xuất bài của sinh viên lớp mình dạy (U11/U15 kiểm quyền rồi gọi `DocxExportPort`). | Câu 7 |

## 7. Rút gọn XML cho AI

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-60 | Chỉ tạo khi có yêu cầu AI chấm: giảng viên nhờ AI đề xuất bài `GRADED` hoặc Student chấm bài `PRACTICE` (U13 gọi): giữ `mxCell` `id`, `value`, `parent`, `source`, `target`, `vertex`, `edge` và phần `style` quy định loại hình; bỏ tọa độ, màu, font. Bản đầy đủ không đổi. | US-ASM-004 S3 |

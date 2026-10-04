# U09 Question Type Authoring - Business Rules

## 1. Chung

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-01 | Cấu hình chỉ sửa khi bài `DRAFT`; người sửa là giảng viên của lớp (theo BR-U08-01). | U08 |
| BR-U09-02 | Không có đề chung cấp môn; Chủ nhiệm môn chỉ phát hành khi là giảng viên của lớp. | Câu 4, 8, 12 |
| BR-U09-03 | Dạng assignment hiện hành: `MULTIPLE_CHOICE_QUIZ`, `TEXT_ESSAY`, `DIAGRAM_ESSAY`, `CODE_LAB`, `GROUP_ASSIGNMENT`, cả năm do U09 soạn cấu hình (UC 23–27, quyết định 2026-10-04); U13 chạy kiểm lời giải mẫu Code Lab qua `CodeLabCheckPort`, U14 dựng tài liệu nhóm khi bài mở. UC 23–27 kế thừa UC 28 Manage Assignments (U08): danh sách, nguồn tạo, chọn câu ngân hàng, duyệt, phát hành, nhân bản, version, ngưng giao theo U08; U09 chỉ soạn phần riêng của dạng bài. `DIAGRAM_ESSAY` dùng mô hình `DOCUMENT`/Draw.io hiện có; tên `QUIZ`/`ESSAY`/`DOCUMENT` vẫn có thể là loại câu hỏi ngân hàng U06. | FR-017, quyết định 2026-09-29 |

## 2. Trắc nghiệm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-10 | `MCQ_MULTI` chấm đúng hết mới có điểm (chọn đủ đáp án đúng và không chọn sai). | Câu 1 |
| BR-U09-11 | Trộn câu / trộn đáp án: mỗi lượt làm một thứ tự, lưu seed trên lượt làm (U11). | Câu 2 |
| BR-U09-12 | Giới hạn thời gian tính từ lúc bắt đầu lượt, không vượt hạn đóng; hết giờ U11 tự nộp. | Câu 2 |
| BR-U09-13 | `showScoreAfterSubmit` và `showCorrectAnswers` (`NEVER`, `AFTER_SUBMIT`, `AFTER_CLOSE`). | Câu 2 |
| BR-U09-14 | Duyệt `QUIZ` cần mọi câu có đáp án hợp lệ và điểm > 0; báo đúng câu lỗi. | US-ASM-006 S2 |

## 3. Bài viết (`ESSAY`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-20 | Bài viết văn bản thường có định dạng cơ bản (đoạn, heading, danh sách, đậm/nghiêng); không bảng, ảnh, sơ đồ. | Câu 9 |
| BR-U09-21 | Không giới hạn số từ hay số dòng; chỉ có trần kỹ thuật 1 000 000 ký tự để bảo vệ hệ thống. | Câu 9; người dùng chốt 2026-10-04 |
| BR-U09-22 | Ngôn ngữ tự nhiên nào cũng được; không phải cấu hình riêng. | US-ASM-007 S2 |
| BR-U09-23 | Text Essay có một rubric cho mỗi câu tự luận: `config.questionRubrics[] = {questionId, rubricId}`, tạo qua `RubricPort.createForAssignment`; điểm tối đa của câu bằng tổng điểm rubric của câu (U09 ghi `assignment_questions.points` qua `AssignmentExtensionPort` mỗi khi tạo hoặc sửa rubric, không nhập tay); điểm bài là tổng điểm các câu. Mọi rubric của bài (từng câu hoặc từng phần) lưu trong `config`, không có cột `assignments.rubric_id`. Quiz và Code Lab chấm tự động theo đáp án/test, không có rubric. | người dùng chốt 2026-10-04 |

## 4. Bài tài liệu (`DOCUMENT`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-30 | Diagram Essay luôn có khung của giảng viên (bắt buộc, không có bài trang trắng); không giới hạn số từ. Khung lưu ở `assignments.config`; Diagram Essay và bài nhóm không có dòng `assignment_questions`, câu `DOCUMENT` ở ngân hàng chỉ là nguồn để sao khung. | Câu 6, 9; người dùng chốt 2026-10-04 |
| BR-U09-31 | Toàn bộ khung của giảng viên bị khóa: người học không sửa, không xóa, không di chuyển bất kỳ block `TEACHER` nào (chữ, ảnh, bảng, sơ đồ). Người học chỉ thêm và sửa block của mình (gồm bảng, sơ đồ của mình). Áp dụng cho Diagram Essay và bài nhóm. | Câu 6, 11; người dùng chốt 2026-10-04 |
| BR-U09-32 | Người học chèn block của mình ở bất kỳ vị trí nào. | Câu 11 |
| BR-U09-33 | `requiredDiagrams` (tùy chọn): mỗi loại 1-20 sơ đồ tối thiểu; kiểm lúc nộp. | demo_do_an |
| BR-U09-34 | Trần kỹ thuật: ≤ 2 000 block, ≤ 100 sơ đồ, mỗi XML sơ đồ ≤ 2 MB, mỗi ảnh ≤ 5 MB. | Thiết kế |
| BR-U09-35 | XML sơ đồ phải qua parser an toàn (không DOCTYPE/XXE/XInclude; gốc `mxfile` hoặc `mxGraphModel`). | SEC-003, US-ASM-004 S2 |
| BR-U09-36 | Lưu nháp: kiểm cấu trúc, id block duy nhất, block giảng viên khóa không bị đổi (so `contentHash`). | US-ASM-004 S2 |
| BR-U09-37 | Nộp: thêm kiểm mọi block giảng viên còn đủ, mọi sơ đồ không rỗng, đủ `requiredDiagrams`, tài liệu có nội dung của người học. | US-ASM-004 S1 |
| BR-U09-38 | Sơ đồ luôn hiển thị bằng SVG xem trước; không bao giờ hiện XML cho người dùng. Bấm vào mở Draw.io nhúng. | demo_do_an |

## 4a. Phần của khung và rubric (Diagram Essay, bài nhóm)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-25 | Heading của khung tạo cây cha–con theo cấp như Word: Tiêu đề 1 chứa các Tiêu đề 2 đứng sau nó tới Tiêu đề 1 kế tiếp, Tiêu đề 2 chứa các Tiêu đề 3, v.v. Phần là heading nhỏ nhất của mỗi nhánh (lá của cây), hệ thống tự tính, người soạn không bật/tắt. Ví dụ H1 có nhánh H2.1 chứa H3.1, H3.2 và nhánh H2.2 không có H3 → các phần là H3.1, H3.2 và H2.2. Block trước heading đầu tiên, Tiêu đề và Phụ đề là phần chung; khung không có heading được coi là một phần. Cây phần chỉ dùng cho hai việc: rubric (BR-U09-26) và giao phần ở bài nhóm (U14). Mọi màn xem tài liệu khác hiện toàn bộ tài liệu. | người dùng chốt 2026-10-04 |
| BR-U09-26 | Rubric cũng theo heading nhỏ nhất: mỗi phần có đúng một rubric, tạo qua `RubricPort.createForAssignment` (cùng phạm vi bài); `rubricId` lưu trong `config.parts[]`. Điểm tối đa của phần là tổng điểm rubric của phần; điểm bài là tổng điểm các phần. | người dùng chốt 2026-10-04 |
| BR-U09-27 | Khi bài còn `DRAFT`, sửa heading làm danh sách phần tính lại (với Text Essay: thêm câu thì phải tạo rubric cho câu đó, bỏ câu thì rubric của câu không còn gắn vào bài): heading mới trở thành heading nhỏ nhất thì phải tạo rubric cho phần đó; heading có thêm heading con thì không còn là phần, rubric của nó không còn gắn vào bài. Phần giữ `partId` theo id của block heading. Duyệt (`TypeConfigPort.check`) cần mọi phần có rubric `ACTIVE`. | người dùng chốt 2026-10-04 |
| BR-U09-28 | Nhân bản, version mới, copy template hoặc copy lớp: `TypeConfigPort.copy` nhân bản rubric của từng phần hoặc từng câu (`RubricPort.cloneForAssignment`) và ghi `rubricId` mới. Sửa rubric của một phần hoặc một câu khi bài `DRAFT` chuyển sang phiên bản mới (`TypeConfigPort.repointRubric`, BR-U06-36). | BR-U06-35, 36 |
| BR-U09-29 | Kiểu đoạn trong trình soạn giống Word: Văn bản thường, Tiêu đề, Phụ đề, Tiêu đề 1–6. Chỉ Tiêu đề 1–6 tạo cấu trúc phần; Tiêu đề và Phụ đề là tiêu đề tài liệu, thuộc phần chung. | người dùng chốt 2026-10-04 |
| BR-U09-24 | AI soạn khung (Diagram Essay, bài nhóm; Assignment Editor và Template Editor): người soạn gửi yêu cầu qua `AiDraftPort` (U13); AI đề xuất cây heading Tiêu đề 1–6, đoạn hướng dẫn dưới mỗi heading và gợi ý rubric cho từng phần, kèm trích dẫn học liệu, không có sơ đồ. Người soạn xem trước; xác nhận thì khung đề xuất thay toàn bộ khung hiện tại (cảnh báo nếu khung đã có nội dung), phần tính lại (BR-U09-25), gợi ý rubric điền sẵn vào `PartRubricPanel` để người soạn sửa rồi mới tạo rubric qua `RubricPort.createForAssignment`. Bỏ đề xuất thì khung không đổi. | người dùng chốt 2026-10-04 |

## 5. Nhập khung từ DOCX (giảng viên)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-40 | Nhận `.docx` ≤ 20 MB; chuyển heading theo style của Word (Title → Tiêu đề, Subtitle → Phụ đề, Heading 1-6 → Tiêu đề 1-6), đoạn văn, danh sách, bảng, ảnh thành block `TEACHER`. | Câu 6, 7 |
| BR-U09-41 | Ảnh PNG có chunk `tEXt`/`zTXt`/`iTXt` khóa `mxfile` (hoặc `mxGraphModel` cũ), hoặc SVG có thuộc tính `content` chứa `mxfile` → giải mã (URL-decode, base64 + inflate nếu nén) → qua BR-U09-35 → thành block `DIAGRAM` (loại `OTHER`, giảng viên đổi được). **Không dùng AI.** | Câu 6, 10 |
| BR-U09-42 | Ảnh không có dữ liệu Draw.io, hoặc giải mã/kiểm lỗi → giữ nguyên là block `IMAGE`. | Câu 6 |
| BR-U09-43 | Nội dung không hỗ trợ (textbox, SmartArt, công thức, header/footer) bỏ qua và liệt kê trong báo cáo nhập. | Thiết kế |
| BR-U09-44 | Kết quả nhập là bản xem trước; giảng viên sửa/xóa block rồi mới lưu làm khung. | Thiết kế |

## 5a. Người học nhập DOCX vào lượt DOCUMENT

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-45 | Chỉ người học sở hữu lượt `DOCUMENT` đang `IN_PROGRESS` được nhập `.docx` ≤ 20 MB; U11 kiểm quyền và thời hạn trước khi gọi U09. Không áp dụng cho `ESSAY`, `QUIZ` hoặc bài đã nộp. | US-ASM-004 S4 |
| BR-U09-46 | Dùng cùng bộ đọc DOCX an toàn BR-U09-40…43; block nhập có `origin = STUDENT`. Trả bản xem trước và báo cáo phần bỏ qua; không lưu bài chỉ vì tải file lên. | US-ASM-004 S4 |
| BR-U09-47 | Sau xác nhận, thêm block nhập vào bản nháp đang làm theo `contentVersion` của U11; không thay, xóa, đổi thứ tự hay đổi `contentHash` của block `TEACHER`. Xung đột phiên bản hoặc lỗi nhập giữ nguyên bản nháp. | US-ASM-004 S4, BR-U09-36 |
| BR-U09-48 | Nếu DOCX chứa sơ đồ Draw.io, chỉ lưu XML sau khi qua BR-U09-35; ảnh không nhận được XML giữ dạng `IMAGE`. Kết quả vẫn phải qua `validateForSave`, và khi nộp qua `validateForSubmit`. | BR-U09-35…37 |

## 6. Xuất DOCX

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-50 | Xuất tài liệu (khung + phần người học) ra `.docx` giữ heading, đoạn, danh sách, bảng, ảnh. | Câu 7 |
| BR-U09-51 | Sơ đồ xuất thành ảnh PNG dựng từ SVG, **nhúng lại XML Draw.io** vào chunk `tEXt` `mxfile` để mở lại được trong draw.io. | Câu 10 |
| BR-U09-52 | Người học xuất bài của mình; giảng viên xuất bài trong lớp mình dạy (U11/U15 kiểm quyền rồi gọi `DocxExportPort`). | Câu 7 |

## 7. Rút gọn XML cho AI

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-60 | Chỉ tạo khi có yêu cầu AI chấm: giảng viên nhờ AI đề xuất bài `GRADED` hoặc Student chấm bài `PRACTICE` (U13 gọi): giữ `mxCell` `id`, `value`, `parent`, `source`, `target`, `vertex`, `edge` và phần `style` quy định loại hình; bỏ tọa độ, màu, font. Bản đầy đủ không đổi. | US-ASM-004 S3 |

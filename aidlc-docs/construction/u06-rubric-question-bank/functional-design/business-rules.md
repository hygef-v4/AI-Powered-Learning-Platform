# U06 Rubric & Question Bank - Business Rules

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Phạm vi và quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-01 | Ngân hàng câu hỏi chỉ có ở cấp môn (Subject Question Bank, UC 56–57): Chủ nhiệm môn của môn (R2) xem, tạo, sửa, xóa trên Question List và Question Detail mở từ Manager Dashboard. Không có ngân hàng câu hỏi của lớp; Admin không vào ngân hàng. | UC 56, 57; người dùng chốt 2026-10-09 |
| BR-U06-02 | Ngân hàng của môn dùng cho mọi dạng bài: quiz ghim câu `MCQ_*`, Text Essay ghim câu `ESSAY`, Code Lab ghim câu `CODE`; Diagram Essay và bài nhóm sao khung từ câu `DOCUMENT`. Khi soạn trên Quiz Detail hoặc Assignment Form, Teacher hoặc Subject Manager được giao dạy một lớp của môn chọn được câu `ACTIVE` của ngân hàng môn (chỉ đọc) hoặc viết câu riêng của bài; Chủ nhiệm môn soạn bài/quiz của môn dùng ngân hàng của môn mình. Không sửa ngân hàng môn chỉ vì dạy lớp. | UC 35, 42–45; người dùng chốt 2026-10-09: ngân hàng dùng cho mọi dạng bài |
| BR-U06-03 | Không có nhân bản câu hỏi giữa lớp và môn. Câu riêng của bài hoặc quiz (`scope_type = ASSIGNMENT`) đi theo bài khi bài được nhân bản hoặc copy (`InlineQuestionPort.copyToAssignment`). | Người dùng chốt 2026-10-09 |
| BR-U06-04 | Ngoài phạm vi → "không tìm thấy". | SEC-002 |

## 2. Phiên bản câu hỏi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-10 | Mỗi dòng `questions` là một phiên bản, bất biến sau khi `ACTIVE`; chỉ `DRAFT` được sửa. | FR-016 |
| BR-U06-11 | Sửa bản `ACTIVE` tạo phiên bản `DRAFT` mới (`version + 1`, cùng `lineage_id`); mỗi `lineage_id` tối đa 1 `DRAFT`. | FR-016, US-QBK-002 S2 |
| BR-U06-12 | Bài và quiz (U08) lưu `id` phiên bản câu hỏi; phiên bản mới **không** tự áp dụng và **không** báo cho bài đang dùng bản cũ. | Câu 7 |
| BR-U06-13 | Question List hiện bản `ACTIVE` mới nhất mỗi `lineage_id` kèm cờ "có bản nháp", và các câu chỉ mới có bản nháp; Question Detail xem được lịch sử phiên bản. | UC 56 |
| BR-U06-14 | `RETIRED`: không còn để chọn vào bài hoặc quiz mới; bài đang dùng vẫn đọc được. | Thiết kế |
| BR-U06-15 | Xóa trên Question Detail (UC 57): bản `DRAFT` chưa từng kích hoạt thì xóa hẳn; bản đã `ACTIVE` hoặc đã dùng trong bài/quiz chỉ ngưng dùng (`RETIRED`), giữ phiên bản cho bài và lượt làm cũ; không xóa lịch sử. | FR-016, UC 57 |
| BR-U06-16 | Sửa câu hỏi của bài/quiz đang giao (US-QBK-002 S3: snapshot lượt làm, làm lại) thuộc U08/U11, không thuộc U06. | Câu 7 |

## 3. Câu hỏi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-20 | `MCQ_SINGLE`: 2-6 lựa chọn, đúng 1 đáp án. `MCQ_MULTI`: 2-6 lựa chọn, ≥ 1 đáp án đúng. Lựa chọn không trùng nội dung. | FR-017 |
| BR-U06-21 | `ESSAY`: `stem` bắt buộc; không giới hạn số từ hay số dòng. | FR-017, U09 Câu 3 |
| BR-U06-22 | `DOCUMENT`: `stem` bắt buộc; `skeleton` hợp lệ theo mô hình tài liệu của U09; `requiredDiagrams` mỗi loại 1-20. | U09 Câu 5-8 |
| BR-U06-23 | `CODE`: `language` thuộc `JAVA`, `PYTHON`, `C`, `CPP`, `JAVASCRIPT`, `DART`, `CSHARP` (U13 hỗ trợ); ≤ 20 file, mỗi file ≤ 64 KB; `memoryLimitMb` 64-1024; 1-50 test case, ≥ 1 test không ẩn; `timeLimitMs` 100-10 000; tổng điểm test = `defaultPoints`. Kết quả kiểm lời giải mẫu do U13 lưu theo `contentHash` của phiên bản câu. | FR-017 |
| BR-U06-24 | Câu hỏi không gắn rubric: rubric thuộc bài (§4), tự tạo cho mỗi câu Text Essay hoặc mỗi phần của khung khi đưa vào bài, để hai bài không dùng chung rubric. Quiz chấm theo đáp án, Code Lab theo test. | Người dùng chốt 2026-10-03, 2026-10-09 |
| BR-U06-25 | `stem`, lựa chọn, hướng dẫn, giải thích là markdown ≤ 20 000 ký tự, hiển thị đã làm sạch. | SEC-003 |
| BR-U06-26 | Kích hoạt câu hỏi yêu cầu `definition` hợp lệ theo loại (BR-U06-20…23, 25, 27), `defaultPoints` > 0 tối đa 2 chữ số thập phân. | FR-016 |
| BR-U06-27 | `lessonRefs` (≤ 10) phải là module/học liệu của môn (U05, học liệu `class_id` rỗng); câu riêng của bài lớp được tham chiếu thêm học liệu của lớp đó.
| BR-U06-28 | Ngân hàng chứa câu hỏi của mọi dạng bài theo bảng dạng bài ở `domain-entities.md` §3. Tìm kiếm lọc được theo dạng bài; khi thêm vào bài, U08 chỉ nhận câu khớp dạng (BR-U08-10, 11). Câu `DOCUMENT` dùng được cho cả Diagram Essay và bài nhóm; phần tự tính theo heading nhỏ nhất của mỗi nhánh, khung không có heading là một phần (BR-U09-25). | Thay đổi 2026-10-01; người dùng chốt 2026-10-09: ngân hàng dùng cho mọi dạng bài |


## 4. Rubric (UC 46)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-30 | Rubric hợp lệ gồm 1-20 tiêu chí, mỗi tiêu chí 1-20 mục checklist; mỗi mục điểm > 0, tối đa 2 chữ số thập phân. Khi bài còn nháp, rubric được phép trống hoặc chưa đủ. | Câu 3, 5; người dùng chốt 2026-10-09 |
| BR-U06-31 | `scaleMax` = tổng điểm mọi mục (tự tính, không nhập tay). | Câu 5 |
| BR-U06-32 | Chấm: mục đạt/không đạt; điểm tiêu chí = tổng mục đạt; điểm rubric = tổng tiêu chí. `score` từ chối `itemId` không thuộc rubric. | Câu 5 |
| BR-U06-33 | Rubric thuộc đúng một bài (`assignment_id`); hai bài không dùng chung rubric. Bài được phát hành thì rubric của bài bị khóa (sau khi kiểm đủ, BR-U06-37) (U08 gọi `RubricPort.lockForAssignment` trong transaction phát hành); rubric đã khóa không sửa, không xóa. | Người dùng chốt 2026-10-09 |
| BR-U06-34 | Mỗi bài Text Essay, Diagram Essay, bài nhóm bắt buộc có rubric và rubric được hệ thống tự tạo: Text Essay mỗi câu một rubric (tạo khi thêm câu), Diagram Essay và bài nhóm mỗi phần một rubric (tạo khi khung được chia phần). Rubric tự tạo ở dạng trống. Quiz và Code Lab không có rubric. U09 gọi `RubricPort` và ghi `rubricId` vào câu hoặc phần trong `config`. Question Bank không có rubric. | UC 46; người dùng chốt 2026-10-09 |
| BR-U06-35 | Rubric không có phiên bản. Trong popup Rubric Detail mở từ Assignment Form (UC 46), Teacher hoặc Subject Manager được giao dạy lớp (R3/R4, bài của lớp) hoặc Chủ nhiệm môn (R2, bài cấp môn) thêm, sửa, xóa tiêu chí và mục khi bài còn nháp; lưu là ghi đè trực tiếp. Không có nút xóa rubric: khi câu hoặc phần bị bỏ khỏi bài nháp, U09 xóa rubric của câu hoặc phần đó. | UC 46; người dùng chốt 2026-10-09 |
| BR-U06-36 | Nhân bản theo đề: khi bài được nhân bản, tạo version mới hoặc copy sang lớp khác, U09 (`TypeConfigPort.copy`) gọi `RubricPort.cloneForAssignment` để tạo rubric mới, chưa khóa, cho bài đích. | Người dùng chốt 2026-10-03 |
| BR-U06-37 | Bài không phát hành được khi còn rubric trống hoặc chưa hợp lệ (BR-U06-30): `RubricPort.lockForAssignment` kiểm mọi rubric của bài trước khi khóa, thiếu thì từ chối và trả danh sách câu hoặc phần cần điền. Assignment Form hiện cảnh báo trên câu hoặc phần có rubric trống. | Người dùng chốt 2026-10-09 |

## 5. Nhập file và AI soạn câu (luồng phụ của UC 57)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-40 | Trên Question List, Chủ nhiệm môn nhập `.xlsx` hoặc `.csv` (UTF-8), mỗi loại câu hỏi một file mẫu riêng (`MCQ`, `ESSAY`, `DOCUMENT`, `CODE`); ≤ 500 dòng, ≤ 5 MB. | Câu 4, 6, 8 |
| BR-U06-41 | Kiểm từng dòng như tạo tay; dòng hợp lệ tạo câu `DRAFT`, dòng lỗi không tạo; trả kết quả từng dòng. | US-QBK-002 S1 |
| BR-U06-42 | Cột riêng: MCQ `lua_chon_1..6`, `dap_an_dung` (ví dụ `1,3`), `giai_thich`; ESSAY `goi_y_dap_an`; DOCUMENT `so_do_bat_buoc` (ví dụ `CLASS:1,SEQUENCE:2`; khung tài liệu chỉ tạo trên giao diện hoặc nhập DOCX); CODE `ngon_ngu`, `code_mau`, `test_cases` (JSON), `gioi_han_ms`. Cột chung: `tieu_de`, `noi_dung`, `diem`, `do_kho`, `tag`, `ma_bai`. Nhập file chỉ tạo câu hỏi, không tạo rubric. | Câu 8 |
| BR-U06-44 | "Nhờ AI soạn câu hỏi" trên Question List: Chủ nhiệm môn chọn loại câu, module/học liệu của môn, số câu, độ khó; U13 soạn câu dựa trên học liệu (RAG U05) và trừ credit của người yêu cầu; câu được giữ lại (có thể sửa) lưu thành `DRAFT`. | Người dùng chốt giữ AI soạn đề 2026-10-09 |

## 6. Audit và ngoài phạm vi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U06-50 | Audit: tạo, kích hoạt, ngưng, xóa câu hỏi; nhập file (một sự kiện mỗi lần nhập); tự tạo, sửa, tự xóa, khóa rubric. | FR-014 |
| BR-U06-51 | Phân tích độ khó/độ phân biệt câu hỏi từ kết quả thực tế nằm ngoài phạm vi dự án, không thiết kế hoặc triển khai. | Quyết định phạm vi 2026-09-25 |

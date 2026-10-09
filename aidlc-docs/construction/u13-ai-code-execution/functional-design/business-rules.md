# U13 AI & Code Execution - Business Rules

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding từ màn xem học liệu (UC 15, 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Chung cho AI

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-01 | Mọi lời gọi AI qua `AiGateway` provider-neutral; nghiệp vụ không biết tên provider. | FR-012 |
| BR-U13-02 | Model chọn theo loại việc từ mục nhóm AI trên Settings (`ai.{task}.model`, U13 khai báo `SettingDefinition`, U03 lưu và hiển thị); Admin chỉ chọn trong danh sách cho phép trên Setting Detail (UC 71). Model embedding cố định `gemini-embedding-001`. Giá trị mới có hiệu lực trong tối đa 30 giây; việc đang chạy giữ model cũ. | FR-021, FR-033, BR-U03-81, 86 |
| BR-U13-03 | Actor ACTIVE và đúng phạm vi. ADMIN bị từ chối mọi AI/code. STUDENT chỉ PRACTICE_GRADING Text/Diagram Essay đã nộp của chính mình hoặc MATERIAL_SUMMARY do U05 xác minh quyền xem; child EMBEDDING chỉ gắn HOLD summary đó, không API embedding độc lập. Teacher/Subject Manager theo R2/R3/R4. AiGuard kiểm Settings, kill-switch, trần và rate trước nhận yêu cầu và trước mỗi provider call; bị chặn không gọi/charge. Thiếu credit không nhận yêu cầu; học liệu/bài nộp còn nguyên. Sau nhận yêu cầu, worker dùng payer trong HOLD, không cấp Student quyền AI khác. | Revision View Material 2026-10-09 |
| BR-U13-04 | Sau khi gọi: `settle` theo token thật (1 credit = 1 000 token, làm tròn lên mỗi lần gọi); lỗi → `release`. | BR-U07-30, 40…43 |
| BR-U13-05 | Chạy nền bằng job U03. Với `GRADED`, kết quả là **đề xuất** (`READY`), không tự phát hành đề hay chốt điểm. Kết quả `PRACTICE` (dòng `evaluations` `kind = PRACTICE`) là điểm/phản hồi luyện tập riêng của Student, không vào sổ điểm. | FR-006, FR-008, FR-030 |
| BR-U13-06 | Lỗi tạm (timeout, 429, 5xx) retry theo U03 tối đa 3 lần; Gemini báo hết quota (429) sau các lần retry → "Hệ thống đang bận", trả phần credit chưa dùng. Đầu ra sai định dạng JSON retry 1 lần rồi `FAILED`/`INVALID_OUTPUT`. Không có đề xuất hoàn tất giả. | US-AIG-001 S3 |
| BR-U13-07 | Nội dung người dùng (học liệu, bài nộp) đưa vào prompt trong khối phân cách, kèm chỉ dẫn "chỉ là dữ liệu"; quét dấu hiệu chèn lệnh, có dấu hiệu thì gắn cờ trong đề xuất cho người dùng. | SEC-003 |
| BR-U13-08 | `ai_suggestions` chỉ lưu số liệu, tham số và kết quả có cấu trúc; không lưu prompt/phản hồi thô. | US-AIG-003 S2 |
| BR-U13-09 | Khi dùng RAG, U13 truyền `requesterId` và `requestRef` riêng cho U05 để tính credit embedding câu hỏi; credit tạo nội dung của U13 giữ bằng `requestRef` khác. Nếu U05 từ chối trước khi gọi Gemini vì hết hạn mức hoặc thiếu credit, U13 trả phần credit tạo nội dung đã giữ. | BR-U05-44, BR-U07-41…43 |

## 2. Đề xuất câu hỏi (`QUESTION_DRAFT`) và khung (`SKELETON_DRAFT`)

Luồng phụ của soạn quiz, bài và câu ngân hàng (UC 35, 42–45, 57), người dùng chốt giữ 2026-10-09.

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-10 | Bài của lớp (giảng viên chính của lớp, R3/R4): đích là quiz hoặc bài `DRAFT` (Text Essay, Code Lab: câu; Diagram Essay, bài nhóm: khung) của lớp; nguồn là học liệu `ACTIVE`, `INDEXED` của môn và của lớp đó (U05). | US-AIG-001 |
| BR-U13-11 | Phạm vi môn (Chủ nhiệm môn, R2): đích là câu của ngân hàng môn (Question List, U06), quiz hoặc bài của môn `DRAFT` (U08: Text Essay, Code Lab, Diagram Essay; không có bài nhóm); nguồn chỉ là học liệu của môn. Không còn template và ngân hàng lớp. | US-AIG-002, FR-027; người dùng chốt 2026-10-09 |
| BR-U13-12 | Nguồn ngoài phạm vi bị loại **trước** khi gọi AI; nguồn chưa `INDEXED` báo "học liệu chưa xử lý xong". | US-AIG-001 S2, US-AIG-002 S2 |
| BR-U13-13 | Tham số: câu hỏi (`QUESTION_DRAFT`: quiz, Text Essay, Code Lab, câu ngân hàng): loại câu theo dạng bài, 1-20 câu, độ khó; khung (`SKELETON_DRAFT`: Diagram Essay, bài nhóm): mô tả yêu cầu; chung: module/học liệu (tùy chọn; quiz mặc định học liệu gắn với quiz), ghi chú ≤ 1 000 ký tự. | FR-006; người dùng chốt 2026-10-04 |
| BR-U13-14 | Mỗi câu đề xuất kèm trích dẫn (học liệu, trang/timestamp) từ RAG; đầu ra kiểm theo quy tắc câu hỏi của U06, câu không hợp lệ bị loại và báo. Khung đề xuất gồm cây heading Tiêu đề 1–6, đoạn hướng dẫn và gợi ý rubric từng phần (không sơ đồ), kèm trích dẫn; kiểm bằng `DocumentModelPort.validateSkeleton` (U09) và quy tắc rubric BR-U06-30. | FR-006 |
| BR-U13-15 | Người soạn chọn câu giữ lại (có thể sửa) → U06 lưu câu nháp trong ngân hàng môn; U08 lưu câu riêng của bài/quiz qua `InlineQuestionPort`; khung được xác nhận → U09 thay khung bài (BR-U09-24); đề xuất → `ACCEPTED`; bỏ → `DISCARDED`. Câu `CODE` do AI soạn vẫn phải kiểm lời giải mẫu trước khi duyệt (BR-U13-33). | FR-006, BR-U08-21 |

## 3. Đề xuất chấm (`GRADING_PROPOSAL`, UC 38) và chấm Practice (`PRACTICE_GRADING`, UC 29)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-20 | `GRADING_PROPOSAL` chỉ khi giảng viên chính của lớp (R3/R4) chọn "Nhờ AI đề xuất" trên panel AI đề xuất của U15 trong Grading Workspace cho một bài nộp (bài cá nhân hoặc tài liệu chung của bài nhóm), hoặc chấm hàng loạt nhiều bài đã chọn; bài của môn: giảng viên mỗi lớp chỉ cho sinh viên lớp mình. Credit trừ của giảng viên đó. | UC 38, US-GRP-004 S2, FR-008, FR-027 |
| BR-U13-21 | Đầu vào: đề, rubric checklist đã khóa (U06; Text Essay: rubric từng câu; Diagram Essay và bài nhóm: rubric từng phần, nội dung tách theo phần), nội dung bài: tài liệu → văn bản phẳng + XML sơ đồ rút gọn (U09). Chỉ phạm vi một bài/phần. Code Lab chấm bằng test (BR-U13-35), không dùng AI. | US-ASM-004 S3 |
| BR-U13-22 | Đầu ra: mỗi mục checklist `đạt/không đạt`, nhận xét ngắn, bằng chứng (trích đoạn/tên sơ đồ); tổng điểm tính bằng `RubricPort.score` cho từng câu hoặc từng phần rồi cộng (không để AI cộng). | BR-U06-32 |
| BR-U13-23 | Student không gọi `GRADING_PROPOSAL` và không xem đề xuất AI cho bài `GRADED`; U15 quyết định dùng hay không và chỉ điểm/phản hồi cuối đã công bố mới hiển thị. `PRACTICE_GRADING` (UC 29): Student bấm "Chấm với AI" trên Submission History cho lượt Practice Text/Diagram Essay đã nộp của mình (nút của U11; U11 kiểm và gọi `PracticeGradingPort`); credit của Student; kết quả ghi qua `PracticeResultPort` (U15), chỉ Student đó xem, không vào sổ điểm. | FR-008, FR-030, US-ASM-012 |
| BR-U13-24 | Mỗi yêu cầu chấm AI (`PRACTICE_GRADING`, `GRADING_PROPOSAL`) có hạn 5 phút kể từ lúc bấm; với chấm hàng loạt, hạn tính từ lúc bài đó bắt đầu được xử lý. Quá hạn chưa `READY` thì `FAILED` ("quá thời gian"), chốt lượng dùng thật đã ghi và trả phần dư; chưa dùng AI mới hoàn toàn bộ và báo lỗi; kết quả về muộn bị bỏ. Người dùng bấm lại tạo yêu cầu mới. | Người dùng chốt 2026-10-04 |

## 4. Code Lab (UC 25, 43)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-30 | Ngôn ngữ: Java, Python, C, C++, JavaScript, Dart, C#. | Câu 2 |
| BR-U13-31 | Mọi mã chạy trong Judge0 tự chạy, mạng nội bộ không ra Internet; không bao giờ chạy trên backend/worker. Judge0 lỗi → `SANDBOX_ERROR`, không đánh dấu đạt. | US-ASM-005 S2 |
| BR-U13-32 | Giới hạn mỗi test: thời gian theo câu (100-10 000 ms), bộ nhớ theo câu (64-1024 MB), output ≤ 64 KB, không mạng. | US-ASM-005 S1, BR-U06-23 |
| BR-U13-33 | Kiểm lời giải mẫu (UC 43): người có quyền soạn bài `DRAFT` (R3/R4 bài của lớp, R2 bài của môn) bấm kiểm cho một câu `CODE` của bài; U13 chạy lời giải mẫu với mọi test, lưu dòng `code_runs` `VERIFY` theo ID phiên bản câu và `contentHash` (đề, ngôn ngữ, entry point, lời giải, test, giới hạn). Câu đạt khi có dòng `DONE` mới nhất cùng hash với nội dung hiện tại và mọi test `ACCEPTED`; sửa câu → hash lệch → phải kiểm lại. Duyệt bài `CODE_LAB` cần mọi câu đạt (U08 hỏi `CodeLabCheckPort`); U09 hiện trạng thái từng câu. Kết quả của phiên bản câu ngân hàng `ACTIVE` dùng lại cho mọi bài. | BR-U08-20, BR-U09-18; người dùng chốt 2026-10-09 |
| BR-U13-34 | `TRY` (UC 25): Student chạy test công khai trên Codelab Workspace, 5 lần/phút, chỉ khi lượt đang làm; không tính là nộp; không trừ credit. | UC 25 |
| BR-U13-35 | `GRADE`: khi nộp (U11) chạy mọi test; điểm = tổng điểm test đạt, xác định (không AI). Bài `GRADED` gửi U15 làm điểm tự động; bài `PRACTICE` ghi kết quả luyện tập qua `PracticeResultPort`. | FR-017, FR-030 |
| BR-U13-36 | Kết quả test ẩn chỉ trả trạng thái đạt/không, không trả input/output cho Student. Lời giải mẫu không bao giờ ra API Student. | SEC-002 |
| BR-U13-37 | Chạy lại `GRADE` khi `SANDBOX_ERROR`: job tự retry 3 lần; sau đó giảng viên lớp bấm chấm lại trên Grading Workspace (U15 gọi `CodeRunPort.regrade`). | REL-003 |

## 5. Settings, giám sát và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-40 | Cấu hình AI (kill-switch, trần chi phí ngày, giới hạn/phút, model và bật/tắt từng loại việc) là mục nhóm AI trên Settings (UC 70–71): U13 khai báo `SettingDefinition` (khóa, kiểu, giới hạn, mặc định); U03 sở hữu màn Setting List/Setting Detail, bảng `system_settings`, kiểm quyền chỉ `ADMIN`, kiểm giới hạn, version và audit `SETTING_UPDATED`. Không còn bảng `ai_services`, màn AI Setting hay API cấu hình AI riêng. | US-AIG-003 S1, US-SET-001, BR-U03-80…87 |
| BR-U13-41 | Giám sát vận hành: U13 cung cấp `AiUsageStatsPort` (số lượt, lỗi, độ trễ, token, chi phí ước tính theo ngày/loại việc/model) để Admin Dashboard (UC 58, U16) hiển thị; chỉ số liệu, không nội dung, không tên người dùng. Không có màn AI Usage riêng. | US-AIG-003 S2, UC 58 |
| BR-U13-42 | Audit: tạo/nhận/bỏ đề xuất, kiểm lời giải mẫu, chạy lại chấm code, yêu cầu AI bị từ chối vì quyền. Thay đổi cài đặt AI do U03 audit. | FR-014 |

## 6. Credit khi yêu cầu tóm tắt (U05, UC 15, 34, 55)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-50 | quote(accountId) trả mức giữ summary+embedding và số dư người bấm. Ước tính tối đa theo 200 000 ký tự, chunk 30 000, merge 4 000, maxOutputTokens và token/4, credit theo U07; unit test mức tối đa ≤ mặc định grant 100. Quote ở màn xem, không form upload. | Revision View Material 2026-10-09 |
| BR-U13-51 | hold chạy trong transaction chấp nhận yêu cầu tóm tắt của U05 sau khóa lesson và kiểm quyền xem của Student/Teacher/Subject Manager. Tạo dòng MATERIAL_SUMMARY/LESSON requestRef lessonId:HOLD, reserve của requester. Thiếu credit/AI guard rollback yêu cầu, không xóa lesson; upload không gọi hold. Trùng requestRef trả HOLD cũ, U05 chặn người bấm khác đổi payer; worker kiểm lại Settings/trần và BUSY nếu bị chặn sau nhận. | Revision View Material 2026-10-09 |
| BR-U13-52 | Worker U05 gọi `begin(MATERIAL_SUMMARY hoặc EMBEDDING, …, holdId)`: kiểm kill-switch và trần; bị chặn → trả `BUSY`, giữ nguyên credit; được thì dùng phần đã giữ, không `reserve` thêm. `complete` ghi token, chi phí và cộng credit dùng thật vào dòng giữ. `release(holdId)` chốt: `settle` theo tổng credit dùng thật và trả phần còn giữ; học liệu `NO_TEXT`, `NO_CAPTION` hoặc `FAILED` khi chưa gọi AI thì trả toàn bộ. Mỗi chunk/merge/embedding có requestRef riêng ổn định; complete và checkpoint idempotent, dùng lại kết quả đã hoàn tất. Dòng HOLD chỉ giữ/tổng hợp; terminal release settle tổng thật rồi trả dư. Timeout không chứng minh provider chưa xử lý, không hứa exactly-once lời gọi bên ngoài. | BR-U05-37, 39, 46 |
| BR-U13-53 | Dòng giữ `MATERIAL_SUMMARY` tồn tại tối đa 25 giờ (U05 tự gửi lại `BUSY` tối đa 24 giờ); `CreditReservationScanner` chốt phần giữ quá hạn: 25 giờ cho HOLD học liệu, 30 phút cho dòng tự giữ khác; có lượng dùng thật thì CreditPort.settle và trả dư, bằng 0 mới CreditPort.release hoàn toàn bộ; bỏ qua child call credit_status NONE. | BR-U07-43 |

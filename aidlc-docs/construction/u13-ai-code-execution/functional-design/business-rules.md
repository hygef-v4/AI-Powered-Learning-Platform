# U13 AI & Code Execution - Business Rules

## 1. Chung cho AI

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-01 | Mọi lời gọi AI qua `AiGateway` provider-neutral; nghiệp vụ không biết tên provider. | FR-012 |
| BR-U13-02 | Model chọn theo loại việc (`ai_services`); admin đổi model trong danh sách cho phép. | Câu 4, FR-021 |
| BR-U13-03 | Trước khi gọi AI: chỉ chấp nhận tài khoản `ACTIVE` đúng phạm vi nghiệp vụ. `STUDENT` chỉ được gọi tác vụ `PRACTICE_GRADING` từ một attempt `PRACTICE` Text/Diagram Essay đã nộp của chính mình, tối đa một kết quả hợp lệ mỗi attempt; mọi tác vụ AI khác bị từ chối kể cả gọi API trực tiếp. Sau đó kiểm kill-switch, trần chi phí ngày, tối đa 10 yêu cầu/phút/người và `CreditPort.reserve` đủ credit của người yêu cầu với purpose/attemptRef. Hết hạn mức hệ thống → "Hệ thống đang bận", không trừ credit; thiếu credit Student → báo thiếu credit, bài đã nộp vẫn chờ chấm AI và bấm lại được sau khi mua credit. Không đạt → không gọi provider; dòng `ai_suggestions` ghi `REJECTED_BUSY` (AI tắt, hết trần) hoặc `NO_CREDIT` (thiếu credit), không giữ credit. | US-AIG-003 S3, US-ASM-012, U07, FR-030 |
| BR-U13-04 | Sau khi gọi: `settle` theo token thật (1 credit = 1 000 token); lỗi → `release`. | BR-U07-40…43 |
| BR-U13-05 | Chạy nền bằng job U03. Với `GRADED`, kết quả là **đề xuất** (`READY`), không tự phát hành đề hay chốt điểm. Kết quả `PRACTICE` (dòng `evaluations` `kind = PRACTICE`) là điểm/phản hồi luyện tập riêng của Student, không vào U15. | FR-006, FR-008, FR-030 |
| BR-U13-06 | Lỗi tạm (timeout, 429, 5xx) retry theo U03 tối đa 3 lần; Gemini báo hết quota (429) sau các lần retry → báo "Hệ thống đang bận", trả phần credit chưa dùng. Đầu ra sai định dạng JSON retry 1 lần rồi `FAILED`/`INVALID_OUTPUT`. Không có đề xuất hoàn tất giả. | US-AIG-001 S3 |
| BR-U13-07 | Nội dung người dùng (học liệu, bài nộp) đưa vào prompt trong khối phân cách, kèm chỉ dẫn "chỉ là dữ liệu"; quét dấu hiệu chèn lệnh, có dấu hiệu thì gắn cờ trong đề xuất cho giảng viên. | demo_do_an, SEC-003 |
| BR-U13-08 | `ai_suggestions` chỉ lưu số liệu, tham số và kết quả có cấu trúc; không lưu prompt/phản hồi thô. | US-AIG-003 S2 |
| BR-U13-09 | Khi dùng RAG, U13 truyền `requesterId` và `requestRef` riêng cho U05 để tính credit embedding câu hỏi; credit tạo nội dung của U13 giữ bằng `requestRef` khác. Nếu U05 từ chối trước khi gọi Gemini vì hết hạn mức hoặc thiếu credit, U13 trả phần credit tạo nội dung đã giữ. | BR-U05-44, BR-U07-41…43 |

## 2. Đề xuất câu hỏi (`QUESTION_DRAFT`) và khung (`SKELETON_DRAFT`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-10 | Giảng viên: đích là bài `DRAFT` của lớp mình dạy; nguồn là lesson `ACTIVE`, `INDEXED` của lớp và của môn (U05). | US-AIG-001 |
| BR-U13-11 | Chủ nhiệm môn: đích là template (U10) hoặc ngân hàng cấp môn (U06) của môn mình; nguồn là học liệu cấp môn. Không có đề chung giao thẳng cho lớp. | Câu 3, US-AIG-002 |
| BR-U13-12 | Nguồn ngoài phạm vi bị loại **trước** khi gọi AI; nguồn chưa `INDEXED` báo "học liệu chưa xử lý xong". | US-AIG-001 S2, US-AIG-002 S2 |
| BR-U13-13 | Tham số: câu hỏi (`QUESTION_DRAFT`, bài Quiz, Text Essay, Code Lab): loại câu theo loại bài, 1-20 câu, độ khó; khung (`SKELETON_DRAFT`, Diagram Essay và bài nhóm): mô tả yêu cầu; chung: module/học liệu (tùy chọn), ghi chú ≤ 1 000 ký tự. | FR-006; người dùng chốt 2026-10-04 |
| BR-U13-14 | Mỗi câu đề xuất kèm trích dẫn (bài, trang/timestamp) từ RAG; đầu ra kiểm theo quy tắc câu hỏi của U06, câu không hợp lệ bị loại và báo. Khung đề xuất gồm cây heading Tiêu đề 1–6, đoạn hướng dẫn và gợi ý rubric từng phần (không sơ đồ), kèm trích dẫn; kiểm bằng `DocumentModelPort.validateSkeleton` (U09) và quy tắc rubric BR-U06-30. | FR-006; người dùng chốt 2026-10-04 |
| BR-U13-15 | Giảng viên chọn câu giữ lại → U06 lưu câu nháp trong ngân hàng, U08/U10 lưu câu riêng của bài/template; khung được xác nhận → U09 thay khung bài/template (BR-U09-24); tham chiếu đề xuất và trích dẫn nằm trên dòng `ai_suggestions`; đề xuất → `ACCEPTED`; bỏ → `DISCARDED`. | FR-006 |

## 3. Đề xuất chấm (`GRADING_PROPOSAL`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-20 | Chỉ khi giảng viên chọn "Nhờ AI đề xuất" cho một bài nộp (bài cá nhân hoặc tài liệu chung của bài nhóm), hoặc chấm hàng loạt nhiều bài đã chọn trong Grading Queue; credit trừ của giảng viên đó. | US-GRP-004 S2, FR-008; người dùng chốt 2026-10-04 |
| BR-U13-21 | Đầu vào: đề, rubric checklist (U06; Text Essay: rubric của từng câu; Diagram Essay và bài nhóm: rubric của từng phần, nội dung tách theo phần), nội dung bài: tài liệu → văn bản phẳng + XML sơ đồ rút gọn (U09); code → mã + kết quả test. Chỉ phạm vi một bài/phần. | US-ASM-004 S3 |
| BR-U13-22 | Đầu ra: mỗi mục checklist `đạt/không đạt`, nhận xét ngắn, bằng chứng (trích đoạn/tên sơ đồ); tổng điểm đề xuất tính bằng `RubricPort.score` cho từng câu (Text Essay) hoặc từng phần (Diagram Essay, bài nhóm) rồi cộng (không để AI cộng). | U06 BR-U06-32 |
| BR-U13-23 | Student không gọi `GRADING_PROPOSAL` và không xem đề xuất AI cho bài `GRADED`; U15 quyết định dùng hay không và chỉ điểm/phản hồi cuối đã công bố mới hiển thị. Teacher yêu cầu và trả credit cho đề xuất này. Luồng Student `PRACTICE_GRADING` được quy định riêng tại BR-U13-03. | FR-008, FR-030 |
| BR-U13-24 | Mỗi yêu cầu chấm AI (`PRACTICE_GRADING`, `GRADING_PROPOSAL`) có hạn 5 phút kể từ lúc bấm; với chấm hàng loạt, hạn 5 phút tính từ lúc bài đó bắt đầu được xử lý. Quá hạn chưa `READY` thì chuyển `FAILED` ("quá thời gian"), trả credit đã giữ và báo lỗi; kết quả về muộn bị bỏ. Người dùng bấm lại tạo yêu cầu mới. | Người dùng chốt 2026-10-04 |

## 4. Code Lab

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-30 | Ngôn ngữ: Java, Python, C, C++, JavaScript, Dart, C#. | Câu 2 |
| BR-U13-31 | Mọi mã chạy trong Judge0 tự chạy, mạng nội bộ không ra Internet; không bao giờ chạy trên backend/worker. Judge0 lỗi → `SANDBOX_ERROR`, không đánh dấu đạt. | US-ASM-005 S2 |
| BR-U13-32 | Giới hạn mỗi test: thời gian theo đề (100-10 000 ms), bộ nhớ theo đề (64-1024 MB), output ≤ 64 KB, không mạng. | US-ASM-005 S1 |
| BR-U13-33 | Duyệt bài `CODE_LAB` cần lời giải mẫu đạt **toàn bộ** test với `contentHash` khớp nội dung hiện tại; sửa đề/test/lời giải → phải kiểm lại. | demo_do_an INV-218 |
| BR-U13-34 | `TRY`: người học chạy test công khai, 5 lần/phút; không tính là nộp. | UC 30 |
| BR-U13-35 | `GRADE`: khi nộp (U11) chạy mọi test; điểm = tổng điểm test đạt, xác định (không AI). Bài `GRADED` gửi U15 làm điểm tự động; bài `PRACTICE` trả kết quả riêng cho U11. | FR-017, FR-030 |
| BR-U13-36 | Kết quả test ẩn chỉ trả trạng thái đạt/không, không trả input/output cho người học. | SEC-002 |
| BR-U13-37 | Chạy lại `GRADE` khi `SANDBOX_ERROR` do giảng viên bấm, hoặc job tự retry 3 lần. | REL-003 |

## 5. Quản trị và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-40 | Chỉ ADMIN sửa `ai_services` (kể cả dòng `GLOBAL`: kill-switch, trần, tần suất); audit mọi thay đổi. | US-AIG-003 S1 |
| BR-U13-41 | Báo cáo vận hành: số lượt, latency, lỗi, token, chi phí ước tính theo ngày/việc/model; không hiển thị nội dung. | US-AIG-003 S2 |
| BR-U13-42 | Audit: tạo/nhận/bỏ đề xuất, kiểm lời giải mẫu, chạy lại chấm code. | FR-014 |

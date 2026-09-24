# U13 AI & Code Execution - Business Rules

## 1. Chung cho AI

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-01 | Mọi lời gọi AI qua `AiGateway` provider-neutral; nghiệp vụ không biết tên provider. | FR-012 |
| BR-U13-02 | Model chọn theo loại việc (`AiTaskConfig`); admin đổi model trong danh sách cho phép. | Câu 4, FR-021 |
| BR-U13-03 | Trước khi gọi: kill-switch tắt, chưa vượt trần chi phí ngày, chưa vượt 10 yêu cầu/phút/người, `CreditPort.reserve` đủ credit của **người yêu cầu**. Không đạt → từ chối trước khi gọi provider, ghi `AiCall` `REJECTED_*`. | US-AIG-003 S3, U07 |
| BR-U13-04 | Sau khi gọi: `settle` theo token thật (1 credit = 1 000 token); lỗi → `release`. | BR-U07-40…43 |
| BR-U13-05 | Chạy nền bằng job U02; kết quả là **đề xuất** (`READY`), không bao giờ tự phát hành đề hay chốt điểm. | FR-006, FR-008 |
| BR-U13-06 | Lỗi tạm (timeout, 429, 5xx) retry theo U02 tối đa 3 lần; đầu ra sai định dạng JSON retry 1 lần rồi `FAILED`/`INVALID_OUTPUT`. Không có đề xuất hoàn tất giả. | US-AIG-001 S3 |
| BR-U13-07 | Nội dung người dùng (học liệu, bài nộp) đưa vào prompt trong khối phân cách, kèm chỉ dẫn "chỉ là dữ liệu"; quét dấu hiệu chèn lệnh, có dấu hiệu thì gắn cờ trong đề xuất cho giảng viên. | demo_do_an, SEC-003 |
| BR-U13-08 | `AiCall` chỉ lưu số liệu, không lưu prompt/phản hồi thô. | US-AIG-003 S2 |

## 2. Đề xuất câu hỏi (`QUESTION_DRAFT`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-10 | Giảng viên: đích là bài `DRAFT` của lớp mình dạy; nguồn là học liệu đã phát hành của lớp + bài cấp môn đã liên kết (U05). | US-AIG-001 |
| BR-U13-11 | Chủ nhiệm môn: đích là template (U10) hoặc ngân hàng cấp môn (U06) của môn mình; nguồn là học liệu cấp môn. Không có đề chung giao thẳng cho lớp. | Câu 3, US-AIG-002 |
| BR-U13-12 | Nguồn ngoài phạm vi bị loại **trước** khi gọi AI; nguồn chưa `INDEXED` báo "học liệu chưa xử lý xong". | US-AIG-001 S2, US-AIG-002 S2 |
| BR-U13-13 | Tham số: loại câu (theo loại bài), 1-20 câu, độ khó, chương/bài (tùy chọn), ghi chú ≤ 1 000 ký tự. | FR-006 |
| BR-U13-14 | Mỗi câu đề xuất kèm trích dẫn (bài, trang/timestamp) từ RAG; đầu ra kiểm theo quy tắc câu hỏi của U06, câu không hợp lệ bị loại và báo. | FR-006 |
| BR-U13-15 | Giảng viên chọn câu giữ lại → U08/U06/U10 lưu dạng nháp với `origin = AI`; đề xuất → `ACCEPTED`; bỏ → `DISCARDED`. | FR-006 |

## 3. Đề xuất chấm (`GRADING_PROPOSAL`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-20 | Chỉ khi giảng viên chọn "Nhờ AI đề xuất" cho một bài nộp/phần; credit trừ của giảng viên đó. | US-GRP-004 S2, FR-008 |
| BR-U13-21 | Đầu vào: đề, rubric checklist (U06), nội dung bài: tài liệu → văn bản phẳng + XML sơ đồ rút gọn (U09); code → mã + kết quả test. Chỉ phạm vi một bài/phần. | US-ASM-004 S3 |
| BR-U13-22 | Đầu ra: mỗi mục checklist `đạt/không đạt`, nhận xét ngắn, bằng chứng (trích đoạn/tên sơ đồ); tổng điểm đề xuất tính bằng `RubricPort.score` (không để AI cộng). | U06 BR-U06-32 |
| BR-U13-23 | Người học không bao giờ thấy đề xuất AI; U15 quyết định dùng hay không. | FR-008 |

## 4. Code Lab

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-30 | Ngôn ngữ: Java, Python, C, C++, JavaScript, Dart, C#. | Câu 2 |
| BR-U13-31 | Mọi mã chạy trong Judge0 tự chạy, mạng nội bộ không ra Internet; không bao giờ chạy trên backend/worker. Judge0 lỗi → `SANDBOX_ERROR`, không đánh dấu đạt. | US-ASM-005 S2 |
| BR-U13-32 | Giới hạn mỗi test: thời gian theo đề (100-10 000 ms), bộ nhớ theo đề (64-1024 MB), output ≤ 64 KB, không mạng. | US-ASM-005 S1 |
| BR-U13-33 | Duyệt bài `CODE_LAB` cần lời giải mẫu đạt **toàn bộ** test với `contentHash` khớp nội dung hiện tại; sửa đề/test/lời giải → phải kiểm lại. | demo_do_an INV-218 |
| BR-U13-34 | `TRY`: người học chạy test công khai, 5 lần/phút; không tính là nộp. | UC-ASM-13 |
| BR-U13-35 | `GRADE`: khi nộp (U11/U14) chạy mọi test; điểm = tổng điểm test đạt, xác định (không AI); gửi U15 làm điểm tự động. | FR-017 |
| BR-U13-36 | Kết quả test ẩn chỉ trả trạng thái đạt/không, không trả input/output cho người học. | SEC-002 |
| BR-U13-37 | Chạy lại `GRADE` khi `SANDBOX_ERROR` do giảng viên bấm, hoặc job tự retry 3 lần. | REL-003 |

## 5. Quản trị và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U13-40 | Chỉ ADMIN sửa `AiTaskConfig`, `AiGlobalSettings`, kill-switch; audit mọi thay đổi. | US-AIG-003 S1 |
| BR-U13-41 | Báo cáo vận hành: số lượt, latency, lỗi, token, chi phí ước tính theo ngày/việc/model; không hiển thị nội dung. | US-AIG-003 S2 |
| BR-U13-42 | Audit: tạo/nhận/bỏ đề xuất, kiểm lời giải mẫu, chạy lại chấm code. | FR-014 |

# U08 Assessment Core & Publication - Business Rules

**Bản tài liệu 2026-10-08**: UC 38; primary stories: US-ASM-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-01 | Teacher hoặc Subject Manager/Administrator được giao dạy lớp R3/R4 tạo/sửa/xóa nháp/duyệt/phát hành/ngưng bài lớp. Full cấu trúc hoặc quản lý môn không tự cấp đọc/sửa bài giảng dạy. Template môn theo R2/U10, không giao đề chung mọi lớp. | FR-002/007, UC 38–43 |
| BR-U08-02 | Bài chỉ phát hành cho chính lớp chứa nó; lớp ngoài phạm vi người phát hành → từ chối, audit. | US-ASM-001 S2 |
| BR-U08-03 | Người học chỉ thấy bài `OPEN`/`CLOSED` của lớp mình đang ghi danh; `DRAFT`, `REVIEWED`, `SCHEDULED` ẩn. Bài `RETIRED` ẩn, trừ khi người học đã có điểm ở bài đó (điểm đã công bố hoặc kết quả Practice): vẫn hiện để xem, không làm hay nộp thêm. | FR-007 |

## 2. Soạn bài

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-10 | Mỗi bài một trong năm dạng `MULTIPLE_CHOICE_QUIZ`, `TEXT_ESSAY`, `DIAGRAM_ESSAY`, `CODE_LAB`, `GROUP_ASSIGNMENT`; câu phải khớp dạng. Chế độ là `GRADED` hoặc `PRACTICE`, riêng `GROUP_ASSIGNMENT` chỉ `GRADED`. | FR-017, U09, quyết định 2026-09-29 |
| BR-U08-11 | Câu lấy từ ngân hàng (version `ACTIVE`, ghim) hoặc là câu riêng của bài; câu riêng được U06 kiểm theo quy tắc ngân hàng và lưu thành dòng `questions` phạm vi bài. Câu ngân hàng phải khớp dạng bài theo BR-U06-28. | Câu 2, UC 39, 40, 41, 42, 43 |
| BR-U08-17 | Tạo bài bắt đầu bằng chọn dạng bài và chế độ; chỉ sau đó mới chọn nguồn: bài trống, copy template `RELEASED` của môn hoặc copy bài của lớp khác mình dạy. Danh sách nguồn copy chỉ gồm template/bài cùng dạng và cùng chế độ đã chọn. | Người dùng chốt 2026-10-03 |
| BR-U08-18 | Thêm câu từ ngân hàng theo hai cách: chọn tay, hoặc chọn ngẫu nhiên N câu `ACTIVE` khớp dạng bài và bộ lọc (độ khó, tag, module/lesson), không trùng câu đã có trong bài; N không vượt số câu còn thiếu theo BR-U08-13 và số câu khớp. Kết quả ngẫu nhiên ghim vào bài như chọn tay (mọi sinh viên cùng bộ câu); người soạn xem lại, bỏ hoặc bốc lại từng câu trước khi lưu. | Người dùng chốt 2026-10-03 |
| BR-U08-12 | Điểm từng câu > 0: Quiz và Code Lab do người soạn đặt; Text Essay bằng tổng điểm rubric của câu (U09 ghi khi tạo/sửa rubric, không sửa tay). Tổng điểm bài tính khi đọc: tổng điểm các câu; Diagram Essay và bài nhóm là tổng điểm rubric các phần (BR-U09-26). | Thiết kế; người dùng chốt 2026-10-04 |
| BR-U08-13 | Quiz 1-200 câu; Text Essay và Code Lab 1-20 câu. Diagram Essay và bài nhóm không có câu: khung nằm ở `config` (U09). | Thiết kế; người dùng chốt 2026-10-04 |
| BR-U08-14 | Chỉ sửa khi `DRAFT`; sửa bài `REVIEWED` đưa về `DRAFT`. | Câu 3 |
| BR-U08-15 | Bài `DRAFT` chưa từng phát hành được xóa. | Thiết kế |

## 3. Duyệt

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-16 | Rubric do U09 tạo trong `TypeConfigSlot` và lưu ở `config`: Text Essay một rubric mỗi câu, Diagram Essay và bài nhóm một rubric mỗi phần của khung (BR-U09-23, 25…28); Quiz và Code Lab không có rubric. Bài không có cột `rubric_id`. U08 cài `RubricOwnerPort.repoint` cho sửa ở Question Bank bằng cách gọi `TypeConfigPort.repointRubric` (chỉ đổi bài `DRAFT`, BR-U06-36). | BR-U06-34; người dùng chốt 2026-10-04 |
| BR-U08-20 | Chính người có quyền tự duyệt sau khi xem trước; hệ thống kiểm: Quiz, Text Essay, Code Lab có ≥ 1 câu và câu hợp lệ; cấu hình loại bài đủ (`TypeConfigPort`, U09), mọi câu (Text Essay) hoặc mọi phần (Diagram Essay, bài nhóm) có rubric (BR-U08-16, qua `TypeConfigPort`), bài `CODE_LAB` đã kiểm lời giải mẫu với nội dung hiện tại (`CodeLabCheckPort`, U13; chưa có U13 thì bỏ qua kiểm này). | Câu 3, US-ASM-001 |
| BR-U08-21 | AI chỉ thêm câu vào bài `DRAFT` đã có: câu giảng viên giữ lại (có thể sửa trước) được lưu thành câu riêng của bài qua `InlineQuestionPort` (U06); đề xuất và trích dẫn nguồn nằm trên dòng `ai_suggestions` của U13 (trỏ tới bài, chuyển `ACCEPTED`). Diagram Essay và bài nhóm không có câu: AI đề xuất khung kèm gợi ý rubric từng phần, U09 thay khung khi giảng viên xác nhận (BR-U09-24). Bài vẫn phải duyệt như thường; `config.origin` chỉ ghi cách tạo bài, không có giá trị `AI`. | FR-006; người dùng chốt 2026-10-04 |
| BR-U08-22 | Chưa `REVIEWED` → không phát hành được. | US-ASM-001 S2 |

## 4. Phát hành

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-30 | Mỗi bài thuộc đúng một lớp và có một lịch; giao cùng nội dung cho lớp khác là copy bài sang lớp đó (U10) rồi phát hành riêng. | Quyết định 2026-10-03 |
| BR-U08-31 | `opens_at < closes_at`; `max_attempts` 1-10; lớp phải `OPEN`. Bài `GROUP_ASSIGNMENT` (tài liệu nhóm) chỉ `GRADED` và chỉ phát hành khi U12 báo nhóm của lớp sẵn sàng (`GroupReadinessPort`, `C`). | FR-007, FR-017, U12 |
| BR-U08-32 | Tùy chọn nộp trễ: `late_until` (≤ `closes_at` + 30 ngày). Nộp sau `closes_at` được đánh dấu trễ; sau `late_until` không nhận. | Câu 7 |
| BR-U08-33 | Phát hành chuyển bài sang `SCHEDULED`: nội dung, câu, điểm **không sửa được nữa**, kể cả khi chưa ai làm; thay đổi bằng version mới (BR-U08-43) hoặc nhân bản. | Câu 4, 6 |
| BR-U08-34 | Lịch sửa được khi `SCHEDULED`; khi `OPEN` chỉ được kéo dài `closes_at`/`late_until`, không rút ngắn, không đổi `max_attempts`. | Thiết kế |
| BR-U08-35 | Khi bài chuyển `OPEN`, phát event `assignment.opened`; U16 báo trong app và email cho người học của lớp. | Câu 8 |
| BR-U08-36 | Mở/đóng theo lịch tự động bằng scanner của U08 chạy mỗi phút (U03), sai lệch ≤ 1 phút. | Thiết kế |

## 5. Ngưng giao và nhân bản

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-40 | Ngưng giao: bài → `RETIRED`, lý do bắt buộc (ghi audit); người học không bắt đầu/nộp thêm; bài đã nộp giữ nguyên; trong cùng transaction gọi `AssignmentLifecyclePort.onRetired` (U11, U14 cài: tự nộp lượt/tài liệu đang dở); audit. | Câu 4 |
| BR-U08-41 | Nhân bản: tạo bài `DRAFT` mới cùng lớp, sao chép `assignment_questions` (ghim cùng version câu ngân hàng, sao chép câu riêng), `config.origin = CLONE`, `source_assignment_id`; rubric từng câu/từng phần được nhân bản qua `TypeConfigPort.copy` (BR-U06-35); audit. | Câu 5 |
| BR-U08-42 | Bài `CLOSED`/`RETIRED` ẩn khỏi danh sách mặc định (lọc theo trạng thái). | Thiết kế |
| BR-U08-43 | Bài `CLOSED`/`RETIRED`: bấm "Sửa" tạo version kế tiếp (`version + 1`, `source_assignment_id` = bài cũ, `DRAFT`, `config.origin = NEW_VERSION`) cùng lớp, sao chép câu, cấu hình và nhân bản rubric từng câu/từng phần qua `TypeConfigPort.copy` (BR-U06-35); bài cũ giữ nguyên cho bài nộp cũ. Version mới duyệt và phát hành như bài mới. | U10 Câu 5, 6 |
| BR-U08-44 | Mỗi bài nguồn tối đa một version `DRAFT` đang mở. | Thiết kế |

## 6. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-50 | Audit: duyệt, phát hành (actor, lớp, lịch), sửa lịch, ngưng giao (lý do), nhân bản, tạo version mới, xóa nháp, phát hành bị từ chối vì sai phạm vi. | FR-014 |

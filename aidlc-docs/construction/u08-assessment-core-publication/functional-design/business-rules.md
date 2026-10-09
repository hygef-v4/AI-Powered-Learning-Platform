# U08 Assessment Core & Publication - Business Rules

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Loại bài và quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-01 | Có ba loại: **bài của lớp** (Teacher hoặc Subject Manager được giao dạy lớp, R3/R4, soạn trên tab Evals của Teacher Class Detail); **bài của môn** (Chủ nhiệm môn, R2, soạn trên Assignment List mở từ Manager Dashboard, giao cho mọi lớp của môn); **quiz** (luyện tập, gắn với một học liệu, BR-U08-60). Admin không soạn hay xem bài. | UC 35, 41–45; người dùng chốt 2026-10-09 |
| BR-U08-02 | Bài của lớp chỉ phát hành cho chính lớp đó; người ngoài phạm vi → từ chối, audit. Giảng viên các lớp thuộc môn chỉ xem bài của môn (nội dung, lịch), không sửa, không phát hành, không ngưng giao. | US-ASM-001 S2; người dùng chốt 2026-10-09 |
| BR-U08-03 | Người học thấy bài `OPEN`/`CLOSED` của lớp mình đang ghi danh (lớp `OPEN`), gồm bài của lớp và bài của môn chứa lớp; `DRAFT`, `REVIEWED`, `SCHEDULED` ẩn. Bài `RETIRED` ẩn, trừ khi người học đã có điểm ở bài đó (điểm đã công bố hoặc kết quả Practice): vẫn hiện để xem, không làm hay nộp thêm. Quiz không nằm trong danh sách bài (BR-U08-60). | FR-007, UC 22 |

## 2. Soạn bài

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-10 | Bài tập là một trong bốn dạng `TEXT_ESSAY`, `DIAGRAM_ESSAY`, `CODE_LAB`, `GROUP_ASSIGNMENT`, chế độ `GRADED` hoặc `PRACTICE` (`GROUP_ASSIGNMENT` chỉ `GRADED`). Bài của môn không có `GROUP_ASSIGNMENT` (nhóm thuộc từng lớp). Quiz là dạng `MULTIPLE_CHOICE_QUIZ`, luôn `PRACTICE`. | FR-017; người dùng chốt 2026-10-09 |
| BR-U08-11 | Quiz, Text Essay, Code Lab có câu ở `assignment_questions`: câu từ ngân hàng của môn khớp dạng (version `ACTIVE`, ghim) hoặc câu riêng của bài (U06 kiểm và lưu với phạm vi bài). Diagram Essay và bài nhóm không có câu: khung ở `config` (U09), có thể sao từ câu `DOCUMENT` của ngân hàng. | BR-U06-02, 28; người dùng chốt 2026-10-09 |
| BR-U08-12 | Điểm từng câu > 0: quiz do người soạn đặt; Code Lab bằng tổng điểm test của câu; Text Essay bằng tổng điểm rubric của câu (tính khi đọc). Tổng điểm bài tính khi đọc: tổng điểm các câu; Diagram Essay và bài nhóm là tổng điểm rubric các phần. | Thiết kế; người dùng chốt 2026-10-04 |
| BR-U08-13 | Quiz 1-200 câu; Text Essay và Code Lab 1-20 câu. Diagram Essay và bài nhóm không có câu. | Thiết kế |
| BR-U08-14 | Chỉ sửa khi `DRAFT`; sửa bài `REVIEWED` đưa về `DRAFT`. | Câu 3 |
| BR-U08-15 | "Xóa" (UC 35, 42–45): bài `DRAFT` hoặc `REVIEWED` chưa từng phát hành được xóa hẳn (kèm câu riêng và rubric); bài đã phát hành thì dùng Ngưng giao (BR-U08-40). | Thiết kế; UC 42–45 |
| BR-U08-17 | Tạo bài bắt đầu bằng chọn dạng bài và chế độ. Bài của lớp có thể bắt đầu từ bài trống hoặc copy bài của lớp khác mình dạy (BR-U08-45, chỉ liệt kê bài cùng dạng và chế độ). Bài của môn và quiz bắt đầu từ bài trống. | Người dùng chốt 2026-10-03, 2026-10-09 |
| BR-U08-18 | Thêm câu vào quiz, Text Essay, Code Lab theo hai cách: chọn tay từ ngân hàng của môn, hoặc chọn ngẫu nhiên N câu `ACTIVE` khớp dạng và bộ lọc (độ khó, tag, module/học liệu), không trùng câu đã có; N không vượt số câu còn thiếu theo BR-U08-13 và số câu khớp. Kết quả ngẫu nhiên ghim vào bài như chọn tay; người soạn xem lại, bỏ hoặc bốc lại từng câu trước khi lưu. Thêm hoặc bỏ câu Text Essay thì U08 gọi `TypeConfigPort.syncQuestionRubrics` để U09 tự tạo/xóa rubric của câu. | Người dùng chốt 2026-10-03, 2026-10-09 |

## 3. Duyệt

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-16 | Rubric thuộc U06, tự tạo cho mỗi câu Text Essay hoặc mỗi phần Diagram Essay/bài nhóm (BR-U06-34); bài không có cột `rubric_id`. Quiz và Code Lab không có rubric. | BR-U06-34; người dùng chốt 2026-10-09 |
| BR-U08-20 | Chính người có quyền với bài tự duyệt sau khi xem trước (giữ bước Duyệt, người dùng chốt 2026-10-09); hệ thống kiểm: quiz, Text Essay, Code Lab có ≥ 1 câu hợp lệ khớp dạng; quiz gắn học liệu còn `ACTIVE`; cấu hình loại bài đủ và mọi rubric đã điền hợp lệ (`TypeConfigPort`, U09); bài `CODE_LAB` đã kiểm lời giải mẫu với nội dung hiện tại (`CodeLabCheckPort`, U13; chưa có U13 thì bỏ qua). | Câu 3, US-ASM-001 |
| BR-U08-21 | AI chỉ thêm câu vào bài `DRAFT` đã có (quiz, Text Essay, Code Lab): câu giữ lại (có thể sửa trước) lưu thành câu riêng của bài qua `InlineQuestionPort` (U06); đề xuất và trích dẫn nguồn nằm trên dòng `ai_suggestions` của U13. Diagram Essay và bài nhóm: AI soạn khung do U09 xử lý. Câu `CODE` do AI soạn vẫn phải được kiểm lời giải mẫu trước khi duyệt. Bài vẫn phải duyệt như thường. | FR-006; người dùng chốt 2026-10-04 |
| BR-U08-22 | Chưa `REVIEWED` → không phát hành được. | US-ASM-001 S2 |

## 4. Phát hành bài tập

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-30 | Bài của lớp thuộc đúng một lớp và có một lịch. Bài của môn có một lịch chung cho mọi lớp của môn: hiện ở mọi lớp `OPEN` của môn, kể cả lớp mở sau khi phát hành. | Quyết định 2026-10-03; người dùng chốt 2026-10-09 |
| BR-U08-31 | `opens_at < closes_at`; `max_attempts` 1-10. Bài của lớp: lớp phải `OPEN`. Bài của môn: môn phải `ACTIVE`. Bài `GROUP_ASSIGNMENT` chỉ phát hành khi U12 báo nhóm của lớp sẵn sàng (`GroupReadinessPort`, `C`): lỗi chặn khi lớp chưa có nhóm, nhóm thiếu trưởng nhóm hoặc không có thành viên; cảnh báo sinh viên chưa có nhóm cần người phát hành xác nhận (`confirmUngroupedStudents`). | FR-007, FR-017, U12 |
| BR-U08-32 | Tùy chọn nộp trễ: `late_until` (≤ `closes_at` + 30 ngày). Nộp sau `closes_at` được đánh dấu trễ; sau `late_until` không nhận. | Câu 7 |
| BR-U08-33 | Phát hành chuyển bài sang `SCHEDULED` và gọi `RubricPort.lockForAssignment` (U06) trong cùng transaction: còn rubric trống thì từ chối (BR-U06-37); từ đây nội dung, câu, điểm, rubric **không sửa được nữa**; thay đổi bằng version mới (BR-U08-43) hoặc nhân bản. | Câu 4, 6; BR-U06-33 |
| BR-U08-34 | Lịch sửa được khi `SCHEDULED`; khi `OPEN` chỉ được kéo dài `closes_at`/`late_until`, không rút ngắn, không đổi `max_attempts`. Lịch bài của môn chỉ Chủ nhiệm môn sửa. | Thiết kế |
| BR-U08-35 | Khi bài chuyển `OPEN`, phát event `assignment.opened` (kèm `classId` hoặc `subjectId`); U16 báo trong app và email cho người học của lớp, hoặc của mọi lớp `OPEN` thuộc môn. | Câu 8 |
| BR-U08-36 | Mở/đóng theo lịch tự động bằng scanner của U08 chạy mỗi phút (U03), sai lệch ≤ 1 phút. | Thiết kế |
| BR-U08-37 | Bài của môn được chấm bởi giảng viên của từng lớp: mỗi giảng viên chấm, chốt, công bố điểm cho sinh viên lớp mình (U15); bài của môn hiện trong sổ điểm của từng lớp. | Người dùng chốt 2026-10-09 |

## 5. Ngưng giao, nhân bản, version

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-40 | Ngưng giao: bài → `RETIRED`, lý do bắt buộc (ghi audit); người học không bắt đầu/nộp thêm; bài đã nộp giữ nguyên; trong cùng transaction gọi `AssignmentLifecyclePort.onRetired` (U11, U14 cài: tự nộp lượt/tài liệu đang dở). Bài của môn chỉ Chủ nhiệm môn ngưng giao. | Câu 4 |
| BR-U08-41 | Nhân bản: tạo bài `DRAFT` mới cùng lớp hoặc cùng môn, sao chép `assignment_questions` (ghim cùng version câu ngân hàng, sao chép câu riêng), `config.origin = CLONE`, `source_assignment_id`; cấu hình và rubric nhân bản qua `TypeConfigPort.copy` (BR-U06-36); audit. | Câu 5 |
| BR-U08-42 | Bài `CLOSED`/`RETIRED` ẩn khỏi danh sách mặc định (lọc theo trạng thái). | Thiết kế |
| BR-U08-43 | Bài `CLOSED`/`RETIRED` (quiz: `OPEN`/`RETIRED`): bấm "Sửa" tạo version kế tiếp (`version + 1`, `source_assignment_id` = bài cũ, `DRAFT`, `config.origin = NEW_VERSION`) cùng lớp hoặc môn, sao chép câu, cấu hình, rubric; bài cũ giữ nguyên cho bài nộp/lượt làm cũ. Version mới duyệt và phát hành như bài mới. | U10 Câu 5, 6 |
| BR-U08-44 | Mỗi bài nguồn tối đa một version `DRAFT` đang mở. Không có màn so sánh khác biệt giữa hai version (bỏ 2026-10-09). | Thiết kế; người dùng chốt 2026-10-09 |
| BR-U08-45 | Copy bài giữa lớp (luồng phụ của UC 42–45, gộp từ U10 cũ): giảng viên phải dạy cả lớp nguồn và lớp đích; khi tạo bài chọn nguồn "Copy bài lớp khác", chọn lớp nguồn mình dạy rồi bài cùng dạng và chế độ. Chỉ áp dụng cho bài của lớp; lớp nguồn hoặc đích ngoài quyền → "không tìm thấy". | FR-028, US-ASM-010; người dùng chốt 2026-10-09 |
| BR-U08-46 | Copy tạo bài `DRAFT` mới ở lớp đích (`source_assignment_id`, `config.origin = CLASS_COPY`) gồm câu (câu ngân hàng giữ tham chiếu version; câu riêng sao thành câu riêng mới qua `InlineQuestionPort.copyToAssignment`), điểm, hướng dẫn, cấu hình, khung và rubric (nhân bản qua `TypeConfigPort.copy`); không copy lịch, lượt làm, bài nộp, điểm; không đồng bộ hai chiều; chạy trong một transaction. | FR-028 |

## 6. Quiz luyện tập

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-60 | Quiz luyện tập gắn đúng một học liệu (`lesson_id`): quiz của lớp (Teacher hoặc Subject Manager được giao dạy, học liệu của lớp đó, soạn từ Teacher Class Detail) hoặc quiz của môn (Chủ nhiệm môn, học liệu của môn, soạn từ Quiz List trên Manager Dashboard; mọi lớp của môn thấy). Kiểm học liệu qua `ContentRefPort.getLessonRef` (U05). | UC 35; BR-U05-03; người dùng chốt 2026-10-09 |
| BR-U08-61 | Quiz tự chấm theo đáp án, không vào sổ điểm, không vào hàng chấm của Teacher, không có lịch mở/đóng, không nằm trong Student Assignments hay Teacher Assignment List. Sinh viên làm từ Learning Material hoặc Quiz Practice (U11). | UC 18–21, 22, 41; người dùng chốt 2026-10-09 |
| BR-U08-62 | Vòng đời quiz: `DRAFT` → `REVIEWED` → phát hành thành `OPEN` ngay → `RETIRED` khi ngưng. Số lượt làm theo cài đặt quiz (`max_attempts` 1-10 hoặc không giới hạn); thời gian làm, xáo câu, hiện đáp án sau khi nộp là cài đặt quiz do U09 soạn. | UC 35; người dùng chốt 2026-10-09 |
| BR-U08-63 | Sinh viên thấy quiz `OPEN` khi đang ghi danh lớp `OPEN` và học liệu của quiz còn hiện trong lớp; học liệu bị xóa (lưu trữ) thì quiz ẩn với sinh viên, lượt làm cũ vẫn xem được. | BR-U05-03 |
| BR-U08-64 | Phát hành version mới của quiz tự chuyển version cũ sang `RETIRED` trong cùng transaction. | Thiết kế |

## 7. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-50 | Audit: duyệt, phát hành (actor, lớp hoặc môn, lịch), sửa lịch, ngưng giao (lý do), nhân bản, tạo version mới, copy bài giữa lớp (nguồn, đích), xóa nháp, phát hành bị từ chối vì sai phạm vi. | FR-014, FR-028 |

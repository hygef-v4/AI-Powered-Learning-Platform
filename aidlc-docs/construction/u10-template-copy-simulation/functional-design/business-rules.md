# U10 Template & Copy - Business Rules

## 1. Template cấp môn

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-01 | Chỉ Chủ nhiệm môn hiện tại của môn (`subjects.manager_id`) tạo, sửa, duyệt, phát hành và xoá template của môn (dòng `assignments` có `subject_id`, soạn trong Template Editor), kể cả template do Chủ nhiệm môn trước tạo; không lưu người tạo trên bài (người tạo nằm trong audit). Giảng viên chỉ xem và copy template `RELEASED`. | FR-027, UC 21; người dùng chốt 2026-10-04 |
| BR-U10-02 | Template không có lịch mở/đóng; không giao thẳng cho lớp. | FR-027, không có đề chung |
| BR-U10-03 | Version template đã phát hành chỉ đọc; sửa template tạo version mới (quy tắc BR-U08-43 áp dụng: version đang phát hành không sửa). | FR-027, US-ASM-009 S2 |
| BR-U10-04 | Không có thao tác rút riêng: template chỉ vào `WITHDRAWN` khi Chủ nhiệm môn xoá template đã phát hành (BR-U10-07); không copy thêm, bản đã copy không bị ảnh hưởng. | UC 21 |
| BR-U10-05 | Giảng viên của lớp thuộc môn, sau khi đã chọn dạng và chế độ (BR-U08-17), copy một version `RELEASED` cùng dạng và chế độ thành bài `DRAFT` của lớp mình; lưu `source_assignment_id` và `config.origin = TEMPLATE_COPY`; không đồng bộ khi template có version mới. | US-ASM-009 S1 |
| BR-U10-06 | Tạo template (chọn dạng và chế độ trước) rồi soạn thủ công, hoặc trong Template Editor nhờ AI đề xuất câu từ RAG cấp môn (chỉ học liệu `class_id` NULL) qua `AiDraftPort` (U13, BR-U13-11). Câu Chủ nhiệm môn giữ lại (có thể sửa trước) lưu thành câu riêng của template như BR-U08-21; template Diagram Essay hoặc bài nhóm thì AI đề xuất khung kèm gợi ý rubric từng phần (BR-U09-24); bỏ thì template không đổi. Credit AI trừ của Chủ nhiệm môn. | FR-006, FR-027, UC 21 |
| BR-U10-07 | Xoá template: nếu chưa phát hành version nào thì xoá bản nháp (BR-U08-15); nếu đã phát hành thì mọi version còn `RELEASED` chuyển `WITHDRAWN` và ẩn khỏi danh sách. Không copy thêm được; bài lớp đã copy, lineage và audit giữ nguyên. | FR-027, UC 21 |
| BR-U10-08 | Template có thể là bài nhóm (`GROUP_ASSIGNMENT`, chỉ `GRADED`) với khung chia phần và rubric từng phần soạn bằng trình soạn khung U09 (UC 27). Template không gắn nhóm; giảng viên copy vào lớp rồi phát hành khi nhóm của lớp hợp lệ (U12). | screen flow (Template Editor), UC 27 |

## 2. Copy giữa lớp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-10 | Giảng viên phải dạy cả lớp nguồn và lớp đích; lớp đích ngoài quyền → "không tìm thấy". Khi tạo bài bằng cách copy từ lớp khác, chỉ liệt kê bài cùng dạng và chế độ đã chọn (BR-U08-17). | FR-028, US-ASM-010 S2 |
| BR-U10-11 | Copy tạo bài `DRAFT` mới ở lớp đích (identity mới, `source_assignment_id`, `config.origin = CLASS_COPY`), gồm câu, điểm, hướng dẫn, cấu hình loại bài, khung tài liệu. | FR-028 |
| BR-U10-12 | Không copy lịch, lượt làm, bài nộp, điểm. | FR-028 |
| BR-U10-13 | Câu ngân hàng cấp môn: giữ nguyên tham chiếu version. Câu ngân hàng cấp lớp nguồn: sao chép sang ngân hàng lớp đích qua `BankCopyPort` (U06, identity/version riêng) rồi trỏ tới bản mới. Câu riêng của bài/template nguồn: sao thành câu riêng mới của bài đích qua `InlineQuestionPort` (U06). Rubric từng câu (Text Essay) và từng phần (Diagram Essay, bài nhóm) luôn được nhân bản thành rubric mới của bài đích qua `TypeConfigPort.copy` (BR-U06-35). | FR-028 |
| BR-U10-14 | Không có copy rubric riêng giữa hai lớp; rubric đi theo bài khi copy (BR-U06-35). | FR-028 |

## 3. Version và diff

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-20 | Xem diff giữa hai version liên tiếp (theo `source_assignment_id`), hoặc giữa bài copy và nguồn. | US-ASM-008 S1, Câu 4 |
| BR-U10-21 | Người xem diff phải xem được cả hai bài (giảng viên lớp; Chủ nhiệm môn hiện tại với template của môn và bài copy từ template đó). | SEC-002 |
| BR-U10-22 | Diff hiển thị: hướng dẫn (theo dòng), câu thêm/bớt/đổi thứ tự/đổi điểm/đổi nội dung, cấu hình loại bài, tổng điểm. | US-ASM-008 |

## 4. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U10-40 | Audit: tạo, xoá, phát hành template và copy template/bài giữa lớp (nguồn, đích, actor). | FR-014, FR-027, FR-028 |

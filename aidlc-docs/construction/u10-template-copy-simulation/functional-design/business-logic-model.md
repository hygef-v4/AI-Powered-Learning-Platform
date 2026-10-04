# U10 Template & Copy - Business Logic Model

## F1 - Template
1. Chủ nhiệm môn hiện tại chọn dạng và chế độ rồi tạo template (dòng `assignments` có `subject_id`, `DRAFT`) và soạn trong Template Editor như bài thường (câu ngân hàng, câu riêng, rubric, cấu hình loại bài qua U06, U08, U09); có thể nhờ AI đề xuất câu, hoặc khung với Diagram Essay và bài nhóm (U13, U09), rồi xem nguồn, sửa, giữ hoặc bỏ (BR-U10-01, 06).
2. Duyệt (U08) → phát hành template: `REVIEWED` → `RELEASED`, nội dung khóa; audit (BR-U10-01…03).
3. Sửa: tạo version mới (`version + 1`, `source_assignment_id`) như BR-U08-43; phát hành version mới.
4. Xoá: kiểm Chủ nhiệm môn hiện tại của môn (BR-U10-01); chưa phát hành → xoá nháp qua U08; đã phát hành → chuyển mọi version `RELEASED` sang `WITHDRAWN` (BR-U10-07); audit.

## F2 - Copy template vào lớp
1. Trong bước chọn nguồn của tạo bài (U08 F1 bước 1a), giảng viên chọn template `RELEASED` của môn cùng dạng và chế độ đã chọn.
2. Tạo bài `DRAFT` trong lớp, sao chép `assignment_questions` (câu ngân hàng cấp môn giữ tham chiếu, câu cấp lớp qua `BankCopyPort`, câu riêng qua `InlineQuestionPort`, BR-U10-13), `TypeConfigPort.copy` (U09, nhân bản cấu hình và rubric từng câu/phần); `source_assignment_id`, `config.origin = TEMPLATE_COPY`; audit.

## F3 - Copy giữa lớp
1. Trong bước chọn nguồn của tạo bài (U08 F1 bước 1a), giảng viên chọn lớp nguồn mình cũng dạy rồi chọn bài cùng dạng và chế độ; kiểm BR-U10-10.
2. Như F2 bước 2 với `config.origin = CLASS_COPY` (BR-U10-11…13).

## F4 - Diff
1. Kiểm BR-U10-21.
2. Đọc hai version qua U08; so sánh theo BR-U10-22.

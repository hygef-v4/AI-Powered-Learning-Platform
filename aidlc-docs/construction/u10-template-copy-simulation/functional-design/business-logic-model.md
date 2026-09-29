# U10 Template & Copy - Business Logic Model

## F1 - Template
1. Chủ nhiệm môn tạo bài `SUBJECT_TEMPLATE` (U08) và soạn như bài thường.
2. Duyệt (U08) → phát hành template: version → `LOCKED`, tạo `TemplateRelease` `RELEASED`; audit (BR-U10-01…03).
3. Sửa: tạo version mới (U08 BR-U08-43) khi không muốn giữ bản cũ; phát hành lại → release mới.
4. Rút: `WITHDRAWN` (BR-U10-04).

## F2 - Copy template vào lớp
1. Giảng viên chọn template `RELEASED` của môn, chọn lớp mình dạy.
2. Tạo bài `DRAFT` trong lớp, sao chép thành phần (BR-U10-13), `TypeConfigPort.copy`; lineage `TEMPLATE_COPY`; audit.

## F3 - Copy giữa lớp
1. Kiểm BR-U10-10.
2. Như F2 bước 2 với lineage `CLASS_COPY` (BR-U10-11…13).

## F4 - Diff
1. Kiểm BR-U10-21.
2. Đọc hai version qua U08; so sánh theo BR-U10-22.

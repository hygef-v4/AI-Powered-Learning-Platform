# U06 Rubric & Question Bank - Business Logic Model

## F1 - Tạo và sửa bản nháp
1. Kiểm quyền theo phạm vi (BR-U06-01, 02).
2. Tạo `lineage_id` mới, `version = 1`, `DRAFT`; hoặc sửa `DRAFT` hiện có.
3. Sửa bản `ACTIVE` → sao chép thành `DRAFT` mới (BR-U06-11).
4. Kiểm `definition` ở mức lưu nháp (định dạng, độ dài).

## F2 - Kích hoạt
1. Kiểm đầy đủ theo loại (BR-U06-20…27, 30, 31).
2. `DRAFT` → `ACTIVE`, `activatedAt`; audit.

## F3 - Ngưng và xóa
1. Ngưng: `ACTIVE` → `RETIRED`; audit (BR-U06-14).
2. Xóa: chỉ `DRAFT` chưa từng kích hoạt (BR-U06-15).

## F4 - Nhân bản
1. Kiểm chiều nhân bản và quyền (BR-U06-03).
2. Sao chép `definition` câu hỏi sang `lineage_id` mới ở phạm vi đích, `DRAFT`. Rubric không nhân bản ở luồng này (BR-U06-35).

## F5 - Tìm kiếm và xem trước
1. Lọc theo phạm vi, loại, độ khó, tag, module/học liệu, từ khóa tiêu đề; trang ≤ 50 (BR-U06-13).
2. Xem trước: hiển thị như người học thấy, ẩn đáp án/test ẩn; người quản lý bật "hiện đáp án".

## F6 - Nhập hàng loạt
1. Chọn loại và phạm vi, tải mẫu tương ứng.
2. Đọc file (xlsx/csv), kiểm giới hạn (BR-U06-40).
3. Mỗi dòng: dựng `definition`, kiểm như F2 bước 1 , tạo `DRAFT` (BR-U06-41, 42).
4. Trả bảng kết quả; audit một sự kiện.

## F8 - Rubric theo đề
1. Tạo: U09 (trong `TypeConfigSlot` của Assignment Editor, Template Editor) gọi `RubricPort.createForAssignment(actor, assignmentScope, criteria)`; kiểm BR-U06-30, 31; lưu rubric `ACTIVE` cùng phạm vi bài; trả `rubricId` để U09 ghi vào `config` của bài (câu hoặc phần, BR-U06-34).
2. Sửa: `RubricPort.revise(rubricId, criteria)` (từ Assignment Editor/Template Editor) hoặc sửa trong Question Bank tạo phiên bản mới cùng `lineage_id` và kích hoạt ngay (BR-U06-30, 31, 36). Bài sở hữu còn `DRAFT`: U09 ghi `rubricId` mới vào câu hoặc phần (sửa trong trình soạn bài) hoặc U06 gọi `RubricOwnerPort.repoint(oldId, newId)` (sửa ở Question Bank). Bài đã duyệt/phát hành giữ version đã ghim (BR-U06-12).
3. Nhân bản theo đề: khi U08 nhân bản/tạo version hoặc U10 copy, U09 (`TypeConfigPort.copy`) gọi `RubricPort.cloneForAssignment(rubricId, targetScope)` → sao phiên bản `ACTIVE` mới nhất của rubric thành rubric mới (`lineage_id` mới) ở phạm vi đích, trả `rubricId` mới (BR-U06-35).

## F9 - Contract cho unit khác
- `getVersion(id)`: trả phiên bản bất kỳ trạng thái trừ `DRAFT` (U08 chỉ gắn bản `ACTIVE`).
- `score(rubricId, checkedItemIds)` cho U15 (BR-U06-32).
- `pickRandom(scope, filter, n, excludeIds)` cho U08: lấy ngẫu nhiên trong bản `ACTIVE` mới nhất mỗi `lineage_id` khớp dạng và bộ lọc (BR-U08-18).
- `InlineQuestionPort.save` cho U08 (câu riêng của bài), `BankCopyPort.copyToClass` cho U10, `QuestionVerificationPort.record` cho U13 (§5 domain).

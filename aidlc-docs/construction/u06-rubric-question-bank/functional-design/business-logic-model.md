# U06 Rubric & Question Bank - Business Logic Model

## F1 - Tạo và sửa bản nháp
1. Kiểm quyền theo phạm vi (BR-U06-01, 02).
2. Tạo `stableKey` mới, `versionNo = 1`, `DRAFT`; hoặc sửa `DRAFT` hiện có.
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
2. Sao chép `definition` sang `stableKey` mới ở phạm vi đích, `DRAFT`; rubric được tham chiếu không tự nhân bản (giữ ID nếu vẫn hợp lệ ở phạm vi đích, không thì bỏ).

## F5 - Tìm kiếm và xem trước
1. Lọc theo phạm vi, loại, độ khó, tag, chương/bài, từ khóa tiêu đề; trang ≤ 50 (BR-U06-13).
2. Xem trước: hiển thị như người học thấy, ẩn đáp án/test ẩn; người quản lý bật "hiện đáp án".

## F6 - Nhập hàng loạt
1. Chọn loại và phạm vi, tải mẫu tương ứng.
2. Đọc file (xlsx/csv), kiểm giới hạn (BR-U06-40).
3. Mỗi dòng: dựng `definition`, kiểm như F2 bước 1 , tạo `DRAFT` (BR-U06-41, 42).
4. Trả bảng kết quả; audit một sự kiện.

## F7 - Contract cho unit khác
- `getVersion(id)`: trả phiên bản bất kỳ trạng thái trừ `DRAFT` (U08 chỉ gắn bản `ACTIVE`).
- `score(rubricId, checkedItemIds)` cho U15 (BR-U06-32).

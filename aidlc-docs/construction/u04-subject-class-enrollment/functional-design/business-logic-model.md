# U04 Subject, Class, Enrollment & Learning Access - Business Logic Model

## F1 - Quản lý môn (ADMIN)
1. Tạo: kiểm `code` (BR-U04-02), lưu `ACTIVE`, audit.
2. Sửa: chỉ `name`, `description`; audit.
3. Gán Chủ nhiệm môn: tra U01, kiểm role (BR-U04-03), thay người cũ, audit.
4. Lưu trữ/mở lại: kiểm mọi lớp đã `ARCHIVED` (BR-U04-04), audit.

## F2 - Tạo lớp và gán giảng viên (ADMIN)
1. Tạo lớp trong môn `ACTIVE`, `DRAFT`, kiểm `code` trong môn (BR-U04-10, 11).
2. Gán/đổi giảng viên: tra U01, kiểm role (BR-U04-12), audit.

## F3 - Sửa và đổi trạng thái lớp (người quản lý lớp)
1. Kiểm quyền (BR-U04-13).
2. Sửa `name`, `description`, `term`.
3. Đổi trạng thái theo BR-U04-14; mở lại kiểm BR-U04-15.
4. `DRAFT→OPEN`: phát `ENROLLMENT_ACTIVATED` cho mọi ghi danh `ACTIVE` (BR-U04-26).
5. Audit.

## F4 - Ghi danh từng người
1. Tìm người học qua `AccountLookupPort` (BR-U04-23).
2. Áp BR-U04-20..22 trong một transaction.
3. Lớp `OPEN` → phát event; audit.

## F5 - Ghi danh theo danh sách
1. Nhận ≤ 200 email (dán hoặc CSV 1 cột); chuẩn hóa chữ thường, bỏ trùng trong danh sách.
2. Tra U01 theo lô.
3. Mỗi dòng áp F4 bước 2-3 và ghi kết quả (BR-U04-24).
4. Trả bảng kết quả; không lưu lô.

## F6 - Gỡ ghi danh
1. Kiểm quyền, chuyển `REMOVED`, `removedAt`, audit (BR-U04-25).

## F7 - Mã mời
1. Bật: sinh mã nếu chưa có (BR-U04-30), đặt hạn (BR-U04-31).
2. Tắt / đổi mã; audit.
3. Người học nhập mã: kiểm rate limit (BR-U04-34) → tìm lớp theo mã → kiểm BR-U04-32 → ghi danh nguồn `INVITE`, phát event. Sai → thông báo chung (BR-U04-33), tăng bộ đếm.

## F8 - Người học xem lớp
1. `listMyClasses`: ghi danh `ACTIVE`, chia "Đang học"/"Đã kết thúc" (BR-U04-40).
2. `getLearnerClass(classId)`: kiểm BR-U04-41, 42; trả thông tin lớp, giảng viên, nội dung qua `PublishedContentPort`. Tải file đi qua API của U05 (U05 hỏi `ClassAccessPort`).
3. Dashboard: frontend ghép danh sách lớp của U04 với API assignment (U08) và thông báo (U16) khi có.

## F9 - Contract cho unit khác
- `ClassScopePort`/`SubjectScopePort` cho U01 quyết định quyền và chặn hạ role.
- `ClassAccessPort` cho U05-U15 kiểm ghi danh và lấy danh sách người học.

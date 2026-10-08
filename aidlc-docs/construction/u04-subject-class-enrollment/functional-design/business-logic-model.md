# U04 Subject, Class, Enrollment & Learning Access - Business Logic Model

**Bản tài liệu 2026-10-08**: UC 12, 13, 27, 28, 45, 46, 47, 48, 49, 50, 63, 64, 65, 66; primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Quản lý môn (ADMIN)
1. Tạo: kiểm `code` (BR-U04-02), lưu `ACTIVE`, audit.
2. Sửa: chỉ `name`, `description`; audit.
3. Gán Chủ nhiệm môn: tra U01, kiểm role (BR-U04-03), thay người cũ, audit.
4. Lưu trữ/mở lại: kiểm mọi lớp đã `ARCHIVED` (BR-U04-04), audit.

## F2 - Tạo lớp và gán giảng viên (ADMIN hoặc Chủ nhiệm môn của môn)
1. Tạo lớp trong môn `ACTIVE`, `DRAFT`, kiểm `code` trong môn (BR-U04-10, 11).
2. Gán/đổi giảng viên: tra U01, kiểm role (BR-U04-12), audit.

## F3 - Sửa và đổi trạng thái lớp (ADMIN hoặc Subject Manager đúng môn)
1. Kiểm quyền (BR-U04-13).
2. Sửa `name`, `description`, `term`.
3. Đổi trạng thái theo BR-U04-14; mở lại kiểm BR-U04-15.
4. Bật/tắt `showGradeDistribution` kiểm quyền roster riêng BR-U04-13/17; mặc định tắt và U16 chỉ dùng khi đủ mẫu (BR-U04-17).
5. `DRAFT→OPEN`: phát `enrollment.activated` cho mọi ghi danh `ACTIVE` (BR-U04-26).
6. Audit.

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
2. `getStudentClass(classId)`: kiểm BR-U04-41, 42; trả thông tin lớp, giảng viên, nội dung qua `PublishedContentPort`. Tải file đi qua API của U05 (U05 hỏi `ClassAccessPort`).
3. `MyClassesPage` là My Classes, trang đích của Student sau khi đăng nhập (BR-U01-48); không có dashboard cá nhân.

## F9 - Contract cho unit khác
- `ClassScopePort`/`SubjectScopePort` cho U01 quyết định quyền và chặn hạ role.
- `ClassAccessPort` cho U05, U06, U08-U16 kiểm ghi danh và lấy danh sách người học.

## F10 - Danh sách/chi tiết theo quyền
My Classes/Class Detail Student UC 12–13 theo R5; Assigned Classes/Class Detail Teacher UC 27–28 theo teacher_id R3/R4. Subject Classes/Class Detail cấu trúc UC 45–49 theo môn được giao hoặc Admin Full; không tự cấp gradebook. Subject Detail tài nguyên môn R2, khác quyền GET cấu trúc Admin. ScopeQueryService trả isSubjectManager/isTeacherOf theo accountId, gồm Admin đã được giao; mất phân công thì request tiếp theo bị từ chối.

# U04 Subject, Class, Enrollment & Learning Access - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Quản lý môn (Admin, UC 64–67)
1. Subject List (UC 64): danh sách mọi môn, tìm theo mã/tên, lọc trạng thái, phân trang.
2. Thêm môn (UC 65): nhập `code`, `name`, `description`, chọn Chủ nhiệm môn (tra U01; phải `SUBJECT_MANAGER` `ACTIVE`, BR-U04-03); kiểm `code` (BR-U04-02); lưu `ACTIVE`; audit.
3. Subject Detail (UC 66): thông tin môn, Chủ nhiệm môn, danh sách lớp của môn chỉ đọc (BR-U04-05).
4. Sửa môn (UC 67): sửa `name`, `description`; đổi Chủ nhiệm môn (BR-U04-03); lưu trữ hoặc mở lại (BR-U04-04); kiểm `version`; audit.

## F2 - Tạo lớp và gán giảng viên (Subject Manager, UC 49–50)
1. Trên Class List, Chủ nhiệm môn bấm Tạo lớp; kiểm mình quản lý môn đó và môn `ACTIVE` (BR-U04-10).
2. Kiểm `code` trong môn (BR-U04-11); tạo lớp `DRAFT`; audit.
3. Trên Class Detail, gán/đổi giảng viên chính: tra U01, kiểm role `TEACHER`/`SUBJECT_MANAGER` `ACTIVE` (BR-U04-12); audit.

## F3 - Sửa lớp, đổi trạng thái, phân bố điểm (Subject Manager, UC 52)
1. Kiểm actor là Chủ nhiệm môn của môn chứa lớp (BR-U04-13).
2. Sửa `name`, `description`, `term` kèm `version`.
3. Đổi trạng thái theo BR-U04-14; mở lại kiểm BR-U04-15.
4. Bật/tắt `showGradeDistribution` (BR-U04-17).
5. `DRAFT→OPEN`: phát `enrollment.activated` cho mọi ghi danh `ACTIVE` (BR-U04-26).
6. Audit.

## F4 - Thêm từng sinh viên (Subject Manager, UC 51)
1. Kiểm actor là Chủ nhiệm môn của môn chứa lớp (BR-U04-20).
2. Tìm người học qua `AccountLookupPort` (BR-U04-23).
3. Áp BR-U04-20…22 trong một transaction.
4. Lớp `OPEN` → phát event; audit.

## F5 - Thêm sinh viên theo danh sách (Subject Manager, UC 51)
1. Nhận ≤ 200 email (dán hoặc CSV 1 cột); chuẩn hóa chữ thường, bỏ trùng trong danh sách.
2. Tra U01 theo lô.
3. Mỗi dòng áp F4 bước 3–4 và ghi kết quả (BR-U04-24).
4. Trả bảng kết quả; không lưu lô.

## F6 - Gỡ sinh viên (Subject Manager, UC 51)
1. Kiểm quyền, chuyển `REMOVED`, `removedAt`, audit (BR-U04-25).

## F7 - Mã mời (đã bỏ 2026-10-09)
Không còn tự ghi danh bằng mã mời; sinh viên chỉ vào lớp qua F4, F5.

## F8 - Student xem lớp (UC 13–14)
1. Class Dashboard (UC 13): `listMyClasses` trả ghi danh `ACTIVE`, chia "Đang học"/"Đã kết thúc" (BR-U04-40).
2. Student Class Detail (UC 14): `getStudentClass(classId)` kiểm BR-U04-41, 42; trả thông tin lớp, giảng viên, nội dung qua `PublishedContentPort`; lối vào Student Assignments (U11), Class Announcements (U05) và nhóm của mình (U12). Tải file đi qua API của U05 (U05 hỏi `ClassAccessPort`).

## F9 - Contract cho unit khác
- `ClassScopePort`/`SubjectScopePort` cho U01 quyết định quyền và chặn đổi role.
- `ClassAccessPort` cho U05, U06, U08–U16 kiểm ghi danh, lấy danh sách người học, lớp `OPEN` một người đang học hoặc dạy (feed thông báo của U05) và số đếm cho Admin Dashboard.

## F10 - Teacher xem lớp (UC 31–32)
1. Class Dashboard (UC 31): lớp mình là giảng viên chính, chia "Sắp mở"/"Đang dạy"/"Đã kết thúc" (BR-U04-45).
2. Teacher Class Detail (UC 32): kiểm actor là giảng viên chính (`isTeacherOf`); tab Class Detail hiện thông tin lớp chỉ đọc; tab Students hiện sinh viên `ACTIVE` (BR-U04-46).
3. Bấm một sinh viên → popup Student Detail: email, tên hiển thị, nhóm (frontend lấy từ API của U12), ngày ghi danh; chỉ đọc (BR-U04-47).

## F11 - Manager Dashboard và Subject Detail (Subject Manager, UC 53)
1. Đăng nhập xong, Subject Manager vào Manager Dashboard (BR-U01-48).
2. Trang hiện các môn mình quản lý và lối vào Subject Detail, Class List, Material List, Question List, Quiz List, Assignment List; nút sang Class Dashboard nếu có dạy lớp (BR-U04-48).
3. Subject Detail: thông tin môn và số lớp theo trạng thái; chỉ môn mình quản lý.

## F12 - Lớp của môn được giao (Subject Manager, UC 47–48)
1. Class List (UC 47): lớp của các môn mình quản lý, lọc môn/trạng thái/học kỳ.
2. Class Detail (UC 48): thông tin lớp, giảng viên chính, danh sách sinh viên; các thao tác F2 bước 3, F3, F4–F6 nằm trên màn này (BR-U04-49).
3. Mất phân công quản lý môn thì request tiếp theo bị từ chối; `ScopeQueryService` đọc phân công hiện thời, không cache.

-- U01: mã số sinh viên (BR-U01-100…102). Cột cho phép trống: tài khoản sinh viên cũ chưa có mã (BR-U01-104).
alter table accounts add column student_code varchar(8);

-- BR-U01-100: lưu ở dạng đã chuẩn hóa, 2 chữ cái in hoa và 6 chữ số.
alter table accounts add constraint accounts_student_code_format
    check (student_code is null or student_code ~ '^[A-Z]{2}[0-9]{6}$');

-- BR-U01-102: chỉ STUDENT có mã; không đổi role vào hoặc ra khỏi STUDENT nên ràng buộc luôn giữ (BR-U01-107).
alter table accounts add constraint accounts_student_code_role
    check (role = 'STUDENT' or student_code is null);

-- BR-U01-101: mỗi mã thuộc tối đa một tài khoản; cũng dùng cho tìm theo mã.
create unique index accounts_student_code_key on accounts (student_code) where student_code is not null;

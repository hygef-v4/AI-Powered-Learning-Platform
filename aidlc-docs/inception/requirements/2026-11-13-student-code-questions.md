# Mã số sinh viên — câu hỏi xác nhận

Câu hỏi đưa ra trong chat ngày 2026-11-13. Người dùng trả lời trong chat lúc 2026-11-13T15:04:23Z (nguyên văn trong `aidlc-docs/audit.md`). Tài liệu yêu cầu: [2026-11-13-student-code.md](2026-11-13-student-code.md).

## Q1 — Định dạng MSSV

A) Cố định trong code, 6–20 ký tự chữ và số
B) Admin cấu hình được
X) Khác

[Answer]: X — 2 chữ cái ở đầu, 6 chữ số ở sau; chữ cái tự đổi thành in hoa. Định dạng cố định trong code.

## Q2 — Tài khoản sinh viên đã có

A) Bắt buộc với tài khoản mới; tài khoản cũ được để trống cho tới khi cập nhật hàng loạt
B) Bắt buộc ngay với mọi tài khoản STUDENT
X) Khác

[Answer]: A

## Q3 — Ai được sửa MSSV sau khi tạo

A) Chỉ Admin, có ghi audit
B) Admin và chính sinh viên
X) Khác

[Answer]: A

## Q4 — Tài khoản đổi khỏi role STUDENT

A) Xóa MSSV
B) Giữ MSSV
X) Khác

[Answer]: X — không có chức năng đổi role từ STUDENT sang role khác, hay từ role khác sang STUDENT. Lịch sử thay đổi ngày 2026-11-13: ban đầu chọn A (xóa MSSV); lúc 15:48:13Z đổi sang giữ MSSV nhưng ẩn; lúc 15:50:59Z chốt bỏ hẳn việc đổi role liên quan đến STUDENT.

## Q5 — Phạm vi chức năng thêm

A) Đủ cả 3: cập nhật MSSV hàng loạt, ghi danh lớp bằng MSSV, lọc STUDENT chưa có MSSV
B) Chỉ cập nhật hàng loạt trước
X) Khác

[Answer]: A

## Q6 — Thu hẹp phạm vi: nhập sai MSSV khi đã bỏ chức năng đổi mã (2026-11-13)

A) Admin chỉ sửa được MSSV khi tài khoản còn `PENDING` (sinh viên chưa kích hoạt)
B) Không sửa được; sai thì sửa trực tiếp trong database
X) Khác

[Answer]: A

## Q7 — Thu hẹp phạm vi: bỏ thêm chức năng thiên về quản lý sinh viên (2026-11-13, chọn nhiều)

A) Bộ lọc "Sinh viên chưa có MSSV" (SC-08)
B) Ghi danh bằng MSSV (SC-09)
C) Tìm bạn mời nhóm theo MSSV (SC-11)
D) Tìm tài khoản theo MSSV (SC-08)

[Answer]: A — chỉ bỏ bộ lọc; giữ B, C, D.

## Q8 — Gán MSSV cho tài khoản sinh viên cũ chưa có mã (2026-11-14)

A) Bỏ hẳn: mọi sinh viên có MSSV từ lúc tạo; database vẫn cho phép cột trống
B) Bỏ hẳn và bắt buộc ở database: tài khoản STUDENT phải có MSSV
X) Khác

[Answer]: A — giữ cột cho phép trống. Gỡ luồng gán hàng loạt (F16) và trường hợp gán mã ở trang chi tiết; chỉ sửa MSSV khi tài khoản còn PENDING.

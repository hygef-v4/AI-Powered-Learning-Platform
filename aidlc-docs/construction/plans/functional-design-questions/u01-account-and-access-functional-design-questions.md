# U01 Account & Access - Functional Design Questions

Please answer each question after its `[Answer]:` tag. The current Account & Access story and service contract disagree about whether administrators can issue temporary passwords.

## Question 1 - Initial account credential

When an administrator creates or imports an account, how should the user establish the first password?

A) Keep the current service design: create the account in pending activation and email a one-time OTP; the user sets a password after verifying the OTP. Administrators never issue temporary passwords.

B) Keep the `US-IAM-007` behavior: the administrator can issue a one-time temporary password, and the user must change it at first login. Activation/reset OTP remains available where needed.

C) Support both flows, with the administrator choosing OTP activation or a forced-change temporary password for each create/import operation.

X) Other (please describe after `[Answer]:` below)

[Answer]:  A

## Follow-up 1 - Administrator support action

Temporary passwords are removed by the answer to Question 1. What may an administrator do for a user who cannot sign in?

A) Resend the activation OTP for pending accounts and trigger a password-reset OTP for active accounts.

B) Only resend the activation OTP for accounts still pending activation. Active users who forget their password use self-service password reset.

C) No replacement action; administrators only create, update, lock and unlock.

[Answer]: B - superseded by Follow-up 3

## Follow-up 2 - Synchronize inception artifacts

Should `US-IAM-007`, `FR-015` in `requirements.md` and `UC-IAM-01` be updated to match the OTP-only decision?

A) Update all three.

B) Record the decision only in U01 Functional Design.

[Answer]: A

## Follow-up 3 - When the activation OTP is sent

Sending an OTP on every account creation or import spends email quota on accounts that may never be used. When should the system send it?

A) Only when the user requests activation at first sign-in; creation and import send no email. Administrators have no resend action.

B) Only when the user requests activation at first sign-in, but administrators keep a resend action.

C) Automatically on creation or import.

[Answer]: A

## Vòng 2 - Câu hỏi Functional Design (tiếng Việt)

Hỏi qua giao diện chọn đáp án, ghi lại nguyên đáp án đã chọn.

### Câu 2 - Đăng nhập bằng tài khoản chưa kích hoạt

Người dùng có tài khoản chưa kích hoạt gõ email và mật khẩu vào màn đăng nhập thì hệ thống làm gì?

A) Màn đăng nhập có liên kết "Kích hoạt tài khoản lần đầu" dẫn sang luồng riêng; đăng nhập bằng tài khoản chưa kích hoạt chỉ nhận lỗi trung tính như sai mật khẩu.

B) Đăng nhập phát hiện tài khoản chưa kích hoạt thì tự chuyển sang bước gửi OTP (lộ việc email đã được cấp tài khoản).

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 3 - Chính sách mật khẩu

Chính sách mật khẩu áp dụng khi kích hoạt, đổi và đặt lại mật khẩu?

A) Tối thiểu 12 ký tự, chặn mật khẩu phổ biến hoặc bị lộ, không bắt buộc ký tự đặc biệt.

B) Tối thiểu 8 ký tự, có cả chữ và số.

C) Tối thiểu 8 ký tự, đủ chữ hoa, chữ thường, số và ký tự đặc biệt.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

### Câu 4 - Quy cách OTP

Quy cách mã OTP dùng chung cho kích hoạt và quên mật khẩu?

A) 6 chữ số, hạn 10 phút, nhập sai 5 lần thì hủy mã; mã mới làm mã cũ hết hiệu lực.

B) 6 chữ số, hạn 5 phút, nhập sai 3 lần thì hủy mã.

C) 8 ký tự chữ-số, hạn 15 phút, nhập sai 5 lần thì hủy mã.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 5 - Nhập sai mật khẩu nhiều lần

Nhập sai mật khẩu nhiều lần thì xử lý thế nào?

A) Sai 5 lần thì khóa tạm 15 phút qua `locked_until`, tự mở, kèm giới hạn theo IP.

B) Sai 5 lần thì khóa, admin phải mở.

C) Không khóa tài khoản, chỉ giới hạn theo IP.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 6 - Đổi role hoặc phạm vi khi người dùng đang đăng nhập

Admin đổi role hoặc phạm vi của một người đang đăng nhập thì phiên của họ xử lý thế nào?

A) Thu hồi mọi phiên bằng cách tăng `credential_version`, người dùng phải đăng nhập lại.

B) Giữ phiên, đọc quyền mới ở mỗi request.

C) Cache quyền ngắn hạn, quyền mới có hiệu lực sau tối đa vài phút.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 7 - Ranh giới U01 và U04 khi gán Chủ nhiệm môn

US-IAM-005 nói gán role kèm phạm vi môn, còn UC-CAT-04 của U04 là phân công Chủ nhiệm môn. Chia việc thế nào?

A) U01 chỉ đặt `accounts.role`; U04 gán người đó vào môn cụ thể.

B) U01 gán cả role lẫn môn trong một thao tác.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 8 - Hạ role của người đang phụ trách môn hoặc lớp

Admin hạ role của người đang là Chủ nhiệm môn hoặc giảng viên chính của lớp thì sao?

A) Chặn và báo đang phụ trách môn/lớp nào; phải gỡ phân công ở U04 trước.

B) Cho hạ và tự gỡ phân công.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 9 - Bảo vệ tài khoản ADMIN

Bảo vệ tài khoản ADMIN thế nào?

A) Admin không tự hạ role hay tự khóa chính mình; hệ thống luôn còn ít nhất một ADMIN đang hoạt động.

B) Chỉ chặn trường hợp hạ hoặc khóa ADMIN cuối cùng.

C) Không chặn.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 10 - Trạng thái tài khoản

ERD có hai trạng thái LOCKED và DISABLED. Khác nhau thế nào?

A) LOCKED là khóa tạm, DISABLED là người đã rời trường nhưng admin vẫn bật lại được.

B) DISABLED là vĩnh viễn, không mở lại được.

C) Gộp thành một trạng thái.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: X - Gộp lại thành DISABLED. Chỉ còn PENDING_ACTIVATION, ACTIVE, DISABLED; admin khóa/mở khóa là ACTIVE ⇄ DISABLED; khóa tạm 15 phút sau 5 lần sai là bộ đếm `locked_until`, không phải trạng thái (đã xác nhận).

### Câu 11 - Trường hồ sơ người dùng tự sửa

Người dùng được tự sửa những trường nào trong hồ sơ?

A) Chỉ tên hiển thị.

B) Tên hiển thị, số điện thoại, ảnh đại diện.

C) Tên hiển thị và mã sinh viên/giảng viên.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

### Câu 12 - Ảnh đại diện và phụ thuộc U01 ↔ U03

Ảnh đại diện phải đi qua U03 File & Artifact, trong khi U03 phụ thuộc U01 để kiểm quyền. Xử lý thế nào?

A) Đảo ngược qua port: U01 khai báo `AvatarPort`, U03 cung cấp implementation; tên hiển thị và số điện thoại xong ngay, ảnh chạy khi U03 xong.

B) Bỏ ảnh đại diện.

C) Đẩy ảnh đại diện sang Phase 2.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 13 - Nhập tài khoản hàng loạt

Định dạng file và xử lý email đã tồn tại khi nhập hàng loạt?

A) Chỉ CSV, tối đa 1000 dòng mỗi lần; email đã tồn tại báo lỗi dòng, không ghi đè; nhập lại cùng file không tạo trùng.

B) CSV hoặc Excel, email trùng thì cập nhật.

C) CSV hoặc Excel, email trùng báo lỗi.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

### Câu 14 - Đăng xuất

Đăng xuất và quản lý phiên đăng nhập?

A) Đăng xuất phiên hiện tại, kèm nút đăng xuất khỏi mọi thiết bị.

B) Chỉ đăng xuất phiên hiện tại; muốn đá mọi thiết bị thì đổi mật khẩu.

C) Hiển thị danh sách thiết bị và thu hồi từng phiên.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

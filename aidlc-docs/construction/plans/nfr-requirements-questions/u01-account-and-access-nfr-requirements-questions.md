# U01 Account & Access - Câu hỏi NFR Requirements

Hỏi qua giao diện chọn đáp án; ghi nguyên đáp án cuối cùng.

## Câu N1 - Kiểu phiên

Kiểu phiên đăng nhập giữa Next.js và Spring Boot?

A) Session cookie opaque, dữ liệu phiên ở Redis.

B) JWT access ngắn + refresh token trong cookie HttpOnly, refresh lưu ở Redis.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu N2 - Thời hạn phiên

Thời hạn phiên?

A) Idle 30 phút, tối đa 8 giờ.

B) Idle 2 giờ, tối đa 7 ngày.

C) Admin idle 15 phút/tối đa 4 giờ, người thường idle 30 phút/tối đa 8 giờ.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu N3 - MFA admin

Requirements bắt buộc admin có MFA. Chọn cách nào?

A) MFA bằng OTP email chỉ cho admin.

B) MFA bằng TOTP app chỉ cho admin.

C) Bỏ MFA, chấp nhận rủi ro; sửa SEC-002, ghi ngoại lệ SECURITY-12.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: C (trả lời ban đầu: "admin ko cần mfa")

## Câu N4 - Thuật toán băm

Thuật toán băm mật khẩu?

A) Argon2id.

B) bcrypt.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu N5 - Thu hồi với JWT

Với JWT, thu hồi quyền có hiệu lực khi nào?

A) Access 5 phút + kiểm credentialVersion mỗi request.

B) Access 5 phút, chấp nhận trễ.

C) Access 15 phút, chấp nhận trễ.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: C

## Câu N6 - Kiểm mật khẩu bị lộ

SEC-002 yêu cầu kiểm danh sách mật khẩu bị lộ. Dùng nguồn nào?

A) Danh sách offline đóng gói sẵn.

B) API Have I Been Pwned.

C) Cả hai.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: X - "bỏ sec002 đi"; làm rõ: chỉ bỏ MFA admin và kiểm mật khẩu bị lộ, giữ các yêu cầu còn lại của SEC-002

## Câu N7 - Rate limit OTP

Ngưỡng giới hạn yêu cầu OTP?

A) Chờ 60 giây, tối đa 5 lần/giờ mỗi email, 20 lần/giờ mỗi IP.

B) Chờ 120 giây, tối đa 3 lần/giờ mỗi email.

C) Chờ 60 giây, tối đa 10 lần/ngày mỗi email.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu N8 - Dịch vụ email

Dịch vụ gửi email OTP?

A) SMTP qua adapter; local dùng Mailpit; production cấu hình SMTP.

B) Gmail API của Google Workspace.

C) SendGrid hoặc Amazon SES.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu N9 - Redis lỗi

Redis sập thì đăng nhập và OTP xử lý sao?

A) Từ chối an toàn.

B) Lưu refresh token ở PostgreSQL.

C) Chỉ cấp access token, không refresh.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A (trả lời ban đầu: "Cho đăng nhập, tạm bỏ rate limit"; đổi sau khi biết refresh token nằm ở Redis)

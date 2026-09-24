# U01 Account & Access - Tech Stack Decisions

| Hạng mục | Chọn | Lý do | Nguồn |
|---|---|---|---|
| Ngôn ngữ/framework backend | Java + Spring Boot | Đã chốt ở requirements | NFR-001 |
| Bảo mật | Spring Security | Có sẵn bcrypt, filter chain, xử lý cookie | NFR-001 |
| Phiên | JWT access 15 phút + refresh token trong cookie HttpOnly | Người dùng chọn; kiểm access không cần tra Redis | Câu N1, N5 |
| Ký JWT | HMAC-SHA256 với khóa bí mật lấy từ secret | Một backend duy nhất ký và kiểm, không cần khóa công khai | NFR-U01-10 |
| Lưu refresh, OTP, rate limit | Redis | Đã chốt ở ERD; có TTL và bộ đếm nguyên tử | ERD §3.4 |
| Dữ liệu tài khoản | PostgreSQL | Đã chốt ở ERD | ERD |
| Băm mật khẩu | bcrypt, cost ≥ 12 | Người dùng chọn | Câu N4 |
| Gửi mail | SMTP qua Mail Port; demo dùng Gmail SMTP + App Password | Miễn phí, đổi nhà cung cấp chỉ bằng cấu hình | Câu N8, REL-005 |
| Mail local/demo | Mailpit trong container | Không tốn lượt mail thật, xem được OTP khi test | Câu N8 |
| Gửi mail bất đồng bộ | Job của U02 + worker | Lỗi mail không làm hỏng yêu cầu | BR-U01-92 |
| Đọc CSV | Thư viện CSV chuẩn của Java (Apache Commons CSV hoặc tương đương) | Xử lý đúng dấu phẩy và ngoặc kép trong tên | BR-U01-80 |
| Frontend | Next.js + TypeScript | Đã chốt ở requirements | NFR-001 |
| Test | JUnit, Testcontainers (PostgreSQL, Redis, Mailpit) | Test với phụ thuộc thật trong container | NFR-004 |

## Không chọn

| Lựa chọn | Lý do bỏ |
|---|---|
| MFA (TOTP, OTP email, passkey) | Người dùng quyết định bỏ, ngoại lệ SECURITY-12 |
| Kiểm mật khẩu bị lộ (offline hoặc HIBP) | Người dùng quyết định bỏ, ngoại lệ SECURITY-12 |
| Session cookie opaque | Người dùng chọn JWT |
| Argon2id | Người dùng chọn bcrypt |
| Lưu refresh token ở PostgreSQL | Người dùng chọn fail-closed khi Redis lỗi |

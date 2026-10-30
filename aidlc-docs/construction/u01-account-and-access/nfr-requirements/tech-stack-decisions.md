# U01 Account & Access — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u01-account-and-access` là thiết kế của U01 hiện hành.

| Hạng mục | Chọn | Lý do | Nguồn |
|---|---|---|---|
| Ngôn ngữ/framework backend | Java + Spring Boot | Đã chốt ở requirements | NFR-001 |
| Bảo mật | Spring Security | Có sẵn bcrypt, filter chain, xử lý cookie | NFR-001 |
| Phiên | JWT access 15 phút + refresh token trong cookie HttpOnly | Người dùng chọn; kiểm chữ ký local, rồi kiểm session/account authority hiện hành phía server | Câu N1, N5 |
| Ký JWT | HMAC-SHA256 với khóa bí mật lấy từ secret | Một backend duy nhất ký và kiểm, không cần khóa công khai | NFR-U01-10 |
| Lưu refresh, OTP, rate limit | Redis | Đã chốt ở Application Design; có TTL và bộ đếm nguyên tử | Application Design |
| Dữ liệu tài khoản | PostgreSQL | Đã chốt ở Application Design | Application Design |
| Băm mật khẩu | bcrypt, cost ≥ 12 | Người dùng chọn | Câu N4 |
| Gửi mail | SMTP qua Mail Port; demo dùng Brevo SMTP + SMTP key | Dùng trong hạn mức miễn phí của Brevo; đổi nhà cung cấp SMTP bằng cấu hình | Câu N8, REL-005 |
| Mail local/demo | Mailpit trong container | Không tốn lượt mail thật, xem được OTP khi test | Câu N8 |
| Gửi mail bất đồng bộ | Job của U03 + worker | Lỗi mail không làm hỏng yêu cầu | BR-U01-92 |
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

## Quy tắc tích hợp revision hiện hành

- Tài khoản trường cấp, không public registration. Admin tạo/import không gửi OTP hoặc đặt mật khẩu; người dùng yêu cầu activation ở login đầu và tự đặt mật khẩu sau OTP.
- Account role cao nhất trong STUDENT/TEACHER/SUBJECT_MANAGER/ADMIN; authorization theo action/resource/current state, không chỉ role JWT. Admin chỉ quản trị, không học vụ.
- Profile cá nhân chỉ displayName/phoneNumber, email định danh/role/status bất biến trong /me. Reset không tiết lộ account tồn tại.
- MCP credentials xác thực actor qua U06 và cùng IAuthorization; không dùng provider API key làm ALAP authentication. Student không shared quota.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Tạo/import không gửi OTP, activation hợp lệ/sai/hết hạn/rate limit; recovery không lộ account.
- Đăng nhập/đăng xuất/phiên revoked; R1 own profile; Admin bị chặn học vụ; Student shared quota bị từ chối.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

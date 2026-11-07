# Gửi email qua API HTTPS của Brevo

Yêu cầu ngày 2026-11-06: "sửa backend để gửi mail qua api https của brevo cho tôi". VPS production (xvps) chặn mọi cổng SMTP đi ra ngoài (587, 465, 2525 của Brevo, Gmail, Resend, SendGrid). Cổng 443 tới `api.brevo.com` vẫn mở. Vì vậy OTP kích hoạt tài khoản (U01) và email thông báo (U14) không gửi được.

## Phân tích yêu cầu (mức tối giản)

- Phạm vi: chỉ thay cách chuyển email của `IMail`. Không đổi nội dung email, OTP, giới hạn gửi theo ngày, thử lại hay trạng thái gửi.
- Cấu hình bằng biến môi trường `MAIL_TRANSPORT`:
  - `smtp`: mặc định, giữ nguyên hành vi hiện tại. Local tiếp tục dùng Mailpit.
  - `brevo-api`: gửi bằng `POST https://api.brevo.com/v3/smtp/email`, xác thực bằng header `api-key`.
  - Giá trị khác thì backend không khởi động.
- Khóa API lấy từ biến `BREVO_API_KEY`. Đây là API key của Brevo, khác với SMTP key. Người gửi vẫn đọc từ `SMTP_FROM`, chấp nhận cả dạng `Tên <email>`.
- Bảo mật:
  - Địa chỉ API cố định, người dùng không đổi được.
  - Không log khóa, nội dung email hay địa chỉ người nhận.
  - Thông báo lỗi chỉ có mã HTTP và mã lỗi của Brevo.
- Độ bền:
  - Timeout kết nối 5 giây, timeout cả yêu cầu 10 giây, giống timeout SMTP.
  - Mọi lỗi ném ra ngoại lệ để job đang gọi thử lại như với SMTP (BR-U01-92, U14).
  - Thiếu khóa hoặc thiếu người gửi thì báo lỗi lúc gửi, không làm backend unhealthy (giống cách xử lý `SMTP_FROM` hiện tại).
- Không cần user story, thiết kế ứng dụng hay chia unit: đây là adapter thứ hai sau một port có sẵn của U01. Phạm vi bảo mật và độ bền giữ như cũ; PBT vẫn tắt.

## Kế hoạch

- [x] `BrevoApiMailAdapter` (U01 integration) cài `IMail` bằng `java.net.http.HttpClient`, theo cách làm của `HttpProviderGateway`.
- [x] Chọn adapter theo `platform.identity.mail-transport`. `SmtpMailAdapter` là mặc định khi biến không được đặt.
- [x] `application.yml`: thêm `MAIL_TRANSPORT` và `BREVO_API_KEY`. `infra/docker-compose.yml` và `infra/.env.example`: truyền hai biến này cho backend và worker.
- [x] Test với HTTP server giả của JDK, kiểm tra:
  - nội dung JSON gửi đi, header `api-key`, tên người gửi;
  - email chỉ có văn bản;
  - lỗi HTTP 401 và 429, timeout;
  - thiếu khóa, thiếu người gửi.
- [x] Test chọn bean: mặc định là SMTP; `brevo-api` dùng adapter mới; giá trị lạ làm backend không khởi động.
- [x] Chạy test backend liên quan và build backend.
- [x] Cập nhật runbook `docs/runbooks/deploy-vps.md`, `aidlc-state.md` và kết quả kiểm tra.

## Kết quả

- Cả hai lần chạy đều trong container `maven:3.9-eclipse-temurin-17`, có Testcontainers.
  - Test liên quan: 41/41 qua (adapter 6, chọn bean 3, OTP 7, gửi thông báo 21, stub 4).
  - `mvn -B clean verify` toàn backend: 1.773 qua, không lỗi, không bỏ qua; build được JAR. Hai suite PostgreSQL bắt buộc (31 + 2) qua.
- Lần verify đầu có 9 lỗi do môi trường: chỉ mount `backend/` nên test không thấy `../infra` và `../agent-skills`. Chạy lại với cả repo thì qua hết.
- Thêm log cảnh báo trong adapter khi Brevo từ chối hoặc không gọi được. Lý do: log thử lại của job không ghi nguyên nhân, nếu không có log này thì sai khóa API sẽ không ai thấy.
- Chưa kiểm:
  - Chưa gọi API Brevo thật, vì chưa có tài khoản và khóa.
  - Chưa build image hay deploy lên VPS.
  - Chưa commit.

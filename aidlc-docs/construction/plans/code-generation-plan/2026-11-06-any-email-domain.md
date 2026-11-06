# Bỏ giới hạn tên miền email

Yêu cầu ngày 2026-11-06: "tôi muốn bỏ phần tên miền email được phép đăng ký, cho phép bất kỳ email nào. sửa cả code và đồng bộ doc luôn cho tôi".

## Phân tích yêu cầu (mức tối giản)

- Bỏ BR-U01-03 phần "thuộc tên miền trong `U01_ALLOWED_EMAIL_DOMAINS`". Áp dụng cho:
  - Admin tạo một tài khoản;
  - nhập tài khoản bằng CSV;
  - yêu cầu OTP (kích hoạt, đặt lại mật khẩu);
  - Admin đầu tiên.
- Mọi email đúng định dạng và dài tối đa 254 ký tự đều được chấp nhận.
- Các điều kiện còn giữ: email duy nhất, chuẩn hóa (BR-U01-02), định dạng.
- Bỏ hẳn biến `U01_ALLOWED_EMAIL_DOMAINS` và mã lỗi `EMAIL_DOMAIN_NOT_ALLOWED`, không giữ chế độ tùy chọn. Biến này còn sót trong `.env` cũ thì không ảnh hưởng gì.
- **Không** thêm tự đăng ký. Tài khoản vẫn do Admin tạo, người dùng kích hoạt bằng OTP gửi tới email của mình (BR-U01-10, US-IAM). Vì vậy người lạ không tự vào hệ thống được. Cái giá: hệ thống không còn chặn Admin nhập nhầm email ngoài trường.
- Không đổi schema. Tên trường `school_email`/`schoolEmail` giữ nguyên để tránh migration; tài liệu ghi rõ đây là email đăng nhập, thuộc tên miền nào cũng được.
- Không cần user story mới hay thiết kế ứng dụng. Phạm vi bảo mật và độ bền giữ như cũ; PBT vẫn tắt.

## Kế hoạch

- [x] `EmailPolicy` thành tiện ích không trạng thái, chỉ kiểm định dạng và độ dài.
- [x] Bỏ `allowedEmailDomains` khỏi `IdentityProperties`, `application.yml`, `infra/docker-compose.yml`, `infra/.env.example`.
- [x] Sửa `AccountAdminService`, `AccountImportService`, `OtpService`, `BootstrapAdminInitializer`.
- [x] Frontend: bỏ nhãn `EMAIL_DOMAIN_NOT_ALLOWED`, sửa test.
- [x] Test backend:
  - `EmailPolicyTest` kiểm email ngoài trường được nhận;
  - tạo một tài khoản và nhập CSV chấp nhận mọi tên miền;
  - Admin đầu tiên dùng tên miền bất kỳ.
- [x] Chạy toàn bộ backend; chạy test, typecheck và lint frontend.
- [x] Đồng bộ tài liệu: requirements, business rules, domain entities, business logic model, frontend components, code summary của U01, main business flows, README E2E, runbook deploy, aidlc-state.

## Kết quả

- Backend (`mvn -B clean verify` trong container Maven, có Testcontainers): 1.776 qua, không lỗi, không bỏ qua. Hai suite PostgreSQL bắt buộc (31 + 2) qua.
- Test mới hoặc đã sửa:
  - `EmailPolicyTest`: nhận gmail và subdomain, chặn email sai định dạng hoặc quá dài.
  - Tạo một tài khoản gmail; nhập CSV có tên miền khác.
  - `BootstrapAdminInitializerTest`: Admin đầu tiên dùng gmail; bỏ qua email sai hoặc khi đã có Admin.
- Frontend: 38 test identity (10 tệp) qua; typecheck và lint phần identity admin qua.
- Tham số `IdentityProperties` không còn dùng đã được bỏ khỏi `OtpService`, `AccountAdminService` và `AccountImportService`.
- Chưa làm:
  - Chưa chạy lại toàn bộ frontend và E2E.
  - Chưa commit, chưa deploy.
  - Trên VPS, `.env` còn dòng `U01_ALLOWED_EMAIL_DOMAINS=gmail.com`. Bản đang chạy vẫn chỉ nhận gmail cho tới khi deploy bản mới; sau đó dòng này không còn tác dụng.

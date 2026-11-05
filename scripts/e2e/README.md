# Kiểm thử end-to-end U02, U06, U12 trên stack local

Các suite cần OTP phải đợi cả backend **và worker** healthy: OTP được sinh và gửi qua job `OTP_DELIVERY`, nên chỉ kiểm tra health của backend là chưa đủ để chạy fixture trên stack mới khởi tạo.

Script Node (>= 20) gọi API qua Nginx `http://localhost:8088`, kèm cookie và CSRF như trình duyệt. OTP kích hoạt đọc từ Mailpit (`http://localhost:8025`).

1. Chạy stack: `docker compose -p platform-local -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up -d --build`.
   `infra/.env.local` là file gitignored. Cần có `U01_BOOTSTRAP_ADMIN_EMAIL`, `U01_ALLOWED_EMAIL_DOMAINS=fpt.edu.vn` và `TEST_ACCOUNT_PASSWORD`.
2. Tạo dữ liệu thử: `node scripts/e2e/seed.mjs`. Script tạo admin, chủ nhiệm môn, giảng viên, 5 sinh viên `*.test@fpt.edu.vn` và lớp SE-TEST1, rồi ghi `seed.json`.
3. Chạy test: `node scripts/e2e/u02.mjs`, `u06.mjs`, `u06b.mjs`, `u12.mjs` (chia nhóm, chạy khi lớp chưa có nhóm), `u12b.mjs` (bài nhóm, có chờ hạn nộp khoảng 3 phút).

Mỗi ca in `PASS` hoặc `FAIL` kèm mã ca theo kế hoạch kiểm thử của nhóm. Kết quả ghi vào `*-result.json`, file này không commit.
Khóa AI dùng trong script là khóa thử (sai). Các ca cần AI thật phải test tay.
Kết quả lần chạy 2026-10-24: `aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md`.

## U04, U13, U14 trên stack cô lập

`u04-u13-u14.mjs` tự tạo tài khoản duy nhất theo mỗi lần chạy (Chủ nhiệm môn, Giảng viên, hai Sinh viên thuộc `fpt.edu.vn`), kích hoạt bằng OTP Mailpit, rồi tạo môn/lớp/module/video riêng. Dữ liệu thử được giữ lại để kiểm tra. Chỉ chạy trên stack dùng database và Mailpit riêng; không dùng stack đang chứa dữ liệu cá nhân.

Script bắt buộc khai báo bốn biến môi trường dưới đây, chỉ nhận origin loopback và từ chối cổng `8088`. Ví dụ PowerShell (đổi cổng và thông tin đăng nhập theo stack cô lập):

```powershell
$env:E2E_BASE_URL = "http://localhost:18088"
$env:E2E_MAILPIT_URL = "http://localhost:18025"
$env:U01_BOOTSTRAP_ADMIN_EMAIL = "admin.isolated@fpt.edu.vn"
$env:TEST_ACCOUNT_PASSWORD = "<password configured for the isolated stack>"
$env:E2E_RESULT_FILE = ".tmp/e2e/u04-u13-u14-result.json" # tùy chọn
node scripts/e2e/u04-u13-u14.mjs
```

Phạm vi: U04 tạo/xem/sửa môn, xung đột version, gán chủ nhiệm, lưu trữ/mở lại và phân quyền; U13 module, tạo/thay video có list/index và lưu URL chuẩn, các dạng URL hiện có, từ chối playlist/link không an toàn, quyền đọc/ghi; U14 đúng loại email và mặc định của cả bốn vai trò, lưu qua đăng nhập lại, loại không hợp lệ, email backup bắt buộc và chống sửa tùy chọn tài khoản khác.

Mỗi ca in `PASS`/`FAIL`; có lỗi thì exit code là `1`. Không in mật khẩu, OTP hay token. JSON kết quả tùy chọn chỉ được ghi trong `.tmp/` (gitignored). Kiểm tra cú pháp không gọi API: `node --check scripts/e2e/u04-u13-u14.mjs`.

Các script cũ vẫn mặc định dùng `localhost:8088` và Mailpit `localhost:8025`. Helper dùng chung nhận `E2E_BASE_URL`/`E2E_MAILPIT_URL` khi có; `env(key)` ưu tiên biến môi trường rồi mới đọc `infra/.env.local`, và không yêu cầu file này tồn tại.

# Kiểm thử end-to-end U02, U06, U12 trên stack local

Script Node (>= 20) gọi API qua Nginx `http://localhost:8088`, kèm cookie và CSRF như trình duyệt. OTP kích hoạt đọc từ Mailpit (`http://localhost:8025`).

1. Chạy stack: `docker compose -p platform-local -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up -d --build`.
   `infra/.env.local` là file gitignored. Cần có `U01_BOOTSTRAP_ADMIN_EMAIL`, `U01_ALLOWED_EMAIL_DOMAINS=fpt.edu.vn` và `TEST_ACCOUNT_PASSWORD`.
2. Tạo dữ liệu thử: `node scripts/e2e/seed.mjs`. Script tạo admin, chủ nhiệm môn, giảng viên, 5 sinh viên `*.test@fpt.edu.vn` và lớp SE-TEST1, rồi ghi `seed.json`.
3. Chạy test: `node scripts/e2e/u02.mjs`, `u06.mjs`, `u06b.mjs`, `u12.mjs` (chia nhóm, chạy khi lớp chưa có nhóm), `u12b.mjs` (bài nhóm, có chờ hạn nộp khoảng 3 phút).

Mỗi ca in `PASS` hoặc `FAIL` kèm mã ca theo kế hoạch kiểm thử của nhóm. Kết quả ghi vào `*-result.json`, file này không commit.
Khóa AI dùng trong script là khóa thử (sai). Các ca cần AI thật phải test tay.
Kết quả lần chạy 2026-10-24: `aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md`.

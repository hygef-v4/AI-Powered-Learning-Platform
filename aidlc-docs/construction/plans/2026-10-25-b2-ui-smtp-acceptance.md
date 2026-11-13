# UI và SMTP local — nghiệm thu B2/B8

Người dùng yêu cầu “kiểm giúp tôi” sau khi thống nhất UI + Mailpit local. Tiếp tục nhánh `fix/notification-links-reminders-u04-tests`; mọi thay đổi source/tài liệu trước đó được giữ nguyên.

- [x] 1. Kiểm stack hiện hữu và cấu hình SMTP; xác nhận Mailpit chưa chạy, không có credential test được cung cấp.
- [x] 2. Dựng stack QA cô lập, backend/worker từ JAR đã verify hiện tại, frontend hiện có; DB, Redis, RabbitMQ, Mailpit và cổng riêng. Kiểm hash JAR runtime và health, không thay stack platform.
- [x] 3. Tạo tài khoản/dữ liệu QA qua API và SMTP activation local; bài CLASS và SUBJECT với ít nhất hai lớp/người học. Không thay/reset mật khẩu tài khoản hiện có.
- [x] 4. Dùng trình duyệt đăng nhập tài khoản QA, kiểm chuông → bài đúng lớp cho thông báo mở bài và nhắc hạn; kiểm người học không truy cập được lớp ngoài quyền. Lưu screenshot bằng chứng.
- [x] 5. Kiểm SMTP thật vào Mailpit từ worker: thư mở bài/nhắc hạn, URL đúng và mở được trên UI; theo dõi dedup/lượt scan. B8 failure injection ở cấp runtime chỉ thực hiện nếu cô lập được trên DB QA; không mở rộng cửa sổ 5 phút.
- [x] 6. Ghi kết quả/giới hạn, cập nhật report/state/audit và quyết định giữ hay dừng stack QA sau nghiệm thu. Không commit/push/deploy ngoài local.

Mailpit nhận SMTP thật trong môi trường thử, không chứng nhận provider Internet/hộp thư ngoài. Không gửi thông tin hoặc thư tới người thật.

Kết quả: tất cả ca trong phạm vi đạt; xem `construction/build-and-test/2026-10-25-b2-ui-smtp-acceptance-results.md`. Đã dừng cả 8 container QA, giữ container/volume/bằng chứng để có thể chạy lại. Stack platform vẫn đủ 7 container healthy; không thêm thay đổi application source trong lượt nghiệm thu.

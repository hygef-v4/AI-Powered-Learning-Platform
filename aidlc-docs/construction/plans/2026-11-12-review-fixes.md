# Sửa lỗi sau rà soát — 2026-11-12

Người dùng cho phép sửa các lỗi trong báo cáo. Giữ Security/Resiliency, PBT tắt; không deploy/commit.

- [x] BUG-01: giữ đáp án và màn hình khi thoát mà lưu lỗi; regression test qua.
- [x] BUG-02: quota rejection riêng không đánh dấu rollback-only; không nuốt lỗi DB.
- [x] BUG-04: ngân sách job theo số kết nối và retry; ngắt retry khi interrupt; đồng bộ scanner.
- [x] BUG-03: người dùng chọn giữ bài cũ, lượt mới theo lớp học lại; tách slot IN_PROGRESS, số lượt, giới hạn và truy vấn chi tiết theo lớp; migration không sửa dữ liệu cũ.
- [x] Backend regression: 88 tests, không lỗi/skip; migration PostgreSQL 16 qua. Frontend quiz: 35 tests; scoped lint và TypeScript qua. Kết quả cuối tại `../build-and-test/2026-11-13/2026-11-13-review-fixes-results.md`.

Yêu cầu ngày 12/11: "Sửa toàn bộ các lỗi vừa tìm được cho tôi". Làm rõ tiếp: "nhưng khi sinh viên đã vào lớp rồi là không được chuyển lớp nữa". Chưa tự đổi lớp sở hữu lượt quiz.

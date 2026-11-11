# Tổng quan thông tin lớp giảng viên

Người dùng đã duyệt đề xuất và yêu cầu triển khai. Làm inline, không subagent, không commit/push/deploy.

- [x] Đổi thông tin chỉ đọc thành văn bản: tên/mã môn, mã lớp, sĩ số đang học, trạng thái; mô tả trống thì ẩn. Giữ trang sinh viên và các tab khác.
- [x] Thay tiến độ nộp bài bằng Tổng quan bài tập: số OPEN, lượt PENDING và DRAFT/FINALIZED chưa công bố. Dùng summary phía server để không nhân số bài nhóm theo thành viên; không tải toàn bộ gradebook/sinh viên.
- [x] Tối đa3 hạn nộp tương lai gần nhất từ bài OPEN, gồm cả Practice; không đưa bài hết hạn/không hạn/nháp/đã đóng. Dùng liên kết Bài tập/Chấm điểm có sẵn.
- [x] Bỏ đăng ký thẻ thông báo trên tổng quan giảng viên, giữ nguyên thông báo và trang sinh viên. Loading/error/retry/empty không hiển thị số0 giả.
- [x] Test tính tổng, nhóm, trạng thái legacy, thứ tự/giới hạn hạn nộp, liên kết, lỗi/empty và thông tin lớp. Typecheck/lint/test liên quan và kiểm UI desktop/mobile.

Security/Resiliency giữ phạm vi hiện có; không đổi API/quyền ghi, không thêm lịch/buổi học/tiến độ học liệu; PBT tắt. Phạm vi chỉ frontend và tài liệu.

## Kết quả xác minh

- RED4 test tổng quan thất bại trước thay đổi. GREEN264 test/31 file grading, academics, announcements; sau chỉnh liên kết chọn bài cần xử lý,34 test/2 file liên quan qua. TypeScript, scoped ESLint và whitespace qua. Không cộng số đếm có giao nhau. Logs: .tmp/class-overview-red.log, class-overview-tests.log, class-overview-final.log.
- UI local lớp SE1933:5 bài mở,1 lượt chờ chấm,0 chưa công bố; đúng3 hạn tăng dần. Bấm số chờ chấm mở đúng bài có lượt PENDING và bộ lọc Chờ chấm, tránh trang trống của bài đầu tiên. Số thống kê là tổng lớp; link mở bài đầu tiên có công việc tương ứng, các bài khác vẫn chọn trong Chấm điểm.
- Đã xem ảnh desktop1440px, mobile390px và dark mode; không tràn ngang ở390px, không vùng cuộn riêng/thanh tiến độ/thông báo gần đây trong tổng quan. Trả browser về light/desktop. Không ghi điểm hay dữ liệu lớp trong lượt này.
- Dùng thời điểm tải dữ liệu để lọc hạn tương lai; cập nhật số/hạn khi tải lại trang. Không thêm realtime/timer, không backend/migration. Chưa chạy lại production build/full backend cho thay đổi frontend này; chưa commit/push/deploy.

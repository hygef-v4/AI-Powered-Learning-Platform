# Tổng quan lớp sinh viên đồng bộ với giảng viên

Người dùng yêu cầu bỏ Thông báo gần đây trên trang Thông tin lớp sinh viên, thay bằng nội dung đồng bộ với giao diện giảng viên. Thực hiện inline theo quyền tự xử lý lựa chọn UI trong phiên; giữ mọi thay đổi bảng điểm đang có, không commit/push/deploy.

Thiết kế: một thẻ Tổng quan bài tập đủ chiều rộng, cùng kiểu tổng quan giảng viên. Ba liên kết thống kê Chưa làm, Đang làm, Đã nộp mở đúng tab bài tập của lớp. Bên dưới giữ bộ lọc cá nhân/nhóm/luyện tập và danh sách bài cần làm theo hạn nộp hiện có. Dùng cùng một lần đọc bài tập của sinh viên; không thêm số liệu lịch/buổi học, không gọi API giảng viên hay dữ liệu điểm nháp. Chưa tải/lỗi không hiển thị số0 giả. Thông báo lớp và Tất cả thông báo vẫn còn ở menu.

- [x] Gỡ slot thông báo gần đây và component hết caller; đổi board sang toàn chiều rộng, giữ phần thông tin/giảng viên/nhóm/học liệu.
- [x] Thêm thống kê cùng tiêu chí tab Bài tập, giữ list/bộ lọc/hạn nộp. Loại bài nhóm đã nộp khỏi Bài cần làm ở helper dùng chung để count/link thống nhất.
- [x] Test số liệu cá nhân/nhóm/luyện tập, nhóm đã nộp/đang làm, liên kết có classId, loading/error/empty và slot registration; typecheck/lint/build và UI sáng/tối, desktop/mobile.
- [x] Ghi kết quả/state/audit và phạm vi chưa kiểm tra. Không commit/push.

Security/Resiliency giữ phạm vi đã duyệt; PBT disabled. Chỉ frontend và tài liệu, không backend/migration hay ghi bài nộp.

[Kết quả kiểm tra](../../build-and-test/2026-11-11-student-class-overview.md): full frontend972/972,138 tệp; kiểm cuối7/7 và lint/typecheck/build qua. UI local sáng/tối, desktop/mobile và link thống kê đã đối chiếu; chưa test VPS/điện thoại thật.

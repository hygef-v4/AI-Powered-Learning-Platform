# Tổng quan lớp sinh viên — kiểm tra 2026-11-11

Requested by: Quang Hung. Nhánh `fix/u05-u09-u15`; giữ toàn bộ thay đổi bảng điểm trước đó, chưa commit/push/deploy.

- Bỏ slot/component Thông báo gần đây hết caller. Giữ menu Thông báo lớp/Tất cả thông báo, API và phân quyền hiện có.
- Một thẻ Tổng quan bài tập đủ chiều rộng, cùng cách trình bày số liệu giảng viên: Chưa làm/Đang làm/Đã nộp mở đúng tab trong lớp. Dùng một lần đọc danh sách bài của chính sinh viên, cùng tiêu chí tab Bài tập, không số0 giả khi loading/error.
- Giữ danh sách cần làm, thứ tự hạn và lọc cá nhân/nhóm/luyện tập; cao tối đa5 dòng, ít bài co theo nội dung. Bài nhóm đã nộp không còn lọt vào `upcoming` dùng chung cho tổng quan/danh sách Bài tập.
- Như trang Bài tập hiện có, bài từng nộp rồi đang sửa lại có thể thuộc cả Đã nộp và Đang làm; ba số không phải tổng số bài hoặc phần trăm hoàn thành. Không thêm thống kê điểm, lịch hay số buổi.

## Bằng chứng

- RED2 test tổng quan trước triển khai; GREEN109 test/20 tệp academics/assignmentwork/announcements/registration. Full frontend **972/972**,138 tệp. Hai tệp cuối **7/7** sau thay đổi CSS gọn danh sách; có ca nhóm đang làm/đã nộp, bài đóng, luyện tập, link class/tab, loading/error/empty và slot không còn thông báo.
- Full ESLint, TypeScript và production build32 trang qua. Lượt scoped lint/typecheck/build cuối cũng qua. `git diff --check` sạch. Log local `.tmp/student-overview-*` không đưa vào Git.
- Orca profile tách riêng `Student overview check`, tài khoản test student2; phiên teacher mặc định được giữ. UI localhost3000 lớp SWD392/SE1933 hiển thị2 Chưa làm,1 Đang làm,3 Đã nộp; không còn Thông báo gần đây. Link Đang làm mở đúng tab và một dòng bài; số trên trang Bài tập khớp2/1/3. Bộ lọc nhóm chuyển thành0 và trạng thái trống.
- Xem ảnh desktop1440×1000 tối và mobile390×844 sáng. Đo document390px bằng viewport390px; không tràn ngang. Đã trả profile kiểm tra về light/desktop và lọc cá nhân. Không ghi/nộp bài, sửa điểm hay thông báo.

Chỉ frontend và tài liệu; không chạy lại backend/VPS vì không đổi backend. Kiểm thử responsive bằng trình duyệt mô phỏng, chưa dùng điện thoại thật. Các cảnh báo CSP có sẵn giữ nguyên, không nới CSP. Security/Resiliency giữ phạm vi đã duyệt; PBT disabled.

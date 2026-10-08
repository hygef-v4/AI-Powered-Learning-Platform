# U16 Reporting & Notification - Business Rules

**Bản tài liệu 2026-10-08**: UC 11, 57; primary stories: US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Thông báo

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-01 | Thông báo tạo từ event sau khi giao dịch nghiệp vụ đã commit; U16 lỗi không làm rollback nghiệp vụ. | US-NTF-001 S2 |
| BR-U16-02 | Mỗi `(sourceEventId, recipientId, type)` tạo một thông báo; event lặp không tạo trùng. | US-NTF-001 S2 |
| BR-U16-03 | Nội dung chỉ về chính người nhận; không ghi điểm số trong thông báo/email, chỉ báo "có điểm mới" và đường dẫn. | US-NTF-001 S1 |
| BR-U16-04 | Thông báo trong app hiển thị realtime (SSE qua fanout `platform.realtime` và `SseHub` của U14, kênh theo `accountId`) và danh sách có đánh dấu đã đọc. | Câu 4, UC 11 |
| BR-U16-05 | Giữ thông báo 180 ngày rồi xóa. | Thiết kế |
| BR-U16-06 | Nhận sự kiện thông báo lớp từ U05 (bình luận dưới thông báo không có sự kiện); chỉ tạo thông báo trong app cho người học còn quyền trong lớp, trừ actor, và chống trùng theo BR-U16-02. | US-CNT-004 |

## 2. Email

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-10 | Loại có email: ghi danh, bài mới mở, điểm công bố, nhắc hạn nộp. Loại khác chỉ trong app. | Câu 1 |
| BR-U16-11 | Người dùng tắt/bật email từng loại; tắt → `SKIPPED`. Lưu ở `accounts.email_preferences`. | Câu 3 |
| BR-U16-12 | Trần 300 email thông báo/ngày (giờ Việt Nam, cấu hình); vượt → `DEFERRED` sang ngày sau theo thứ tự tạo; ưu tiên: nhắc hạn > bài mới mở > điểm > ghi danh. Email OTP (U01) không tính vào trần này. | NFR-U04-20, REL-005 |
| BR-U16-13 | Nhắc hạn nộp bị dời qua sau hạn → hủy (`SKIPPED`), không gửi muộn vô nghĩa. | Thiết kế |
| BR-U16-14 | Gửi qua queue `jobs.email` của U03: lỗi SMTP retry theo backoff U03 tối đa 5 lần rồi `FAILED`; không gửi trùng (idempotent theo `notifications.id` và `email_status`). | US-NTF-001 S2 |

## 3. Nhắc hạn nộp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-20 | Chỉ nhắc tự động, một lần, 24 giờ trước `closes_at` của bài; giảng viên không nhắc tay. | Câu 2 |
| BR-U16-21 | Người nhận: người học đang ghi danh chưa có bài nộp (bài cá nhân) hoặc nhóm chưa nộp (bài nhóm: gửi cho mọi thành viên). | US-RPT-001 S2 |
| BR-U16-22 | Bài ngừng giao, đã đóng, hoặc đổi hạn: hủy/lên lịch lại theo hạn mới. Bài mở muộn hơn thời điểm nhắc → không nhắc. | US-RPT-001 S2 |

## 4. Báo cáo tiến độ

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-30 | Giảng viên lớp xem theo bài: đã nộp, đang làm, chưa bắt đầu, nộp trễ, thời gian còn lại; bài nhóm theo nhóm (đã nộp/chưa, số mục xong). | US-RPT-001 S1 |
| BR-U16-31 | Tiến độ bài/bài nhóm chỉ tài khoản được giao dạy lớp R3/R4. Subject Manager/Administrator chỉ quản lý môn/cấu trúc không đủ; ngoài quyền 404. | FR-002/024, UC 34 hỗ trợ |
| BR-U16-32 | Báo cáo thống kê độ lệch điểm AI đề xuất và điểm giảng viên chốt nằm ngoài phạm vi. Thống kê quản trị và xuất bảng điểm thuộc MVP; dashboard cá nhân của Student đã bỏ (2026-10-03). | Quyết định phạm vi 2026-09-25, 2026-10-03 |

## 5. Thống kê quản trị, phân bố điểm và xuất bảng điểm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-40 | Thống kê hiện trên Statistic (trang đích của Admin, cùng các nút dẫn tới trang quản lý; không có popup riêng), chỉ cho `ADMIN`: số tài khoản theo vai trò × trạng thái (`PENDING`, `ACTIVE`, `DISABLED`), số môn theo trạng thái (`ACTIVE`, `ARCHIVED`), số lớp theo trạng thái (`DRAFT`, `OPEN`, `ARCHIVED`) và số ghi danh `ACTIVE`. Vai trò khác bị từ chối `403`, không trả số liệu. | US-RPT-002 S1, S2, FR-024 |
| BR-U16-41 | Số liệu thống kê đếm mỗi lần mở Statistic, không lưu, không cache; chỉ trả số đếm, không trả tên, email hay ID. Mỗi tài khoản có một `role` nên được đếm đúng một lần; Statistic hiện thêm tổng số tài khoản. | US-RPT-002 S1, SEC-005 |
| BR-U16-42 | Phân bố điểm lớp chỉ hiện khi `showGradeDistribution` của U04 bật và có ít nhất 20 người học có điểm `PUBLISHED` cho bài đó; trả các khoảng điểm tổng hợp, ẩn khoảng có dưới 5 người, không trả tên/điểm cá nhân người khác. Phân bố hiện cạnh bài tương ứng trên các danh sách bài theo loại của Student (U11 nhúng `GradeDistributionBadge`, badge gọi API của U16); không còn dashboard cá nhân. | US-RPT-001 S3, SEC-005 |
| BR-U16-43 | Xuất bảng điểm UC 37 chỉ Teacher/Subject Manager/Administrator được giao dạy R3/R4. Kiểm mỗi lớp/bài/bộ lọc trước CSV/XLSX; ngoài quyền 404; không cấp chỉ vì phụ trách môn. | FR-024, US-RPT-003 |
| BR-U16-44 | Tệp xuất chỉ gồm bài `GRADED`: Student, lớp/bài, trạng thái nộp/chấm, thời gian nộp, điểm cuối đã chốt hoặc đã công bố và phản hồi theo quyền Teacher; đánh dấu rõ chưa nộp/chưa chốt. Không có cột điểm tổng/hệ số, đề xuất AI hoặc kết quả `PRACTICE`. | US-RPT-003 S1, BR-U15-50, FR-030 |
| BR-U16-45 | Tạo tệp khi yêu cầu, trả stream cho người có quyền; không lưu tệp xuất lâu dài. Giá trị CSV được escape để tránh công thức bảng tính; tên tệp không chứa dữ liệu cá nhân. Audit người xuất, phạm vi và thời gian. | US-RPT-003 S2, SEC-003 |

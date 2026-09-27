# U16 Reporting & Notification - Business Rules

## 1. Thông báo

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-01 | Thông báo tạo từ event sau khi giao dịch nghiệp vụ đã commit; U16 lỗi không làm rollback nghiệp vụ. | US-NTF-001 S2 |
| BR-U16-02 | Mỗi `(sourceEventId, recipientId, type)` tạo một thông báo; event lặp không tạo trùng. | US-NTF-001 S2 |
| BR-U16-03 | Nội dung chỉ về chính người nhận; không ghi điểm số trong thông báo/email, chỉ báo "có điểm mới" và đường dẫn. | US-NTF-001 S1 |
| BR-U16-04 | Thông báo trong app hiển thị realtime (SSE dùng chung hạ tầng U14) và danh sách có đánh dấu đã đọc. | Câu 4, UC-OPS-01 |
| BR-U16-05 | Giữ thông báo 180 ngày rồi xóa. | Thiết kế |
| BR-U16-06 | Nhận sự kiện thông báo lớp, câu hỏi và câu trả lời từ U05; chỉ tạo thông báo trong app cho người nhận còn quyền trong lớp, trừ actor, và chống trùng theo BR-U16-02. | US-CNT-004 |

## 2. Email

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-10 | Loại có email: ghi danh, bài mới mở, điểm công bố, nhắc hạn nộp. Loại khác chỉ trong app. | Câu 1 |
| BR-U16-11 | Người dùng tắt/bật email từng loại; tắt → `SKIPPED`. | Câu 3 |
| BR-U16-12 | Trần 300 email thông báo/ngày (giờ Việt Nam, cấu hình); vượt → `DEFERRED` sang ngày sau theo thứ tự tạo; ưu tiên: nhắc hạn > bài mới mở > điểm > ghi danh. Email OTP (U01) không tính vào trần này. | NFR-U04-20, REL-005 |
| BR-U16-13 | Nhắc hạn nộp bị dời qua sau hạn → hủy (`SKIPPED`), không gửi muộn vô nghĩa. | Thiết kế |
| BR-U16-14 | Gửi qua job U02: lỗi SMTP retry theo backoff U02 tối đa 5 lần rồi `FAILED`; không gửi trùng (idempotent theo `EmailOutbox.id`). | US-NTF-001 S2 |

## 3. Nhắc hạn nộp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-20 | Chỉ nhắc tự động, một lần, 24 giờ trước `closesAt` của publication; giảng viên không nhắc tay. | Câu 2 |
| BR-U16-21 | Người nhận: người học đang ghi danh chưa có bài nộp (bài cá nhân) hoặc nhóm chưa nộp (bài nhóm: gửi cho mọi thành viên). | US-RPT-001 S2 |
| BR-U16-22 | Publication ngừng giao, đã đóng, hoặc đổi hạn: hủy/lên lịch lại theo hạn mới. Bài mở muộn hơn thời điểm nhắc → không nhắc. | US-RPT-001 S2 |

## 4. Báo cáo tiến độ

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-30 | Giảng viên lớp xem theo lượt phát hành: đã nộp, đang làm, chưa bắt đầu, nộp trễ, thời gian còn lại; bài nhóm theo nhóm (đã nộp/chưa, số mục xong). | US-RPT-001 S1 |
| BR-U16-31 | Chỉ giảng viên lớp, Chủ nhiệm môn của môn, ADMIN xem; ngoài quyền `404`. | SEC-002 |
| BR-U16-32 | Báo cáo thống kê độ lệch điểm AI đề xuất và điểm giảng viên chốt nằm ngoài phạm vi. Dashboard cá nhân và xuất bảng điểm thuộc MVP. | Quyết định phạm vi 2026-09-25 |

## 5. Dashboard người học và xuất bảng điểm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-40 | Dashboard chỉ cho tài khoản người học đang đăng nhập: lớp đang ghi danh, bài sắp hạn, trạng thái lượt/bài nộp của chính mình và điểm `PUBLISHED` từ U15; điểm chờ chấm/chưa công bố hiện trạng thái, không lộ số. | US-RPT-002 S1 |
| BR-U16-41 | Dashboard không tính điểm tổng hay hệ số; sắp bài sắp hạn tăng dần, không đưa bài đã ngừng giao vào danh sách cần làm. | Quyết định 2026-09-25 |
| BR-U16-42 | Phân bố điểm lớp chỉ hiện khi `showGradeDistribution` của U04 bật và có ít nhất 20 người học có điểm `PUBLISHED` cho bài đó; trả các khoảng điểm tổng hợp, ẩn khoảng có dưới 5 người, không trả tên/điểm cá nhân người khác. | US-RPT-002 S2, SEC-005 |
| BR-U16-43 | Chỉ giảng viên lớp, Chủ nhiệm môn được gán môn hoặc ADMIN xuất bảng điểm lớp/bài mình được xem. Kiểm toàn bộ bộ lọc trước khi tạo CSV/XLSX; ngoài quyền `404`. | US-RPT-003 |
| BR-U16-44 | Tệp xuất gồm người học, lớp/bài, trạng thái nộp/chấm, thời gian nộp, điểm cuối đã chốt hoặc đã công bố và phản hồi theo quyền giảng viên; đánh dấu rõ chưa nộp/chưa chốt. Không có cột điểm tổng/hệ số; không xuất đề xuất điểm AI. | US-RPT-003 S1, BR-U15-50 |
| BR-U16-45 | Tạo tệp khi yêu cầu, trả stream cho người có quyền; không lưu tệp xuất lâu dài. Giá trị CSV được escape để tránh công thức bảng tính; tên tệp không chứa dữ liệu cá nhân. Audit người xuất, phạm vi và thời gian. | US-RPT-003 S2, SEC-003 |

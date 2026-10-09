# U16 Reporting & Notification - Business Rules

**Bản tài liệu 2026-10-09**: UC 12, 58 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); luồng phụ: phân bố điểm của UC 22, tiến độ nộp của UC 37, phần xuất tệp của UC 40; primary stories: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Thông báo

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-01 | Thông báo tạo từ event sau khi giao dịch nghiệp vụ đã commit; U16 lỗi không làm rollback nghiệp vụ. | US-NTF-001 S2 |
| BR-U16-02 | Mỗi `(sourceEventId, recipientId, type)` tạo một thông báo; event lặp không tạo trùng. | US-NTF-001 S2 |
| BR-U16-03 | Nội dung chỉ về chính người nhận; không ghi điểm số trong thông báo/email, chỉ báo "có điểm mới" và đường dẫn. | US-NTF-001 S1 |
| BR-U16-04 | Thông báo trong app hiển thị realtime (SSE qua fanout `platform.realtime` và `SseHub` của U14, kênh theo `accountId`). UC 12 không có màn riêng: chuông và popup thông báo trên thanh điều hướng của mọi vai trò, có danh sách phân trang và đánh dấu đã đọc/tất cả đã đọc. | Câu 4, UC 12 |
| BR-U16-05 | Giữ thông báo 180 ngày rồi xóa. | Thiết kế |
| BR-U16-06 | Thông báo lớp mới (`class.announcement-posted` của U05) chỉ tạo thông báo trong app (không email) cho người học ghi danh `ACTIVE` của lớp, trừ actor, chống trùng theo BR-U16-02. Sửa/xóa thông báo lớp không báo lại; không có bình luận dưới thông báo (bỏ 2026-10-09). | US-CNT-004, BR-U05-64 |
| BR-U16-07 | Mỗi thông báo có `link` tới màn liên quan (`domain-entities.md` §2). Mở thông báo không cấp quyền: màn đích kiểm quyền hiện hành, mất quyền (ví dụ bị gỡ khỏi lớp) thì màn đích từ chối; thông báo vẫn đánh dấu đã đọc. | US-NTF-001 S3 |
| BR-U16-08 | Chuông hiện cho mọi vai trò. Hiện chưa có loại nào gửi cho Admin (Admin không vào lớp, không có ví), nên popup của Admin trống. | Quyết định 2026-10-09 |
| BR-U16-09 | Bài hoặc quiz của môn (`assignment.opened` có `subjectId`): người nhận là người học ghi danh `ACTIVE` của mọi lớp `OPEN` thuộc môn tại thời điểm xử lý event; người học nhiều lớp cùng môn không xảy ra (BR-U04-22), nếu có vẫn chỉ một thông báo nhờ BR-U16-02. | FR-027, quyết định 2026-10-09 |

## 2. Email

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-10 | Loại có email: ghi danh (`ENROLLED`), bài tập mới mở (`ASSIGNMENT_OPENED`), điểm công bố (`GRADE_PUBLISHED`), nhắc hạn nộp (`DEADLINE_REMINDER`). Loại khác chỉ trong app, gồm quiz luyện tập mới phát hành (`QUIZ_PUBLISHED`). | Câu 1, quyết định 2026-10-09 |
| BR-U16-11 | Người dùng tắt/bật email từng loại có email; tắt → `SKIPPED`. Lưu ở `accounts.email_preferences`; thông báo trong app luôn bật. | Câu 3 |
| BR-U16-12 | Trần 300 email thông báo/ngày (giờ Việt Nam, cấu hình); vượt → `DEFERRED` sang ngày sau theo thứ tự tạo; ưu tiên: nhắc hạn > bài mới mở > điểm > ghi danh. Email OTP (U01) không tính vào trần này. | NFR-U04-20, REL-005 |
| BR-U16-13 | Nhắc hạn nộp bị dời qua sau hạn → hủy (`SKIPPED`), không gửi muộn vô nghĩa. | Thiết kế |
| BR-U16-14 | Gửi qua queue `jobs.email` của U03: lỗi SMTP retry theo backoff U03 tối đa 5 lần rồi `FAILED`; không gửi trùng (idempotent theo `notifications.id` và `email_status`). | US-NTF-001 S2 |
| BR-U16-15 | Cài đặt email nằm trong popup thông báo (không có màn cài đặt riêng) và chỉ hiện cho Student, vì bốn loại có email chỉ gửi cho người học. | UC 12, quyết định 2026-10-09 |

## 3. Nhắc hạn nộp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-20 | Chỉ nhắc tự động, một lần, 24 giờ trước `closes_at` của bài tập; quiz luyện tập không có lịch nên không nhắc; giảng viên không nhắc tay. | Câu 2, FR-019 |
| BR-U16-21 | Người nhận: người học ghi danh `ACTIVE` chưa có bài nộp (bài cá nhân) hoặc thành viên của nhóm chưa nộp (bài nhóm). Bài của môn tính trên mọi lớp `OPEN` của môn. | US-RPT-001 S2 |
| BR-U16-22 | Bài ngừng giao, đã đóng, hoặc đổi hạn: hủy/lên lịch lại theo hạn mới. Bài mở muộn hơn thời điểm nhắc → không nhắc. | US-RPT-001 S2 |

## 4. Tiến độ nộp bài

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-30 | Tiến độ là luồng phụ của UC 37, hiện trên Submission Detail (U15) của một bài: đã nộp, đang làm, chưa bắt đầu, nộp trễ, thời gian còn lại; bài nhóm theo nhóm (đã nộp/chưa, số mục xong). Bài của môn tính riêng cho người học của lớp đang xem. | US-RPT-001 S1, FR-019 |
| BR-U16-31 | Chỉ tài khoản được giao dạy lớp (R3/R4: Teacher hoặc Subject Manager được giao dạy) xem tiến độ lớp đó. Chủ nhiệm môn chỉ có R2 (kể cả với bài của môn) không đủ; Admin không có quyền lớp. Ngoài quyền `404`. | FR-002/019, UC 37 |
| BR-U16-32 | Báo cáo độ lệch điểm AI đề xuất và điểm giảng viên chốt nằm ngoài phạm vi. Admin Dashboard và xuất bảng điểm thuộc MVP; dashboard cá nhân của Student đã bỏ (2026-10-03). | Quyết định phạm vi 2026-09-25, 2026-10-03 |

## 5. Admin Dashboard, phân bố điểm và xuất bảng điểm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U16-40 | Admin Dashboard (UC 58, trang đích `/admin` của Admin) chỉ cho `ADMIN`: số tài khoản theo vai trò × trạng thái (`PENDING`, `ACTIVE`, `DISABLED`), số môn theo trạng thái (`ACTIVE`, `ARCHIVED`), số lớp theo trạng thái (`DRAFT`, `OPEN`, `ARCHIVED`) và số ghi danh `ACTIVE`. Vai trò khác bị từ chối `403`, không trả số liệu. | US-RPT-002 S1, S2, FR-024 |
| BR-U16-41 | Số liệu đếm mỗi lần mở Admin Dashboard, không lưu, không cache; chỉ trả số đếm, không trả tên, email hay ID. Mỗi tài khoản có một `role` nên được đếm đúng một lần; trang hiện thêm tổng số tài khoản. | US-RPT-002 S1, SEC-005 |
| BR-U16-42 | Phân bố điểm là luồng phụ của UC 22, hiện cạnh bài trên Student Assignments (U11 nhúng `GradeDistributionBadge`) khi lớp bật `showGradeDistribution` (Chủ nhiệm môn bật trong UC 52) và lớp có ít nhất 20 người học có điểm `PUBLISHED` cho bài đó; trả các khoảng điểm tổng hợp, ẩn khoảng có dưới 5 người, không trả tên/điểm cá nhân người khác. Chỉ bài `GRADED`; bài của môn tính theo từng lớp (điểm công bố theo lớp); quiz và kết quả `PRACTICE` không có phân bố. | US-RPT-001 S3, SEC-005 |
| BR-U16-43 | Xuất bảng điểm là phần xuất của UC 40 (Gradebook do U15 hiển thị), chỉ tài khoản được giao dạy lớp R3/R4. Kiểm lớp/bài/bộ lọc trước khi tạo CSV/XLSX; ngoài quyền `404`; không cấp chỉ vì phụ trách môn; Admin không xuất. | FR-024, US-RPT-003 |
| BR-U16-44 | Tệp xuất chỉ gồm bài `GRADED` của lớp (bài của lớp và bài của môn giao cho lớp): Student, lớp/bài, trạng thái nộp/chấm, thời gian nộp, điểm cuối đã chốt hoặc đã công bố và phản hồi theo quyền Teacher; đánh dấu rõ chưa nộp/chưa chốt. Không có cột điểm tổng/hệ số, đề xuất AI, quiz hoặc kết quả `PRACTICE`. | US-RPT-003 S1, BR-U15-50, FR-030 |
| BR-U16-45 | Tạo tệp khi yêu cầu, trả stream cho người có quyền; không lưu tệp xuất lâu dài. Giá trị CSV được escape để tránh công thức bảng tính; tên tệp không chứa dữ liệu cá nhân. Audit người xuất, phạm vi và thời gian. | US-RPT-003 S2, SEC-003 |
| BR-U16-46 | Admin Dashboard là điểm vào các màn quản trị: Account List (U01), Subject List (U04), Credit Package List và Payment History (U07), Setting List (U03), Audit Log (U02). Không có lối vào Manager Dashboard, Class Dashboard hay nội dung lớp. | UC 58, screen flow Page-2 |

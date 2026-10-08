# Components

## 1. Kiến trúc tổng thể

MVP dùng modular monolith: frontend Next.js, một backend Spring Boot chia thành 16 module theo unit, và một process `worker` (cùng image, profile `worker`) cho tác vụ dài qua RabbitMQ. PostgreSQL (có pgvector) lưu dữ liệu giao dịch và vector; Redis giữ phiên, OTP, bộ đếm, token tải file; file lưu trên Google Shared Drive qua port lưu trữ. Chi tiết từng module nằm trong `aidlc-docs/construction/uXX-*/`.

## 2. Thành phần frontend

| Component | Trách nhiệm | Unit |
|---|---|---|
| Web Shell | Điều hướng, phiên, layout desktop-first, chuông thông báo (SSE) | U01, U16 |
| Account & Admin Console | Kích hoạt, đăng nhập, hồ sơ (không đổi avatar), quản lý tài khoản/role, Audit Log (UC 70), Credit Package Setting, Payment History, cấu hình AI và Statistic | U01, U02, U07, U13, U16 |
| Class Console | Môn, lớp, ghi danh, mã mời, module và học liệu, thông báo và bình luận lớp | U04, U05 |
| Student Space | Lớp của tôi, học liệu, bình luận thông báo, làm bài, lịch sử nộp, điểm, phân bố điểm ẩn danh; ví/credit và kết quả AI Practice Text/Diagram Essay theo từng attempt | U04, U05, U07, U11, U13, U15, U16 |
| Bank Console | Ngân hàng câu hỏi/rubric cấp môn và lớp, nhập Excel/CSV | U06 |
| Assignment Console | Soạn năm dạng bài và chọn chế độ `GRADED`/`PRACTICE` hợp lệ, AI draft, duyệt, phát hành, version/diff, template và copy | U08, U09, U10, U13 |
| Document Editor | Trình soạn tài liệu theo block, khung khóa, sơ đồ Draw.io nhúng (iframe `embed.diagrams.net`), nhập/xuất DOCX | U09 (dùng ở U06, U11, U14, U15) |
| Group Workspace | Nhóm của lớp (trong danh sách sinh viên), trưởng nhóm, tài liệu nhóm, trưởng nhóm giao phần, nhận phần, sửa mục trong popup, ghép realtime (SSE), nộp | U12, U14 |
| Grading Console | Hàng chờ chấm, chấm tay/AI đề xuất theo rubric checklist, chốt, công bố, sổ điểm, tiến độ nộp, xuất CSV/XLSX | U15, U16 |
| Code Editor | Monaco nhiều file, chạy thử, kết quả test | U13 |

Frontend không quyết định authorization. Tên màn theo `docs/use-cases-and-screens.md` và Drawio mới: Credit Packages, Learning Material, Class/Subject Question Bank, Question Editor, Student Submissions, Submission History, Assignment List của Teacher và năm danh sách bài của Student. Teacher dùng Assigned Classes, quản lý lớp môn dùng Subject Classes; My Classes giữ cho Student. UC 53–54 dùng Subject Template và Template Editor.

## 3. Thành phần backend

| Module (unit) | Trách nhiệm cấp cao |
|---|---|
| Identity & Access (U01) | Tài khoản `PENDING/ACTIVE/DISABLED`, OTP kích hoạt/khôi phục, JWT + refresh cookie, hồ sơ, role, nhập CSV, `authorize` |
| Audit (U02) | Audit append-only ghi trong transaction, tra cứu cho Admin |
| File, Job & Event (U03) | Upload qua backend (học liệu, ảnh trong tài liệu), kiểm magic bytes, lưu Google Drive, token tải 5 phút; 7 queue RabbitMQ theo tính chất (gửi sau commit, retry bằng queue TTL, trạng thái trên dòng nghiệp vụ, `PendingSweeper`, `ScheduledScanner`), worker, sự kiện thông báo; không có bảng job |
| Academic & Learning Access (U04) | Môn, lớp `DRAFT/OPEN/ARCHIVED`, giảng viên, Chủ nhiệm môn, ghi danh, mã mời, lớp của người học |
| Content & RAG (U05) | Module của môn (Chủ nhiệm môn tạo), học liệu tải lên của môn hoặc của lớp (tệp, YouTube chỉ caption có sẵn), trích chữ, embedding AI + pgvector, `retrieve`; thông báo lớp tạo/sửa/xóa theo phân công và bình luận hỗ trợ |
| Question Bank (U06) | Câu hỏi cho cả năm dạng bài (trắc nghiệm, Text Essay, tài liệu/Diagram Essay, Code Lab, khung bài nhóm) và rubric checklist có phiên bản, cấp môn/lớp, nhập Excel/CSV |
| Payment & AI Credit (U07) | Gói credit do Admin xem/thêm/sửa (UC 67–68), Payment History toàn nền tảng (UC 69), PayOS/webhook/tự đối soát, ví credit (tặng tháng cấu hình + mua), giữ/trừ/trả; snapshot giá/credit của giao dịch |
| Assessment Core (U08) | Bài có version, thành phần, duyệt, phát hành từng lớp, nộp trễ, khóa nội dung, ngưng giao, nhân bản, lịch mở/đóng |
| Question Types & Documents (U09) | Cấu hình trắc nghiệm/bài viết/tài liệu, mô hình tài liệu, khung, nhập/xuất DOCX, nhận sơ đồ trong ảnh, rút gọn XML |
| Template & Copy (U10) | Template cấp môn, copy giữa lớp, lineage, diff version |
| Attempt & Submission (U11) | Lượt làm, snapshot, tự lưu, nộp, tự nộp, biên nhận |
| Group (U12) | Nhóm của lớp (tạo tay, chia ngẫu nhiên), trưởng nhóm, yêu cầu đổi trưởng nhóm |
| AI & Code Execution (U13) | Cổng AI (Gemini, model theo việc), trần/kill-switch/credit, đề xuất câu hỏi và chấm, Judge0 (7 ngôn ngữ), kiểm lời giải mẫu |
| Group Document (U14) | Tài liệu nhóm theo từng bài, trưởng nhóm giao phần của khung, nhận/khóa phần, sửa mục trong popup che kín trang, Xong → ghép realtime, trưởng nhóm nộp, hết hạn tự nộp |
| Grading (U15) | Tự chấm trắc nghiệm/code, chấm tay/AI, chốt, công bố, sửa có lý do, bài nhóm, sổ điểm |
| Reporting & Notification (U16) | Thông báo trong app (SSE), email có trần, nhắc hạn, tiến độ nộp, phân bố điểm ẩn danh, thống kê quản trị, xuất bảng điểm CSV/XLSX |

## 4. Thành phần ngoài hệ thống

| Port | Adapter |
|---|---|
| Storage Port | Google Shared Drive (service account); thư mục local khi không có key |
| AI Provider Port / Embedding Port | Gemini (`generateContent`, `gemini-embedding-001`); adapter giả khi không có key |
| Code Runner Port | Judge0 CE 1.13.1 tự chạy trong mạng `sandbox` |
| Payment Provider Port | PayOS; adapter giả cho local |
| YouTube Port | YouTube Data API v3 + đọc caption công khai |
| Mail Port | Brevo SMTP khi demo/production, Mailpit khi dev |
| Cache/Session Port | Redis |
| Message Broker Port | RabbitMQ (`jobs.*`, `platform.events`, `platform.realtime`) |

## 5. Boundary bắt buộc

- Module chỉ dùng dữ liệu module khác qua port/contract đã công bố; không đọc bảng của module khác.
- Mọi truy cập đối tượng nhận actor context và kiểm quyền phía server (U01 `authorize` + phạm vi U04).
- Bài đã phát hành bị khóa nội dung; muốn đổi thì ngưng giao rồi tạo version mới, hoặc nhân bản. Lượt làm giữ version đã dùng.
- Bài nộp bất biến sau khi nộp; Grading chỉ tham chiếu bài nộp.
- XML Draw.io đầy đủ nằm trong bài tài liệu; XML rút gọn chỉ tạo khi có yêu cầu AI chấm (giảng viên đề xuất bài `GRADED` hoặc Student chấm `PRACTICE`).
- AI chỉ trả đề xuất; không phát hành đề, không chốt điểm. Lời gọi Gemini tạo nội dung hoặc embedding trừ credit AI của người yêu cầu; embedding học liệu chạy nền trừ người đã tải học liệu. Nếu hết hạn mức AI của hệ thống, báo "Hệ thống đang bận" và không trừ credit cho lời gọi bị từ chối.
- Thanh toán chỉ cộng credit AI sau webhook đã xác minh hoặc job tự đối soát; không ảnh hưởng quyền vào lớp.
- Không có đề chung cấp môn; không sao chép khóa học/lớp.
- Hệ thống không tự quét malware; file bị Google Drive gắn cờ abuse bị coi là không dùng được.

## 6. Phạm vi authorization theo SRS mới

User chung theo R1; Student theo R5; Teacher theo R3; Subject Manager/Administrator dùng Teacher theo R4 và tài nguyên môn theo R2. Admin quản trị cấu trúc Full không cấp quyền đọc/chấm gradebook toàn nền tảng. Question Bank dùng hai scope CLASS/SUBJECT; Audit Log chỉ Admin và chỉ đọc. Inception hiệu lực có 70 UC/51 story; thiết kế Construction chưa được đồng bộ trong lần sửa này.

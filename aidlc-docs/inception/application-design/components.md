# Components

## 1. Kiến trúc tổng thể

MVP dùng modular monolith: frontend Next.js, một backend Spring Boot chia thành 15 module theo unit, và một process `worker` (cùng image, profile `worker`) cho tác vụ dài qua RabbitMQ. PostgreSQL (có pgvector) lưu dữ liệu giao dịch và vector; Redis giữ phiên, OTP, bộ đếm, token tải file; file lưu trên Google Shared Drive qua port lưu trữ. Chi tiết từng module nằm trong `aidlc-docs/construction/uXX-*/`.

## 2. Thành phần frontend

| Component | Trách nhiệm | Unit |
|---|---|---|
| Web Shell | Điều hướng, phiên, layout desktop-first, chuông thông báo (SSE) | U01, U16 |
| Account & Admin Console | Tài khoản/hồ sơ, Audit Log UC 73, Credit Package List/Detail, Payment History, Setting List/Detail và Admin Dashboard | U01, U02, U03, U07, U13, U16 |
| Class Console | Môn/lớp/ghi danh theo quyền; module, học liệu và Class Announcements; không mã mời/bình luận | U04, U05 |
| Student Space | Class Dashboard, học liệu và quiz luyện tập; Student Assignments, làm bài/lịch sử/điểm, ví và AI Practice riêng tư | U04, U05, U07, U11, U13, U15, U16 |
| Bank Console | Question List/Detail của ngân hàng môn, nhập Excel/CSV; rubric thuộc bài qua Rubric Detail | U06, U09 |
| Assignment Console | Quiz luyện tập gắn học liệu; bốn dạng assignment và mode hợp lệ, AI draft, duyệt/phát hành, version/copy ở U08; bài của môn không có nhóm | U08, U09, U13 |
| Document Editor | Trình soạn tài liệu theo block, khung khóa, sơ đồ Draw.io nhúng (iframe `embed.diagrams.net`), nhập/xuất DOCX | U09 (dùng ở U06, U11, U14, U15) |
| Group Workspace | Nhóm của lớp (trong danh sách sinh viên), trưởng nhóm, tài liệu nhóm, trưởng nhóm giao phần, nhận phần, sửa mục trong popup, ghép realtime (SSE), nộp | U12, U14 |
| Grading Console | Hàng chờ chấm, chấm tay/AI đề xuất theo rubric checklist, chốt, công bố, sổ điểm, tiến độ nộp, xuất CSV/XLSX | U15, U16 |
| Code Editor | Monaco nhiều file, chạy thử, kết quả test | U13 |

Frontend không quyết định authorization. Màn/entry theo [shared contract](../../construction/current-srs-contract.md) và Page-2 của `docs/G21_Diagrams.drawio`; Teacher Class Detail/Student Class Detail tách scope. Student Assignments là danh sách gộp, Quiz Practice riêng; Manager Dashboard có Assignment List, Quiz List, Material List, Question List. Admin Dashboard có Settings và chức năng quản trị, không màn giảng dạy.

## 3. Thành phần backend

| Module (unit) | Trách nhiệm cấp cao |
|---|---|
| Identity & Access (U01) | Tài khoản `PENDING/ACTIVE/DISABLED`, OTP kích hoạt/khôi phục, JWT + refresh cookie, hồ sơ, role, nhập CSV, `authorize` |
| Audit (U02) | Audit append-only ghi trong transaction, tra cứu cho Admin |
| File, Job, Event & Settings (U03) | Tệp Drive/token, worker/7 queue/retry/sweeper/scanner/events; Settings chung với schema khai báo bởi U03/U07/U13, version/audit và cache tối đa 30 giây; không bảng job |
| Academic & Learning Access (U04) | Môn do Admin quản trị; lớp và enrollment do Subject Manager của môn quản lý; Teacher chỉ lớp được giao; Student lớp đã ghi danh |
| Content & RAG (U05) | Module/học liệu môn hoặc lớp; giữ credit khi tạo lesson, trích chữ/summary/embedding từ summary, RAG; thông báo create/update/soft-delete; không bình luận hay quét lại thủ công |
| Question Bank (U06) | Ngân hàng môn có version; câu riêng ASSIGNMENT qua InlineQuestionPort; rubric thuộc từng bài/câu/phần, tự tạo trống và khóa khi phát hành; nhập Excel/CSV |
| Payment & AI Credit (U07) | UC 08–11 ví của Student/Teacher/Subject Manager; Admin UC 68–69 quản trị gói/UC 72 history; grant từ Settings; verified payment và snapshot; giữ/trừ/trả credit |
| Assessment Core (U08) | Bài lớp/bài môn và quiz gắn học liệu; vòng đời/review/publish, lịch bài môn chung mọi lớp OPEN, khóa nội dung, version/clone/copy; không template/U10 |
| Question Types & Documents (U09) | Cấu hình trắc nghiệm/bài viết/tài liệu, mô hình tài liệu, khung, nhập/xuất DOCX, nhận sơ đồ trong ảnh, rút gọn XML |
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
- Có bài của môn cho mọi lớp OPEN; không sao chép khóa học/lớp.
- Hệ thống không tự quét malware; file bị Google Drive gắn cờ abuse bị coi là không dùng được.

## 6. Phạm vi authorization theo 73 UC

Theo FR-002 và shared contract: R1 của chủ tài khoản; Student R5; Teacher R3; Subject Manager tài nguyên môn R2 hoặc giảng dạy R4 khi được giao lớp. Admin chỉ User/quản trị, không phân công môn/lớp, không ví/AI. Ngân hàng chỉ SUBJECT; rubric CLASS/SUBJECT theo bài và câu riêng ASSIGNMENT là scope khác. Baseline 73 UC/51 story/15 unit; code/contracts cần triển khai checklist revision, không được coi là hoàn tất bởi cập nhật tài liệu.

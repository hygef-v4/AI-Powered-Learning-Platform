# Components

## 1. Kiến trúc tổng thể

MVP dùng modular monolith: frontend Next.js, một backend Spring Boot chia thành 16 module theo unit, và một process `worker` (cùng image, profile `worker`) cho tác vụ dài qua RabbitMQ. PostgreSQL (có pgvector) lưu dữ liệu giao dịch và vector; Redis giữ phiên, OTP, bộ đếm, token tải file; file lưu trên Google Shared Drive qua port lưu trữ. Chi tiết từng module nằm trong `aidlc-docs/construction/uXX-*/`.

## 2. Thành phần frontend

| Component | Trách nhiệm | Unit |
|---|---|---|
| Web Shell | Điều hướng, phiên, layout desktop-first, chuông thông báo (SSE) | U01, U16 |
| Account & Admin Console | Kích hoạt, đăng nhập, hồ sơ, quản lý tài khoản/role, audit, cấu hình AI, gói credit | U01, U02, U07, U13 |
| Class Console | Môn, lớp, ghi danh, mã mời, nội dung lớp, thông báo và hỏi đáp lớp | U04, U05 |
| Learner Space | Lớp của tôi, bài học, hỏi đáp, làm bài, lịch sử nộp, điểm, dashboard cá nhân, ví credit | U04, U05, U11, U15, U16, U07 |
| Bank Console | Ngân hàng câu hỏi/rubric cấp môn và lớp, nhập Excel/CSV | U06 |
| Assignment Console | Soạn bài (trắc nghiệm, bài viết, bài tài liệu, Code Lab, bài nhóm), AI draft, duyệt, phát hành, version/diff, template, copy, thi thử | U08, U09, U10, U13 |
| Document Editor | Trình soạn tài liệu theo block, khung khóa, sơ đồ Draw.io nhúng (iframe `embed.diagrams.net`), nhập/xuất DOCX | U09 (dùng ở U06, U11, U14, U15) |
| Group Workspace | Bộ nhóm, trưởng nhóm, tài liệu nhóm, nhận mục, ghép realtime (SSE), nộp | U12, U14 |
| Grading Console | Hàng chờ chấm, chấm tay/AI đề xuất theo rubric checklist, chốt, công bố, sổ điểm, tiến độ nộp, xuất CSV/XLSX | U15, U16 |
| Code Editor | Monaco nhiều file, chạy thử, kết quả test | U13 |

Frontend không phải nguồn quyết định authorization; ẩn/hiện UI chỉ hỗ trợ trải nghiệm.

## 3. Thành phần backend

| Module (unit) | Trách nhiệm cấp cao |
|---|---|
| Identity & Access (U01) | Tài khoản `PENDING/ACTIVE/DISABLED`, OTP kích hoạt/khôi phục, JWT + refresh cookie, hồ sơ, role, nhập CSV, `authorize` |
| Audit, Job & Event (U02) | Audit append-only, bảng `jobs` + RabbitMQ (retry theo DB, sweeper), event sau commit |
| File & Artifact (U03) | Upload qua backend (avatar, học liệu, ảnh trong tài liệu), kiểm magic bytes, lưu Google Drive, token tải 5 phút |
| Academic & Learning Access (U04) | Môn, lớp `DRAFT/OPEN/ARCHIVED`, giảng viên, Chủ nhiệm môn, ghi danh, mã mời, lớp của người học |
| Content & RAG (U05) | Chương → bài → mục, phiên bản bài, bài cấp môn liên kết vào lớp, YouTube (chỉ caption có sẵn), trích chữ, embedding Gemini + pgvector, `retrieve`; thông báo và hỏi đáp lớp |
| Question Bank (U06) | Câu hỏi 5 loại và rubric checklist có phiên bản, cấp môn/lớp, nhập Excel/CSV |
| Payment & AI Credit (U07) | Gói credit, PayOS, webhook có chữ ký, tự đối soát định kỳ, ví credit (tặng tháng + mua), giữ/trừ/trả credit |
| Assessment Core (U08) | Bài có version, thành phần, duyệt, phát hành từng lớp, nộp trễ, khóa nội dung, ngưng giao, nhân bản, lịch mở/đóng |
| Question Types & Documents (U09) | Cấu hình trắc nghiệm/bài viết/tài liệu, mô hình tài liệu, khung, nhập/xuất DOCX, nhận sơ đồ trong ảnh, rút gọn XML |
| Template, Copy & Simulation (U10) | Template cấp môn, copy giữa lớp, lineage, diff version, chính sách thi thử |
| Attempt & Submission (U11) | Lượt làm, snapshot, tự lưu, nộp, tự nộp, biên nhận |
| Group (U12) | Bộ nhóm theo bài nhóm, trưởng nhóm, yêu cầu đổi trưởng nhóm |
| AI & Code Execution (U13) | Cổng AI (Gemini, model theo việc), trần/kill-switch/credit, đề xuất câu hỏi và chấm, Judge0 (7 ngôn ngữ), kiểm lời giải mẫu |
| Group Document (U14) | Tài liệu nhóm, nhận/khóa mục, Xong → ghép realtime, bình luận, trưởng nhóm nộp |
| Grading (U15) | Tự chấm trắc nghiệm/code, chấm tay/AI, chốt, công bố, sửa có lý do, bài nhóm, sổ điểm |
| Reporting & Notification (U16) | Thông báo trong app (SSE), email có trần, nhắc hạn, tiến độ nộp, dashboard cá nhân, xuất bảng điểm CSV/XLSX |

## 4. Thành phần ngoài hệ thống

| Port | Adapter |
|---|---|
| Storage Port | Google Shared Drive (service account); thư mục local khi không có key |
| AI Provider Port / Embedding Port | Gemini (`generateContent`, `gemini-embedding-001`); adapter giả khi không có key |
| Code Runner Port | Judge0 CE 1.13.1 tự chạy trong mạng `sandbox` |
| Payment Provider Port | PayOS; adapter giả cho local |
| YouTube Port | YouTube Data API v3 + đọc caption công khai |
| Mail Port | SMTP Gmail (App Password) khi demo, Mailpit khi dev |
| Cache/Session Port | Redis |
| Message Broker Port | RabbitMQ (`jobs.*`, `platform.events`, `platform.realtime`) |

## 5. Boundary bắt buộc

- Module chỉ dùng dữ liệu module khác qua port/contract đã công bố; không đọc bảng của module khác.
- Mọi truy cập đối tượng nhận actor context và kiểm quyền phía server (U01 `authorize` + phạm vi U04).
- Bài đã phát hành bị khóa nội dung; muốn đổi thì ngưng giao rồi tạo version mới, hoặc nhân bản. Lượt làm giữ version đã dùng.
- Bài nộp bất biến sau khi nộp; Grading chỉ tham chiếu bài nộp.
- XML Draw.io đầy đủ nằm trong bài tài liệu; XML rút gọn chỉ tạo khi giảng viên yêu cầu AI chấm.
- AI chỉ trả đề xuất; không phát hành đề, không chốt điểm. Lời gọi Gemini tạo nội dung hoặc embedding trừ credit AI của người yêu cầu; embedding học liệu chạy nền trừ người đã tải/phát hành học liệu. Nếu hết hạn mức AI của hệ thống, báo "Hệ thống đang bận" và không trừ credit cho lời gọi bị từ chối.
- Thanh toán chỉ cộng credit AI sau webhook đã xác minh hoặc job tự đối soát; không ảnh hưởng quyền vào lớp.
- Không có đề chung cấp môn; không sao chép khóa học/lớp.
- Hệ thống không tự quét malware; file bị Google Drive gắn cờ abuse bị coi là không dùng được.

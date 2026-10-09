# Application Design - Consolidated

## 1. Quyết định kiến trúc

| Chủ đề | Quyết định |
|---|---|
| Backend | Spring Boot modular monolith, 15 module theo unit, cùng image cho `backend` và `worker` |
| Frontend | Next.js desktop-first, gọi REST `/api/v1` |
| Contract | OpenAPI mỗi unit; backend authorization là nguồn chuẩn |
| Tác vụ dài | RabbitMQ (gửi sau commit, retry bằng queue TTL), trạng thái trên dòng nghiệp vụ, `PendingSweeper`/`ScheduledScanner`, worker riêng; không có bảng job |
| File | Google Shared Drive qua port lưu trữ; metadata trong thuộc tính tệp (`appProperties`), bảng sở hữu giữ `file_id` |
| AI | Gemini qua port provider-neutral, model theo loại việc, trừ credit AI |
| Realtime | SSE cho tài liệu nhóm và thông báo; còn lại polling trạng thái job |
| Triển khai | Docker Compose trên VPS, Nginx + Let's Encrypt (xem `construction/shared-infrastructure.md`) |

## 2. Các domain module

15 module trùng với 15 unit: Identity & Access, Audit, File/Job/Event, Academic & Learning Access, Content & RAG, Question Bank, Payment & AI Credit, Assessment Core, Question Types & Documents, Attempt & Submission, Group, AI & Code Execution, Group Document, Grading, Reporting & Notification.

Chi tiết: `components.md` (trách nhiệm), `component-methods.md` (chữ ký), `services.md` (orchestration), `component-dependency.md` (luồng dữ liệu), `unit-of-work*.md` (unit, phụ thuộc, story). Thiết kế chi tiết và bảng dữ liệu của từng module nằm trong `aidlc-docs/construction/uXX-*/`.

## 3. Các invariants thiết kế

1. 73 UC theo catalog local. User R1, Student R5, Teacher R3; Subject Manager R2 hoặc R4 theo phân công. Admin chỉ User và chức năng quản trị, không phân công môn/lớp, không AI/credit wallet.
2. Subject Manager quản lý môn R2, tạo/duyệt/phát hành bài của môn cho mọi lớp OPEN (một lịch chung, không nhóm). Chấm/gradebook theo từng lớp bởi Teacher/Subject Manager được giao dạy R3/R4; giảng viên lớp không sửa bài của môn.
3. Mỗi nhóm có đúng một trưởng nhóm; bài nhóm là một tài liệu chung gồm các phần của khung, trưởng nhóm giao phần hoặc thành viên tự nhận phần, mỗi phần tại một thời điểm chỉ một người sửa; trưởng nhóm nộp.
4. Bài đã phát hành bị khóa nội dung; thay đổi bằng version mới sau khi ngưng giao/đóng, hoặc nhân bản. Bài nộp bất biến sau khi nộp.
5. XML Draw.io đầy đủ nằm trong bài tài liệu; XML rút gọn chỉ tạo khi có yêu cầu AI chấm (giảng viên hoặc Student với bài `PRACTICE`).
6. AI chỉ tạo đề xuất; giảng viên giữ quyết định phát hành đề và điểm cuối.
7. Tài liệu nhóm chấm như bài `DOCUMENT` (tay hoặc AI đề xuất); điểm đóng góp từng thành viên mặc định bằng điểm tài liệu chung, giảng viên chấm tay khi cần.
8. Chỉ Student, Teacher, Subject Manager ACTIVE có ví, grant tháng và mua credit. Student được AI chấm Practice Text/Diagram Essay đã nộp (UC 29) và tóm tắt học liệu có quyền xem (luồng phụ UC 15); Teacher AI proposals UC 38. Quiz/Code Lab tự chấm, không credit AI. Payment chỉ cộng credit khi webhook/đối soát đã xác minh, đúng một lần.
9. U08 giữ version/clone/copy; chỉ giảng viên dạy cả lớp nguồn/đích mới copy bài lớp. Không copy lịch, attempt, submission hoặc điểm; rubric sao thành rubric của bài đích, câu ngân hàng ghim version; không template/U10/diff screen.
10. Bài `PRACTICE` Text/Diagram Essay không tự chấm khi nộp; mỗi attempt có tối đa một kết quả AI hợp lệ khi Student bấm chấm và đủ credit. Kết quả Practice không vào sổ điểm và Teacher không chấm.
11. Audit chỉ thêm, không sửa/xóa; Admin tra cứu UC 73.
12. Admin quản trị gói UC 68–69, history UC 72, Settings UC 70–71 (U03 lưu, U07/U13 khai báo nhóm credit/AI). Giao dịch giữ snapshot; thay monthly grant hiệu lực kỳ sau, không chỉnh credit tay.
13. Hồ sơ không cập nhật avatar. Thông báo lớp create/update/soft-delete, không bình luận; ngân hàng chỉ môn. Quiz là luyện tập gắn học liệu, không lịch/gradebook; Student Assignments chỉ bốn dạng bài.

## 4. Luồng triển khai

- Nginx, frontend, backend, worker, PostgreSQL (pgvector), Redis, RabbitMQ và 4 container Judge0 chạy bằng Docker Compose trên VPS của nhóm.
- Worker xử lý: gửi email/OTP, ingest RAG, AI, chạy code, mở/đóng bài, tự nộp, tự đối soát PayOS, dọn file, nhắc hạn.
- Dịch vụ ngoài (Google Drive, Gemini, YouTube, PayOS, SMTP) nằm sau port/adapter, có adapter giả để chạy local và test.

## 5. Traceability

Bản hiệu lực 2026-10-09 có 73 UC, 51 story và 15 unit, ánh xạ trong `unit-of-work-story-map.md` và `stories.md` mục 14. UC/màn từ `docs/use-cases-73.md`; US-AIG-003 giữ là vận hành hỗ trợ không có UC trực tiếp. Construction/code plans đã đồng bộ cho các thay đổi ngày 2026-10-09; contracts/code còn cần triển khai theo các revision tasks. Thông báo lớp, thống kê quản trị và xuất bảng điểm đều thuộc MVP; Class Dashboard chỉ điều hướng lớp; không dashboard thống kê cá nhân của Student.

| Story domain | Unit chủ đạo |
|---|---|
| IAM | U01 |
| AUD | U02 |
| CAT, LRN | U04 |
| CNT | U05 |
| QBK | U06 |
| PAY | U07 |
| ASM | U08, U09, U11; U13 hỗ trợ AI |
| SET | U03 |
| AIG | U13 |
| GRP | U12, U14, U15 |
| GRD | U15 |
| RPT, NTF | U16 |

## 6. Security và resiliency compliance

- **Security** (phạm vi rút gọn SECURITY-03, 04, 05, 08, 09, 12, 15): Compliant ở cấp thiết kế với kiểm quyền phía server, kiểm file/XML, cô lập sandbox, không log secret, audit bất biến, webhook có chữ ký.
- **Resiliency** (RESILIENCY-04, 06, 10): Compliant với deploy/rollback bằng Compose, healthcheck, timeout cho mọi dịch vụ ngoài, retry hữu hạn qua job.
- **Property-Based Testing**: N/A (extension tắt).

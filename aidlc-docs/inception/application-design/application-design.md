# Application Design - Consolidated

## 1. Quyết định kiến trúc

| Chủ đề | Quyết định |
|---|---|
| Backend | Spring Boot modular monolith, 16 module theo unit, cùng image cho `backend` và `worker` |
| Frontend | Next.js desktop-first, gọi REST `/api/v1` |
| Contract | OpenAPI mỗi unit; backend authorization là nguồn chuẩn |
| Tác vụ dài | RabbitMQ (gửi sau commit, retry bằng queue TTL), trạng thái trên dòng nghiệp vụ, `PendingSweeper`/`ScheduledScanner`, worker riêng; không có bảng job |
| File | Google Shared Drive qua port lưu trữ; metadata trong thuộc tính tệp (`appProperties`), bảng sở hữu giữ `file_id` |
| AI | Gemini qua port provider-neutral, model theo loại việc, trừ credit AI |
| Realtime | SSE cho tài liệu nhóm và thông báo; còn lại polling trạng thái job |
| Triển khai | Docker Compose trên VPS, Nginx + Let's Encrypt (xem `construction/shared-infrastructure.md`) |

## 2. Các domain module

16 module trùng với 16 unit: Identity & Access, Audit, File/Job/Event, Academic & Learning Access, Content & RAG, Question Bank, Payment & AI Credit, Assessment Core, Question Types & Documents, Template & Copy, Attempt & Submission, Group, AI & Code Execution, Group Document, Grading, Reporting & Notification.

Chi tiết: `components.md` (trách nhiệm), `component-methods.md` (chữ ký), `services.md` (orchestration), `component-dependency.md` (luồng dữ liệu), `unit-of-work*.md` (unit, phụ thuộc, story). Thiết kế chi tiết và bảng dữ liệu của từng module nằm trong `aidlc-docs/construction/uXX-*/`.

## 3. Các invariants thiết kế

1. 70 UC theo SRS 4.2; quyền User R1, Student R5, Teacher R3, Subject Manager/Administrator R2/R4 được kiểm phía server. Admin Full chỉ cho chức năng quản trị cấu trúc/tài khoản/gói/lịch sử/statistics/audit được nêu.
2. Subject Manager/Administrator quản lý tài nguyên môn khi có R2; chỉ dùng chức năng Teacher (bài/chấm/gradebook) khi chính họ được giao lớp R4. Người được phân công môn/lớp có thể có role Admin; không có đề chung cấp môn.
3. Mỗi nhóm có đúng một trưởng nhóm; bài nhóm là một tài liệu chung gồm các phần của khung, trưởng nhóm giao phần hoặc thành viên tự nhận phần, mỗi phần tại một thời điểm chỉ một người sửa; trưởng nhóm nộp.
4. Bài đã phát hành bị khóa nội dung; thay đổi bằng version mới sau khi ngưng giao/đóng, hoặc nhân bản. Bài nộp bất biến sau khi nộp.
5. XML Draw.io đầy đủ nằm trong bài tài liệu; XML rút gọn chỉ tạo khi có yêu cầu AI chấm (giảng viên hoặc Student với bài `PRACTICE`).
6. AI chỉ tạo đề xuất; giảng viên giữ quyết định phát hành đề và điểm cuối.
7. Tài liệu nhóm chấm như bài `DOCUMENT` (tay hoặc AI đề xuất); điểm đóng góp từng thành viên mặc định bằng điểm tài liệu chung, giảng viên chấm tay khi cần.
8. Cả bốn vai trò `ACTIVE` có thể mua và xem credit của chính mình. Student chỉ dùng credit để AI chấm attempt `PRACTICE` Text/Diagram Essay đã nộp: bấm chấm khi đủ credit, mỗi attempt tối đa một kết quả; thiếu credit thì mua thêm rồi bấm lại. Quiz/Code Lab Practice tự chấm không dùng AI. Teacher chỉ chấm và quyết định điểm cuối bài `GRADED`; AI đề xuất cho bài đó tính credit Teacher. Credit mua chỉ cộng từ webhook đã xác minh hoặc job tự đối soát, đúng một lần; không ảnh hưởng quyền vào lớp.
9. Không sao chép khóa học/lớp; template/copy bài tạo identity mới có lineage, không copy lịch, lượt làm, bài nộp, điểm.
10. Bài `PRACTICE` Text/Diagram Essay không tự chấm khi nộp; mỗi attempt có tối đa một kết quả AI hợp lệ khi Student bấm chấm và đủ credit. Kết quả Practice không vào sổ điểm và Teacher không chấm.
11. Audit chỉ thêm, không sửa/xóa; Administrator tra cứu Audit Log bằng UC 70.
12. Administrator thêm/sửa gói credit và đọc lịch sử thanh toán toàn nền tảng (UC 67–69); giao dịch giữ snapshot, mức tặng tháng vẫn cấu hình triển khai. Không chỉnh credit thủ công.
13. Hồ sơ không cập nhật avatar. Thông báo lớp được Teacher tạo/sửa/xóa có audit và scope. Class/Subject Question Bank có UC riêng, cùng mô hình scope.

## 4. Luồng triển khai

- Nginx, frontend, backend, worker, PostgreSQL (pgvector), Redis, RabbitMQ và 4 container Judge0 chạy bằng Docker Compose trên VPS của nhóm.
- Worker xử lý: gửi email/OTP, ingest RAG, AI, chạy code, mở/đóng bài, tự nộp, tự đối soát PayOS, dọn file, nhắc hạn.
- Dịch vụ ngoài (Google Drive, Gemini, YouTube, PayOS, SMTP) nằm sau port/adapter, có adapter giả để chạy local và test.

## 5. Traceability

Bản hiệu lực có 70 UC và 51 story, ánh xạ trong `unit-of-work-story-map.md` và `stories.md` mục 14. UC/màn từ `docs/use-cases-and-screens.md`; US-AIG-003 giữ là vận hành hỗ trợ không có UC trực tiếp. Construction/code plans vẫn cần đồng bộ tiếp cho các thay đổi ngày 2026-10-08. Thông báo và bình luận lớp, thống kê quản trị và xuất bảng điểm đều thuộc MVP; không có dashboard cá nhân của Student.

| Story domain | Unit chủ đạo |
|---|---|
| IAM | U01 |
| AUD | U02 |
| CAT, LRN | U04 |
| CNT | U05 |
| QBK | U06 |
| PAY | U07 |
| ASM | U08-U11, U13 |
| AIG | U13 |
| GRP | U12, U14, U15 |
| GRD | U15 |
| RPT, NTF | U16 |

## 6. Security và resiliency compliance

- **Security** (phạm vi rút gọn SECURITY-03, 04, 05, 08, 09, 12, 15): Compliant ở cấp thiết kế với kiểm quyền phía server, kiểm file/XML, cô lập sandbox, không log secret, audit bất biến, webhook có chữ ký.
- **Resiliency** (RESILIENCY-04, 06, 10): Compliant với deploy/rollback bằng Compose, healthcheck, timeout cho mọi dịch vụ ngoài, retry hữu hạn qua job.
- **Property-Based Testing**: N/A (extension tắt).

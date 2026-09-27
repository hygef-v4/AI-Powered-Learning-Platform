# Application Design - Consolidated

## 1. Quyết định kiến trúc

| Chủ đề | Quyết định |
|---|---|
| Backend | Spring Boot modular monolith, 16 module theo unit, cùng image cho `backend` và `worker` |
| Frontend | Next.js desktop-first, gọi REST `/api/v1` |
| Contract | OpenAPI mỗi unit; backend authorization là nguồn chuẩn |
| Tác vụ dài | Bảng `jobs` + RabbitMQ, retry theo DB, worker riêng |
| File | Google Shared Drive qua port lưu trữ; metadata trong PostgreSQL |
| AI | Gemini qua port provider-neutral, model theo loại việc, trừ credit AI |
| Realtime | SSE cho tài liệu nhóm và thông báo; còn lại polling trạng thái job |
| Triển khai | Docker Compose trên VPS, Nginx + Let's Encrypt (xem `construction/shared-infrastructure.md`) |

## 2. Các domain module

16 module trùng với 16 unit: Identity & Access, Audit/Job/Event, File & Artifact, Academic & Learning Access, Content & RAG, Question Bank, Payment & AI Credit, Assessment Core, Question Types & Documents, Template/Copy/Simulation, Attempt & Submission, Group, AI & Code Execution, Group Document, Grading, Reporting & Notification.

Chi tiết: `components.md` (trách nhiệm), `component-methods.md` (chữ ký), `services.md` (orchestration), `component-dependency.md` (luồng dữ liệu), `unit-of-work*.md` (unit, phụ thuộc, story). Thiết kế chi tiết và bảng dữ liệu của từng module nằm trong `aidlc-docs/construction/uXX-*/`.

## 3. Các invariants thiết kế

1. Quyền role và phạm vi đối tượng được kiểm tra phía server cho mọi use case.
2. Chủ nhiệm môn quản lý phạm vi môn (học liệu, ngân hàng, template); chỉ phát hành bài cho lớp mà chính họ là giảng viên. Không có đề chung cấp môn.
3. Mỗi nhóm có đúng một trưởng nhóm; bài nhóm là một tài liệu chung, thành viên tự nhận mục, mỗi mục tại một thời điểm chỉ một người sửa; trưởng nhóm nộp.
4. Bài đã phát hành bị khóa nội dung; thay đổi bằng version mới sau khi ngưng giao/đóng, hoặc nhân bản. Bài nộp bất biến sau khi nộp.
5. XML Draw.io đầy đủ nằm trong bài tài liệu; XML rút gọn chỉ tạo khi giảng viên yêu cầu AI chấm.
6. AI chỉ tạo đề xuất; giảng viên giữ quyết định phát hành đề và điểm cuối.
7. Tài liệu nhóm luôn chấm tay; AI chỉ hỗ trợ phần đóng góp của từng thành viên; điểm cuối từng người do giảng viên nhập, không có công thức bắt buộc.
8. Thanh toán chỉ cộng credit AI từ webhook đã xác minh hoặc job tự đối soát, đúng một lần; không ảnh hưởng quyền vào lớp.
9. Không sao chép khóa học/lớp; template/copy bài tạo identity mới có lineage, không copy lịch, lượt làm, bài nộp, điểm.
10. Thi thử khóa chính sách khi lượt đầu tiên bắt đầu.
11. Audit chỉ thêm, không sửa, không xóa.

## 4. Luồng triển khai

- Nginx, frontend, backend, worker, PostgreSQL (pgvector), Redis, RabbitMQ và 4 container Judge0 chạy bằng Docker Compose trên VPS của nhóm.
- Worker xử lý: gửi email/OTP, ingest RAG, AI, chạy code, mở/đóng bài, tự nộp, tự đối soát PayOS, dọn file, nhắc hạn.
- Dịch vụ ngoài (Google Drive, Gemini, YouTube, PayOS, SMTP) nằm sau port/adapter, có adapter giả để chạy local và test.

## 5. Traceability

Toàn bộ story và use case trong hai catalog hiện hành thuộc MVP và được gán cho unit trong `unit-of-work-story-map.md`. Thông báo/hỏi đáp lớp, dashboard cá nhân và xuất bảng điểm đều thuộc MVP.

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

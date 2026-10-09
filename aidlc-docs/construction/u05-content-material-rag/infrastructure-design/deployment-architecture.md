# U05 Content, Material & RAG - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Trình duyệt → Nginx → backend U05/U04 (quyền xem).
Upload/trích chữ: backend → RabbitMQ jobs.triggered/jobs.youtube → worker → PostgreSQL EXTRACTED.
Nút tóm tắt: backend U05 → U13 AiGuard/HOLD → jobs.gemini → worker → Gemini → PostgreSQL summary/vector.
U03 cung cấp byte file; U13 quản lý Redis cost cap/credit; nguồn vẫn xem được trước AI.
RAG chỉ lấy ACTIVE/INDEXED; video nhúng youtube-nocookie.

**Text alternative**: Trình duyệt dùng màn học liệu qua Nginx tới module U05 trong backend và nhúng video bằng iframe `youtube-nocookie`. Backend lưu module và lesson vào PostgreSQL có pgvector và gửi việc trích chữ qua jobs.triggered/YouTube qua jobs.youtube; khi người xem bấm Tóm tắt tài liệu mới giữ credit requester và gửi MATERIAL_SUMMARY qua jobs.gemini. Worker trích chữ chỉ đọc tệp U03/phụ đề tới EXTRACTED. Worker MATERIAL_SUMMARY dùng chữ đã lưu, xin phép AI qua U13 (trần chi phí trong Redis), gọi Gemini tóm tắt và tạo một vector từ bản tóm tắt rồi ghi kết quả quét vào `lessons`. Khi U13 truy xuất, backend gọi Gemini tạo vector câu hỏi rồi tìm lesson gần nhất trong pgvector.

## Lưu ý triển khai
- Đổi image PostgreSQL giữ nguyên volume dữ liệu (cùng major 16).
- Không có key → backend và worker dùng adapter giả, vẫn chạy được local.

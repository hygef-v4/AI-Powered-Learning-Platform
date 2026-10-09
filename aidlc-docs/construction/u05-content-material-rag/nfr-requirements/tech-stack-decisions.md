# U05 Content, Material & RAG - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Vector | pgvector trong PostgreSQL, cột `lessons.embedding`, index HNSW, khoảng cách cosine | Không thêm dịch vụ; một vector mỗi lesson theo database |
| Embedding | Gemini `gemini-embedding-001`, 768 chiều, gọi REST bằng Spring `RestClient` | Nhà cung cấp nhóm đã chọn |
| Trích chữ | Apache Tika (PDF qua PDFBox, DOCX/PPTX qua POI) | Một thư viện cho cả 3 loại |
| Phụ đề | Thư viện Java đọc phụ đề công khai của YouTube, bọc sau `YoutubePort` | Không có API chính thức cho video không phải của mình; thay được khi YouTube đổi |
| Markdown (thông báo) | Frontend `react-markdown` + `rehype-sanitize` | Hiển thị an toàn |
| JPA với vector | `JdbcTemplate` cho câu query vector | Query vector viết SQL trực tiếp |

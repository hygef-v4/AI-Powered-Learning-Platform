# U05 Content, Material & RAG - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Vector | pgvector trong PostgreSQL, cột `lessons.embedding`, index HNSW, khoảng cách cosine | Không thêm dịch vụ; một vector mỗi lesson theo database |
| Embedding | Gemini `gemini-embedding-001`, 768 chiều, gọi REST bằng Spring `RestClient` | Nhà cung cấp nhóm đã chọn |
| Trích chữ | Apache Tika (PDF qua PDFBox, DOCX/PPTX qua POI) | Một thư viện cho cả 3 loại |
| Phụ đề | Thư viện Java đọc phụ đề công khai của YouTube, bọc sau `YoutubePort` | Không có API chính thức cho video không phải của mình; thay được khi YouTube đổi |
| Markdown (thông báo; bình luận là văn bản thuần) | Frontend `react-markdown` + `rehype-sanitize` | Hiển thị an toàn |
| JPA với vector | `JdbcTemplate` cho câu query vector | Query vector viết SQL trực tiếp |

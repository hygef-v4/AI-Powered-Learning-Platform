# U05 Content, Material & RAG - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Vector | pgvector trong PostgreSQL, index HNSW, khoảng cách cosine | Không thêm dịch vụ |
| Embedding | Gemini `gemini-embedding-001`, 768 chiều, gọi REST `batchEmbedContents` bằng Spring `RestClient` | Nhà cung cấp nhóm đã chọn |
| Trích chữ | Apache Tika (PDF qua PDFBox, DOCX/PPTX qua POI) | Một thư viện cho cả 3 loại |
| Playlist | YouTube Data API v3 `playlistItems.list` với API key | Miễn phí |
| Caption | Thư viện Java đọc caption công khai của YouTube (ví dụ `youtube-transcript-api` bản Java), bọc sau `YoutubePort` | Không có API chính thức cho video không phải của mình; thay được khi YouTube đổi |
| Markdown | `commonmark-java` phía server khi cần; frontend `react-markdown` + `rehype-sanitize` | Hiển thị an toàn |
| Đếm trần | Redis `INCRBY` khóa theo ngày | Đã có Redis |
| JPA với vector | Hibernate kiểu tùy chỉnh hoặc `JdbcTemplate` cho câu query vector | Query vector viết SQL trực tiếp |

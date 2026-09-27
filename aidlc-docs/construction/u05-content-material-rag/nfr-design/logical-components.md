# U05 Content, Material & RAG - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (CN môn, GV)                         U04 / U13 (gọi nội bộ)
   |                                                   |
   v                                                   v
 +------------------------------ backend --------------------------------------+
 | ContentController --> ChapterService, LessonService (phiên bản, phát hành)   |
 |                      ItemService --> ArtifactPort (U03), JobPort (U02)       |
 | LearnerDownloadController --> ClassAccessPort (U04) --> ArtifactPort         |
 | PublishedContentService (PublishedContentPort)                              |
 | ClassCommunicationController --> ClassCommunicationService --> ClassAccessPort |
 |                               --> EventPublisherPort (U02, sau commit)       |
 | RetrievalService (RagRetrievalPort) --> EmbeddingBudget --> EmbeddingPort    |
 |                                     --> VectorSearchRepository (pgvector)   |
 +-----------------------------------------------------------------------------+
                         | job YOUTUBE_RESOLVE / RAG_INGEST
                         v
 +------------------------------ worker ---------------------------------------+
 | YoutubeResolveHandler --> YoutubePort (Data API / caption)                  |
 | IngestJobHandler --> TextExtractor --> Chunker --> EmbeddingBudget          |
 |                  --> EmbeddingPort (Gemini) --> ChunkWriter (PostgreSQL)    |
 +-----------------------------------------------------------------------------+
```

**Text alternative**: Chủ nhiệm môn và giảng viên thao tác qua `ContentController`; các service quản lý chương, bài, phiên bản, mục, lưu file qua U03 và tạo job qua U02. Người trong lớp đọc/viết thông báo, câu hỏi và trả lời qua `ClassCommunicationController`; service kiểm quyền U04 rồi phát event sau commit cho U16. Học viên tải file qua `LearnerDownloadController`, kiểm ghi danh ở U04 rồi lấy token U03. U04 đọc nội dung đã phát hành qua `PublishedContentService`; U13 gọi `RetrievalService`, service này kiểm trần và giữ credit người yêu cầu qua U07 trước khi tạo vector câu hỏi bằng Gemini và tìm đoạn gần nhất trong pgvector. Trong worker, `YoutubeResolveHandler` giải playlist và tạo job ingest; `IngestJobHandler` trích chữ, cắt đoạn, kiểm trần, giữ credit của người tạo nguồn qua U07, gọi Gemini và ghi đoạn vào PostgreSQL. Hết quota hệ thống được báo là "Hệ thống đang bận".

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ChapterService`, `LessonService`, `ItemService` | backend | F1-F4 |
| `ClassCommunicationController`, `ClassCommunicationService` | backend | F10, quyền lớp và event U16 |
| `LearnerDownloadController` | backend | F8 |
| `PublishedContentService` | backend | `PublishedContentPort` |
| `RetrievalService`, `VectorSearchRepository` | backend | F9, P6 |
| `YoutubeResolveHandler` | worker | F5 |
| `IngestJobHandler`, `TextExtractor`, `Chunker`, `ChunkWriter` | worker | F6, P1-P3 |
| `EmbeddingBudget`, `AiBudgetPort` (U13), `CreditPort` (U07) | backend, worker | P4; kiểm kill-switch và trần chi phí Gemini chung (U13), credit người dùng (U07) trước khi gọi Gemini |
| `GeminiEmbeddingAdapter`, `YoutubeAdapter` + adapter giả | backend, worker | P5, P8 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `GEMINI_API_KEY` | Rỗng → adapter giả |
| `YOUTUBE_API_KEY` | Rỗng → adapter giả |
| `U05_INGEST_CONCURRENCY` | 4 (VPS < 8 GB: 2) |
| `U05_EMBED_MODEL` | `gemini-embedding-001` |
| `U05_EMBED_DIMENSIONS` | 768 |
| `U05_MAX_TEXT_CHARS` | 2000000 |
| `U05_PLAYLIST_MAX_VIDEOS` | 50 |
| `AI_KILL_SWITCH` | `false` (tạm, tới khi U13 sở hữu) |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | P5 không log key |
| SECURITY-04 | Compliant | P7, CSP |
| SECURITY-05 | Compliant | P5 regex ID, P7 |
| SECURITY-08 | Compliant | Kiểm phạm vi ở service, `retrieve` nội bộ |
| SECURITY-09 | Compliant | Key từ `.env` |
| SECURITY-15 | Compliant | P1 transaction, P4 |
| RESILIENCY-10 | Compliant | P5 timeout |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

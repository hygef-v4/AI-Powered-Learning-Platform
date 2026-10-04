# U05 Content, Material & RAG - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (CN môn, GV, SV)                     U04 / U13 (gọi nội bộ)
   |                                                   |
   v                                                   v
 +------------------------------ backend --------------------------------------+
 | ContentController --> ModuleService, LessonService                          |
 |                      --> ArtifactPort (U03), JobPort (U03)                  |
 | StudentDownloadController --> ClassAccessPort (U04) --> ArtifactPort         |
 | PublishedContentService (PublishedContentPort)                              |
 | ClassCommunicationController --> ClassCommunicationService --> ClassAccessPort |
 |                               --> EventPublisherPort (U03, sau commit)       |
 | RetrievalService (RagRetrievalPort) --> AiUsagePort (U13) --> EmbeddingPort  |
 |                                     --> LessonVectorRepository (pgvector)   |
 +-----------------------------------------------------------------------------+
                         | việc LESSON_SCAN / YOUTUBE_CAPTION
                         v
 +------------------------------ worker ---------------------------------------+
 | LessonScanHandler --> TextExtractor / CaptionFetcher (YoutubePort)          |
 |                   --> AiUsagePort (U13) --> EmbeddingPort (Gemini)          |
 |                   --> ghi lessons (PostgreSQL)                              |
 | LessonPendingSweeper                                                        |
 +-----------------------------------------------------------------------------+
```

**Text alternative**: Chủ nhiệm môn và giảng viên tải học liệu qua `ContentController`; service lưu lesson, gắn tệp qua U03 và gửi việc quét qua U03. Giảng viên đăng thông báo, thành viên lớp bình luận dưới thông báo qua `ClassCommunicationController`; service kiểm quyền U04 rồi phát event sau commit cho U16. Người học tải tệp qua `StudentDownloadController` sau khi U04 xác nhận ghi danh. Worker quét lesson: trích chữ từ tệp hoặc lấy phụ đề, xin phép AI qua U13, tạo một vector và ghi vào `lessons`. Khi U13 truy xuất, `RetrievalService` tạo vector câu hỏi và tìm lesson gần nhất trong pgvector.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ModuleService`, `LessonService` | backend | F1, F2, F4 |
| `ClassCommunicationController`, `ClassCommunicationService` | backend | F7, quyền lớp và event U16 |
| `StudentDownloadController` | backend | F5 |
| `PublishedContentService` | backend | `PublishedContentPort` |
| `RetrievalService`, `LessonVectorRepository` | backend | F6, P4 |
| `LessonScanHandler`, `TextExtractor`, `CaptionFetcher` | worker | F3, P1, P2 |
| `LessonPendingSweeper` | worker | BR-U03-58 |
| `GeminiEmbeddingAdapter`, `YoutubeAdapter` + adapter giả | backend, worker | P5, P7 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `GEMINI_API_KEY` | Rỗng → adapter giả |
| `YOUTUBE_API_KEY` | Rỗng → adapter giả |
| `U05_SCAN_CONCURRENCY` | 4 (VPS < 8 GB: 2) |
| `U05_EMBED_MODEL` | `gemini-embedding-001` |
| `U05_EMBED_DIMENSIONS` | 768 |
| `U05_MAX_TEXT_CHARS` | 2000000 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | P5 không log key |
| SECURITY-04 | Compliant | P6, CSP |
| SECURITY-05 | Compliant | P5 regex ID, P6 |
| SECURITY-08 | Compliant | Kiểm phạm vi ở service, `retrieve` nội bộ |
| SECURITY-09 | Compliant | Key từ `.env` |
| SECURITY-15 | Compliant | P1 transaction |
| RESILIENCY-10 | Compliant | P5 timeout |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

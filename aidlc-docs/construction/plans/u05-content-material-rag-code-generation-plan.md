# U05 Content, Material & RAG - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U05. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-CNT-001, US-CNT-002, US-CNT-005. US-CNT-003, US-CNT-004 (Phase 2) chưa thiết kế, chờ nhóm hội ý.
- **Use case**: UC-CNT-01, 02, 03, 08; hỗ trợ UC-CNT-04 (qua U04).
- **Thiết kế nguồn**: `construction/u05-content-material-rag/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `JobPort`, `JobHandler`, `AuditPort` | U02 | Dùng thật |
| `ArtifactPort`, `FileUploader` | U03 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật; U05 thay `EmptyPublishedContentAdapter` của U04 bằng `PublishedContentService` |
| `AiKillSwitchPort` | U13 | Tạm đọc `AI_KILL_SWITCH` từ `.env`; U13 thay sau |
| `EmbeddingPort`, `YoutubePort` | Gemini, YouTube | Adapter thật + adapter giả khi không có key |

### Dữ liệu U05 sở hữu

PostgreSQL `chapters`, `lessons`, `lesson_versions`, `lesson_items`, `class_lesson_links`, `youtube_sources`, `youtube_videos`, `source_documents`, `rag_chunks`; Redis `u05:embed-tokens:*`; queue `jobs.u05.youtube-resolve`, `jobs.u05.ingest`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u05/
    api/                ContentController, LessonController, LearnerDownloadController, DTO
    application/        ChapterService, LessonService, ItemService, PublishedContentService,
                        RetrievalService, EmbeddingBudget
    domain/             Chapter, Lesson, LessonVersion, LessonItem, ClassLessonLink,
                        YoutubeSource, YoutubeVideo, SourceDocument, RagChunk, YoutubeUrlParser
    ingest/             TextExtractor, Chunker, ChunkWriter
    infrastructure/     JPA repository, VectorSearchRepository (JdbcTemplate),
                        GeminiEmbeddingAdapter, YoutubeAdapter, FakeEmbeddingAdapter,
                        FakeYoutubeAdapter, EnvAiKillSwitchAdapter
    worker/             YoutubeResolveHandler, IngestJobHandler
    port/               PublishedContentPort, RagRetrievalPort, ContentRefPort,
                        EmbeddingPort, YoutubePort, AiKillSwitchPort
/backend/src/main/resources/db/migration/u05/
/infra/postgres/init/01-extensions.sql
/frontend/src/app/teaching/subjects/[id]/content/
/frontend/src/app/teaching/classes/[id]/content/
/frontend/src/shared/content/
/contracts/openapi/u05-content.yaml
```

## 3. Các bước

### Nhóm A - Khung và hạ tầng

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - `pom.xml`: `tika-parsers-standard-package`, `com.pgvector:pgvector`, thư viện đọc caption YouTube, `commonmark`. Biến cấu hình U05 theo `logical-components.md` §3.
- [ ] **Bước 2** - Docker Compose: image `pgvector/pgvector:pg16`, script `infra/postgres/init/01-extensions.sql` (`CREATE EXTENSION vector`), RAM worker 1,5 GB, truyền `GEMINI_API_KEY`, `YOUTUBE_API_KEY`, `AI_KILL_SWITCH`. Nginx CSP thêm `frame-src https://www.youtube-nocookie.com`.

### Nhóm B - Domain và logic

- [ ] **Bước 3** - Domain: chương, bài, phiên bản (1 `DRAFT` + 1 `PUBLISHED`), mục, liên kết, YouTube, `SourceDocument` và chuyển trạng thái; `YoutubeUrlParser` (regex ID) (BR-U05-10…15, 22, NFR-U05-21).
- [ ] **Bước 4** - Port và adapter giả: `EmbeddingPort`, `YoutubePort`, `AiKillSwitchPort`, `PublishedContentPort`, `RagRetrievalPort`, `ContentRefPort` (P8).
- [ ] **Bước 5** - `ChapterService`, `LessonService`: tạo/sửa/đổi thứ tự/lưu trữ, tạo bản nháp sao chép mục, phát hành, audit (F1-F3, BR-U05-01, 02, 11-14, 50).
- [ ] **Bước 6** - `ItemService`: mục `TEXT`/`FILE`/`YOUTUBE`, dùng lại `SourceDocument` theo `contentKey`, tạo job (F2, BR-U05-20…22, 30, 31).
- [ ] **Bước 7** - Liên kết bài cấp môn vào lớp (F4, BR-U05-03, 15).
- [ ] **Bước 8** - `EmbeddingBudget` với Redis và kill-switch (P4).
- [ ] **Bước 9** - `TextExtractor` (Tika theo luồng, giới hạn ký tự, `NO_TEXT`), `Chunker` (P2, P3).
- [ ] **Bước 10** - `YoutubeResolveHandler` và `IngestJobHandler` (claim idempotent, concurrency, lỗi tạm/vĩnh viễn/`BUSY`, ghi đoạn một transaction) (F5, F6, P1).
- [ ] **Bước 11** - Retry thủ công (F7).
- [ ] **Bước 12** - `PublishedContentService` và `LearnerDownloadController` (kiểm ghi danh, lớp `OPEN`, mục hiển thị) (F8, BR-U05-04, 23).
- [ ] **Bước 13** - `RetrievalService` (kiểm phạm vi, trần, vector câu hỏi, k ≤ 20) (F9, BR-U05-40…43).
- [ ] **Bước 14** - Unit test cho mọi `BR-U05-xx`: URL YouTube giả mạo, markdown có script, PDF không chữ, video không caption, vượt trần.
- [ ] **Bước 15** - Tóm tắt: `aidlc-docs/construction/u05-content-material-rag/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và adapter ngoài

- [ ] **Bước 16** - Flyway `V20260925_1200__u05_content_rag.sql` theo `infrastructure-design.md` §4.
- [ ] **Bước 17** - JPA repository; `VectorSearchRepository` (lọc tài liệu trong phạm vi, `<=>`, `ef_search`) (P6).
- [ ] **Bước 18** - `GeminiEmbeddingAdapter` (`batchEmbedContents`, 768 chiều, key trong header, timeout) và `YoutubeAdapter` (Data API playlist, caption ưu tiên vi → en → tự động) (P5).
- [ ] **Bước 19** - Integration test Testcontainers (image pgvector, Redis, RabbitMQ) với adapter giả: ingest end-to-end; `retrieve` không trả đoạn của bài nháp/lưu trữ/lớp khác; phiên bản mới giữ mục không ingest lại. Adapter thật test bằng mock HTTP (429, 403, playlist nhiều trang).
- [ ] **Bước 20** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 21** - `/contracts/openapi/u05-content.yaml` (endpoint theo `frontend-components.md`).
- [ ] **Bước 22** - Controller + DTO + validation.
- [ ] **Bước 23** - Test MockMvc: giảng viên không sửa được nội dung cấp môn, học viên lớp khác không tải được file, không có endpoint `retrieve` công khai.
- [ ] **Bước 24** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 25** - `ChapterList`, `LessonEditor`, `VersionBar`, `ItemList` với `TextItemEditor`, `FileItemEditor`, `YoutubeItemEditor`.
- [ ] **Bước 26** - `IngestionStatusBadge` (poll, Thử lại) và `SubjectLessonPicker`.
- [ ] **Bước 27** - `LessonViewer` dùng chung (`TextItemView` với `rehype-sanitize`, `FileItemView`, `YoutubeItemView`); gắn vào `LearnerClassPage` của U04.
- [ ] **Bước 28** - Test frontend: markdown có script bị lọc, URL YouTube sai bị chặn, poll dừng ở trạng thái cuối.
- [ ] **Bước 29** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 30** - Cập nhật `README.md`: tạo `GEMINI_API_KEY`, `YOUTUBE_API_KEY`; chạy local không có key; hạ `U05_INGEST_CONCURRENCY` khi VPS nhỏ; cách U13/U15 dùng `RagRetrievalPort`.
- [ ] **Bước 31** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-CNT-001 (UC-CNT-01) | 5, 6, 9, 10, 11, 25, 26 |
| US-CNT-002 (UC-CNT-02, 03) | 5, 6, 7, 25 |
| US-CNT-005 (UC-CNT-08) | 3, 6, 10, 18 |
| UC-CNT-04 (qua U04) | 12, 27 |
| RAG cho U13/U15 | 8, 13, 17, 19 |

## 5. Ngoài phạm vi

- US-CNT-003, US-CNT-004 (Phase 2, chờ hội ý).
- Gọi LLM tạo đề/chấm (U13, U15).
- Cờ kill-switch AI thật và cấu hình quota trên UI (U13, FR-021).

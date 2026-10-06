# U05 Content, Material & RAG - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U05. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story trong phạm vi**: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Catalog chỉ chứa story MVP.
- **Use case**: UC 11, UC 12 (popup View Learning Material), UC 13, UC 14 (Comment on Announcement).
- **Quyết định 2026-10-04**: chapter đổi thành module của môn (Chủ nhiệm môn tạo trên Subject Detail, mọi lớp dùng chung); mỗi module có nút tải tệp/gắn link; học liệu của môn (`class_id` rỗng) hoặc của lớp; hỏi đáp lớp đổi thành bình luận dưới thông báo, hiện 2 bình luận mới nhất, popup xem thêm.
- **Thiết kế nguồn**: `construction/u05-content-material-rag/` (functional-design, nfr-requirements, nfr-design, infrastructure-design), `construction/shared-infrastructure.md` và [database](../../../../docs/database.md).
- **Quyết định 2026-10-03**: học liệu chỉ tải lên rồi quét; không soạn markdown, không phiên bản, không phát hành.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `JobPort`, `JobHandler`, `PendingSweeper` | U03 | Dùng thật |
| `ArtifactPort`, `FileUploader` | U03 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật; U05 thay `EmptyPublishedContentAdapter` của U04 bằng `PublishedContentService` |
| `EventPublisherPort` | U03 | Phát sự kiện thông báo lớp sau commit (bình luận không phát); U16 tiêu thụ |
| `AiUsagePort` | U13 (`C`) | Adapter tạm đọc `AI_KILL_SWITCH` từ `.env`, không trừ credit; U13 thay bằng bản thật (`ai_services`, trần chi phí, `ai_suggestions`, credit U07) trước khi bật Gemini |
| `EmbeddingPort`, `YoutubePort` | Gemini, YouTube | Adapter thật + adapter giả khi không có key |

### Dữ liệu U05 sở hữu

PostgreSQL `modules`, `lessons` (gồm `class_id`, `extracted_text`, `embedding vector(768)`), `announcements`, `announcement_comments` (bảng nối); việc `LESSON_SCAN` trên `jobs.gemini`, `YOUTUBE_CAPTION` trên `jobs.youtube` (U03 khai báo queue).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  content/
    api/                ContentController, StudentDownloadController,
                        ClassCommunicationController, DTO
    application/        ModuleService, LessonService, PublishedContentService,
                        RetrievalService, ClassCommunicationService
    domain/             Module, Lesson, ScanStatus, YoutubeUrlParser,
                        Announcement, AnnouncementComment
    scan/               TextExtractor, CaptionFetcher, PassageSelector
    infrastructure/     JPA repository, LessonVectorRepository (JdbcTemplate),
                        GeminiEmbeddingAdapter, YoutubeAdapter, FakeEmbeddingAdapter,
                        FakeYoutubeAdapter, EnvAiUsageAdapter
    worker/             LessonScanHandler, LessonPendingSweeper
    port/               PublishedContentPort, RagRetrievalPort, ContentRefPort,
                        EmbeddingPort, YoutubePort, AiUsagePort
/backend/src/main/resources/db/migration/content/
/infra/postgres/init/01-extensions.sql
/frontend/src/shared/content/
/frontend/src/app/classes/[id]/communication/
/contracts/openapi/content.yaml
```

## 3. Các bước

### Nhóm A - Khung và hạ tầng

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `tika-parsers-standard-package`, `com.pgvector:pgvector`, thư viện đọc phụ đề YouTube. Biến cấu hình U05 theo `logical-components.md` §3.
- [ ] **Bước 2** - Docker Compose (image `pgvector/pgvector:pg16` đã có từ khung): script `infra/postgres/init/01-extensions.sql` (`CREATE EXTENSION vector`), RAM worker 1,5 GB, truyền `GEMINI_API_KEY`, `YOUTUBE_API_KEY`, `AI_KILL_SWITCH`. Nginx CSP thêm `frame-src https://www.youtube-nocookie.com`.

### Nhóm B - Domain và logic

- [ ] **Bước 3** - Domain: `Module` (thuộc môn), `Lesson` (`FILE`/`YOUTUBE`, của môn hoặc của lớp) với `ScanStatus` và chuyển trạng thái; `YoutubeUrlParser` (chỉ một video, regex ID) (BR-U05-02, 10, 11, 14, 22, NFR-U05-21).
- [ ] **Bước 4** - Port và adapter giả: `EmbeddingPort`, `YoutubePort`, `AiUsagePort`, `PublishedContentPort`, `RagRetrievalPort`, `ContentRefPort` (P7).
- [ ] **Bước 5** - `ModuleService`: Chủ nhiệm môn tạo, đổi tên, đổi thứ tự, lưu trữ module của môn trên Subject Detail (mọi lớp dùng chung, không sao chép); audit (F1, BR-U05-01, 14, 50).
- [ ] **Bước 6** - `LessonService`: tải tệp (đổi `FileRef` qua `ArtifactPort.attach`) hoặc link video vào module; Chủ nhiệm môn tải trên Subject Detail → `class_id` rỗng, giảng viên tải trên Class Detail → `class_id` của lớp (BR-U05-02, 12); `PENDING` + gửi việc trong cùng transaction, Quét lại, lưu trữ (F2, F4, BR-U05-21, 22, 30, 37).
- [ ] **Bước 7** - `TextExtractor` (Tika theo luồng, giới hạn ký tự, `NO_TEXT`), `CaptionFetcher` (vi → en → tự động, `NO_CAPTION`) (P2, BR-U05-32, 33).
- [ ] **Bước 8** - `LessonScanHandler` (cập nhật có điều kiện, semaphore, `AiUsagePort` begin/complete/fail, một vector, ghi một transaction, `BUSY`/`NO_CREDIT`/`FAILED`) và `LessonPendingSweeper` (lesson `PENDING` có `scanned_at` quá 5 phút) (F3, P1, P3, BR-U05-35…39).
- [ ] **Bước 9** - `PublishedContentService` và `StudentDownloadController` (kiểm ghi danh, lớp `OPEN`, lesson `ACTIVE`) (F5, BR-U05-04, 23).
- [ ] **Bước 10** - `RetrievalService` (kiểm phạm vi, `AiUsagePort` cho `requesterId`, vector câu hỏi, k ≤ 10, `PassageSelector` ≤ 12 000 ký tự) (F6, P4, BR-U05-40…44).
- [ ] **Bước 11** - `ClassCommunicationService`: giảng viên đăng thông báo (phát event U16 sau commit), thành viên lớp bình luận (không phát event), danh sách kèm 2 bình luận mới nhất, xem toàn bộ bình luận, ẩn có lý do; kiểm quyền U04, lọc markdown (F7, F8, BR-U05-60…65).
- [ ] **Bước 12** - Unit test cho mọi `BR-U05-xx`: URL YouTube giả mạo hoặc playlist, markdown có script, PDF không chữ, video không phụ đề, vượt trần, thiếu credit.
- [ ] **Bước 13** - Tóm tắt: `aidlc-docs/construction/u05-content-material-rag/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và adapter ngoài

- [ ] **Bước 14** - Flyway `V20260925_1200__u05_content.sql` theo `infrastructure-design.md` §4.
- [ ] **Bước 15** - JPA repository; `LessonVectorRepository` (lọc lesson trong phạm vi, `<=>`) (P4).
- [ ] **Bước 16** - `GeminiEmbeddingAdapter` (768 chiều, key trong header, timeout) và `YoutubeAdapter` (phụ đề ưu tiên vi → en → tự động) (P5).
- [ ] **Bước 17** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers (image pgvector, Redis, RabbitMQ) với adapter giả: tải lên → quét → `INDEXED`; `retrieve` không trả lesson lưu trữ/lớp khác/môn khác, phạm vi môn không trả học liệu của lớp; lớp khác không thấy học liệu riêng của lớp; quét chạy lại không trừ credit hai lần; thiếu credit khác `BUSY`. Adapter thật test bằng mock HTTP (429, 403).
- [ ] **Bước 18** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 19** - `/contracts/openapi/content.yaml` (endpoint theo `frontend-components.md`).
- [ ] **Bước 20** - Controller + DTO + validation.
- [ ] **Bước 21** - Test MockMvc: giảng viên không sửa được học liệu của môn, học viên lớp khác không tải được tệp hoặc bình luận, người học không đăng được thông báo, giảng viên không tạo được module, không có endpoint `retrieve` công khai.
- [ ] **Bước 22** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 23** - `ModuleList`, `ModuleItem` (nút "Tải tệp", "Gắn link video"), `LessonRow` gắn vào Subject Detail (chế độ môn) và Class Detail (chế độ lớp) của U04; `ScanStatusBadge` (poll, Quét lại).
- [ ] **Bước 24** - Popup `UploadLearningMaterialsDialog` mở từ nút của module (nhiều tệp hoặc một link video, module đã chọn sẵn).
- [ ] **Bước 25** - Popup `ViewLearningMaterialDialog` cho Student (PDF xem trực tiếp, tải tệp, video `youtube-nocookie`).
- [ ] **Bước 26** - Màn Announcements: `AnnouncementFeed`, `AnnouncementForm` (giảng viên), `AnnouncementCard` (2 bình luận mới nhất, ô bình luận), popup `CommentsDialog` (xem thêm), trạng thái ẩn (UC 13, UC 14).
- [ ] **Bước 27** - Test frontend: markdown có script bị lọc, URL YouTube sai hoặc playlist bị chặn, poll dừng ở trạng thái cuối; badge phân biệt "Hệ thống đang bận" và "Không đủ credit AI".
- [ ] **Bước 28** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 29** - Cập nhật `README.md`: tạo `GEMINI_API_KEY`, `YOUTUBE_API_KEY`; chạy local không có key; hạ `U05_SCAN_CONCURRENCY` khi VPS nhỏ; cách U13 dùng `RagRetrievalPort`.
- [ ] **Bước 30** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-CNT-001 (UC 11) | 5, 6, 7, 8, 23, 24 |
| US-CNT-002 (UC 11) | 5, 6, 23, 24 |
| US-CNT-005 (UC 11) | 3, 6, 8, 16 |
| US-CNT-004 (UC 13, UC 14) | 11, 14, 19-21, 26 |
| UC 12 | 9, 25 |
| RAG cho U13 | 10, 15, 17 |

## 5. Ngoài phạm vi

- Soạn nội dung trực tiếp, phiên bản, phát hành học liệu, playlist YouTube (bỏ ngày 2026-10-03).
- Tìm kiếm/tóm tắt học liệu cho người dùng (ngoài phạm vi dự án).
- Gọi LLM tạo đề/chấm (U13, U15).

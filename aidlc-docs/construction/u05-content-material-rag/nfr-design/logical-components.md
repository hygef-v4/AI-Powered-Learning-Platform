# U05 Content, Material & RAG - Logical Components

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (CN môn, GV, SV)                    U04 / U13 (gọi nội bộ)
   |                                               |
   v                                               v
+-----------------------------------------------------------------------------+
| backend                                                                     |
| ContentController --> ModuleService, LessonService                          |
|                   --> ArtifactPort (U03), JobPort (U03)                     |
|                   --> AiUsagePort (U13): giữ credit khi tải lên             |
| LessonViewController --> ClassAccessPort (U04) --> ArtifactPort (U03)       |
| PublishedContentService (PublishedContentPort, U04 gọi)                     |
| AnnouncementController --> AnnouncementService --> ClassAccessPort (U04)    |
|                        --> EventPublisherPort (U03, sau commit)             |
| RetrievalService (RagRetrievalPort, U13 gọi) --> AiUsagePort (U13)          |
|                  --> EmbeddingPort --> LessonVectorRepository (pgvector)    |
+-----------------------------------------------------------------------------+
                   | việc LESSON_SCAN / YOUTUBE_CAPTION
                   v
+-----------------------------------------------------------------------------+
| worker                                                                      |
| LessonScanHandler --> TextExtractor / CaptionFetcher (YoutubePort)          |
|                   --> AiUsagePort (U13) --> LessonSummarizer (SummaryPort)  |
|                   --> AiUsagePort (U13) --> EmbeddingPort (Gemini)          |
|                   --> ghi lessons (PostgreSQL)                              |
| LessonPendingSweeper                                                        |
+-----------------------------------------------------------------------------+
```

**Text alternative**: Chủ nhiệm môn và giảng viên quản lý học liệu qua `ContentController`; service lưu lesson, gắn tệp qua U03, giữ credit của người tải lên qua U13 và gửi việc quét qua U03. Mọi người xem và tải học liệu qua `LessonViewController` sau khi kiểm phạm vi (Chủ nhiệm môn, giảng viên lớp, hoặc ghi danh qua U04). Giảng viên tạo, sửa, xóa thông báo qua `AnnouncementController`; service kiểm quyền U04 rồi phát event sau commit cho U16. Worker quét lesson: trích chữ hoặc lấy phụ đề, xin phép AI qua U13, tóm tắt, tạo một vector từ bản tóm tắt và ghi vào `lessons`. Khi U13 truy xuất, `RetrievalService` tạo vector câu hỏi và tìm lesson gần nhất trong pgvector.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ModuleService`, `LessonService` | backend | F1, F2, F3, F5, F8; giữ credit khi tải lên |
| `AnnouncementController`, `AnnouncementService` | backend | F11–F13, quyền lớp và event U16 |
| `LessonViewController` | backend | F4, F9: xem, tải học liệu theo R2, R3/R4, R5 |
| `PublishedContentService` | backend | `PublishedContentPort` (F9 bước 1) |
| `RetrievalService`, `LessonVectorRepository` | backend | F10, P4 |
| `LessonScanHandler`, `TextExtractor`, `CaptionFetcher` | worker | F6, P1, P2 |
| `LessonSummarizer` | worker | Chia đoạn, gọi `SummaryPort` từng đoạn rồi gộp (BR-U05-45…47) |
| `LessonPendingSweeper` | worker | BR-U03-58; gửi lại `PENDING` quá 5 phút và `BUSY` mỗi 30 phút, `BUSY` quá 24 giờ → `FAILED` (BR-U05-37) |
| `GeminiEmbeddingAdapter`, `GeminiSummaryAdapter`, `YoutubeAdapter` + adapter giả | backend, worker | P5, P7 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `GEMINI_API_KEY` | Rỗng → adapter giả |
| `YOUTUBE_API_KEY` | Rỗng → adapter giả |
| `U05_SCAN_CONCURRENCY` | 4 (VPS < 8 GB: 2) |
| `U05_EMBED_MODEL` | `gemini-embedding-001` |
| `U05_EMBED_DIMENSIONS` | 768 |
| `U05_MAX_TEXT_CHARS` | 2000000 |
| `U05_SUMMARY_MAX_INPUT_CHARS` / `U05_SUMMARY_CHUNK_CHARS` / `U05_SUMMARY_MAX_CHARS` | 200000 / 30000 / 4000 |

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

## Announcement lifecycle
AnnouncementService có createAnnouncement, updateAnnouncement, deleteAnnouncement theo version, R3/R4 và audit cùng transaction; feed `GET /me/announcements` lấy lớp `OPEN` actor học hoặc dạy qua `ClassAccessPort.listOpenClassesOf` và bỏ `DELETED`. Event `class.announcement-posted` chỉ phát khi tạo, không phát khi sửa/xóa.

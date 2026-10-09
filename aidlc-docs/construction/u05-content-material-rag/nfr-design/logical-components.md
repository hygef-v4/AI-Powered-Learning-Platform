# U05 Content, Material & RAG - Logical Components

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

Upload → U03 lưu tệp → U05 trích chữ/phụ đề → EXTRACTED (không AI/credit).
View Material → kiểm quyền xem U04 → nguồn, SummaryAction, SummaryBlock.
SummaryAction → SummaryRequestService → U13 AiGuard/HOLD → U03 MATERIAL_SUMMARY job.
Worker → SummaryPort/checkpoint → EmbeddingPort → INDEXED → settle HOLD của requester.
RetrievalService → chỉ ACTIVE/INDEXED trong phạm vi → pgvector.

**Text alternative**: Chủ nhiệm môn và giảng viên quản lý học liệu qua `ContentController`; service lưu lesson, gắn tệp qua U03 và gửi việc trích chữ không AI/credit; SummaryRequestService riêng nhận nút của người xem rồi giữ credit qua U13. Mọi người xem và tải học liệu qua `LessonViewController` sau khi kiểm phạm vi (Chủ nhiệm môn, giảng viên lớp, hoặc ghi danh qua U04). Giảng viên tạo, sửa, xóa thông báo qua `AnnouncementController`; service kiểm quyền U04 rồi phát event sau commit cho U16. Worker quét lesson: trích chữ hoặc lấy phụ đề tới EXTRACTED; khi người xem yêu cầu mới xin phép AI qua U13, tóm tắt và tạo một vector từ bản tóm tắt và ghi vào `lessons`. Khi U13 truy xuất, `RetrievalService` tạo vector câu hỏi và tìm lesson gần nhất trong pgvector.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ModuleService`, `LessonService` | backend | F1, F2, F3, F5, F8; upload không AI/credit |
| `AnnouncementController`, `AnnouncementService` | backend | F11–F13, quyền lớp và event U16 |
| `LessonViewController` | backend | F4, F9 và endpoint F7: xem, tải, yêu cầu tóm tắt theo R2, R3/R4, R5 |
| `PublishedContentService` | backend | `PublishedContentPort` (F9 bước 1) |
| `RetrievalService`, `LessonVectorRepository` | backend | F10, P4 |
| `LessonScanHandler`, `TextExtractor`, `CaptionFetcher` | worker | F6, P1, P2 |
| `SummaryRequestService` | backend | F7: scope, quote, khóa lesson, HOLD và metadata/enqueue nguyên tử |
| `MaterialSummaryHandler` | worker | F7: claim/checkpoint và HOLD summary+embedding |
| `LessonSummarizer` | worker | Chia đoạn, gọi `SummaryPort` từng đoạn rồi gộp (BR-U05-45…47) |
| `LessonPendingSweeper` | worker | Gửi lại PENDING/BUSY khi đến retry_at; phục hồi SCANNING lease hết hạn bằng CAS; deadline tuyệt đối 24 giờ, terminal chốt credit (BR-U05-37) |
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

# U05 Content, Material & RAG - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service quản lý nội dung, `PublishedContentService`, `RetrievalService`, `ClassCommunicationController/Service` | `backend` |
| `YoutubeResolveHandler`, `IngestJobHandler` | `worker` |
| Bảng nội dung, `source_documents`, `rag_chunks` (cột `vector(768)`) | `postgres` (image có pgvector) |
| Trần chi phí Gemini | Dùng chung bộ đếm `gemini:daily-cost:{yyyyMMdd}` của U13 qua `AiBudgetPort` (U05 không có key Redis riêng) |
| Queue | `jobs.youtube`, `jobs.gemini` (listener concurrency 4) |
| Event lớp | `EventPublisherPort` (U02) phát sau commit trên `platform.events`: `class.announcement-posted`, `class.question-posted`, `class.answer-posted`; U16 tiêu thụ |

## 2. Thay đổi hạ tầng dùng chung

| Mục | Giá trị |
|---|---|
| PostgreSQL | Đổi image sang `pgvector/pgvector:pg16` (cùng PostgreSQL 16, thêm extension); `CREATE EXTENSION IF NOT EXISTS vector` trong script khởi tạo DB (chạy bằng user `postgres`), không cho `migrator` quyền superuser |
| Worker | Giới hạn RAM 768 MB → **1,5 GB** vì 4 job trích chữ song song; VPS cần ≥ 8 GB RAM. VPS nhỏ hơn: `U05_INGEST_CONCURRENCY=2`, giữ 1 GB |
| CSP | Thêm `frame-src https://www.youtube-nocookie.com` |
| Kết nối ra | Backend, worker tới `generativelanguage.googleapis.com:443`; worker tới `www.googleapis.com:443` (YouTube Data API) và `www.youtube.com:443` (caption) |
| Secret CI/CD | `GEMINI_API_KEY`, `YOUTUBE_API_KEY`; biến thường `AI_KILL_SWITCH=false` |

## 3. Tạo key và giới hạn chi phí

1. Tạo một Google Cloud project, bật **Generative Language API** và **YouTube Data API v3**.
2. Tạo `GEMINI_API_KEY` trong Google AI Studio và `YOUTUBE_API_KEY` giới hạn chỉ cho YouTube Data API. Hạn mức/chi phí thực tế phụ thuộc cấu hình nhà cung cấp; hệ thống vẫn tính credit AI của người chịu phí khi embedding được xử lý.
3. Đặt giới hạn quota/ngân sách trong Google Cloud nếu bật billing (REL-005).

## 4. Migration

`V20260925_1200__u05_content_rag.sql`:
- `chapters`, `lessons`, `lesson_versions` (unique `(lesson_id, version_no)`, partial unique 1 `DRAFT` và 1 `PUBLISHED` mỗi bài), `lesson_items`, `class_lesson_links`, `youtube_sources`; `source_documents` có thêm `youtube_source_id`, `video_title`, `order_no` cho video YouTube (index `(youtube_source_id, order_no)`); `class_announcements`, `class_questions`, `class_answers` (index theo classId/questionId và createdAt, không xóa cứng).
- `source_documents` (unique `content_key`, `charged_to_account_id`), `youtube_sources.charged_to_account_id`; `rag_chunks` với `embedding vector(768)` và index `USING hnsw (embedding vector_cosine_ops)`, index `source_document_id`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | CSP chỉ mở `frame-src` cho `youtube-nocookie` |
| SECURITY-09 | Compliant | Key trong secret CI/CD, không commit |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| RESILIENCY-06 | N/A | Health dùng chung; Gemini lỗi không làm backend `DOWN` |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |

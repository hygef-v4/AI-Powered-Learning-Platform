# U05 Content, Material & RAG - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `ContentController`, `LessonViewController`, service quản lý học liệu, `PublishedContentService`, `RetrievalService`, `AnnouncementController/Service` | `backend` |
| `LessonScanHandler`, `LessonPendingSweeper` | `worker` |
| Bảng `modules`, `lessons` (cột `summary`, `embedding vector(768)`), `announcements` | `postgres` (image có pgvector) |
| Trần chi phí Gemini, credit | Qua `AiUsagePort` của U13 (U05 không có key Redis riêng) |
| Queue | `jobs.gemini` (`LESSON_SCAN`, concurrency 4), `jobs.youtube` (`YOUTUBE_CAPTION`) |
| Event | `EventPublisherPort` (U03) phát sau commit trên `platform.events`: `class.announcement-posted` (U16 tiêu thụ) |

## 2. Thay đổi hạ tầng dùng chung

| Mục | Giá trị |
|---|---|
| PostgreSQL | Image `pgvector/pgvector:pg16` đã có từ khung dự án (plan U03 K5); `CREATE EXTENSION IF NOT EXISTS vector` trong script khởi tạo DB (chạy bằng user `postgres`), không cho `migrator` quyền superuser |
| Worker | Giới hạn RAM 768 MB → **1,5 GB** vì 4 việc quét học liệu song song; VPS cần ≥ 8 GB RAM. VPS nhỏ hơn: `U05_SCAN_CONCURRENCY=2`, giữ 1 GB |
| CSP | Thêm `frame-src https://www.youtube-nocookie.com` |
| Kết nối ra | Backend, worker tới `generativelanguage.googleapis.com:443`; worker tới `www.googleapis.com:443` (YouTube Data API: tiêu đề video) và `www.youtube.com:443` (caption) |
| Secret CI/CD | `GEMINI_API_KEY`, `YOUTUBE_API_KEY`; biến thường `AI_KILL_SWITCH=false` |

## 3. Tạo key và giới hạn chi phí

1. Tạo một Google Cloud project, bật **Generative Language API** và **YouTube Data API v3**.
2. Tạo `GEMINI_API_KEY` trong Google AI Studio và `YOUTUBE_API_KEY` giới hạn chỉ cho YouTube Data API. Hạn mức/chi phí thực tế phụ thuộc cấu hình nhà cung cấp; hệ thống vẫn tính credit AI của người chịu phí khi embedding được xử lý.
3. Đặt giới hạn quota/ngân sách trong Google Cloud nếu bật billing (REL-005).

## 4. Migration

`db/migration/content/V20260925_1200__create_modules_lessons_announcements.sql` (chưa áp dụng nên sửa trực tiếp, không cần migration bổ sung):
- `modules` (FK `subject_id` → `subjects`, index `(subject_id, order_no)`).
- `lessons` (FK `module_id` → `modules`, `class_id` → `course_classes` cho phép rỗng, `uploaded_by` → `accounts`; cột `uploaded_at`, `summary` ≤ 4 000 ký tự cho phép rỗng; CHECK `source_type` khớp cột tệp/YouTube; `embedding vector(768)` với index `USING hnsw (embedding vector_cosine_ops)`; index `(module_id, class_id, order_no)`, `(scan_status, scanned_at)` cho sweeper).
- `announcements` (FK `class_id` → `course_classes`, `author_id`, `updated_by`, `deleted_by` → `accounts`; `status` `VISIBLE`/`DELETED`; `version` NOT NULL mặc định 0; `updated_at`, `deleted_at`; index `(class_id, status, posted_at)` cho feed).

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | CSP chỉ mở `frame-src` cho `youtube-nocookie` |
| SECURITY-09 | Compliant | Key trong secret CI/CD, không commit |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| RESILIENCY-06 | N/A | Health dùng chung; Gemini lỗi không làm backend `DOWN` |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |


# U05 Content, Material & RAG - NFR Design Patterns

**Bản tài liệu 2026-10-08**: UC 14, 26, 29, 30, 31, 51, 52; primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Quét trong worker
1. `LessonScanHandler` nhận `LESSON_SCAN {lessonId}` hoặc `YOUTUBE_CAPTION {lessonId}`; giữ semaphore `U05_SCAN_CONCURRENCY` (mặc định 4) (NFR-U05-01).
2. `UPDATE lessons SET scan_status = 'SCANNING' WHERE id = :id AND scan_status = 'PENDING'`; không dòng nào bị cập nhật thì bỏ qua (idempotent).
3. `TextExtractor` hoặc `CaptionFetcher` → `AiUsagePort.begin` (một `requestRef` = `lessonId:scanNo`, cố định qua thử lại) → `EmbeddingPort.embed` → `AiUsagePort.complete` → ghi kết quả trong một transaction (BR-U05-35, 36, 39).
4. Lỗi tạm ném `RetryableJobException` cho U03; `BUSY`/`NO_CREDIT` ghi trạng thái và kết thúc; hết lượt → `onFailed` ghi `FAILED` và `AiUsagePort.fail`.
5. `LessonPendingSweeper` trả lesson `PENDING` có `scanned_at` quá 5 phút để U03 gửi lại.

## P2 - Trích chữ theo luồng
- `TextExtractor` dùng Tika với `InputStream` từ `ArtifactPort.open`, trả chữ theo trang (PDF) hoặc theo slide/đoạn (PPTX/DOCX).
- Dừng khi đạt 2 000 000 ký tự (NFR-U05-02).
- Trung bình < 50 ký tự/trang → `NO_TEXT` (BR-U05-32).

## P3 - Một vector mỗi lesson
- Lấy phần đầu `extracted_text` trong giới hạn đầu vào của `gemini-embedding-001` (khoảng 2 000 token), gọi một lần, lưu `embedding vector(768)`.
- Index HNSW `vector_cosine_ops` trên `lessons.embedding`.

## P4 - Truy xuất
- SQL: lọc lesson `ACTIVE`, `INDEXED` thuộc module `ACTIVE` của môn, có `class_id` rỗng (phạm vi môn) hoặc rỗng/bằng lớp yêu cầu (phạm vi lớp), `ORDER BY embedding <=> :q LIMIT :k` (NFR-U05-04).
- Trong bộ nhớ: tách `extracted_text` theo đoạn, chấm theo số từ của câu hỏi xuất hiện, lấy đoạn tốt nhất đến khi tổng ≤ 12 000 ký tự (BR-U05-42).

## P5 - Gọi Gemini và YouTube
- Spring `RestClient`, timeout theo NFR-U05-14; header `x-goog-api-key`, không đưa key vào URL hay log.
- YouTube: server dựng URL từ `videoId` đã kiểm bằng regex `[A-Za-z0-9_-]{11}` (NFR-U05-21).

## P6 - Hiển thị an toàn
- Frontend `react-markdown` + `rehype-sanitize` (schema mặc định, bỏ `iframe`, `style`); link chỉ `http`, `https` (NFR-U05-20).
- Video dựng từ `videoId`: `https://www.youtube-nocookie.com/embed/{videoId}`.

## P7 - Adapter giả
- `FakeEmbeddingAdapter` (vector băm từ nội dung, cố định) và `FakeYoutubeAdapter` (phụ đề mẫu) khi không có key hoặc trong test (NFR-U05-31).

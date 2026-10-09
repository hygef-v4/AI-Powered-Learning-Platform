# U05 Content, Material & RAG - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Quét trong worker, retry và credit
1. LessonScanHandler nhận lessonId, semaphore concurrency 4; claim transaction ngắn bằng CAS từ PENDING/BUSY khi retry_at tới, scan_expires_at chưa tới và không lease đang giữ. Đặt SCANNING, claim UUID và lease 5 phút; mọi checkpoint/kết quả/gia hạn kiểm claim/hạn để ngăn worker cũ ghi muộn.
2. Trích chữ/caption → AiUsagePort.begin với holdId tra từ U13. SummaryPort theo chunk/merge rồi embedding từ summary; requestRef ổn định `lessonId:SUMMARY:chunkIndex`, `lessonId:SUMMARY:MERGE`, `lessonId:EMBEDDING`. Complete và checkpoint ai_suggestions.result idempotent; không gọi lại chunk đã complete. Summary hoàn tất ghi ngay, dùng lại sau retry.
3. Lỗi tạm chuyển SCANNING → PENDING, tăng retry count, đặt retry_at U03 backoff, xóa claim/lease rồi ném RetryableJobException. Tối đa 5 retry/recovery; BUSY đặt retry_at 30 phút, không đếm lỗi tạm. Giới hạn scan_expires_at 24 giờ từ tạo lesson không kéo dài; tới hạn/hết retry/lỗi vĩnh viễn → FAILED.
4. Sweeper mỗi phút gửi PENDING quá 5 phút/retry_at tới và BUSY khi retry_at tới; SCANNING lease hết hạn thì CAS thu hồi claim và reset PENDING hoặc FAILED theo giới hạn. Worker cũ không tiếp tục ghi/settle. Hold U13 25 giờ là dự phòng, deadline quét vẫn 24 giờ.
5. Terminal INDEXED/NO_TEXT/NO_CAPTION/FAILED chốt hold trong transaction idempotent; settle chỉ tổng sử dụng thật, trả phần chưa dùng. Chưa gọi AI trả toàn bộ. FAILED sau summary giữ summary; không RAG khi chưa INDEXED. Timeout provider không có bảo đảm exactly-once bên ngoài; không cộng credit dùng trùng cùng checkpoint.

## P2 - Trích chữ theo luồng
- `TextExtractor` dùng Tika với `InputStream` từ `ArtifactPort.open`, trả chữ theo trang (PDF) hoặc theo slide/đoạn (PPTX/DOCX).
- Dừng khi đạt 2 000 000 ký tự (NFR-U05-02).
- Trung bình < 50 ký tự/trang → `NO_TEXT` (BR-U05-32).

## P3 - Một vector mỗi lesson
- Tạo vector từ `summary` (≤ 4 000 ký tự, vừa giới hạn đầu vào của `gemini-embedding-001`), gọi một lần, lưu `embedding vector(768)` (BR-U05-35).
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

## Contract checkpoint qua port

findHold trả HoldSnapshot; begin trả UsageStart, READY là REPLAY kèm CallSnapshot/checkpoint nên không gọi provider. RUN trả ticket gắn scanClaimId. U05 khóa/kiểm claim, lease và deadline trong cùng transaction khi begin/complete/fail; U13 kiểm ticket metadata và HOLD RESERVED. Khóa HOLD serialize complete với release/scanner; stale ticket hoặc HOLD đã chốt không ghi/cộng usage. Không giữ transaction qua provider; contract chi tiết ở [domain](../functional-design/domain-entities.md).

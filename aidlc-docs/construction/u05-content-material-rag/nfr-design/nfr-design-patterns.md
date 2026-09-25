# U05 Content, Material & RAG - NFR Design Patterns

## P1 - Pipeline ingest trong worker
1. `IngestJobHandler` nhận `U05_INGEST {sourceDocumentId}`; handler giữ semaphore `U05_INGEST_CONCURRENCY` (mặc định 4) (NFR-U05-01).
2. `claim`: UPDATE `indexStatus = PROCESSING` khi đang `PENDING`; đã `INDEXED` thì bỏ qua (idempotent).
3. `TextExtractor` → `Chunker` → với mỗi lô tối đa 100 đoạn: `EmbeddingBudget.reserve(tokens)` → `CreditPort.reserve(chargedToAccountId, estimate, requestRef = sourceId + batchNo)` → `EmbeddingPort.embed` → `CreditPort.settle` theo token của lô; khi mọi lô xong, `ChunkWriter` ghi đoạn trong một transaction (BR-U05-36, 39). Nếu lô chưa gọi Gemini mà bị lỗi, `CreditPort.release` phần giữ của lô đó.
4. Lỗi tạm ném `RetryableJobException` cho U02; lỗi vĩnh viễn/`BUSY` ghi `FAILED` và kết thúc job.

## P2 - Trích chữ theo luồng
- `TextExtractor` dùng Tika với `InputStream` từ `ArtifactPort.open`, trả chữ theo trang (PDF) hoặc theo slide/đoạn (PPTX/DOCX).
- Dừng khi đạt 2 000 000 ký tự, ghi cờ `truncated` (NFR-U05-02).
- Trung bình < 50 ký tự/trang → `NO_TEXT` (BR-U05-32).

## P3 - Cắt đoạn
- Gom theo trang/timestamp tới ~3 000 ký tự, chồng lấn ~400 ký tự, không cắt giữa từ; mỗi đoạn giữ `pageNo` đầu hoặc `startMs`/`endMs` (BR-U05-34).
- Token ước tính = số ký tự / 4.

## P4 - Trần embedding (EmbeddingBudget)
- Redis `u05:embed-tokens:{yyyyMMdd}` (giờ Asia/Ho_Chi_Minh), TTL 48 giờ.
- `reserve(n)`: `INCRBY n`; vượt `U05_EMBED_DAILY_TOKENS` → `DECRBY n` và ném `BusyException` (NFR-U05-10, 11). Nếu thiếu credit hoặc lỗi khác trước khi gọi Gemini, `DECRBY n` để trả lượt đã giữ.
- Kill-switch AI **bật** (`true`, đọc qua `AiKillSwitchPort`; U13 sở hữu cờ, chưa có U13 thì đọc biến `.env`) → `BusyException` (NFR-U05-12). Giá trị `false` cho phép tiếp tục kiểm trần và credit.
- `BusyException` trong ingest → `FAILED`, `errorCode = BUSY`; trong `retrieve` → `503` "Hệ thống đang bận". Kiểm trần trước khi giữ credit; nếu trần đổi sau khi giữ, trả phần giữ và không gọi Gemini.
- Thiếu credit của người tạo nguồn hoặc người yêu cầu AI là lỗi riêng `INSUFFICIENT_CREDIT`, không hiển thị như lỗi hệ thống. `requestRef` riêng cho từng lô embedding và cố định qua retry; nguồn đã `INDEXED` không bị tính phí lại. Gemini báo hết quota (429) sau các lần retry cũng hiển thị "Hệ thống đang bận".

## P5 - Gọi Gemini và YouTube
- Spring `RestClient`, timeout theo NFR-U05-14; header `x-goog-api-key`, không đưa key vào URL hay log.
- YouTube: server dựng URL từ `videoId`/`playlistId` đã kiểm bằng regex (`[A-Za-z0-9_-]{11}` cho video, `[A-Za-z0-9_-]{10,64}` cho playlist) (NFR-U05-21).

## P6 - Truy xuất vector
- Query SQL: lọc `source_document_id IN (tài liệu thuộc bài PUBLISHED trong phạm vi)` rồi `ORDER BY embedding <=> :q LIMIT :k` (NFR-U05-04).
- Index HNSW `vector_cosine_ops`; với lọc chặt, đặt `hnsw.ef_search = 100` trong phiên query.
- Danh sách tài liệu trong phạm vi tính bằng một query join `lesson_versions (PUBLISHED)` → `lesson_items` → `class_lesson_links`.

## P7 - Hiển thị an toàn
- Frontend `react-markdown` + `rehype-sanitize` (schema mặc định, bỏ `iframe`, `style`); link chỉ `http`, `https` (NFR-U05-20).
- Video dựng từ `videoId`: `https://www.youtube-nocookie.com/embed/{videoId}`.

## P8 - Adapter giả
- `FakeEmbeddingAdapter` (vector băm từ nội dung, cố định) và `FakeYoutubeAdapter` (caption mẫu) khi không có key hoặc trong test (NFR-U05-31).

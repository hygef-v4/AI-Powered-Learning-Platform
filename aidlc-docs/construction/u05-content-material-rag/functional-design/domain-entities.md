# U05 Content, Material & RAG - Domain Entities

## 1. Phạm vi sở hữu

U05 sở hữu chương, bài giảng và phiên bản, mục nội dung, liên kết bài cấp môn vào lớp, nguồn YouTube, tài liệu nguồn RAG và đoạn (chunk) có vector, thông báo và hỏi đáp lớp. U05 **không** sở hữu: byte file (U03), quyền vào lớp (U04), gọi LLM tạo đề/chấm (U13), job (U02) hoặc inbox thông báo (U16).

## 2. `Chapter`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `scopeType` | enum | `SUBJECT`, `CLASS` |
| `subjectId` | UUID | Luôn có |
| `classId` | UUID | Có khi `scopeType = CLASS` |
| `title` | chuỗi ≤ 200 | |
| `orderNo` | số | Thứ tự trong phạm vi |
| `archived` | bool | |

## 3. `Lesson` và `LessonVersion`

`Lesson`: `id`, `chapterId`, `orderNo`, `archived`.

`LessonVersion`:

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `lessonId` | UUID | |
| `versionNo` | số | Tăng dần, duy nhất theo bài |
| `title` | chuỗi ≤ 200 | |
| `status` | enum | `DRAFT`, `PUBLISHED`, `SUPERSEDED` |
| `publishedAt`, `publishedBy` | | Khi phát hành |

Mỗi bài tối đa 1 `DRAFT` và 1 `PUBLISHED`.

## 4. `LessonItem`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `lessonVersionId` | UUID | |
| `orderNo` | số | |
| `type` | enum | `TEXT`, `FILE`, `YOUTUBE` |
| `title` | chuỗi ≤ 200 | |
| `body` | markdown ≤ 50 000 ký tự | Khi `TEXT` |
| `artifactId` | UUID | Khi `FILE` (U03, purpose `MATERIAL`) |
| `youtubeSourceId` | UUID | Khi `YOUTUBE`; mỗi phiên bản bài tối đa 1 mục YouTube |
| `sourceDocumentId` | UUID | Tài liệu RAG tương ứng (`TEXT`, `FILE`) |

## 5. `ClassLessonLink`

`classChapterId`, `subjectLessonId`, `orderNo`. Lớp hiển thị **bản `PUBLISHED` mới nhất** của bài cấp môn; bài cấp môn bị lưu trữ hoặc chưa có bản phát hành thì không hiện.

## 6. YouTube

`YoutubeSource`: `id`, `url`, `kind` (`VIDEO`, `PLAYLIST`), `externalId`, `chargedToAccountId` (người thêm nguồn), `status` (`PENDING`, `RESOLVED`, `FAILED`).

`YoutubeVideo`: `id`, `sourceId`, `videoId`, `title`, `orderNo`, `sourceDocumentId`.

## 7. RAG

`SourceDocument`:

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `kind` | enum | `TEXT`, `FILE`, `YOUTUBE_VIDEO` |
| `contentKey` | chuỗi | SHA-256 nội dung (`TEXT`), `artifactId` (`FILE`) hoặc `videoId` (`YOUTUBE_VIDEO`); duy nhất - dùng lại khi phiên bản mới giữ nguyên mục |
| `chargedToAccountId` | UUID | Người tải/phát hành đã tạo nguồn mới; job ingest và retry dùng cùng tài khoản này để trừ credit embedding |
| `indexStatus` | enum | `PENDING`, `PROCESSING`, `INDEXED`, `NO_TEXT`, `NO_CAPTION`, `FAILED` |
| `errorCode` | chuỗi | Mã lỗi an toàn |
| `chunkCount` | số | |
| `language` | chuỗi | Ngôn ngữ caption |

`RagChunk`: `id`, `sourceDocumentId`, `chunkNo`, `text` (≤ ~3 000 ký tự), `pageNo` hoặc `startMs`/`endMs`, `embedding` (vector 768 chiều).

## 7a. Nội dung trao đổi trong lớp

`ClassAnnouncement`: `id`, `classId`, `title`, `body`, `authorId`, `status` (`VISIBLE`, `HIDDEN`), `createdAt`, `hiddenAt`, `hiddenBy`, `hideReason`.

`ClassQuestion`: `id`, `classId`, `title`, `body`, `authorId`, `status` (`VISIBLE`, `HIDDEN`), `createdAt`, thông tin ẩn như trên.

`ClassAnswer`: `id`, `questionId`, `authorId`, `body`, `status` (`VISIBLE`, `HIDDEN`), `createdAt`, thông tin ẩn như trên. Câu trả lời luôn thuộc lớp của câu hỏi; không có bảng thành viên hay thông báo riêng trong U05.

## 8. Trạng thái

```
LessonVersion: DRAFT --phát hành--> PUBLISHED --bản mới phát hành--> SUPERSEDED
SourceDocument: PENDING -> PROCESSING -> INDEXED
                                      -> NO_TEXT | NO_CAPTION   (kết thúc, không retry)
                                      -> FAILED --retry--> PENDING
```

**Text alternative**: Phiên bản bài đi từ `DRAFT` sang `PUBLISHED`; khi bản mới được phát hành, bản cũ thành `SUPERSEDED`. Tài liệu RAG đi từ `PENDING` sang `PROCESSING` rồi `INDEXED`; nếu không có chữ hoặc không có caption thì kết thúc ở `NO_TEXT`/`NO_CAPTION`; lỗi tạm thì `FAILED` và có thể retry về `PENDING`.

## 9. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `PublishedContentPort` | U05 cung cấp cho U04 | `listForClass(classId)`: chương, bài, mục đã phát hành (của lớp + bài cấp môn đã liên kết) |
| `RagRetrievalPort` | U05 cung cấp cho U13 | `retrieve(scope, query, k, requesterId, requestRef)` → đoạn kèm nguồn (bài, phiên bản, trang/timestamp); embedding câu hỏi tính credit cho `requesterId` |
| `ContentRefPort` | U05 cung cấp cho U08 | Kiểm `lessonVersionId` tồn tại, thuộc phạm vi |
| `ClassAccessPort` | U05 dùng U04 | Ghi danh, phạm vi lớp |
| `ArtifactPort` | U05 dùng U03 | `attach`, `open`, `issueDownloadToken` |
| `JobPort`, `AuditPort` | U05 dùng U02 | Job ingest, audit |
| `EmbeddingPort` | U05 dùng, adapter Gemini | `embed(texts)` → vector |
| `CreditPort` | U05 dùng U07 | `reserve/settle/release` credit cho embedding; adapter thật bắt buộc khi bật Gemini |
| `YoutubePort` | U05 dùng, adapter YouTube | Giải playlist, lấy caption |
| `EventPublisherPort` | U05 dùng U02; U16 nhận | Sau commit phát `u05.class.announcement-posted`, `u05.class.question-posted`, `u05.class.answer-posted`; payload gồm eventId, đối tượngId, actorId, classId; sự kiện trả lời có thêm questionAuthorId để U16 chọn người nhận mà không đọc bảng U05 |

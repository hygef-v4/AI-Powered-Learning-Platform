# U08 Assessment Core & Publication - Domain Entities

## 1. Phạm vi sở hữu

U08 sở hữu bài đánh giá (assignment), thành phần của bài, trạng thái duyệt, phát hành cho từng lớp và lịch. U08 **không** sở hữu: cấu hình riêng từng loại bài (U09), template/copy giữa lớp/simulation (U10), lượt làm và bài nộp (U11), nhóm (U12), AI và chạy code (U13), điểm (U15).

## 2. `Assignment`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `scopeType`, `scopeId` | | `CLASS` (U08) hoặc `SUBJECT` (đề chung, U09 dùng) |
| `title` | chuỗi ≤ 200 | |
| `instructions` | markdown ≤ 20 000 ký tự | |
| `assignmentType` | enum | `QUIZ`, `ESSAY`, `DRAWIO`, `CODE_LAB`, `GROUP`; một loại mỗi bài |
| `status` | enum | `DRAFT`, `REVIEWED`, `LOCKED`, `ARCHIVED` |
| `totalPoints` | numeric(6,2) | Tổng điểm thành phần, tự tính |
| `origin` | enum | `MANUAL`, `AI`, `CLONE` (`TEMPLATE`, `COPY` do U10) |
| `sourceAssignmentId` | UUID | Khi nhân bản |
| `aiProposalRef` | chuỗi | Khi từ AI: tham chiếu đề xuất và nguồn (U13) |
| `reviewedBy`, `reviewedAt` | | |
| `createdBy`, `version` | | Khóa lạc quan |

## 3. `AssignmentComponent`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `assignmentId` | UUID | |
| `sequenceNo` | số | |
| `componentType` | enum | `QUESTION`, `RUBRIC` |
| `bankItemId` | UUID | Câu/rubric từ ngân hàng U06 (phiên bản `ACTIVE`, ghim) |
| `inlineDefinition` | JSON | Câu riêng của bài, cùng cấu trúc `definition` U06, khi không có `bankItemId` |
| `points` | numeric(6,2) | Mặc định bằng `defaultPoints` của câu; sửa được khi `DRAFT` |

Mỗi thành phần có đúng một trong `bankItemId`, `inlineDefinition`. Loại câu phải khớp `assignmentType` (`QUIZ` ↔ `MCQ_*`, `ESSAY` ↔ `ESSAY`, `DRAWIO` ↔ `DRAWIO`, `CODE_LAB` ↔ `CODE`; `GROUP` gồm `ESSAY`/`DRAWIO`/`CODE`).

## 4. `Publication`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `assignmentId` | UUID | |
| `classId` | UUID | Một lớp mỗi lần phát hành |
| `opensAt`, `closesAt` | thời gian | `opensAt < closesAt` |
| `allowLate` | bool | |
| `lateUntil` | thời gian | Khi `allowLate`; `closesAt < lateUntil ≤ closesAt + 30 ngày` |
| `maxAttempts` | số | 1-10 |
| `deliveryMode` | enum | `STANDARD` (U08), `SIMULATION` (chính sách ở U10) |
| `status` | enum | `SCHEDULED`, `OPEN`, `CLOSED`, `RETIRED` |
| `publishedBy`, `publishedAt`, `retiredBy`, `retiredAt`, `retireReason` | | |

Unique: một `Publication` chưa `RETIRED` cho mỗi `(assignmentId, classId)`.

## 5. Trạng thái

```
Assignment: DRAFT --duyệt--> REVIEWED --phát hành lần đầu--> LOCKED --lưu trữ--> ARCHIVED
              ^                 |
              +----sửa----------+
Publication: SCHEDULED --tới opensAt--> OPEN --tới closesAt (hoặc lateUntil)--> CLOSED
                 |                        |
                 +--------ngưng giao------+--> RETIRED
```

**Text alternative**: Bài đi từ `DRAFT` sang `REVIEWED` khi giảng viên duyệt; sửa bài `REVIEWED` đưa về `DRAFT`. Phát hành lần đầu chuyển bài sang `LOCKED` và từ đó nội dung không sửa được; bài `LOCKED` có thể lưu trữ. Mỗi lần phát hành bắt đầu `SCHEDULED`, tới giờ mở thành `OPEN`, tới hạn (hoặc hạn nộp trễ) thành `CLOSED`; giảng viên có thể ngưng giao (`RETIRED`) khi đang `SCHEDULED` hoặc `OPEN`.

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `AssignmentQueryPort` | U08 cung cấp cho U09-U16 | Bài, thành phần, publication; `isSubmissionOpen(publicationId, now)` |
| `PublicationService` | U08 cung cấp cho U09 (đề chung), U10 | Tạo publication theo quy tắc U08 |
| `TypeConfigCheckPort` | U08 khai báo, U09 cài (`C`) | Cấu hình riêng loại bài đủ để duyệt chưa; chưa có U09 → chỉ kiểm phần U08 |
| `AiDraftPort` | U08 dùng, U13 cung cấp (`C`) | Yêu cầu và nhận đề xuất AI |
| `BankQueryPort`, `DefinitionValidationPort` | U08 dùng U06 | Lấy phiên bản, kiểm câu riêng |
| `ClassAccessPort` | U08 dùng U04 | Phạm vi, danh sách lớp |
| `JobPort`, `AuditPort`, `EventPublisherPort` | U08 dùng U02 | Mở/đóng theo lịch, audit, event |

# U08 Assessment Core & Publication - Domain Entities

Thiết kế độc lập công nghệ. Truy vết: `US-ASM-001`; `UC-ASM-01`, `UC-ASM-07`; khóa nội dung cho `US-QBK-002` S2, S3.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `Assignment` | Aggregate root (một bản ghi = một version) | `assignments` | U08 |
| `AssignmentComponent` | Entity | `assignment_components` | U08 |
| `Publication` | Aggregate root | `publications` | U08 |
| `QuestionTypeConfig` | Value object của `Assignment` | `assignments` | U09 |
| `DocumentSkeleton` | Value object của `Assignment` | `assignments` | U09 |
| `AssignmentLineage` | Value object của `Assignment` | `assignments` | U10 |
| `GradeRelease` | Value object của `Publication` | `publications` | U15 |

U08 tạo migration cho các bảng trên; U09, U10, U15 ghi phần của mình qua port của U08. U08 sở hữu dạng và chế độ bài; U08 **không** sở hữu cấu hình riêng từng loại bài (U09), template/copy (U10), lượt làm và bài nộp (U11), nhóm (U12), AI và chạy code (U13), điểm (U15).

## 2. `Assignment`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | Một bản ghi = một version |
| `stableKey` | UUID | Gom các version của cùng một bài |
| `versionNo` | số | Tăng dần theo `stableKey` |
| `ownerType` | enum | `CLASS` (bài của lớp) hoặc `SUBJECT_TEMPLATE` (template cấp môn của U10, không phát hành trực tiếp) |
| `classId` / `subjectId` | UUID | Theo `ownerType`; không có đề chung cấp môn |
| `title` | chuỗi ≤ 200 | |
| `instructions` | markdown ≤ 20 000 ký tự | |
| `assignmentType` | enum | `MULTIPLE_CHOICE_QUIZ`, `TEXT_ESSAY`, `DIAGRAM_ESSAY` (dùng cấu hình DOCUMENT), `CODE_LAB`, `GROUP_ASSIGNMENT`; một dạng mỗi bài |
| `gradingMode` | enum | `GRADED` hoặc `PRACTICE`; `GROUP_ASSIGNMENT` chỉ cho `GRADED` |
| `status` | enum | `DRAFT`, `REVIEWED`, `LOCKED`, `ARCHIVED` |
| `totalPoints` | numeric(6,2) | Tổng điểm thành phần, tự tính |
| `origin` | enum | `MANUAL`, `AI`, `CLONE`, `NEW_VERSION`, `TEMPLATE_COPY`, `CLASS_COPY` |
| `aiProposalRef` | chuỗi | Khi từ AI: tham chiếu đề xuất và nguồn (U13) |
| `typeConfig` | `QuestionTypeConfig` | U09 ghi |
| `skeleton` | `DocumentSkeleton` | U09 ghi; chỉ bài `DIAGRAM_ESSAY`/`GROUP_ASSIGNMENT` |
| `lineage` | `AssignmentLineage` | U10 ghi khi copy/nhân bản/version mới |
| `reviewedBy`, `reviewedAt` | | |
| `createdBy`, `version` | | Khóa lạc quan |

### Trạng thái

```mermaid
stateDiagram-v2
    [*] --> DRAFT: Tạo, nhân bản hoặc tạo version mới
    DRAFT --> REVIEWED: Giảng viên duyệt
    REVIEWED --> DRAFT: Sửa bài đã duyệt
    REVIEWED --> LOCKED: Phát hành lần đầu
    LOCKED --> ARCHIVED: Lưu trữ
```

**Text alternative**: Bài tạo ra ở `DRAFT`. Giảng viên duyệt thì thành `REVIEWED`; sửa bài đã duyệt đưa về `DRAFT`. Phát hành lần đầu chuyển bài sang `LOCKED` và từ đó nội dung không sửa được; muốn đổi thì ngưng giao hoặc đợi đóng rồi tạo version mới. Bài `LOCKED` có thể lưu trữ (`ARCHIVED`).

## 3. `AssignmentComponent`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `assignmentId` | UUID | |
| `sequenceNo` | số | |
| `componentType` | enum | `QUESTION`, `RUBRIC` |
| `bankItemId` | UUID | Câu/rubric từ ngân hàng U06 (phiên bản `ACTIVE`, ghim) |
| `inlineDefinition` | JSON | Câu riêng của bài, cùng cấu trúc `QuestionDefinition` U06, khi không có `bankItemId` |
| `points` | numeric(6,2) | Mặc định bằng `defaultPoints` của câu; sửa được khi `DRAFT` |

Mỗi thành phần có đúng một trong `bankItemId`, `inlineDefinition`. Loại câu phải khớp `assignmentType` (`MULTIPLE_CHOICE_QUIZ` ↔ `MCQ_*`, `TEXT_ESSAY` ↔ `ESSAY`, `DIAGRAM_ESSAY` ↔ `DOCUMENT`, `CODE_LAB` ↔ `CODE`; `GROUP_ASSIGNMENT` là tài liệu nhóm có đúng một thành phần `DOCUMENT` và khung mục việc).

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
| `status` | enum | `SCHEDULED`, `OPEN`, `CLOSED`, `RETIRED` |
| `gradeRelease` | `GradeRelease` | U15 ghi |
| `publishedBy`, `publishedAt`, `retiredBy`, `retiredAt`, `retireReason` | | |

Unique: một `Publication` chưa `RETIRED` cho mỗi `(assignmentId, classId)`.

### Trạng thái

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED: Phát hành cho lớp
    SCHEDULED --> OPEN: Tới opensAt
    OPEN --> CLOSED: Tới closesAt hoặc lateUntil
    SCHEDULED --> RETIRED: Ngưng giao
    OPEN --> RETIRED: Ngưng giao
```

**Text alternative**: Mỗi lần phát hành bắt đầu ở `SCHEDULED`; tới giờ mở thì `OPEN`; tới hạn (hoặc hạn nộp trễ nếu cho phép) thì `CLOSED`. Giảng viên có thể ngưng giao (`RETIRED`, bắt buộc lý do) khi đang `SCHEDULED` hoặc `OPEN`. `CLOSED` và `RETIRED` là trạng thái cuối.

## 5. Value object do unit khác ghi

| Value object | Định nghĩa ở | Unit ghi |
|---|---|---|
| `QuestionTypeConfig`, `DocumentSkeleton` | U09 domain entities | U09 |
| `AssignmentLineage` | U10 domain entities | U10 |
| `GradeRelease` | U15 domain entities | U15 |

## 6. Contract

### Port U08 cung cấp

| Port | Dùng bởi | Mô tả |
|---|---|---|
| `AssignmentQueryPort` | U09-U12, U14-U16 | Bài, thành phần, publication; `isSubmissionOpen(publicationId, now)` |
| `AssignmentService`, `PublicationService` | U10 | Tạo bài nháp (copy, template) |
| `AssignmentExtensionPort` | U09, U10, U15 | Ghi value object của mình vào bài/publication khi trạng thái cho phép |

### Port U08 khai báo, unit khác cài

| Port | Cài bởi | Mô tả |
|---|---|---|
| `TypeConfigPort` | U09 (`C`) | `check` cấu hình đủ để duyệt; `copy(fromId, toId)` khi tạo version mới hoặc nhân bản; chưa có U09 → bỏ qua |
| `GroupReadinessPort` | U12 (`C`) | Bài nhóm đủ nhóm hợp lệ, mỗi nhóm một trưởng nhóm, mọi Student có nhóm; chưa có U12 → không cho phát hành bài `GROUP_ASSIGNMENT` |
| `CodeLabCheckPort` | U13 (`C`) | Bài `CODE_LAB` đã kiểm lời giải mẫu với nội dung hiện tại |
| `PublicationLifecyclePort` | U11, U14 (`C`) | `onOpened(publicationId)`, `onRetired(publicationId)` gọi trong transaction mở bài/ngưng giao; cài đặt chỉ tạo job của unit nhận (tạo tài liệu nhóm, tự nộp). Chưa có U11/U14 → adapter rỗng |

### Port U08 dùng

| Port | Unit | Mô tả |
|---|---|---|
| `AiDraftPort` | U13 (`C`) | Yêu cầu và nhận đề xuất AI |
| `BankQueryPort`, `DefinitionValidationPort` | U06 | Lấy phiên bản, kiểm câu riêng |
| `ClassAccessPort` | U04 | Phạm vi, danh sách lớp |
| `JobPort`, `AuditPort`, `EventPublisherPort` | U02 | Mở/đóng theo lịch, audit, event `assignment.opened` (chỉ cho thông báo U16) |

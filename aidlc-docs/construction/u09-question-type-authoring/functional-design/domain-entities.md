# U09 Question Type Authoring - Domain Entities

**Bản tài liệu 2026-10-08**: UC 39, 40, 41, 42, 43; primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Thiết kế độc lập công nghệ. Truy vết: `US-ASM-004`…`007`, `US-GRP-003` (phần soạn bài); UC 39, 40, 41, 42, 43.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `QuestionTypeConfig` | Value object của `Assignment` (U08) | `assignments.config` | U09 |
| `DocumentSkeleton` | Value object của `Assignment` (U08) hoặc câu `DOCUMENT` (U06) | `assignments.config`, `questions.definition` | U09 (bài), U06 (câu ngân hàng) |
| `Document` | Value object (mô hình tài liệu dùng chung) | Nằm trong đối tượng chứa: khung, bài làm (U11), tài liệu nhóm (U14) | Unit sở hữu đối tượng chứa |
| `Block` | Value object của `Document` | Như trên | Như trên |
| `DocxImportPreview` | Kết quả trả về | Không lưu | U09 |

U09 sở hữu cấu hình riêng của assignment `MULTIPLE_CHOICE_QUIZ`, `TEXT_ESSAY`, `DIAGRAM_ESSAY`, `CODE_LAB` (ngôn ngữ, file khởi đầu, test, lời giải mẫu; U13 chạy kiểm), `GROUP_ASSIGNMENT` (khung chia phần) và mô hình `DOCUMENT` dùng cho Diagram Essay/Group Assignment; nhập khung từ DOCX, chuyển DOCX của Student thành block nháp, xuất DOCX và kiểm/rút gọn XML Draw.io. U09 **không** sở hữu: bài/publication và `gradingMode` (U08), câu hỏi ngân hàng (U06), bài làm (U11), Code Lab (U13). Không có đề chung cấp môn.

## 2. `QuestionTypeConfig`

| Loại bài | Nội dung cấu hình |
|---|---|
| `MULTIPLE_CHOICE_QUIZ` | `shuffleQuestions`, `shuffleOptions` (bool); `timeLimitMinutes` (rỗng hoặc 1-300); `showScoreAfterSubmit` (bool); `showCorrectAnswers` (`NEVER`, `AFTER_SUBMIT`, `AFTER_CLOSE`) |
| `TEXT_ESSAY` | `richText` (luôn `true`: đoạn văn, heading, danh sách, đậm/nghiêng); không giới hạn số từ hay số dòng; `questionRubrics[]` (mỗi câu một `rubricId`) |
| `DIAGRAM_ESSAY` | `DocumentSkeleton` bắt buộc; `parts[]` (mỗi phần một `rubricId`); `requiredDiagrams` (loại sơ đồ → số tối thiểu, tùy chọn) |
| `CODE_LAB` | Không có cấu hình riêng ngoài câu `CODE` (U06: ngôn ngữ, file khởi đầu, test, lời giải mẫu); U09 điều phối soạn câu trong bài và gọi U13 kiểm lời giải mẫu trước khi duyệt |
| `GROUP_ASSIGNMENT` | `DocumentSkeleton` bắt buộc, có ít nhất một phần; `parts[]` (mỗi phần một `rubricId`); chỉ chế độ `GRADED` |

Chỉ sửa khi bài `DRAFT`; khi tạo version mới hoặc nhân bản thì được sao chép qua `TypeConfigPort.copy`.

## 3. `DocumentSkeleton`

| Thuộc tính | Ý nghĩa |
|---|---|
| `blocks[]` | Toàn bộ block `origin = TEACHER` |
| `sourceDocxName` | Tên file nếu nhập từ DOCX |
| `contentHash` | Băm từng block để phát hiện người học sửa block bị khóa |

### `SkeletonPart` (phần tử của `config.parts[]`)

| Thuộc tính | Ý nghĩa |
|---|---|
| `partId` | ID phần, ổn định qua các lần sửa khung; U14 dùng làm khóa mục của tài liệu nhóm |
| `headingBlockId` | Heading nhỏ nhất của phần; `partId` lấy theo id block này |
| `ancestorHeadingIds[]` | Các heading cha trên nhánh của phần (từ Tiêu đề 1 xuống); chỉ U14 dùng khi mở popup làm phần được giao ở bài nhóm |
| `title` | Đường dẫn tiêu đề, ví dụ "H1 › H2.1 › H3.1" |
| `rubricId` | Rubric của phần (U06); bắt buộc khi duyệt |

Phần là heading nhỏ nhất của mỗi nhánh trong cây heading (BR-U09-25). Cây phần chỉ dùng cho rubric (mỗi phần một rubric) và cho việc giao phần ở bài nhóm; ở bài nhóm, popup làm phần được giao hiện các heading trên nhánh của phần cùng nội dung phần, không hiện nhánh khác (U14). Điểm tối đa của phần đọc từ rubric; điểm bài là tổng các phần (BR-U09-26).

## 4. `Document` và `Block`

```text
Document
  blocks[]                      thứ tự trong tài liệu
    id                          UUID do client/server sinh, duy nhất trong tài liệu
    origin                      TEACHER | STUDENT
    type                        HEADING | PARAGRAPH | LIST | TABLE | IMAGE | DIAGRAM
    HEADING    style TITLE | SUBTITLE | H1-H6 (giống Word: Tiêu đề, Phụ đề, Tiêu đề 1-6), text
               (H1-H6 tạo cây cha–con; heading nhỏ nhất của mỗi nhánh là một phần)
    PARAGRAPH  runs[] (text, bold, italic, underline, code)
    LIST       ordered, items[] (runs, level 0-4)
    TABLE      rows[][] ô (runs), headerRow
    IMAGE      fileId (U03 DOCUMENT_IMAGE), alt, width
    DIAGRAM    diagramType, xml (mxfile đầy đủ), svgPreview
```

**Text alternative**: Tài liệu là danh sách block theo thứ tự. Mỗi block có id, nguồn (giảng viên hay người học) và loại: heading (cấp 1-6), đoạn văn (các đoạn chữ có định dạng), danh sách, bảng, ảnh (tham chiếu file U03) hoặc sơ đồ (loại sơ đồ, XML Draw.io đầy đủ, ảnh SVG xem trước).

`DiagramType`: `USE_CASE`, `CLASS`, `SEQUENCE`, `ACTIVITY`, `ER`, `COMPONENT`, `STATE`, `OTHER`.

Quyền sửa theo block:

| Block của giảng viên | Người học được |
|---|---|
| Mọi loại (`HEADING`, `PARAGRAPH`, `LIST`, `TABLE`, `IMAGE`, `DIAGRAM`) | Không sửa, không xóa, không di chuyển (khung khóa hoàn toàn) |
| Block của người học | Toàn quyền, chèn ở bất kỳ vị trí nào |

## 5. `DocxImportPreview`

| Thuộc tính | Ý nghĩa |
|---|---|
| `blocks[]` | Block sẽ thêm (khung giảng viên: `TEACHER`; bài làm: `STUDENT`) |
| `diagramCount`, `imageCount` | Số sơ đồ nhận ra, số ảnh giữ nguyên |
| `droppedParts[]` | Phần không chuyển được, báo cho người dùng |

File DOCX chỉ xử lý trong bộ nhớ, không lưu.

## 6. Contract

### Port U09 cung cấp / cài

| Port | Dùng bởi | Mô tả |
|---|---|---|
| `TypeConfigPort` | U08 khai báo (`C`), U10 dùng; U11, U15 đọc cấu hình (trộn câu, giờ làm, hiện điểm) | `check` cấu hình đủ để duyệt (gồm rubric từng câu/từng phần); `copy(fromId, toId)` sao chép cấu hình, khung và nhân bản rubric từng câu/từng phần; `repointRubric(oldId, newId)` cho bài `DRAFT` khi rubric của câu hoặc phần được sửa ở Question Bank |
| `DocumentModelPort` | U06 (`C`), U11, U13, U14, U15 | `validateSkeleton`, `validateForSave`, `validateForSubmit(skeleton, doc, requiredDiagrams)`, `toPlainText` |
| `DocxExportPort` | U11, U14, U15 | Tài liệu → DOCX |
| `DocxStudentImportPort` | U11 | DOCX → `DocxImportPreview`; U11 xác nhận và lưu nháp bằng kiểm phiên bản |
| `DiagramCompactPort` | U13 | XML đầy đủ → XML rút gọn theo allowlist, trong bộ nhớ |

### Port U09 dùng

| Port | Unit | Mô tả |
|---|---|---|
| `AuthorizationPort` | U01 | Tài khoản `ACTIVE`, vai trò |
| `AuditPort` | U02 | Audit |
| `ClassAccessPort` | U04 | Kiểm giảng viên của lớp/Chủ nhiệm môn khi soạn khung, nhập/xuất DOCX |
| `RubricPort` | U06 | `createForAssignment`, `revise`, `cloneForAssignment`, `getRubric` cho rubric từng câu và từng phần (BR-U09-23, 26…28) |
| `AssignmentQueryPort`, `AssignmentExtensionPort` | U08 | Đọc loại bài/trạng thái; ghi cấu hình, khung và điểm câu Text Essay theo rubric |
| `ArtifactPort` | U03 | Ảnh `DOCUMENT_IMAGE` |
| `CodeLabCheckPort` | U13 (`C`) | Trạng thái kiểm lời giải mẫu Code Lab khi soạn (UC 41); nút chạy kiểm là `VerifySolutionButton` của U13 (`CODE_RUN` loại `VERIFY`); chưa có U13 thì adapter giả báo "chưa kiểm được lời giải" |
| `AiDraftPort` | U13 (`C`) | AI soạn khung `SKELETON_DRAFT` cho Diagram Essay, bài nhóm (BR-U09-24); chưa có U13 thì ẩn nút AI |
| `BankQueryPort` | U06 | Kiểm câu quiz khi duyệt; đọc khung câu `DOCUMENT` để làm điểm xuất phát cho Diagram Essay hoặc bài nhóm (F2 bước 4) |

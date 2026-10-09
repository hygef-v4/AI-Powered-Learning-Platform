# U09 Question Type Authoring - Domain Entities

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Thiết kế độc lập công nghệ. Truy vết: `US-ASM-004`…`007`, `US-GRP-003` (phần soạn bài); UC 35, 42–46.

Quyết định 2026-10-09: câu của quiz, Text Essay, Code Lab nằm ở `assignment_questions` của U08 (từ ngân hàng của môn hoặc câu riêng của bài); rubric tự tạo cho mỗi câu Text Essay/phần; có bài của môn (không có bài nhóm ở cấp môn); quiz là quiz luyện tập.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `QuestionTypeConfig` | Value object của `Assignment` (U08) | `assignments.config` | U09 |
| `DocumentSkeleton`, `SkeletonPart` | Value object của `Assignment` | `assignments.config` | U09 |
| `Document` | Value object (mô hình tài liệu dùng chung) | Nằm trong đối tượng chứa: khung, bài làm (U11), tài liệu nhóm (U14) | Unit sở hữu đối tượng chứa |
| `Block` | Value object của `Document` | Như trên | Như trên |
| `DocxImportPreview` | Kết quả trả về | Không lưu | U09 |

U09 sở hữu cấu hình riêng của từng dạng bài (rubric từng câu Text Essay, khung và phần), cài đặt quiz, mô hình `DOCUMENT` dùng cho Diagram Essay/bài nhóm; nhập khung từ DOCX, chuyển DOCX của Student thành block nháp, xuất DOCX và kiểm/rút gọn XML Draw.io. U09 **không** sở hữu: bài, vòng đời và `gradingMode` (U08), câu của quiz, Text Essay, Code Lab (U08, U06), rubric (U06), bài làm (U11), chạy code và kết quả kiểm lời giải (U13).

## 2. `QuestionTypeConfig`

| Loại bài | Nội dung |
|---|---|
| `MULTIPLE_CHOICE_QUIZ` | `shuffleQuestions`, `shuffleOptions` (bool); `timeLimitMinutes` (rỗng hoặc 1-300); `showScoreAfterSubmit` (bool); `showCorrectAnswers` (`NEVER`, `AFTER_SUBMIT`) |
| `TEXT_ESSAY` | `questionRubrics[]` (`questionId` của câu trong bài → `rubricId`); văn bản thường có định dạng cơ bản, không giới hạn số từ hay số dòng |
| `CODE_LAB` | Không có cấu hình riêng ngoài câu `CODE` (U06); U09 hiện trạng thái kiểm lời giải từ U13 |
| `DIAGRAM_ESSAY` | `DocumentSkeleton` bắt buộc; `parts[]` (mỗi phần một `rubricId`); `requiredDiagrams` (loại sơ đồ → số tối thiểu, tùy chọn) |
| `GROUP_ASSIGNMENT` | `DocumentSkeleton` bắt buộc, có ít nhất một phần; `parts[]` (mỗi phần một `rubricId`); chỉ `GRADED`, chỉ bài của lớp |

Chỉ sửa khi bài `DRAFT`; khi tạo version mới, nhân bản hoặc copy thì được sao qua `TypeConfigPort.copy`.

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
| `ancestorHeadingIds[]` | Các heading cha trên nhánh của phần (từ Tiêu đề 1 xuống); U14 dùng khi mở popup làm phần được giao |
| `title` | Đường dẫn tiêu đề, ví dụ "H1 › H2.1 › H3.1" |
| `rubricId` | Rubric của phần (U06), tự tạo khi phần xuất hiện |

Phần là heading nhỏ nhất của mỗi nhánh trong cây heading (BR-U09-25). Điểm tối đa của phần đọc từ rubric; điểm bài là tổng các phần (BR-U09-26).

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

**Text alternative**: Tài liệu là danh sách block theo thứ tự. Mỗi block có id, nguồn (người soạn hay người học) và loại: heading (cấp 1-6), đoạn văn (các đoạn chữ có định dạng), danh sách, bảng, ảnh (tham chiếu file U03) hoặc sơ đồ (loại sơ đồ, XML Draw.io đầy đủ, ảnh SVG xem trước).

`DiagramType`: `USE_CASE`, `CLASS`, `SEQUENCE`, `ACTIVITY`, `ER`, `COMPONENT`, `STATE`, `OTHER`.

| Block của người soạn | Người học được |
|---|---|
| Mọi loại (`HEADING`, `PARAGRAPH`, `LIST`, `TABLE`, `IMAGE`, `DIAGRAM`) | Không sửa, không xóa, không di chuyển (khung khóa hoàn toàn) |
| Block của người học | Toàn quyền, chèn ở bất kỳ vị trí nào |

## 5. `DocxImportPreview`

| Thuộc tính | Ý nghĩa |
|---|---|
| `blocks[]` | Block sẽ thêm (khung: `TEACHER`; bài làm: `STUDENT`) |
| `diagramCount`, `imageCount` | Số sơ đồ nhận ra, số ảnh giữ nguyên |
| `droppedParts[]` | Phần không chuyển được, báo cho người dùng |

File DOCX chỉ xử lý trong bộ nhớ, không lưu.

## 6. Contract

### Port U09 cung cấp / cài

| Port | Dùng bởi | Mô tả |
|---|---|---|
| `TypeConfigPort` | U08 khai báo (`C`) và gọi; U11, U15 đọc cấu hình | `check(assignmentId)` cấu hình đủ để duyệt (gồm rubric đã điền); `syncQuestionRubrics(assignmentId, questionIds)` tự tạo/xóa rubric khi U08 thêm/bỏ câu Text Essay; `copy(fromId, toId)` sao cấu hình, khung và nhân bản rubric |
| `DocumentModelPort` | U11, U13, U14, U15 | `validateSkeleton`, `validateForSave`, `validateForSubmit(skeleton, doc, requiredDiagrams)`, `toPlainText` |
| `DocxExportPort` | U11, U14, U15 | Tài liệu → DOCX |
| `DocxStudentImportPort` | U11 | DOCX → `DocxImportPreview`; U11 xác nhận và lưu nháp bằng kiểm phiên bản |
| `DiagramCompactPort` | U13 | XML đầy đủ → XML rút gọn theo allowlist, trong bộ nhớ |

### Port U09 dùng

| Port | Unit | Mô tả |
|---|---|---|
| `AuthorizationPort` | U01 | Tài khoản `ACTIVE`, vai trò |
| `AuditPort` | U02 | Audit |
| `ClassScopePort`, `SubjectScopePort` | U04 | Giảng viên của lớp (R3/R4), Chủ nhiệm môn (R2) |
| `RubricPort` | U06 | `create` (rubric trống), `update`, `delete`, `cloneForAssignment`, `getRubric` cho rubric từng câu và từng phần |
| `AssignmentQueryPort`, `AssignmentExtensionPort` | U08 | Đọc dạng bài, phạm vi, trạng thái, câu của bài; ghi `config` |
| `BankQueryPort` | U06 | Đọc khung câu `DOCUMENT` làm điểm xuất phát cho Diagram Essay, bài nhóm |
| `ArtifactPort` | U03 | Ảnh `DOCUMENT_IMAGE` |
| `CodeLabCheckPort` | U13 (`C`) | `statusOf`: trạng thái kiểm lời giải mẫu từng câu `CODE` để hiện trên form; chưa có U13 thì báo "chưa kiểm được lời giải" |
| `AiDraftPort` | U13 (`C`) | AI soạn khung (`SKELETON_DRAFT`); chưa có U13 thì ẩn nút AI |

## 7. API

| API | Dùng để | Màn, UC | Quyền |
|---|---|---|---|
| `GET`, `PUT /api/v1/assignments/{id}/type-config` | Đọc, lưu cài đặt quiz | Quiz Detail, UC 35 | Người có quyền với quiz (U08) |
| `GET`, `PUT /api/v1/assignments/{id}/skeleton` | Đọc, lưu khung (có thể kèm `fromQuestionId` để sao khung từ câu `DOCUMENT` của ngân hàng); tính lại phần, tự tạo/xóa rubric phần | Assignment Form, UC 44, 45 | Người có quyền với bài |
| `POST /api/v1/assignments/{id}/skeleton:import-docx` | Nhập khung từ DOCX (xem trước) | Assignment Form | Người có quyền với bài |
| `POST /api/v1/assignments/{id}/skeleton:ai-draft`, `:apply-ai-draft` | Nhờ AI soạn khung, áp đề xuất | Assignment Form | Người có quyền với bài |
| `GET`, `PUT /api/v1/assignments/{id}/questions/{questionId}/rubric` | Xem, lưu rubric của câu Text Essay | Rubric Detail, UC 46 | Người có quyền với bài; giảng viên lớp chỉ xem bài của môn |
| `GET`, `PUT /api/v1/assignments/{id}/parts/{partId}/rubric` | Xem, lưu rubric của phần | Rubric Detail, UC 46 | Như trên |
| `POST /api/v1/attempts/{id}/docx:preview` (API của U11) | Người học nhập DOCX vào lượt DOCUMENT (xem trước); U11 kiểm quyền rồi gọi `DocxStudentImportPort` | Diagram Essay Workspace (U11) | Chủ lượt (U11 kiểm) |

Rubric không có API tạo hay xóa riêng: rubric tự tạo/xóa theo câu hoặc phần.

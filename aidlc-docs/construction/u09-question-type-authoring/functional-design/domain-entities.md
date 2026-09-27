# U09 Question Type Authoring - Domain Entities

Thiết kế độc lập công nghệ. Truy vết: `US-ASM-004`, `006`, `007`; `UC-ASM-02`, `03`, `04`, `06`.

## 1. Tổng quan

| Entity | Loại | Lưu ở | Unit ghi |
|---|---|---|---|
| `QuestionTypeConfig` | Value object của `Assignment` (U08) | `assignments` | U09 |
| `DocumentSkeleton` | Value object của `Assignment` (U08) hoặc câu `DOCUMENT` (U06) | `assignments`, `bank_items` | U09 (bài), U06 (câu ngân hàng) |
| `Document` | Value object (mô hình tài liệu dùng chung) | Nằm trong đối tượng chứa: khung, bài làm (U11), tài liệu nhóm (U14) | Unit sở hữu đối tượng chứa |
| `Block` | Value object của `Document` | Như trên | Như trên |
| `DocxImportPreview` | Kết quả trả về | Không lưu | U09 |

U09 sở hữu nghiệp vụ: cấu hình riêng của bài `QUIZ`, `ESSAY`, `DOCUMENT`; **mô hình tài liệu** (dùng chung cho soạn khung, làm bài, chấm); nhập khung từ DOCX và chuyển DOCX của người học thành block nháp; xuất DOCX; nhận sơ đồ Draw.io nhúng trong ảnh; kiểm và rút gọn XML Draw.io. U09 **không** sở hữu: bài/publication (U08), câu hỏi ngân hàng (U06), bài làm (U11), Code Lab (U13). Không có đề chung cấp môn.

## 2. `QuestionTypeConfig`

| Loại bài | Nội dung cấu hình |
|---|---|
| `QUIZ` | `shuffleQuestions`, `shuffleOptions` (bool); `timeLimitMinutes` (rỗng hoặc 1-300); `showScoreAfterSubmit` (bool); `showCorrectAnswers` (`NEVER`, `AFTER_SUBMIT`, `AFTER_CLOSE`) |
| `ESSAY` | `richText` (luôn `true`: đoạn văn, heading, danh sách, đậm/nghiêng); không giới hạn số từ |
| `DOCUMENT` | Có hoặc không có `DocumentSkeleton` (không có = trang trắng); `requiredDiagrams` (loại sơ đồ → số tối thiểu, tùy chọn) |

Chỉ sửa khi bài `DRAFT`; khi tạo version mới hoặc nhân bản thì được sao chép qua `TypeConfigPort.copy`.

## 3. `DocumentSkeleton`

| Thuộc tính | Ý nghĩa |
|---|---|
| `blocks[]` | Toàn bộ block `origin = TEACHER` |
| `sourceDocxName` | Tên file nếu nhập từ DOCX |
| `contentHash` | Băm từng block để phát hiện người học sửa block bị khóa |

## 4. `Document` và `Block`

```text
Document
  blocks[]                      thứ tự trong tài liệu
    id                          UUID do client/server sinh, duy nhất trong tài liệu
    origin                      TEACHER | LEARNER
    type                        HEADING | PARAGRAPH | LIST | TABLE | IMAGE | DIAGRAM
    HEADING    level 1-6, text, workSection (bài nhóm: heading đánh dấu một mục việc để thành viên nhận)
    PARAGRAPH  runs[] (text, bold, italic, underline, code)
    LIST       ordered, items[] (runs, level 0-4)
    TABLE      rows[][] ô (runs), headerRow
    IMAGE      artifactId (U03 DOCUMENT_IMAGE), alt, width
    DIAGRAM    diagramType, xml (mxfile đầy đủ), svgPreview
```

**Text alternative**: Tài liệu là danh sách block theo thứ tự. Mỗi block có id, nguồn (giảng viên hay người học) và loại: heading (cấp 1-6), đoạn văn (các đoạn chữ có định dạng), danh sách, bảng, ảnh (tham chiếu file U03) hoặc sơ đồ (loại sơ đồ, XML Draw.io đầy đủ, ảnh SVG xem trước).

`DiagramType`: `USE_CASE`, `CLASS`, `SEQUENCE`, `ACTIVITY`, `ER`, `COMPONENT`, `STATE`, `OTHER`.

Quyền sửa theo block:

| Block của giảng viên | Người học được |
|---|---|
| `HEADING`, `PARAGRAPH`, `LIST`, `IMAGE` | Không sửa, không xóa, không di chuyển |
| `TABLE` | Sửa nội dung ô, thêm/xóa dòng; không xóa bảng, không di chuyển |
| `DIAGRAM` | Sửa bản vẽ; không xóa, không di chuyển |
| Block của người học | Toàn quyền, chèn ở bất kỳ vị trí nào |

## 5. `DocxImportPreview`

| Thuộc tính | Ý nghĩa |
|---|---|
| `blocks[]` | Block sẽ thêm (khung giảng viên: `TEACHER`; bài làm: `LEARNER`) |
| `diagramCount`, `imageCount` | Số sơ đồ nhận ra, số ảnh giữ nguyên |
| `droppedParts[]` | Phần không chuyển được, báo cho người dùng |

File DOCX chỉ xử lý trong bộ nhớ, không lưu.

## 6. Contract

### Port U09 cung cấp / cài

| Port | Dùng bởi | Mô tả |
|---|---|---|
| `TypeConfigPort` | U08 khai báo (`C`), U10 dùng | `check` cấu hình đủ để duyệt; `copy(fromId, toId)` sao chép cấu hình và khung tài liệu |
| `DocumentModelPort` | U06 (`C`), U11, U13, U14, U15 | `validateSkeleton`, `validateForSave`, `validateForSubmit(skeleton, doc, requiredDiagrams)`, `toPlainText` |
| `DocxExportPort` | U11, U14, U15 | Tài liệu → DOCX |
| `DocxLearnerImportPort` | U11 | DOCX → `DocxImportPreview`; U11 xác nhận và lưu nháp bằng kiểm phiên bản |
| `DiagramCompactPort` | U13 | XML đầy đủ → XML rút gọn theo allowlist, trong bộ nhớ |

### Port U09 dùng

| Port | Unit | Mô tả |
|---|---|---|
| `AssignmentQueryPort`, `AssignmentExtensionPort` | U08 | Đọc loại bài/trạng thái; ghi cấu hình và khung |
| `ArtifactPort` | U03 | Ảnh `DOCUMENT_IMAGE` |

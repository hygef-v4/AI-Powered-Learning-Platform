# U09 Question Type Authoring - Domain Entities

## 1. Phạm vi sở hữu

U09 sở hữu cấu hình riêng của bài `QUIZ`, `ESSAY`, `DOCUMENT`; **mô hình tài liệu** (dùng chung cho soạn khung, làm bài, chấm); nhập khung từ DOCX; xuất tài liệu ra DOCX; nhận sơ đồ Draw.io nhúng trong ảnh; quy tắc kiểm và rút gọn XML Draw.io. U09 **không** sở hữu: bài/publication (U08), câu hỏi ngân hàng (U06), bài làm (U11), Code Lab (U13). Không có đề chung cấp môn.

## 2. `QuestionTypeConfig` (bảng `question_type_config`, khóa = `assignmentId`)

| Loại bài | `config` |
|---|---|
| `QUIZ` | `shuffleQuestions`, `shuffleOptions` (bool); `timeLimitMinutes` (rỗng hoặc 1-300); `showScoreAfterSubmit` (bool); `showCorrectAnswers` (`NEVER`, `AFTER_SUBMIT`, `AFTER_CLOSE`) |
| `ESSAY` | `richText` (luôn `true`: đoạn văn, heading, danh sách, đậm/nghiêng); không giới hạn số từ |
| `DOCUMENT` | `skeletonId` (khung, có thể rỗng = trang trắng); `requiredDiagrams` (loại → số tối thiểu, tùy chọn) |

## 3. Mô hình tài liệu

```
Document
  blocks[]                      thứ tự trong tài liệu
    id                          UUID do client/server sinh, duy nhất trong tài liệu
    origin                      TEACHER | LEARNER
    type                        HEADING | PARAGRAPH | LIST | TABLE | IMAGE | DIAGRAM
    HEADING    level 1-6, text
    PARAGRAPH  runs[] (text, bold, italic, underline, code)
    LIST       ordered, items[] (runs, level 0-4)
    TABLE      rows[][] ô (runs), headerRow
    IMAGE      artifactId (U03 DOCUMENT_IMAGE), alt, width
    DIAGRAM    diagramType, xml (mxfile đầy đủ), svgPreview
```

**Text alternative**: Tài liệu là danh sách block theo thứ tự. Mỗi block có id, nguồn (giảng viên hay người học) và loại: heading (cấp 1-6), đoạn văn (các đoạn chữ có định dạng), danh sách, bảng, ảnh (tham chiếu file U03) hoặc sơ đồ (loại sơ đồ, XML Draw.io đầy đủ, ảnh SVG xem trước).

`DiagramType`: `USE_CASE`, `CLASS`, `SEQUENCE`, `ACTIVITY`, `ER`, `COMPONENT`, `STATE`, `OTHER`.

## 4. `DocumentSkeleton` (khung của giảng viên)

`id`, `assignmentId` hoặc `bankItemId`, `blocks[]` (toàn bộ `origin = TEACHER`), `sourceDocxName` (nếu nhập từ DOCX), `contentHash` từng block.

## 5. Quyền sửa theo block

| Block của giảng viên | Người học được |
|---|---|
| `HEADING`, `PARAGRAPH`, `LIST`, `IMAGE` | Không sửa, không xóa, không di chuyển |
| `TABLE` | Sửa nội dung ô, thêm/xóa dòng; không xóa bảng, không di chuyển |
| `DIAGRAM` | Sửa bản vẽ; không xóa, không di chuyển |
| Block của người học | Toàn quyền, chèn ở bất kỳ vị trí nào |

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `TypeConfigPort` | U09 cài cho U08 (`C`), U10 dùng | `check` cấu hình đủ để duyệt; `copy(fromId, toId)` sao chép cấu hình và khung tài liệu |
| `DocumentModelPort` | U09 cài cho U06 (`C`), U11, U15 | `validateSkeleton`, `validateForSave`, `validateForSubmit(skeleton, doc, requiredDiagrams)`, `toPlainText` |
| `DocxExportPort` | U09 cung cấp cho U11, U15 | Tài liệu → DOCX |
| `DiagramCompactPort` | U09 cung cấp cho U13 | XML đầy đủ → XML rút gọn theo allowlist |
| `ArtifactPort` | U09 dùng U03 | Ảnh `DOCUMENT_IMAGE`, file DOCX tạm |
| `AssignmentQueryPort` | U09 dùng U08 | Loại bài, trạng thái `DRAFT` |

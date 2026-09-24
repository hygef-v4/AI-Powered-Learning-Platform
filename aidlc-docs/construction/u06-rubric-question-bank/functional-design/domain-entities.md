# U06 Rubric & Question Bank - Domain Entities

## 1. Phạm vi sở hữu

U06 sở hữu câu hỏi và rubric có phiên bản ở cấp môn và cấp lớp. U06 **không** sở hữu: bài đánh giá và việc dùng câu hỏi trong bài (U08), cấu hình riêng từng bài (U09), chạy code (U13), chấm (U15).

## 2. `BankItem` (một dòng = một phiên bản)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | Chính là ID phiên bản; bài của U08 lưu ID này |
| `stableKey` | UUID | Gom các phiên bản của cùng một câu/rubric |
| `versionNo` | số | Duy nhất theo `stableKey` |
| `itemType` | enum | `QUESTION`, `RUBRIC` |
| `scopeType` | enum | `SUBJECT`, `CLASS` |
| `subjectId` | UUID | Luôn có |
| `classId` | UUID | Khi `CLASS` |
| `title` | chuỗi ≤ 200 | |
| `definition` | JSON | Nội dung theo loại (§3, §4) |
| `status` | enum | `DRAFT`, `ACTIVE`, `RETIRED` |
| `difficulty` | enum | `EASY`, `MEDIUM`, `HARD` (chỉ câu hỏi) |
| `tags` | danh sách chuỗi | ≤ 10 tag, mỗi tag ≤ 50 ký tự |
| `lessonRefs` | danh sách ID | Chương/bài U05 cùng phạm vi, ≤ 10 |
| `clonedFrom` | UUID | Phiên bản gốc khi nhân bản |
| `createdBy`, `createdAt`, `activatedAt` | | |

## 3. `definition` của câu hỏi

| `questionType` | Nội dung |
|---|---|
| `MCQ_SINGLE`, `MCQ_MULTI` | `stem` (markdown), 2-6 `options` (`id`, `text`), `correctOptionIds`, `explanation` tùy chọn |
| `ESSAY` | `stem`, `answerGuide` tùy chọn, `rubricId` tùy chọn, `maxWords` tùy chọn |
| `DRAWIO` | `stem`, `sampleXmlArtifactId` tùy chọn (U03 `DRAWIO_FULL`), `rubricId` tùy chọn |
| `CODE` | `stem`, `language`, `starterCode`, `testCases` (`input`, `expectedOutput`, `hidden`, `points`), `timeLimitMs`, `rubricId` tùy chọn |

Mọi câu có `defaultPoints` (> 0, tối đa 2 chữ số thập phân).

## 4. `definition` của rubric (checklist)

```
Rubric
  scaleMax (tổng điểm = tổng điểm mọi mục)
  criteria[]            tiêu chí: id, title
    items[]             mục checklist: id, text, points (> 0)
```

**Text alternative**: Rubric gồm các tiêu chí; mỗi tiêu chí có các mục checklist, mỗi mục một số điểm dương. Chấm là tích đạt/không đạt từng mục; điểm tiêu chí là tổng mục được tích, điểm rubric là tổng các tiêu chí.

## 5. Trạng thái

```
DRAFT --kích hoạt--> ACTIVE --ngưng--> RETIRED
                        |
                        +--sửa--> phiên bản mới DRAFT (bản ACTIVE giữ nguyên)
```

**Text alternative**: Phiên bản mới ở `DRAFT`, kích hoạt thành `ACTIVE`; sửa bản `ACTIVE` tạo phiên bản `DRAFT` mới, khi kích hoạt bản mới thì bản cũ vẫn `ACTIVE` cho tới khi người dùng ngưng (`RETIRED`). Chỉ bản `DRAFT` chưa từng kích hoạt được xóa.

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `BankQueryPort` | U06 cung cấp cho U08, U09, U10, U13 | `getVersion(id)` (bản bất biến), `search(scope, filter)` chỉ trả `ACTIVE` |
| `RubricPort` | U06 cung cấp cho U11, U15 | `getRubric(id)`, `score(rubricId, checkedItemIds)` |
| `ClassAccessPort`, `SubjectScopePort` | U06 dùng U04 | Phạm vi |
| `ArtifactPort` | U06 dùng U03 | XML mẫu Draw.io |
| `ContentRefPort` | U06 dùng U05 (`C`) | Kiểm `lessonRefs`; chưa có U05 thì bỏ qua kiểm và lưu ID |

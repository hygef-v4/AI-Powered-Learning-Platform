# U06 Rubric & Question Bank - Logical Components

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (CN môn)                         U08 / U09 / U11 / U13 / U15
   |                                               |
   v                                               v
+-----------------------------------------------------------------------------+
| backend                                                                     |
| QuestionController --> QuestionService --> DefinitionValidator              |
|                            |          --> QuestionScopeGuard (U04)          |
|                            |          --> ContentRefPort (U05)              |
|                            |          --> DocumentModelPort (U09, C)        |
|                            v                                                |
|                    QuestionRepository (PostgreSQL questions)                |
| ImportController --> ImportService --> XlsxRowReader / CsvRowReader         |
|                                    --> RowMapper --> QuestionService        |
| QuestionQueryService (BankQueryPort, InlineQuestionPort)                    |
| RubricService (RubricPort) --> RubricScorer, RubricRepository (rubrics)     |
+-----------------------------------------------------------------------------+
```

**Text alternative**: Chủ nhiệm môn thao tác ngân hàng qua `QuestionController`; `QuestionService` kiểm phạm vi qua U04, kiểm `definition` theo loại (khung tài liệu qua U09) và `lessonRefs` (U05) rồi lưu vào bảng `questions`. Nhập file đi qua `ImportController` và `ImportService`, đọc xlsx hoặc csv, ánh xạ từng dòng rồi dùng lại `QuestionService`. Các unit khác tìm, chọn ngẫu nhiên và đọc câu qua `QuestionQueryService`; U09 thêm, sửa, xóa, nhân bản rubric và U08 khóa rubric qua `RubricService`, U15 tính điểm rubric qua `RubricScorer`.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `QuestionService` | F3-F5; P1 |
| `DefinitionValidator` | P2 |
| `QuestionScopeGuard` | BR-U06-01…04 |
| `ImportService`, `XlsxRowReader`, `CsvRowReader`, `RowMapper` | F6; P3 |
| `QuestionQueryService` | F1, F2, F9; P5, P6 |
| `RubricService`, `RubricScorer` | F8; P4, P7 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U06_IMPORT_MAX_ROWS` | 500 |
| `U06_IMPORT_MAX_BYTES` | 5MB |
| `U06_CODE_LANGUAGES` | `java,python,c,cpp,javascript,dart,csharp` (khớp U13) |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log `definition` |
| SECURITY-05 | Compliant | P2, P3 |
| SECURITY-08 | Compliant | `QuestionScopeGuard`, P6 |
| SECURITY-15 | Compliant | P3 transaction mỗi dòng, P7 khóa có điều kiện |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

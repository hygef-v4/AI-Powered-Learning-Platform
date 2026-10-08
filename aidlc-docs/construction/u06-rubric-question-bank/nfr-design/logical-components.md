# U06 Rubric & Question Bank - Logical Components

**Bản tài liệu 2026-10-08**: UC 32, 33, 44, 55, 56; primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (CN môn, GV)                      U08 / U09 / U10 / U11 / U13 / U15
   |                                                    |
   v                                                    v
 +------------------------------ backend ------------------------------------+
 | BankController --> BankItemService --> DefinitionValidator                 |
 |                        |          --> BankScopeGuard (U04)                 |
 |                        v                                                   |
 |                 BankItemRepository (PostgreSQL questions, rubrics)         |
 | ImportController --> ImportService --> XlsxRowReader / CsvRowReader        |
 |                                    --> RowMapper --> BankItemService       |
 |                                    --> DocumentModelPort (U09, C)          |
 | BankQueryService (BankQueryPort), RubricScorer (RubricPort)                |
 +----------------------------------------------------------------------------+
```

**Text alternative**: Chủ nhiệm môn và giảng viên thao tác qua `BankController`; `BankItemService` kiểm phạm vi qua U04, kiểm `definition` rồi lưu vào bảng `questions` hoặc `rubrics`. Nhập file đi qua `ImportController` và `ImportService`, đọc xlsx hoặc csv, ánh xạ từng dòng rồi dùng lại `BankItemService`; khung tài liệu kiểm qua U09. Các unit khác đọc phiên bản qua `BankQueryService` và tính điểm rubric qua `RubricScorer`.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `BankItemService` | F1-F4; P1 |
| `DefinitionValidator` | P2 |
| `BankScopeGuard` | BR-U06-01…04 |
| `ImportService`, `XlsxRowReader`, `CsvRowReader`, `RowMapper` | F6; P3 |
| `BankQueryService` | F5, F9; P5, P6 |
| `RubricScorer` | P4 |

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
| SECURITY-08 | Compliant | `BankScopeGuard`, P6 |
| SECURITY-15 | Compliant | P3 transaction mỗi dòng |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

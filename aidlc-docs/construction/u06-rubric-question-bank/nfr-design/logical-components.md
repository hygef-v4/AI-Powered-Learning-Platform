# U06 Rubric & Question Bank - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (CN môn, GV)                      U08 / U09 / U10 / U11 / U13 / U15
   |                                                    |
   v                                                    v
 +------------------------------ backend ------------------------------------+
 | BankController --> BankItemService --> DefinitionValidator                 |
 |                        |          --> BankScopeGuard (U04)                 |
 |                        v                                                   |
 |                 BankItemRepository (PostgreSQL bank_items)                 |
 | ImportController --> ImportService --> XlsxRowReader / CsvRowReader        |
 |                                    --> RowMapper --> BankItemService       |
 |                                    --> ArtifactPort (U03, XML mẫu)         |
 | BankQueryService (BankQueryPort), RubricScorer (RubricPort)                |
 +----------------------------------------------------------------------------+
```

**Text alternative**: Chủ nhiệm môn và giảng viên thao tác qua `BankController`; `BankItemService` kiểm phạm vi qua U04, kiểm `definition` rồi lưu vào bảng `bank_items`. Nhập file đi qua `ImportController` và `ImportService`, đọc xlsx hoặc csv, ánh xạ từng dòng rồi dùng lại `BankItemService`; XML mẫu Draw.io lưu qua U03. Các unit khác đọc phiên bản qua `BankQueryService` và tính điểm rubric qua `RubricScorer`.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `BankItemService` | F1-F4; P1 |
| `DefinitionValidator` | P2 |
| `BankScopeGuard` | BR-U06-01…04 |
| `ImportService`, `XlsxRowReader`, `CsvRowReader`, `RowMapper` | F6; P3 |
| `BankQueryService` | F5, F7; P5, P6 |
| `RubricScorer` | P4 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U06_IMPORT_MAX_ROWS` | 500 |
| `U06_IMPORT_MAX_BYTES` | 5MB |
| `U06_CODE_LANGUAGES` | `java,python,cpp,javascript` (khớp U13) |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log `definition` |
| SECURITY-05 | Compliant | P2, P3 |
| SECURITY-08 | Compliant | `BankScopeGuard`, P6 |
| SECURITY-15 | Compliant | P3 transaction mỗi dòng |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

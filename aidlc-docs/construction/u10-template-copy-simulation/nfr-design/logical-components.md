# U10 Template & Copy - Logical Components

## 1. Sơ đồ

TemplateController gọi TemplateService và U08 để phát hành template. CopyController gọi AssignmentCopier, ScopeGuard (U04), U08, U06 và U09 trong một transaction. DiffController gọi AssignmentDiffer và đọc version từ U08. U10 lưu `template_releases` và lineage qua `AssignmentExtensionPort`.

**Text alternative**: Chủ nhiệm môn phát hành template qua `TemplateService` (template là bài U08). Giảng viên copy qua `AssignmentCopier`, kiểm quyền hai phía qua U04 rồi gọi U08, U06, U09 trong một transaction. `AssignmentDiffer` đọc hai version từ U08 để so sánh.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `TemplateService` | F1 |
| `AssignmentCopier`, `ScopeGuard` | F2, F3; P1 |
| `AssignmentDiffer` | F4; P2 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | Kiểm nguồn, đích và cấu hình copy |
| SECURITY-08 | Compliant | `ScopeGuard` |
| SECURITY-15 | Compliant | P1 rollback |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

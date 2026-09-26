# U10 Template, Copy & Simulation - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (CN môn, GV)                         U11 / U15
   |                                                 |
   v                                                 v
 +---------------------------- backend -------------------------------------+
 | TemplateController --> TemplateService --> U08 (bài SUBJECT_TEMPLATE)     |
 | CopyController --> AssignmentCopier --> ScopeGuard (U04)                 |
 |                                     --> U08, U06 (copyToClass), U09 copy |
 | DiffController --> AssignmentDiffer --> U08 (đọc version)               |
 | SimulationPolicyService (SimulationPolicyPort), SimulationResultCalculator|
 | Repository (template_releases) + AssignmentExtensionPort (U08)           |
 +--------------------------------------------------------------------------+
```

**Text alternative**: Chủ nhiệm môn phát hành template qua `TemplateService` (template là bài U08). Giảng viên copy qua `AssignmentCopier`, thành phần kiểm quyền hai phía qua U04 rồi gọi U08, U06, U09 trong một transaction. `AssignmentDiffer` đọc hai version từ U08 để so sánh. U11 và U15 dùng `SimulationPolicyService` và `SimulationResultCalculator` cho thi thử.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `TemplateService` | F1 |
| `AssignmentCopier`, `ScopeGuard` | F2, F3; P1 |
| `AssignmentDiffer` | F4; P2 |
| `SimulationPolicyService` | F5; P3 |
| `SimulationResultCalculator` | F6; P4 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | Kiểm chính sách |
| SECURITY-08 | Compliant | `ScopeGuard` |
| SECURITY-15 | Compliant | P1 rollback, P3 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

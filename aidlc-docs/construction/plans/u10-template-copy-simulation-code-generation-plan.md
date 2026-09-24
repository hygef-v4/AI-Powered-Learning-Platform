# U10 Template, Copy & Simulation - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U10. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-ASM-008 (diff version), US-ASM-009, US-ASM-010, US-ASM-011 (cấu hình chính sách; làm bài ở U11). **Use case**: UC-ASM-15..18.
- **Thiết kế nguồn**: `construction/u10-template-copy-simulation/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ClassAccessPort`, `SubjectScopePort` | U04 | Dùng thật |
| `BankCopyPort` | U06 | Dùng thật (sao chép cấp lớp → lớp) |
| `AssignmentQueryPort`, `AssignmentService`, `PublicationService`, `PublishDialog` | U08 | Dùng thật; U08 cần có `ownerType`, version, `createDraftFrom` |
| `TypeConfigPort.copy` | U09 | Dùng thật |
| U10 cung cấp `SimulationPolicyPort` | cho U11, U15 | U11/U15 dùng khi được code |

### Dữ liệu U10 sở hữu

PostgreSQL `template_releases`, `assignment_lineage`, `simulation_policies`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u10/
    api/                TemplateController, CopyController, DiffController,
                        SimulationPolicyController, DTO
    application/        TemplateService, AssignmentCopier, ScopeGuard, AssignmentDiffer,
                        SimulationPolicyService, SimulationResultCalculator
    domain/             TemplateRelease, AssignmentLineage, SimulationPolicy, AssignmentDiff
    infrastructure/     JPA repository
    port/               SimulationPolicyPort, BankCopyPort
/backend/src/main/resources/db/migration/u10/
/frontend/src/app/teaching/subjects/[id]/templates/
/frontend/src/app/teaching/assignments/[id]/   (VersionHistoryPanel, AssignmentDiffView)
/frontend/src/shared/assessment/SimulationBadge.tsx
/contracts/openapi/u10-template-copy-simulation.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - `pom.xml`: `io.github.java-diff-utils:java-diff-utils`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `TemplateRelease`, `AssignmentLineage`, `SimulationPolicy`, `AssignmentDiff`; port `SimulationPolicyPort`, `BankCopyPort`.
- [ ] **Bước 3** - `TemplateService`: phát hành/rút template (F1, BR-U10-01…04).
- [ ] **Bước 4** - `ScopeGuard` và `AssignmentCopier` (template → lớp, lớp → lớp; sao chép câu/rubric cấp lớp; `TypeConfigPort.copy`; lineage; một transaction) (F2, F3, P1, BR-U10-05, 10…14).
- [ ] **Bước 5** - `AssignmentDiffer` (F4, P2, BR-U10-20…22).
- [ ] **Bước 6** - `SimulationPolicyService` (tạo/sửa, khóa có điều kiện) và `SimulationResultCalculator` (F5, F6, P3, P4, BR-U10-30…35).
- [ ] **Bước 7** - Audit theo BR-U10-40.
- [ ] **Bước 8** - Unit test mọi `BR-U10-xx`.
- [ ] **Bước 9** - Tóm tắt: `aidlc-docs/construction/u10-template-copy-simulation/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 10** - Flyway `V20260925_1700__u10_template_copy_simulation.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 11** - JPA repository.
- [ ] **Bước 12** - Integration test: copy lỗi giữa chừng không để lại dữ liệu; hai lần khóa chính sách đồng thời; `app` không sửa/xóa lineage.
- [ ] **Bước 13** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 14** - `/contracts/openapi/u10-template-copy-simulation.yaml`.
- [ ] **Bước 15** - Controller + DTO + validation.
- [ ] **Bước 16** - Test MockMvc: giảng viên không dạy lớp đích bị `404`; giảng viên thường không phát hành template; sửa chính sách đã khóa `409`.
- [ ] **Bước 17** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 18** - `TemplateListPage`, `CopyFromTemplateDialog`, `CopyToClassDialog`.
- [ ] **Bước 19** - `VersionHistoryPanel`, `AssignmentDiffView`.
- [ ] **Bước 20** - `SimulationPolicyForm` (gắn vào `PublishDialog` U08), `SimulationBadge`.
- [ ] **Bước 21** - Test frontend: badge ghi "Thi thử" và "không giới hạn", diff đánh dấu đúng loại thay đổi.
- [ ] **Bước 22** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 23** - Cập nhật `README.md`: template, copy, diff, thi thử; cách U11/U15 dùng `SimulationPolicyPort`.
- [ ] **Bước 24** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-008 (UC-ASM-15) | 5, 19 |
| US-ASM-009 (UC-ASM-16) | 3, 4, 18 |
| US-ASM-010 (UC-ASM-17) | 4, 18 |
| US-ASM-011 (UC-ASM-18) | 6, 20 |

## 5. Ngoài phạm vi

- Tạo version mới sau khi ngừng giao (U08), làm bài thi thử (U11), tính điểm vào điểm chính thức (U15).

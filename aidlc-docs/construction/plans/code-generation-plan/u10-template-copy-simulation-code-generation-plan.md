# U10 Template & Copy - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U10. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-ASM-008 (diff version), US-ASM-009, US-ASM-010; nhận bản nháp AI của US-AIG-002. **Use case**: UC 21 (chủ trì), UC 28. Simulation Exam đã rút; tên thư mục cũ giữ để ổn định liên kết.
- **Thiết kế nguồn**: `construction/u10-template-copy-simulation/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ClassAccessPort`, `SubjectScopePort` | U04 | Dùng thật |
| `BankCopyPort`, `InlineQuestionPort` | U06 | Dùng thật (sao câu cấp lớp và câu riêng) |
| `AssignmentQueryPort`, `AssignmentService`, `AssignmentExtensionPort`; các phần của `AssignmentEditorPage` | U08 | Dùng thật; `createDraftFrom`, ghi `subject_id`, trạng thái template, lineage qua `AssignmentExtensionPort`; Template Editor dùng lại trình soạn của U08 (không có lịch) |
| Bước 2 của `CreateAssignmentDialog` | U08 | U10 gắn `CopyFromTemplateDialog`, `CopyFromClassDialog` vào (U08 lượt trước chỉ có "Bài trống") |
| `TypeConfigPort.copy` | U09 | Dùng thật |
| `AiDraftPort` | U13 (`C`, code ngay sau U10 trong wave 4) | Chưa có U13: ẩn `AiDraftDialog` trong Template Editor; U13 thay bằng bản thật |

### Dữ liệu U10 sở hữu

PostgreSQL: dòng `assignments` của môn (template) và cột `source_assignment_id`, `config.origin` (U08 tạo bảng, U10 ghi qua `AssignmentExtensionPort`). Không có bảng riêng.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  templates/
    api/                TemplateController, CopyController, DiffController, DTO
    application/        TemplateService, AssignmentCopier, ScopeGuard, AssignmentDiffer
    domain/             Template (trạng thái trên `assignments`), AssignmentLineage, AssignmentDiff
    infrastructure/     JPA repository
    (không có port riêng; dùng port của U04, U06, U08, U09, U13)
/frontend/src/app/teaching/subjects/[id]/templates/
/frontend/src/app/teaching/assignments/[id]/   (VersionHistoryPanel, AssignmentDiffView)
/contracts/openapi/u10-template-copy.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `io.github.java-diff-utils:java-diff-utils`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `Template` (`DRAFT`, `REVIEWED`, `RELEASED`, `WITHDRAWN` trên `assignments`), `AssignmentLineage`, `AssignmentDiff`.
- [ ] **Bước 3** - `TemplateService`: kiểm Chủ nhiệm môn hiện tại của môn; tạo template, nhận câu AI giữ lại (câu riêng), duyệt (qua U08), phát hành, version mới, xoá template (đã phát hành → `WITHDRAWN`) (F1, BR-U10-01…04, 06…08).
- [ ] **Bước 4** - `ScopeGuard` và `AssignmentCopier` (template → lớp, lớp → lớp; câu cấp lớp qua `BankCopyPort`, câu riêng qua `InlineQuestionPort`; `TypeConfigPort.copy` (nhân bản rubric từng câu/phần); lineage; một transaction) (F2, F3, P1, BR-U10-05, 10…14).
- [ ] **Bước 5** - `AssignmentDiffer` (F4, P2, BR-U10-20…22).
- [ ] **Bước 6** - Kiểm invariants khi copy: bản nháp mới giữ `gradingMode` hợp lệ, không copy lịch, attempt, submission hoặc grade; test cho copy template và copy giữa lớp.
- [ ] **Bước 7** - Audit theo BR-U10-40.
- [ ] **Bước 8** - Unit test mọi `BR-U10-xx`.
- [ ] **Bước 9** - Tóm tắt: `aidlc-docs/construction/u10-template-copy-simulation/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 10** - Không có migration (template và lineage dùng `assignments` của U08).
- [ ] **Bước 11** - JPA repository.
- [ ] **Bước 12** - Integration test: copy lỗi giữa chừng không để lại dữ liệu; `source_assignment_id` không đổi sau khi tạo.
- [ ] **Bước 13** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 14** - `/contracts/openapi/u10-template-copy.yaml`.
- [ ] **Bước 15** - Controller + DTO + validation.
- [ ] **Bước 16** - Test MockMvc: Teacher không dạy lớp đích bị `404`; Teacher thường không tạo/phát hành/xoá template; Chủ nhiệm môn không sửa/xoá template của môn khác, Chủ nhiệm môn hiện tại sửa/xoá được template do Chủ nhiệm môn trước tạo; Student không copy bài.
- [ ] **Bước 17** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 18** - `TemplateListPage` (tạo, xoá), `TemplateEditorPage` (dùng lại trình soạn U08, `TypeConfigSlot` U09, `AiDraftDialog` ẩn tới khi có U13, phát hành, version mới), `CopyFromTemplateDialog`, `CopyFromClassDialog` gắn vào `CreateAssignmentDialog` của U08.
- [ ] **Bước 19** - `VersionHistoryPanel`, `AssignmentDiffView`.
- [ ] **Bước 20** - Hiển thị nguồn template/version và lineage trên `VersionHistoryPanel`.
- [ ] **Bước 21** - Test frontend: diff đánh dấu đúng loại thay đổi và bản copy không mang lịch/lượt/điểm.
- [ ] **Bước 22** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 23** - Cập nhật `README.md`: template, copy, diff và ranh giới U08/U10.
- [ ] **Bước 24** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-008 (UC 28) | 5, 19 |
| US-ASM-009 (UC 21, UC 28) | 3, 4, 16, 18 |
| US-AIG-002 (UC 21, nhận bản nháp AI) | 3, 18 |
| US-ASM-010 (UC 28) | 4, 18 |

## 5. Ngoài phạm vi

- Tạo version mới sau khi ngừng giao (U08), nộp bài (U11) và tính điểm chính thức (U15).

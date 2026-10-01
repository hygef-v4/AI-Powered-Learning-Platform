# U10 Template & Copy - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U10. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-ASM-008 (diff version), US-ASM-009, US-ASM-010. **Use case**: UC 28. Simulation Exam đã rút; tên thư mục cũ giữ để ổn định liên kết.
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
| `AssignmentQueryPort`, `AssignmentService`, `AssignmentExtensionPort`, `PublishDialog` | U08 | Dùng thật; U08 cần có `ownerType`, version, `createDraftFrom`; ghi lineage qua `AssignmentExtensionPort` |
| `TypeConfigPort.copy` | U09 | Dùng thật |
| `AiDraftPort` | U13 (`C`) | Chưa có U13: ẩn nút "Nhờ AI tạo câu hỏi" trong trình soạn template; U13 thay bằng bản thật |

### Dữ liệu U10 sở hữu

PostgreSQL `template_releases` và cột lineage của `assignments` (U08 tạo bảng, U10 thêm cột và ghi qua `AssignmentExtensionPort`).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  templates/
    api/                TemplateController, CopyController, DiffController, DTO
    application/        TemplateService, AssignmentCopier, ScopeGuard, AssignmentDiffer
    domain/             TemplateRelease, AssignmentLineage, AssignmentDiff
    infrastructure/     JPA repository
    port/               BankCopyPort
/backend/src/main/resources/db/migration/u10/
/frontend/src/app/teaching/subjects/[id]/templates/
/frontend/src/app/teaching/assignments/[id]/   (VersionHistoryPanel, AssignmentDiffView)
/contracts/openapi/u10-template-copy.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - `pom.xml`: `io.github.java-diff-utils:java-diff-utils`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `TemplateRelease`, `AssignmentLineage`, `AssignmentDiff`; port `BankCopyPort`.
- [ ] **Bước 3** - `TemplateService`: phát hành/rút template (F1, BR-U10-01…04).
- [ ] **Bước 4** - `ScopeGuard` và `AssignmentCopier` (template → lớp, lớp → lớp; sao chép câu/rubric cấp lớp; `TypeConfigPort.copy`; lineage; một transaction) (F2, F3, P1, BR-U10-05, 10…14).
- [ ] **Bước 5** - `AssignmentDiffer` (F4, P2, BR-U10-20…22).
- [ ] **Bước 6** - Kiểm invariants khi copy: bản nháp mới giữ `gradingMode` hợp lệ, không copy lịch, attempt, submission hoặc grade; test cho copy template và copy giữa lớp.
- [ ] **Bước 7** - Audit theo BR-U10-40.
- [ ] **Bước 8** - Unit test mọi `BR-U10-xx`.
- [ ] **Bước 9** - Tóm tắt: `aidlc-docs/construction/u10-template-copy-simulation/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 10** - Flyway `V20260925_1700__u10_template_copy.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 11** - JPA repository.
- [ ] **Bước 12** - Integration test: copy lỗi giữa chừng không để lại dữ liệu; `app` không sửa/xóa lineage.
- [ ] **Bước 13** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 14** - `/contracts/openapi/u10-template-copy.yaml`.
- [ ] **Bước 15** - Controller + DTO + validation.
- [ ] **Bước 16** - Test MockMvc: Teacher không dạy lớp đích bị `404`; Teacher thường không phát hành template; Student không copy bài.
- [ ] **Bước 17** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 18** - `TemplateListPage`, `CopyFromTemplateDialog`, `CopyToClassDialog`.
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
| US-ASM-009 (UC 28) | 3, 4, 18 |
| US-ASM-010 (UC 28) | 4, 18 |

## 5. Ngoài phạm vi

- Tạo version mới sau khi ngừng giao (U08), nộp bài (U11) và tính điểm chính thức (U15).

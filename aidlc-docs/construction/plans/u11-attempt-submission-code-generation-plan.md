# U11 Attempt & Submission - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U11. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-ASM-003; phần làm bài của US-ASM-004, US-ASM-011. **Use case**: UC-ASM-09..14 (UC-ASM-13 phần chạy code ở U13).
- **Thiết kế nguồn**: `construction/u11-attempt-submission/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `JobPort`, `AuditPort`, `EventPublisherPort` | U02 | Dùng thật |
| `ArtifactPort` | U03 | Dùng thật (ảnh `DOCUMENT_IMAGE`) |
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort`, `QuestionView` | U06 | Dùng thật |
| `AssignmentQueryPort`, `isSubmissionOpen` | U08 | Dùng thật |
| `TypeConfigPort`, `DocumentModelPort`, `DocxExportPort`, `DocxLearnerImportPort`, `DocumentEditor`, `EssayEditor` | U09 | Dùng thật |
| `SimulationPolicyPort`, `SimulationBadge` | U10 | Dùng thật |
| `CodeRunPort` | U13 (`C`) | Adapter tạm báo "chạy thử chưa sẵn sàng"; U13 thay |
| Điểm hiển thị | U15 | Ẩn phần điểm tới khi U15 có |

### Dữ liệu U11 sở hữu

PostgreSQL `submissions` (gồm nội dung bài làm); Redis `u11:save:*`; queue `jobs.u11.auto-submit`, `u11.retired-listener`; routing key `u11.submission.submitted`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u11/
    api/                AttemptController, MyAssignmentsController, DTO, GzipRequestFilter
    application/        AttemptStarter, DraftSaver, AttemptSubmitter, AttemptQueryService
    domain/             Attempt, AttemptStatus, SubmitMode, AttemptContent, DeadlineCalculator
    infrastructure/     JPA repository, UnavailableCodeRunAdapter
    worker/             AutoSubmitHandler, RetiredListener
    port/               SubmissionQueryPort, CodeRunPort
/backend/src/main/resources/db/migration/u11/
/frontend/src/app/learn/...                (MyAssignmentsPage, AssignmentOverviewPage,
                                            AttemptWorkspacePage, SubmittedAttemptView)
/frontend/src/features/attempt/useAutosave.ts
/contracts/openapi/u11-attempt.yaml
/contracts/messages/u11-submission-submitted.json
/tests/load/u11-autosave.js               (k6)
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - Biến cấu hình U11 theo `logical-components.md` §3; Nginx route lưu 12 MB.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `Attempt`, `AttemptContent` (4 loại), `DeadlineCalculator` (BR-U11-06); port `SubmissionQueryPort`, `CodeRunPort` + adapter tạm.
- [ ] **Bước 3** - `AttemptStarter`: advisory lock, đếm lượt (bài thường/thi thử), snapshot, seed, khóa chính sách thi thử, job tự nộp (F2, P1, BR-U11-01…06).
- [ ] **Bước 4** - Trả đề cho người học: góc nhìn đã lọc đáp án, trộn theo seed (F2 bước 4).
- [ ] **Bước 5** - `DraftSaver`: UPDATE có điều kiện, `409`/`410`, ân hạn 30 s, kiểm tài liệu, `GzipRequestFilter` giới hạn 10 MB, rate limit; preview DOCX chỉ cho lượt DOCUMENT đang làm, xác nhận thêm block qua cùng luồng lưu có `contentVersion` (F3, F3a, P2, P6, BR-U11-10…14, BR-U09-45…48).
- [ ] **Bước 6** - `AttemptSubmitter` một đường, idempotent, biên nhận, event sau commit; nộp tay kiểm `validateForSubmit`, tự nộp ghi `warnings` (F4, F5, P3, BR-U11-20…24).
- [ ] **Bước 7** - `AutoSubmitHandler`, `RetiredListener` (P5).
- [ ] **Bước 8** - `AttemptQueryService`: danh sách bài của người học, lịch sử, lượt được chấm (lượt nộp cuối / chính sách U10), xuất DOCX, `SubmissionQueryPort` (F1, F6, F7, BR-U11-30…34).
- [ ] **Bước 9** - Audit theo BR-U11-40.
- [ ] **Bước 10** - Unit test mọi `BR-U11-xx`, gồm các mốc giờ quanh `deadlineAt`.
- [ ] **Bước 11** - Tóm tắt: `aidlc-docs/construction/u11-attempt-submission/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 12** - Flyway `V20260925_1800__u11_attempts.sql` theo `infrastructure-design.md` §3, gồm trigger bất biến.
- [ ] **Bước 13** - JPA repository.
- [ ] **Bước 14** - Integration test: hai lần bắt đầu đồng thời; nộp tay và tự nộp đồng thời; ngừng giao tự nộp hàng loạt; trigger chặn sửa sau nộp.
- [ ] **Bước 15** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 16** - `/contracts/openapi/u11-attempt.yaml` và schema event.
- [ ] **Bước 17** - Controller + DTO + validation, gồm `POST /api/v1/attempts/{id}/docx:preview` chỉ cho chủ lượt DOCUMENT đang làm; xác nhận dùng `PUT /api/v1/attempts/{id}/content`.
- [ ] **Bước 18** - Test MockMvc: lượt người khác `404`; hết lượt/quá hạn bị từ chối; đề không chứa đáp án.
- [ ] **Bước 19** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 20** - `MyAssignmentsPage`, `AssignmentOverviewPage` (`AssignmentInfo`, `StartAttemptButton`, `AttemptHistoryList`).
- [ ] **Bước 21** - `AttemptWorkspacePage` với `QuizWorkspace`, `EssayWorkspace`, `DocumentWorkspace` (gồm `LearnerDocxImportDialog`), `CodeWorkspace`, `AttemptHeader` (đồng hồ).
- [ ] **Bước 22** - `useAutosave` (P7), `SubmitConfirmDialog`, `ReceiptView`, `SubmittedAttemptView`.
- [ ] **Bước 23** - Test frontend: tự lưu không gửi chồng, `409` dừng tự lưu, hết giờ tự chuyển biên nhận.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Tải thử k6 100 người tự lưu 10 phút; ghi kết quả.
- [ ] **Bước 26** - Cập nhật `README.md`: luồng làm bài, tự nộp, cách U15 nghe `u11.submission.submitted`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-003 S1 (UC-ASM-10..13) | 3, 4, 6, 21 |
| US-ASM-003 S2 | 3, 6, 18 |
| US-ASM-003 S3 | 8, 18 |
| US-ASM-003 S4 | 5, 22 |
| US-ASM-003 S5 (UC-ASM-14) | 8, 20 |
| UC-ASM-09 | 8, 20 |
| US-ASM-011 (làm thi thử) | 3, 8 |

## 5. Ngoài phạm vi

- Chạy code (U13), bài nhóm (U14), chấm và điểm (U15), thông báo (U16).

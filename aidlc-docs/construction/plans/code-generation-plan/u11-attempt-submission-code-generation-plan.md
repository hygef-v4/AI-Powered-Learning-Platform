# U11 Attempt & Submission - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U11. Mỗi bước xong thì đánh `[x]` ngay.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-ASM-003, US-ASM-012; phần làm bài của US-ASM-004. **Use case**: UC 29, UC 30, UC 31, UC 40 (UC 30 phần chạy code ở U13).
- **Thiết kế nguồn**: `construction/u11-attempt-submission/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ScheduledScanner` | U03 | Dùng thật (tự nộp theo hạn, `AttemptDeadlineScanner`) |
| `ArtifactPort` | U03 | Dùng thật (ảnh `DOCUMENT_IMAGE`) |
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort`, `RubricPort`, `QuestionView` | U06 | Dùng thật (đề và rubric cho người học) |
| `AssignmentQueryPort`, `isSubmissionOpen` | U08 | Dùng thật |
| `TypeConfigPort`, `DocumentModelPort`, `DocxExportPort`, `DocxStudentImportPort`, `DocumentEditor`, `EssayEditor` | U09 | Dùng thật |
| `CodeRunPort` | U13 (`C`) | Adapter tạm báo "chạy thử chưa sẵn sàng"; Practice Code Lab chưa có kết quả; U13 thay |
| `PracticeGradingPort` | U13 (`C`) | Adapter tạm báo "AI chưa sẵn sàng" (nút "Chấm với AI" hiện thông báo); U13 thay |
| `SubmissionSubmittedPort` | U15 (`C`, U11 khai báo) | Adapter rỗng; U15 (ngay sau U14 trong wave 3) cài |
| `PracticeResultPort` | U15 (`C`) | Adapter rỗng: Practice Quiz hiện "chưa có kết quả" tới khi U15 cài |
| `GradeQueryPort` | U15 (`C`) | Ẩn phần điểm tới khi U15 có |
| U11 cài `AssignmentLifecyclePort` (U08 khai báo) | cho U08 | Thay adapter rỗng của U08: `onRetired` đặt hạn ngay cho lượt dở |
| U11 cung cấp `SubmissionQueryPort`, `AttemptRunResultPort` | cho U13, U15, U16 | U13 ghi `run_result`; U15, U16 đọc bài nộp |

### Dữ liệu U11 sở hữu

PostgreSQL `attempts` (gồm nội dung bài làm); Redis `ratelimit:attempt-save:*`; scanner tự nộp trong worker; không phát và không nghe event. Kết quả Practice ghi vào `evaluations` qua U15.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  attempts/
    api/                AttemptController, MyAssignmentsController, DTO, GzipRequestFilter
    application/        AttemptStarter, DraftSaver, AttemptSubmitter, AttemptQueryService
    domain/             Attempt, AttemptStatus, SubmitMode, AttemptContent, DeadlineCalculator
    infrastructure/     JPA repository, UnavailableCodeRunAdapter, UnavailablePracticeGradingAdapter,
                        NoopSubmissionSubmittedAdapter, NoopPracticeResultAdapter
    worker/             AttemptDeadlineScanner
    adapter/            AssignmentLifecycleAdapter (cài port của U08)
    port/               SubmissionQueryPort, AttemptRunResultPort, SubmissionSubmittedPort,
                        CodeRunPort, PracticeGradingPort, PracticeResultPort, GradeQueryPort
/backend/src/main/resources/db/migration/attempts/
/frontend/src/app/learning/...                (MyAssignmentsPage, AssignmentOverviewPage,
                                            AttemptWorkspacePage, SubmittedAttemptView)
/frontend/src/features/attempt/useAutosave.ts
/contracts/openapi/attempts.yaml
/tests/load/attempt-autosave.js               (k6)
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - Biến cấu hình U11 theo `logical-components.md` §3; Nginx route lưu 12 MB.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `Attempt`, `AttemptContent` (4 loại cá nhân), trạng thái chấm AI đọc từ U13, kết quả Practice qua `PracticeResultPort` (U15), `DeadlineCalculator` (BR-U11-06); port `SubmissionQueryPort`, `AttemptRunResultPort`; port khai báo `SubmissionSubmittedPort`, `CodeRunPort`, `PracticeGradingPort`, `PracticeResultPort`, `GradeQueryPort` + adapter tạm.
- [ ] **Bước 3** - `AttemptStarter`: advisory lock, đếm lượt theo `maxAttempts` U08, snapshot dạng/chế độ bài và seed, tính `deadline_at` (F2, P1, BR-U11-01…06).
- [ ] **Bước 4** - Trả đề cho người học: góc nhìn đã lọc đáp án, trộn theo seed (F2 bước 4).
- [ ] **Bước 5** - `DraftSaver`: UPDATE có điều kiện, `409`/`410`, ân hạn 30 s, kiểm tài liệu, `GzipRequestFilter` giới hạn 10 MB, rate limit; preview DOCX chỉ cho lượt DOCUMENT đang làm, xác nhận thêm block qua cùng luồng lưu có `contentVersion` (F3, F3a, P2, P6, BR-U11-10…14, BR-U09-45…48).
- [ ] **Bước 6** - `AttemptSubmitter` một đường, idempotent, biên nhận; chỉ `GRADED` gọi `SubmissionSubmittedPort` trong transaction. `PRACTICE` Quiz gọi `PracticeResultPort.scoreQuiz`, Code Lab gọi `CodeRunPort.grade`; Text/Diagram Essay không gọi AI khi nộp. Endpoint "Chấm với AI" (`POST /attempts/{id}/ai-grading`) kiểm chủ lượt, `SUBMITTED`, `PRACTICE` Text/Diagram Essay rồi gọi `PracticeGradingPort`; `GET /attempts/{id}/practice-result` đọc kết quả (BR-U11-35). Nộp tay kiểm `validateForSubmit`, tự nộp ghi `warnings` (F4, F5, P3, BR-U11-20…24).
- [ ] **Bước 7** - `AttemptDeadlineScanner` (scanner U03: theo hạn và khi ngưng giao) và `AssignmentLifecycleAdapter.onRetired` (đặt hạn ngay, `AUTO_RETIRED`); khai báo `SubmissionSubmittedPort` với adapter rỗng tới khi U15 có (P3, P5).
- [ ] **Bước 8** - `AttemptQueryService`: danh sách bài của người học, lịch sử, lượt được chấm (lượt nộp cuối), xuất DOCX, `SubmissionQueryPort` (F1, F6, F7, BR-U11-30…34).
- [ ] **Bước 9** - Audit theo BR-U11-40.
- [ ] **Bước 10** - Unit test mọi `BR-U11-xx`, gồm các mốc giờ quanh `deadlineAt`.
- [ ] **Bước 11** - Tóm tắt: `aidlc-docs/construction/u11-attempt-submission/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 12** - Flyway `V20260925_1800__u11_attempts.sql` theo `infrastructure-design.md` §3, bảng `attempts` theo [database](../../../../docs/database.md), cột `submit_mode` và trigger bất biến nội dung sau nộp.
- [ ] **Bước 13** - JPA repository.
- [ ] **Bước 14** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: hai lần bắt đầu đồng thời; nộp tay và tự nộp đồng thời; ngừng giao tự nộp hàng loạt; nộp Practice không tự chấm; bấm "Chấm với AI" gọi `PracticeGradingPort` (adapter giả trả đủ/thiếu credit, quá 5 phút) và hiện đúng thông báo, lượt không phải Practice Text/Diagram Essay bị từ chối (luồng credit thật kiểm ở U13); bài `RETIRED` có điểm vẫn hiện; lượt gần nhất và chuyển lượt ‹ ›; trigger chặn sửa sau nộp.
- [ ] **Bước 15** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 16** - `/contracts/openapi/attempts.yaml` (U11 không phát event).
- [ ] **Bước 17** - Controller + DTO + validation, gồm `POST /api/v1/attempts/{id}/docx:preview` chỉ cho chủ lượt DOCUMENT đang làm; xác nhận dùng `PUT /api/v1/attempts/{id}/content`.
- [ ] **Bước 18** - Test MockMvc: lượt người khác `404`; hết lượt/quá hạn bị từ chối; đề không chứa đáp án; "Chấm với AI" trên lượt của người khác hoặc bài `GRADED` bị từ chối.
- [ ] **Bước 19** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 20** - `MyAssignmentsPage`, `AssignmentOverviewPage` (`AssignmentInfo`, `StartAttemptButton`, `AttemptHistoryList`).
- [ ] **Bước 21** - `AttemptWorkspacePage` với `QuizWorkspace`, `EssayWorkspace`, `DocumentWorkspace` (gồm `StudentDocxImportDialog`), `CodeWorkspace`, `AttemptHeader` (đồng hồ).
- [ ] **Bước 22** - `useAutosave` (P7), `SubmitConfirmDialog`, `ReceiptView`, `SubmittedAttemptView` (lượt gần nhất, nút ‹ › chuyển lượt, BR-U11-36), `GradeWithAiDialog` (popup Grade with AI, UC 40: nút "Chấm với AI", lỗi sau 5 phút; chế độ `STUDENT_PRACTICE` và `TEACHER_PROPOSAL` để U15 nhúng).
- [ ] **Bước 23** - Test frontend: tự lưu không gửi chồng, `409` dừng tự lưu, hết giờ tự chuyển biên nhận.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Tải thử k6 100 người tự lưu 10 phút; ghi kết quả.
- [ ] **Bước 26** - Cập nhật `README.md`: luồng làm bài, tự nộp, cách U15 cài `SubmissionSubmittedPort`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-003 S1 (UC 30) | 3, 4, 6, 21 |
| US-ASM-003 S2 | 3, 6, 18 |
| US-ASM-003 S3 | 8, 18 |
| US-ASM-003 S4 | 5, 22 |
| US-ASM-003 S5 (UC 31) | 8, 20 |
| UC 29 | 8, 20 |
| US-ASM-012 (Practice và AI theo credit) | 2, 6, 8, 14, 17, 22; phối hợp U07/U13 |

## 5. Ngoài phạm vi

- Chạy code (U13), bài nhóm (U14), chấm và điểm (U15), thông báo (U16).

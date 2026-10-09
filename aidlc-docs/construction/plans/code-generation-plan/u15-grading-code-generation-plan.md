# U15 Grading - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U15. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story trong phạm vi**: US-GRD-001..005, US-GRP-006; phần chấm của US-GRP-004.
- **Primary UC hiện hành**: UC 37 View Student Submissions, UC 38 Grade Submission With AI, UC 39 Grade Submissions Manually, UC 40 View/Export GradeBook (U16 tạo tệp xuất). Chốt và công bố là luồng phụ của UC 38, 39. Hỗ trợ: chấm quiz luyện tập (UC 20, 21), điểm của Student trên Student Assignments, Assignment Detail, Submission History (UC 22, 23, 28), kết quả AI chấm Practice (UC 29, U13 ghi qua U15). Supporting flows theo current-srs-contract.md.
- **Màn theo screen flow**: Teacher Class Detail (tab Evals) → Submission Detail → Grading Workspace; sổ điểm là phần "Sổ điểm" trên tab Evals (screen flow không có màn GradeBook riêng).
- **Quyết định 2026-10-09**: quyền chấm chỉ R3/R4 (Teacher hoặc Subject Manager là giảng viên chính của lớp), Admin không vào lớp, không chấm; quiz chỉ là luyện tập (tự chấm, không vào sổ điểm, không vào hàng chấm); bài của môn được giảng viên từng lớp chấm, chốt, công bố cho lớp mình và hiện trong sổ điểm của từng lớp, nên thời điểm công bố của (bài, lớp) suy ra từ các đánh giá đã công bố của lớp đó (không thêm bảng, chỉ bảng của ERD; bỏ `assignments.grades_released_at`); AI đề xuất chấm của giảng viên là panel U15 tự làm; rubric thuộc từng bài, khóa khi phát hành (U06); U10 đã xóa.
- **Thiết kế nguồn**: `construction/u15-grading/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `EventPublisherPort` | U03 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật (giảng viên chính của lớp, sinh viên đang ghi danh) |
| `BankQueryPort`, `RubricPort`, `QuestionView`, `RubricView` | U06 | Dùng thật |
| `AssignmentQueryPort` | U08 | Dùng thật (`listForClass` gồm bài của môn; dạng, chế độ, phạm vi) |
| `DocumentModelPort`, `DocxExportPort`, `DocumentEditor` | U09 | Dùng thật |
| `SubmissionQueryPort` | U11 | Dùng thật (cần lớp của lượt); U15 cài `SubmissionSubmittedPort`, `PracticeResultPort.scoreQuiz` của U11 |
| `AiGradingPort`, `CodeRunPort` | U13 (`C`) | U13 ở wave 4, code sau U15: adapter tạm báo "AI chưa sẵn sàng" (ẩn nút Nhờ AI, chấm hàng loạt) và Code Lab "chưa chấm được"; U13 thay bằng bản thật. U15 cài `CodeGradedPort`, `PracticeResultPort.record` cho U13 |
| `GroupSubmissionQueryPort`, `GroupDocsOverviewPanel` | U14 | Dùng thật (cần lớp của nhóm); U15 cài `GroupSubmittedPort` của U14 |
| `EvalsTab` | U08 | U15 gắn lối vào Submission Detail và `GradebookPanel` |
| U15 cài `SubmissionSubmittedPort`, `PracticeResultPort`, cung cấp `GradeQueryPort` cho U11; `GroupSubmittedPort` cho U14 | cho U11, U14 | Thay adapter rỗng của U11, U14 (code trước U15 trong wave 3): bài `GRADED` vào Submission Detail, quiz luyện tập có kết quả, điểm hiện cho Student |
| U15 cung cấp `GradeQueryPort`, `GradebookQueryPort`, event `grade.published`, chỗ gắn `GradebookExportAction` | cho U16 | U16 đọc điểm cuối/trạng thái theo lớp cho phân bố điểm và tệp xuất |

### Dữ liệu U15 sở hữu

PostgreSQL `evaluations` (lịch sử trong cột `history`; có `assignment_id`, `class_id`; thời điểm công bố theo bài và lớp suy ra từ `published_at`); không có bảng công bố riêng, không ghi `assignments.grades_released_at`; không có job riêng; event `grade.published` (chỉ cho thông báo U16).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  grading/
    api/                GradingController, GradebookController, StudentGradeController, DTO
    application/        GradeWriter, GradingScopeGuard, GradingService, BulkGradeService,
                        PublishService, GradebookService, QuizScorer
    domain/             Evaluation, EvaluationKind, EvaluationStatus, EvaluationMethod,
                        EvaluationHistory, GradeReleaseView, StudentGradeView, TeacherGradeView
    infrastructure/     JPA repository (evaluations)
    adapter/            SubmissionSubmittedAdapter, GroupSubmittedAdapter, CodeGradedAdapter,
                        PracticeResultAdapter
    port/               GradeQueryPort, GradebookQueryPort
    event/              GradePublished
/backend/src/main/resources/db/migration/grading/
/frontend/src/app/classes/[id]/teaching/assignments/[assignmentId]/submissions/   SubmissionDetailPage
/frontend/src/app/classes/[id]/teaching/grading/[targetKind]/[evaluationId]/      GradingWorkspacePage
/frontend/src/shared/grades/        GradebookPanel (gắn vào EvalsTab của U08), AssignmentGradeCell (U11 gắn)
/contracts/openapi/grading.yaml
/contracts/messages/grade-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.

### Nhóm B - Domain và logic

- [ ] **Bước 1** - Domain `Evaluation` (`kind` `ATTEMPT`/`GROUP_DOCUMENT`/`MEMBER`/`PRACTICE`; `assignment_id`, `class_id`) và chuyển trạng thái, `EvaluationHistory`, `GradeReleaseView` (MIN/MAX `published_at` theo bài, lớp), hai góc nhìn; port `GradeQueryPort` và `GradebookQueryPort` theo lớp (điểm cuối/trạng thái, không lộ đề xuất AI).
- [ ] **Bước 2** - `GradingScopeGuard` (R3/R4 theo `class_id` của đánh giá; Admin, Chủ nhiệm môn không dạy lớp → 404 + audit) và `GradeWriter` một đường (version, luật lý do, khoảng điểm, lịch sử, event sau commit) (P1, BR-U15-01, 03, 13, 22, 33).
- [ ] **Bước 3** - Adapter cài `SubmissionSubmittedPort`, `GroupSubmittedPort` (tạo `evaluations` `PENDING` kèm bài và lớp trong transaction nộp, chỉ bài `GRADED`), `CodeGradedPort`, `PracticeResultPort` (`scoreQuiz` cho quiz luyện tập của U11, `record` cho U13; ghi `kind = PRACTICE`); `QuizScorer` chỉ cho quiz luyện tập (F1, F9, P2, P3, BR-U15-10…12, 35, FR-030).
- [ ] **Bước 4** - `GradingService`: danh sách bài nộp theo (bài, lớp) cho Submission Detail, chi tiết đánh giá kèm nội dung bài nộp, tải DOCX; chấm tay theo checklist rubric (UC 39), nhờ AI (UC 38) qua `AiGradingPort.request` (endpoint `ai-proposal`), dùng đề xuất (lưu như chấm tay, không cần lý do khi khác đề xuất), chấm hàng loạt qua `AiGradingPort.requestBatch`, thứ tự chuyển bài cho workspace (F2, F3, BR-U15-20…25).
- [ ] **Bước 5** - `BulkGradeService` chốt hàng loạt, `PublishService` công bố từng bài và công bố hàng loạt các bài đã chốt được chọn; mỗi mục một transaction con; một `grade.published` cho mỗi (bài, lớp); không ghi bảng công bố riêng (F4, F5, P4, BR-U15-31, 32).
- [ ] **Bước 6** - Chấm bài nhóm: tài liệu chung như bài `DOCUMENT` (tay hoặc AI), điểm đóng góp từng thành viên mặc định bằng điểm tài liệu chung, chấm tay từng người, lý do tùy chọn; nộp lại đưa đánh giá về `PENDING` (F7, BR-U15-40…44).
- [ ] **Bước 7** - `GradebookService`: bài `GRADED` của lớp và của môn chứa lớp, theo sinh viên đang ghi danh (không điểm tổng, không quiz/Practice), điểm của Student cho Student Assignments/Assignment Detail/Submission History, lịch sử (F8, P5, P6, BR-U15-50…52); lượt chính thức theo U11 (BR-U15-35).
- [ ] **Bước 8** - Audit theo BR-U15-53.
- [ ] **Bước 9** - Unit test mọi `BR-U15-xx`: quiz luyện tập không vào Submission Detail/sổ điểm; bài của môn chỉ hiện bài nộp của đúng lớp và công bố riêng từng lớp; Admin và Chủ nhiệm môn không dạy lớp bị từ chối.
- [ ] **Bước 10** - Tóm tắt: `aidlc-docs/construction/u15-grading/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 11** - Flyway `db/migration/grading/V20260925_1950__create_evaluations.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 12** - JPA repository, query Submission Detail và sổ điểm theo `(assignment_id, class_id)`.
- [ ] **Bước 13** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: nộp Code Lab `GRADED` → điểm test `DRAFT` → chốt → công bố; quiz luyện tập và `PRACTICE` không vào sổ điểm hoặc Submission Detail; gọi port nộp lặp không tạo điểm trùng; chốt hàng loạt có mục lệch version; lịch sử chỉ thêm; công bố bài của môn ở một lớp không làm lớp kia thành đã công bố; sổ điểm 200 × 30 ≤ 1 s.
- [ ] **Bước 14** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 15** - `/contracts/openapi/grading.yaml` và schema event; cần sửa theo mục 7.
- [ ] **Bước 16** - Controller + DTO + validation.
- [ ] **Bước 17** - Test MockMvc: Student không thấy điểm chưa công bố/đề xuất AI; giảng viên lớp khác, Admin, Chủ nhiệm môn không dạy lớp → `404` + audit; sửa không lý do bị từ chối; danh sách bài nộp bài của môn chỉ gồm lớp trong path.
- [ ] **Bước 18** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 19** - `SubmissionDetailPage` (màn Submission Detail: `SubmissionHeader`, `SubmissionFilters`, `SubmissionTable`, `SubmissionPreview`, `BulkAiGradingDialog`, `FinalizeConfirmDialog`, `BulkPublishButton`; bài nhóm nhúng `GroupDocsOverviewPanel` của U14); lối vào "Bài nộp" từ `EvalsTab` của U08.
- [ ] **Bước 20** - `GradingWorkspacePage` (`SubmissionNavigator` ‹ › và tự sang bài kế, `SubmissionViewer` có "Tải DOCX", `MethodChooser`, `RubricChecklistForm`, `AiProposalPanel` (panel AI đề xuất của U15, không dùng popup U11), `FeedbackEditor`, `OverrideReasonDialog`, `GradeHistoryDrawer`, `FinalizeButton`, `PublishButton`).
- [ ] **Bước 21** - `GroupGradingMode` trong `GradingWorkspacePage` khi `targetKind = groups` (tài liệu chung như bài `DOCUMENT`, `MemberScoresPanel` điểm mặc định bằng nhau).
- [ ] **Bước 22** - `GradebookPanel` gắn vào `EvalsTab` (gom theo sinh viên, mỗi sinh viên đóng/mở, chỗ cho `GradebookExportAction` của U16); `AssignmentGradeCell` cho Student Assignments, Assignment Detail, Submission History của U11 (không có trang My Grades riêng).
- [ ] **Bước 23** - Test frontend: điểm hiển thị "x / tổng", sửa điểm đã chốt bắt lý do còn khác đề xuất AI thì không, ‹ › và tự sang bài kế, sổ điểm đóng/mở theo sinh viên không có điểm tổng.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: luồng xem bài nộp, chấm, chốt, công bố theo lớp; chấm bài nhóm; bài của môn chấm ở từng lớp.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| UC 37 (View Student Submissions) | 4, 12, 19 |
| UC 38 (Grade Submission With AI), US-GRD-002 | 4, 19, 20 |
| UC 39 (Grade Submissions Manually), US-GRD-003 | 2, 4, 20 |
| US-GRD-005 (chốt, công bố; luồng phụ UC 38, 39) | 2, 5, 19, 20 |
| UC 40 (View/Export GradeBook), US-GRD-004 S2, S3 | 7, 12, 22 |
| US-GRD-004 S1 (UC 22, 23, 28) | 7, 22 |
| US-GRD-001 (quiz luyện tập UC 20, 21; Code Lab tự chấm) | 3, 13 |
| US-GRP-004 S3, US-GRP-006 (UC 38, 39) | 6, 21 |
| UC 29 (kết quả AI chấm Practice, U13 ghi) | 3 |

## 5. Ngoài phạm vi

- Gia hạn/phúc khảo/kiểm tra tương đồng ngoài phạm vi dự án; U16 tạo tệp xuất bảng điểm của UC 40 và thông báo. Hiển thị kết quả quiz luyện tập và Practice thuộc U11.

## 6. Revision implementation scope - 2026-10-08
- [ ] R3/R4 cho bài nộp/chấm/sổ điểm (cập nhật 2026-10-09: Admin không được phép kể cả khi có dữ liệu phân công cũ; Chủ nhiệm môn chỉ phụ trách môn bị từ chối).
- [ ] Submission Detail UC 37, Grading Workspace UC 38–39 với AI Grading Proposals, sổ điểm trên tab Evals UC 40; Practice riêng.

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] Contract `grading.yaml`: bỏ `ADMIN` khỏi mọi `x-roles`; đổi `GET /api/v1/assignments/{assignmentId}/evaluations` thành `GET /api/v1/classes/{classId}/assignments/{assignmentId}/evaluations` (operationId giữ `listEvaluations`); thêm `GET /api/v1/evaluations/{evaluationId}/document.docx`; schema `Evaluation`, `EvaluationSummary` thêm `assignmentId`, `classId`; `EvaluationSummary.kind` thêm `MEMBER`; mô tả UC 34–37 đổi thành UC 37–40; mô tả bỏ "hiện điểm ngay" cho bài `GRADED`.
- [ ] Contract `grade-events.json`: giữ payload `assignmentId`, `classId`, `accountIds`; ghi rõ mỗi event cho một (bài, lớp), bài của môn phát riêng từng lớp.
- [ ] Code port `GradeQueryPort`: thay `publishedForAssignment(assignmentId)` bằng `publishedForAssignmentInClass(assignmentId, classId)`; `publishedForAttempt` trả cả kết quả `PRACTICE`. `GradebookQueryPort`: thay `forAssignment(assignmentId)` bằng `forAssignmentInClass(assignmentId, classId)`. `GradebookRow`, `PublishedGrade` thêm `classId`.
- [ ] Code adapter: `SubmissionSubmittedAdapter` bỏ nhánh quiz `GRADED`; ghi `assignment_id`, `class_id` từ `SubmissionQueryPort`/`GroupSubmissionQueryPort`. `PracticeResultAdapter.scoreQuiz` là đường duy nhất dùng `QuizScorer`.
- [ ] Migration: dùng tên `db/migration/grading/V20260925_1950__create_evaluations.sql` (thay `V20260925_1950__u15_grading.sql`, chưa áp dụng); thêm cột `evaluations.assignment_id`, `evaluations.class_id`, CHECK lớp, index `(assignment_id, class_id, status)`; không tạo bảng công bố riêng (chỉ bảng của ERD). Nếu baseline cũ đã áp dụng thì thêm migration forward-only `V20261009_1950__add_evaluation_assignment_class.sql`.
- [ ] Công bố theo lớp: `GradeReleaseView` tính MIN/MAX `published_at` của `evaluations` theo `assignment_id`, `class_id`; không ghi `assignments.grades_released_at`.
- [ ] AI đề xuất chấm (UC 38): `AiProposalPanel` trong Grading Workspace và `BulkAiGradingDialog` trên Submission Detail do U15 tự làm; bỏ phụ thuộc `GradeWithAiDialog` chế độ `TEACHER_PROPOSAL` của U11.
- [ ] Bỏ phụ thuộc `AssignmentExtensionPort` (không ghi `assignments.grades_released_at`) và `TypeConfigPort` (không còn "hiện điểm ngay" cho bài `GRADED`).
- [ ] Màn theo screen flow: route `app/teaching/assignments/[id]/grading/` → `app/classes/[id]/teaching/assignments/[assignmentId]/submissions/` (Submission Detail); `app/teaching/grading/...` → `app/classes/[id]/teaching/grading/...` (Grading Workspace); `app/teaching/classes/[id]/gradebook/` → `GradebookPanel` trên tab Evals; `GradingQueuePage` đổi tên `SubmissionDetailPage`, `FinalizeGradesDialog` đổi tên `FinalizeConfirmDialog` (không còn là popup trong screen flow).
- [ ] Ảnh hưởng unit khác (sửa ở lượt của unit đó): U08 bỏ cột `assignments.grades_released_at` và dòng `GradeRelease` trong domain (thời điểm công bố theo lớp U15 suy ra từ `evaluations`), `EvalsTab` có nút "Bài nộp" mở Submission Detail theo lớp và nút "Sổ điểm"; U11 bỏ chế độ `TEACHER_PROPOSAL` của `GradeWithAiDialog` (chỉ còn `STUDENT_PRACTICE`), lưu lớp của lượt (`attempts.class_id`, cần cho bài của môn) và trả qua `SubmissionQueryPort`, `AssignmentGradeCell` gắn vào Student Assignments, Assignment Detail, Submission History (UC 22, 23, 28), quiz luyện tập hiện kết quả theo cài đặt quiz; U14 trả lớp của nhóm qua `GroupSubmissionQueryPort`, `GroupDocsOverviewPanel` gắn vào Submission Detail; U16 gọi `GradeQueryPort`/`GradebookQueryPort` theo (bài, lớp), `GradebookExportAction` gắn vào `GradebookPanel` (route `/api/v1/classes/{classId}/gradebook/export` giữ), bỏ quyền Admin ở export.

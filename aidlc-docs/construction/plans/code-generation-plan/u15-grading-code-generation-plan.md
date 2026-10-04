# U15 Grading - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U15. Mỗi bước xong thì đánh `[x]` ngay.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story trong phạm vi**: US-GRD-001..005, US-GRP-006; phần chấm của US-GRP-004. **Use case**: UC 32, UC 33, UC 34, UC 35, UC 36, UC 17. Gia hạn/phúc khảo/kiểm tra tương đồng nằm ngoài phạm vi.
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
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort`, `RubricPort`, `QuestionView` | U06 | Dùng thật |
| `AssignmentQueryPort`, `AssignmentExtensionPort` | U08 | Dùng thật (ghi trạng thái công bố điểm qua `AssignmentExtensionPort`) |
| `TypeConfigPort`, `DocumentEditor` | U09 | Dùng thật |
| `SubmissionQueryPort` | U11 | Dùng thật; U15 cài `SubmissionSubmittedPort` của U11 |
| `AiGradingPort`, `CodeRunPort` | U13 (`C`) | U13 ở wave 4, code sau U15: adapter tạm báo "AI chưa sẵn sàng" (ẩn nút Nhờ AI, chấm hàng loạt) và Code Lab "chưa chấm được"; U13 thay bằng bản thật. U15 cài `CodeGradedPort` của U13 |
| `GradeWithAiDialog` | U11 | Popup Grade with AI (UC 40) chế độ `TEACHER_PROPOSAL` |
| `GroupSubmissionQueryPort` | U14 | Dùng thật; U15 cài `GroupSubmittedPort` của U14 |
| U15 cài `SubmissionSubmittedPort`, `PracticeResultPort` (`scoreQuiz`), `GradeQueryPort` cho U11; `GroupSubmittedPort` cho U14 | cho U11, U14 | Thay adapter rỗng của U11, U14 (code trước U15 trong wave 3): bài `GRADED` vào hàng chấm, Practice Quiz có kết quả, điểm hiện cho Student |
| U15 cung cấp `GradeQueryPort`, `GradebookQueryPort`, event `grade.published` | cho U11, U16 | U11 bật hiển thị điểm; U16 đọc điểm cuối/trạng thái công bố cho phân bố điểm và tệp xuất |

### Dữ liệu U15 sở hữu

PostgreSQL `evaluations` (lịch sử trong cột `history`); cột `grades_released_at` của `assignments` (U08 tạo, U15 ghi qua `AssignmentExtensionPort`); không có job riêng; event `grade.published` (chỉ cho thông báo U16).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  grading/
    api/                GradingController, GradebookController, StudentGradeController, DTO
    application/        GradeWriter, GradingService, BulkGradeService, PublishService,
                        GradebookService, QuizScorer
    domain/             Evaluation, EvaluationKind, EvaluationStatus, EvaluationMethod,
                        EvaluationHistory, StudentGradeView, TeacherGradeView
    infrastructure/     JPA repository
    adapter/            SubmissionSubmittedAdapter, GroupSubmittedAdapter, CodeGradedAdapter,
                        PracticeResultAdapter
    port/               GradeQueryPort, GradebookQueryPort
/backend/src/main/resources/db/migration/grading/
/frontend/src/app/teaching/assignments/[id]/grading/
/frontend/src/app/teaching/grading/
/frontend/src/app/teaching/classes/[id]/gradebook/
/frontend/src/components/grades/ (AssignmentGradeCell gắn vào MyAssignmentsPage của U11)
/contracts/openapi/grading.yaml
/contracts/messages/grade-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.

### Nhóm B - Domain và logic

- [ ] **Bước 1** - Domain `Evaluation` (`kind` `ATTEMPT`/`GROUP_DOCUMENT`/`MEMBER`/`PRACTICE`) và chuyển trạng thái, `EvaluationHistory`, hai góc nhìn; port `GradeQueryPort` và `GradebookQueryPort` cho U16 (điểm cuối/trạng thái, không lộ đề xuất AI).
- [ ] **Bước 2** - `GradeWriter` một đường (version, luật lý do, khoảng điểm, lịch sử, event sau commit) (P1, BR-U15-13, 22, 33).
- [ ] **Bước 3** - Adapter cài `SubmissionSubmittedPort`, `GroupSubmittedPort` (tạo `evaluations` `PENDING` trong transaction nộp, chỉ bài `GRADED`), `CodeGradedPort`, `PracticeResultPort` (`scoreQuiz` cho U11, `record` cho U13; ghi `kind = PRACTICE`); `QuizScorer` chấm xác định ngay khi nộp, dùng chung cho `PRACTICE` (F1, F8, P2, P3, BR-U15-10…12, 35, FR-030).
- [ ] **Bước 4** - `GradingService`: chấm tay theo checklist rubric, nhờ AI, dùng đề xuất (lưu như chấm tay) (giải thích AI sửa được, không cần lý do khi khác đề xuất); nhờ AI qua `AiGradingPort.request` (endpoint `ai-proposal` của U15), chấm hàng loạt qua `AiGradingPort.requestBatch`, thứ tự chuyển bài cho workspace (F2, BR-U15-20…25).
- [ ] **Bước 5** - `BulkGradeService` chốt hàng loạt, `PublishService` công bố từng bài và công bố hàng loạt các bài đã chốt được chọn, ghi `grades_released_at` lần đầu (F3, F4, P4, BR-U15-31, 32).
- [ ] **Bước 6** - Chấm bài nhóm: tài liệu chung như bài `DOCUMENT` (tay hoặc AI), điểm đóng góp từng thành viên mặc định bằng điểm tài liệu chung, chấm tay từng người, lý do tùy chọn; nộp lại đưa đánh giá về `PENDING` (F6, BR-U15-40…44).
- [ ] **Bước 7** - `GradebookService` chỉ lấy bài `GRADED`, trả theo sinh viên (không điểm tổng), điểm của Student cho Assignment List, lịch sử (F7, P5, P6, BR-U15-50…52); lượt chính thức theo U11 (BR-U15-35).
- [ ] **Bước 8** - Audit theo BR-U15-53.
- [ ] **Bước 9** - Unit test mọi `BR-U15-xx`.
- [ ] **Bước 10** - Tóm tắt: `aidlc-docs/construction/u15-grading/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 11** - Flyway `V20260925_1950__u15_grading.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 12** - JPA repository và query sổ điểm.
- [ ] **Bước 13** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: nộp `GRADED` → tự chấm → hiện đúng chính sách; `PRACTICE` không vào gradebook hoặc hàng đợi Teacher; gọi port nộp lặp không tạo điểm trùng; chốt hàng loạt có mục lệch version; lịch sử chỉ thêm; sổ điểm 200 × 30 ≤ 1 s.
- [ ] **Bước 14** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 15** - `/contracts/openapi/grading.yaml` và schema event.
- [ ] **Bước 16** - Controller + DTO + validation.
- [ ] **Bước 17** - Test MockMvc: người học không thấy điểm chưa công bố/đề xuất AI; giảng viên lớp khác `404` + audit; sửa không lý do bị từ chối.
- [ ] **Bước 18** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 19** - `GradingQueuePage` (lọc, chọn nhiều, `BulkAiGradingDialog`, `FinalizeGradesDialog`, `BulkPublishButton`; bài nhóm nhúng `GroupDocsOverviewPanel` của U14).
- [ ] **Bước 20** - `GradingWorkspacePage` (`SubmissionNavigator` ‹ › và tự sang bài kế, `SubmissionViewer`, `MethodChooser`, `RubricChecklistForm`, `GradeWithAiDialog` (U11), `FeedbackEditor`, `OverrideReasonDialog`, `GradeHistoryDrawer`, `PublishButton`).
- [ ] **Bước 21** - `GroupGradingMode` trong `GradingWorkspacePage` khi `targetKind = groups` (tài liệu chung như bài `DOCUMENT`, `MemberScoresPanel` điểm mặc định bằng nhau).
- [ ] **Bước 22** - `GradebookPage` (gom theo sinh viên, mỗi sinh viên đóng/mở), `AssignmentGradeCell` trên Assignment List của Student (không có trang My Grades riêng); bật phần điểm trong trang bài đã nộp của U11.
- [ ] **Bước 23** - Test frontend: điểm hiển thị "x / tổng", sửa điểm đã chốt bắt lý do còn khác đề xuất AI thì không, ‹ › và tự sang bài kế, sổ điểm đóng/mở theo sinh viên không có điểm tổng.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: luồng chấm, chốt, công bố; chấm bài nhóm.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-GRD-001 | 3, 13 |
| US-GRD-002 (UC 33) | 4, 20 |
| US-GRD-003 (UC 32, UC 33) | 2, 4, 20 |
| US-GRD-004 (UC 35; lịch sử điểm của UC 36) | 7, 22 |
| US-GRD-005 (UC 34) | 2, 5, 19, 20 |
| US-GRP-004 S3, US-GRP-006 (UC 17) | 6, 21 |

## 5. Ngoài phạm vi

- Gia hạn/phúc khảo/kiểm tra tương đồng ngoài phạm vi dự án; U16 sở hữu xuất bảng điểm trong MVP và thông báo hệ thống.

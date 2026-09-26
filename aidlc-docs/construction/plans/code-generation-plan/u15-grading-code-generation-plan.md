# U15 Grading - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U15. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story trong phạm vi**: US-GRD-001..005, US-GRP-006; phần chấm của US-GRP-004. **Use case**: UC-GRD-01..07, UC-GRP-08. Gia hạn/phúc khảo/kiểm tra tương đồng nằm ngoài phạm vi.
- **Thiết kế nguồn**: `construction/u15-grading/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort`, `EventPublisherPort` | U02 | Dùng thật |
| `ClassAccessPort` | U04 | Dùng thật |
| `BankQueryPort`, `RubricPort`, `QuestionView` | U06 | Dùng thật |
| `AssignmentQueryPort`, `AssignmentExtensionPort` | U08 | Dùng thật (ghi trạng thái công bố điểm qua `AssignmentExtensionPort`) |
| `TypeConfigPort`, `DocumentEditor` | U09 | Dùng thật |
| `SimulationPolicyPort` | U10 | Dùng thật |
| `SubmissionQueryPort` | U11 | Dùng thật; U15 cài `SubmissionSubmittedPort` của U11 |
| `AiGradingPort`, `AiGradingPanel`, `CodeRunPort` | U13 | Dùng thật; U15 cài `CodeGradedPort` của U13 |
| `GroupSubmissionQueryPort` | U14 | Dùng thật; U15 cài `GroupSubmittedPort` của U14 |
| U15 cung cấp `GradeQueryPort`, `GradebookQueryPort`, event `grade.published` | cho U11, U16 | U11 bật hiển thị điểm; U16 đọc điểm cuối/trạng thái công bố cho dashboard và tệp xuất |

### Dữ liệu U15 sở hữu

PostgreSQL `grades`, `grade_history`; cột `grades_released_by`, `grades_released_at` của `publications` (U15 thêm cột, ghi qua `AssignmentExtensionPort` của U08); job `GRADE_INIT` trên queue `jobs.triggered`; event `grade.published` (chỉ cho thông báo U16).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  grading/
    api/                GradingController, GradebookController, LearnerGradeController, DTO
    application/        GradeWriter, GradingService, BulkGradeService, PublishService,
                        GradebookService, QuizScorer
    domain/             Grade, GradeStatus, GradeMethod, TargetKind, GradeHistory,
                        GradeRelease, LearnerGradeView, TeacherGradeView
    infrastructure/     JPA repository
    worker/             GradeInitHandler
    adapter/            SubmissionSubmittedAdapter, GroupSubmittedAdapter, CodeGradedAdapter
    port/               GradeQueryPort, GradebookQueryPort
/backend/src/main/resources/db/migration/u15/
/frontend/src/app/teaching/publications/[id]/grading/
/frontend/src/app/teaching/grading/
/frontend/src/app/teaching/classes/[id]/gradebook/
/frontend/src/app/learn/grades/
/contracts/openapi/u15-grading.yaml
/contracts/messages/u15-grade-published.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.

### Nhóm B - Domain và logic

- [ ] **Bước 1** - Domain `Grade` và chuyển trạng thái, `GradeHistory`, hai góc nhìn; port `GradeQueryPort` và `GradebookQueryPort` cho U16 (điểm cuối/trạng thái, không lộ đề xuất AI).
- [ ] **Bước 2** - `GradeWriter` một đường (version, luật lý do, khoảng điểm, lịch sử, event sau commit) (P1, BR-U15-13, 22, 33).
- [ ] **Bước 3** - Adapter cài `SubmissionSubmittedPort`, `GroupSubmittedPort` (tạo job `GRADE_INIT`), `CodeGradedPort` (ghi điểm code); `GradeInitHandler` + `QuizScorer` (trắc nghiệm, gọi `CodeRunPort.grade` cho Code Lab, bài nhóm; idempotent; hiện điểm ngay) (F1, P2, P3, BR-U15-10…12).
- [ ] **Bước 4** - `GradingService`: chọn phương thức, chấm tay theo rubric/điểm câu, nhờ AI, dùng đề xuất (F2, BR-U15-20…23).
- [ ] **Bước 5** - `BulkGradeService` chốt hàng loạt, `PublishService` công bố theo lượt phát hành (F3, F4, P4, BR-U15-31, 32).
- [ ] **Bước 6** - Chấm bài nhóm: tài liệu chung (chỉ tay), đóng góp thành viên (tay/AI), điểm cuối thành viên (F6, BR-U15-40…43).
- [ ] **Bước 7** - `GradebookService` (không cột tổng), điểm của người học, lịch sử (F7, P5, P6, BR-U15-50…52); lượt được chấm theo U11/U10 (BR-U15-35).
- [ ] **Bước 8** - Audit theo BR-U15-53.
- [ ] **Bước 9** - Unit test mọi `BR-U15-xx`.
- [ ] **Bước 10** - Tóm tắt: `aidlc-docs/construction/u15-grading/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 11** - Flyway `V20260925_2200__u15_grading.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 12** - JPA repository và query sổ điểm.
- [ ] **Bước 13** - Integration test: nộp → tự chấm → hiện ngay; job `GRADE_INIT` chạy lặp; chốt hàng loạt có mục lệch version; `app` không sửa lịch sử; sổ điểm 200 × 30 ≤ 1 s.
- [ ] **Bước 14** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 15** - `/contracts/openapi/u15-grading.yaml` và schema event.
- [ ] **Bước 16** - Controller + DTO + validation.
- [ ] **Bước 17** - Test MockMvc: người học không thấy điểm chưa công bố/đề xuất AI; giảng viên lớp khác `404` + audit; sửa không lý do bị từ chối.
- [ ] **Bước 18** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 19** - `GradingQueuePage` (lọc, chọn nhiều, `BulkFinalizeDialog`, `PublishGradesButton`).
- [ ] **Bước 20** - `GradingWorkspacePage` (`SubmissionViewer`, `MethodChooser`, `RubricChecklistForm`, `AiGradingPanel`, `FeedbackEditor`, `OverrideReasonDialog`, `GradeHistoryDrawer`).
- [ ] **Bước 21** - `GroupGradingPage` (tài liệu tô màu theo tác giả, đóng góp, điểm cuối).
- [ ] **Bước 22** - `GradebookPage`, `MyGradesPage`; bật phần điểm trong trang bài đã nộp của U11.
- [ ] **Bước 23** - Test frontend: điểm hiển thị "x / tổng", sửa điểm bắt lý do, sổ điểm không có cột tổng.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: luồng chấm, chốt, công bố; chấm bài nhóm.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-GRD-001 | 3, 13 |
| US-GRD-002 (UC-GRD-01, 03) | 4, 20 |
| US-GRD-003 (UC-GRD-02, 04) | 2, 4, 5, 20 |
| US-GRD-004 (UC-GRD-06, 07) | 7, 22 |
| US-GRD-005 (UC-GRD-05) | 5, 19 |
| US-GRP-004 S3, US-GRP-006 (UC-GRP-08) | 6, 21 |

## 5. Ngoài phạm vi

- Gia hạn/phúc khảo/kiểm tra tương đồng ngoài phạm vi dự án; U16 sở hữu xuất bảng điểm trong MVP và thông báo hệ thống.

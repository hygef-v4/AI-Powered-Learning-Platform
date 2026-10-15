# U09 Assignment Grading — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Thiết kế: [business-logic-model](../../u09-assignment-grading/functional-design/business-logic-model.md), [business-rules](../../u09-assignment-grading/functional-design/business-rules.md), [domain-entities](../../u09-assignment-grading/functional-design/domain-entities.md), [frontend-components](../../u09-assignment-grading/functional-design/frontend-components.md), [NFR](../../u09-assignment-grading/nfr-requirements/nfr-requirements.md), [NFR design](../../u09-assignment-grading/nfr-design/nfr-design-patterns.md), [infrastructure](../../u09-assignment-grading/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan mới, chưa được duyệt và chưa code. Thay phần chấm Assignment và sổ điểm của plan U15 cũ, phần tiến độ, phân bố điểm, xuất bảng điểm của plan U16 cũ.

## Code hiện có

| Path | Hiện trạng | Việc trong plan này |
|---|---|---|
| `backend/.../grading/` (`GradingService`, `GradebookService`, `GradePublished`, port `GradebookQueryPort`, `GradebookRow`, `GradeQueryPort`, `PublishedGrade`) | Khung cũ, chưa có test | Giữ package `grading`; viết lại theo thiết kế mới |
| `backend/.../grading/adapter/SubmissionSubmittedAdapter` | Cài port cũ của U11 cũ | Thay bằng `EvaluationLifecycleService` cài `SubmissionListener` |
| `backend/.../grading/adapter/GroupSubmittedAdapter` | Cài port cũ của U14 cũ | Thay bằng `GroupEvaluationPort` do U12 gọi |
| `backend/.../grading/adapter/PracticeResultAdapter`, `CodeGradedAdapter` | Kết quả Quiz luyện tập, Code Lab | Xóa; kết quả Quiz để plan U11, Code Lab đã bỏ |
| `contracts/openapi/grading.yaml`, `reporting.yaml`, `contracts/messages/grade-events.json` | Contract cũ | Cập nhật `grading.yaml` theo API mới (gồm xuất); `reporting.yaml` để plan U14, U15; `grade-events.json` theo `grade.published` |

## Checklist

- [ ] **Bước 1 — Dọn code cũ:** theo bảng trên; code biên dịch được.
- [ ] **Bước 2 — Migration:** `db/migration/grading/V20260925_1500__create_grading_tables.sql` theo infrastructure-design.
- [ ] **Bước 3 — Domain:** `Evaluation`, `EvaluationHistory`, `GradingProposal` và enum; kiểm chuyển trạng thái; repository chỉ dùng trong package.
- [ ] **Bước 4 — Tạo đánh giá:** `EvaluationLifecycleService` cài `SubmissionListener`, `GroupEvaluationPort`; contract `GroupSubmissionSource` với adapter rỗng (F1, F14).
- [ ] **Bước 5 — Bài nộp:** `SubmissionReviewService` (F2, F3).
- [ ] **Bước 6 — Chấm, chốt, công bố:** `GradingService` (F4, F8–F10, P1, P2, P4); phát `grade.published`.
- [ ] **Bước 7 — AI đề xuất:** `GradingProposalService`, `GradingProposalResultHandler` (cài `AiTaskResultHandler` cho `GRADING_PROPOSAL`), `GradingMcpTools` (cài `McpToolProvider`) (F5–F7, P3).
- [ ] **Bước 8 — Sổ điểm, xuất:** `GradebookService`, `GradebookExporter` CSV và XLSX (F11, F12).
- [ ] **Bước 9 — Người học:** `StudentGradeService`, `GradeDistributionCalculator` (F13).
- [ ] **Bước 10 — API:** controller theo domain-entities mục 5; `contracts/openapi/grading.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 11 — Frontend:** `ClassGradebookSection` (đăng ký `TeacherClassTab`), `SubmissionsLink` (đăng ký `AssignmentRowAction`), `SubmissionDetailPage`, `GradingWorkspacePage` và các panel, `StudentGradeCell`, `StudentGradePanel` (đăng ký slot của U08).
- [ ] **Bước 12 — Unit test (người code viết):** tạo đánh giá chỉ với bài `GRADED`; lượt mới làm đánh giá cũ `SUPERSEDED`; điểm từ checklist và điều chỉnh có lý do; kẹp điểm; chốt hàng loạt có mục lỗi; công bố phát sự kiện đúng người; sửa sau chốt thiếu lý do bị từ chối; đề xuất sai id mục hoặc hash cũ; hết quota giữa lô; sổ điểm không có Quiz, Practice, điểm tổng; CSV chặn công thức; phân bố dưới 20 người hoặc khoảng dưới 5 người; Chủ nhiệm môn không dạy lớp bị từ chối; component frontend. Integration test do tester viết.
- [ ] **Bước 13 — Review:** đối chiếu UC 42–45, story, BR-U09 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Nộp bài `GRADED` tạo đánh giá trong cùng transaction; nộp lượt mới sau khi đã chấm.
- Chấm, chốt, công bố hàng loạt 200 bài với vài mục lỗi; U14 nhận `grade.published`.
- AI đề xuất bằng nguồn giả; đề xuất về sau khi nhóm nộp lại thành `STALE`.
- Xuất CSV, XLSX đúng phạm vi; xuất lớp không dạy bị từ chối.
- Phân quyền 4 role trên mọi endpoint, gồm người học đọc điểm của người khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.

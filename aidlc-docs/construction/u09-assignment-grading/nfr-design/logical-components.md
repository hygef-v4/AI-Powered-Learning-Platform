# U09 Assignment Grading — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Lấy phần chấm Assignment và sổ điểm của U15 cũ và phần tiến độ nộp, phân bố điểm, xuất bảng điểm của U16 cũ. Kết quả Quiz luyện tập thuộc U11, thông báo thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `SubmissionReviewController`, `EvaluationController`, `GradingProposalController`, `GradebookController`, `MyGradeController` | backend | API mục 5 của domain-entities |
| `EvaluationLifecycleService` (cài `SubmissionListener`, `GroupEvaluationPort`) | backend | Tạo, thay thế đánh giá (F1, F14) |
| `SubmissionReviewService` | backend | Bài nộp và tiến độ, mở bài (F2, F3) |
| `GradingService` | backend | Chấm, chốt, công bố, sửa (F4, F8–F10, P1, P2, P4) |
| `GradingProposalService`, `GradingProposalResultHandler`, `GradingMcpTools` | backend, worker | AI đề xuất, hàng loạt, MCP (F5–F7, P3) |
| `GradebookService`, `GradebookExporter` | backend | Sổ điểm, xuất (F11, F12, P5) |
| `StudentGradeService`, `GradeDistributionCalculator` | backend | Điểm của người học, phân bố (F13) |
| Repository `evaluations`, `grading_proposals` | backend, worker | Chỉ dùng trong package |

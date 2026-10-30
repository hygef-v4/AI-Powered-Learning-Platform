# U09 Assignment Grading — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `SubmissionReviewController`, `EvaluationController`, `GradingProposalController`, `GradebookController`, `MyGradeController` | backend | API mục 5 của domain-entities |
| `EvaluationLifecycleService` (cài `ISubmissionListener`, `IGroupEvaluation`) | backend | Tạo, thay thế đánh giá (F1, F14) |
| `SubmissionReviewService` | backend | Bài nộp và tiến độ, mở bài (F2, F3) |
| `GradingService` | backend | Chấm, chốt, công bố, sửa (F4, F8–F10, P1, P2, P4) |
| `GradingProposalService`, `GradingProposalResultHandler`, `GradingMcpTools` | backend, worker | AI đề xuất, hàng loạt, MCP (F5–F7, P3) |
| `GradebookService`, `GradebookExporter` | backend | Sổ điểm, xuất (F11, F12, P5) |
| `StudentGradeService`, `GradeDistributionCalculator` | backend | Điểm của người học, phân bố (F13) |
| Repository `evaluations`, `grading_proposals` | backend, worker | Chỉ dùng trong package |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

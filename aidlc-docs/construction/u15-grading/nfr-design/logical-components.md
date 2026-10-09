# U15 Grading - Logical Components

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
+----------------------------------------------------------+
| U11, U14 nộp bài; U13 chấm Code Lab, chấm Practice xong  |
+----------------------------------------------------------+
                              |
                              |  port do U15 cài, cùng transaction
                              v
+----------------------------------------------------------+
| backend                                                  |
| SubmissionSubmittedAdapter, GroupSubmittedAdapter,       |
| CodeGradedAdapter, PracticeResultAdapter --> QuizScorer  |
| GradingController --> GradingService (P1, P3)            |
|   --> RubricPort (U06), AiGradingPort (U13)              |
| BulkGradeService, PublishService (P4)                    |
| GradebookService (P6), StudentGradeController (P5)       |
| Mọi ghi điểm --> GradeWriter (P1)                        |
| Repository: evaluations                                  |
+----------------------------------------------------------+
                              |
                              |  event grade.published sau commit
                              v
+----------------------------------------------------------+
| U16: thông báo, phân bố điểm, xuất bảng điểm             |
+----------------------------------------------------------+
```

**Text alternative**: U11 và U14 khi nộp, U13 khi chấm xong Code Lab hoặc có kết quả Practice, gọi port do U15 cài trong cùng transaction; adapter tạo dòng `evaluations` (kèm bài và lớp), chấm quiz luyện tập bằng `QuizScorer` hoặc cập nhật điểm Code Lab. Trong backend, giảng viên lớp xem bài nộp và chấm qua `GradingController`/`GradingService` (dùng rubric U06, đề xuất AI U13); chốt/công bố hàng loạt qua `BulkGradeService`/`PublishService`; sổ điểm qua `GradebookService`; Student chỉ đọc điểm đã công bố qua `StudentGradeController`. Mọi thay đổi điểm đi qua `GradeWriter`; dữ liệu nằm ở `evaluations` (thời điểm công bố theo lớp suy ra từ `published_at`). Công bố phát event `grade.published` cho U16.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `GradeWriter` | backend, worker | P1 |
| `GradingScopeGuard` | backend | R3/R4 theo `class_id` của đánh giá; Admin và Chủ nhiệm môn không dạy lớp bị từ chối (BR-U15-01, 03) |
| `QuizScorer` | backend | F9; P3 |
| `SubmissionSubmittedAdapter`, `GroupSubmittedAdapter`, `CodeGradedAdapter`, `PracticeResultAdapter` | backend, worker | Cài port của U11, U14, U13: tạo dòng `evaluations` (P2), cập nhật điểm Code Lab, kết quả Practice |
| `GradingController`, `GradingService` | backend | F2, F3, F6, F7 |
| `BulkGradeService`, `PublishService` | backend | F4, F5; P4 |
| `GradebookController`, `GradebookService`, `StudentGradeController` | backend | F8; P5, P6 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log phản hồi |
| SECURITY-05 | Compliant | Kiểm điểm, lý do |
| SECURITY-08 | Compliant | P5, `GradingScopeGuard` |
| SECURITY-15 | Compliant | P1, P2, P4 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

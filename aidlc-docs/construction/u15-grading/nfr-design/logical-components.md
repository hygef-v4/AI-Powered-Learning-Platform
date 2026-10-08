# U15 Grading - Logical Components

**Bản tài liệu 2026-10-08**: UC 34, 35, 36, 37; primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 U11/U14 nộp, U13 chấm xong --> port (cùng transaction) --> QuizScorer --> GradeWriter
 +--------------------------------- backend -------------------------------------+
 | GradingController --> GradingService --> RubricPort (U06), AiGradingPort (U13)|
 |                                      --> GradeWriter (P1)                     |
 | BulkGradeService (P4)   PublishService (P4)                                   |
 | GradebookService (P6)   StudentGradeController (P5)                           |
 | Repository (evaluations) + AssignmentExtensionPort (U08)                       |
 +-------------------------------------------------------------------------------+
        | event grade.published
        v
       U16
```

**Text alternative**: U11 và U14 khi nộp, U13 khi chấm xong code, gọi port do U15 cài trong cùng transaction; port tạo dòng `evaluations`, tự chấm trắc nghiệm bằng `QuizScorer` hoặc cập nhật điểm code, ghi qua `GradeWriter`. Trong backend, giảng viên chấm qua `GradingService` (dùng rubric U06, đề xuất AI U13), chốt/công bố hàng loạt qua `BulkGradeService`/`PublishService`; sổ điểm qua `GradebookService`; người học chỉ đọc điểm đã công bố. Công bố phát event `grade.published` cho U16.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `GradeWriter` | backend, worker | P1 |
| `QuizScorer` | backend | F1; P3 |
| `SubmissionSubmittedAdapter`, `GroupSubmittedAdapter`, `CodeGradedAdapter`, `PracticeResultAdapter` | backend, worker | Cài port của U11, U14, U13: tạo dòng `evaluations` (P2) hoặc cập nhật điểm code, kết quả Practice |
| `GradingService` | backend | F2, F5, F6 |
| `BulkGradeService`, `PublishService` | backend | F3, F4; P4 |
| `GradebookService`, `StudentGradeController` | backend | F7; P5, P6 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log phản hồi |
| SECURITY-05 | Compliant | Kiểm điểm, lý do |
| SECURITY-08 | Compliant | P5, quyền giảng viên lớp |
| SECURITY-15 | Compliant | P1, P2, P4 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

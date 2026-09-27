# U15 Grading - Logical Components

## 1. Sơ đồ

```
 U11/U14 nộp, U13 chấm xong --> port (cùng transaction) --> job GRADE_INIT (jobs.triggered)
        |
        v
 worker: GradeInitHandler --> QuizScorer --> GradeWriter
 +--------------------------------- backend -------------------------------------+
 | GradingController --> GradingService --> RubricPort (U06), AiGradingPort (U13)|
 |                                      --> GradeWriter (P1)                     |
 | BulkGradeService (P4)   PublishService (P4)                                   |
 | GradebookService (P6)   LearnerGradeController (P5)                           |
 | Repository (grades, grade_history) + AssignmentExtensionPort (U08)            |
 +-------------------------------------------------------------------------------+
        | event grade.published
        v
       U16
```

**Text alternative**: U11 và U14 khi nộp, U13 khi chấm xong code, gọi port do U15 cài trong cùng transaction; port tạo job `GRADE_INIT` hoặc cập nhật điểm code. Worker chạy `GradeInitHandler`, tự chấm trắc nghiệm bằng `QuizScorer` và ghi qua `GradeWriter`. Trong backend, giảng viên chấm qua `GradingService` (dùng rubric U06, đề xuất AI U13), chốt/công bố hàng loạt qua `BulkGradeService`/`PublishService`; sổ điểm qua `GradebookService`; người học chỉ đọc điểm đã công bố. Công bố phát event `grade.published` cho U16.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `GradeWriter` | backend, worker | P1 |
| `GradeInitHandler`, `QuizScorer` | worker | F1; P2, P3 |
| `SubmissionSubmittedAdapter`, `GroupSubmittedAdapter`, `CodeGradedAdapter` | backend, worker | Cài port của U11, U14, U13: tạo job `GRADE_INIT` hoặc cập nhật điểm code |
| `GradingService` | backend | F2, F5, F6 |
| `BulkGradeService`, `PublishService` | backend | F3, F4; P4 |
| `GradebookService`, `LearnerGradeController` | backend | F7; P5, P6 |

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log phản hồi |
| SECURITY-05 | Compliant | Kiểm điểm, lý do |
| SECURITY-08 | Compliant | P5, quyền giảng viên lớp |
| SECURITY-15 | Compliant | P1, P2, P4 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

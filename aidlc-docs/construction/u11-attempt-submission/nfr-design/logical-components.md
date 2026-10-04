# U11 Attempt & Submission - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (người học)
   |  bắt đầu / lưu (gzip) / nộp / chấm AI Practice
   v
 +------------------------------- backend ---------------------------------------------+
 | AttemptController --> AttemptStarter (advisory lock) --> U08 (đề, lượt)             |
 |                   --> DraftSaver --> DocumentModelPort (U09)                        |
 |                   --> AttemptSubmitter --> SubmissionSubmittedPort (U15, GRADED)    |
 |                                        --> PracticeResultPort (U15, Practice Quiz)  |
 |                                        --> CodeRunPort.grade (U13, Code Lab)        |
 |                   --> "Chấm với AI" --> PracticeGradingPort (U13)                   |
 | AssignmentLifecycleAdapter (onRetired) --> đặt deadline_at = now                    |
 | AttemptQueryService (SubmissionQueryPort cho U13, U15, U16)                         |
 | Repository (attempts + trigger bất biến)                                            |
 +-------------------------------------------------------------------------------------+
            | scanner tự nộp (U03)
            v
 worker: AttemptDeadlineScanner --> AttemptSubmitter
```

**Text alternative**: Người học bắt đầu lượt qua `AttemptStarter` (khóa theo người học và bài), lưu nháp qua `DraftSaver` (kiểm tài liệu bằng U09), nộp qua `AttemptSubmitter`. Chỉ bài `GRADED` gọi `SubmissionSubmittedPort` của U15 trong cùng transaction; bài `PRACTICE` Quiz nhờ U15 chấm qua `PracticeResultPort`, Code Lab nhờ U13 chạy test, Text/Diagram Essay chỉ gọi `PracticeGradingPort` khi Student bấm "Chấm với AI". Khi bài bị ngưng giao, U08 gọi `AssignmentLifecycleAdapter` để đặt hạn ngay cho mọi lượt dở; `AttemptDeadlineScanner` trong worker tự nộp chúng ở lượt quét kế tiếp, cũng như lượt hết hạn. Các unit khác đọc bài nộp qua `AttemptQueryService`.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AttemptStarter` | backend | F2; P1 |
| `DraftSaver` | backend | F3; P2, P6 |
| `AttemptSubmitter` | backend, worker | F4, F5; P3 |
| `AttemptDeadlineScanner` | worker | F5; P5 |
| `AssignmentLifecycleAdapter` | backend, worker | Cài `AssignmentLifecyclePort.onRetired` của U08: đặt hạn ngay cho lượt dở |
| `AttemptQueryService` | backend | F1, F6, F7 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U11_AUTOSAVE_SECONDS` | 10 |
| `U11_GRACE_SECONDS` | 30 |
| `U11_MAX_CONTENT_BYTES` | 10MB |
| `U11_SAVE_PER_MINUTE` | 30 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung |
| SECURITY-05 | Compliant | P2 giới hạn, kiểm |
| SECURITY-08 | Compliant | Kiểm chủ sở hữu |
| SECURITY-15 | Compliant | P3, P4 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

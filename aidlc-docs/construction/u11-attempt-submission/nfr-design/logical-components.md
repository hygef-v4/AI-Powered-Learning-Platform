# U11 Attempt & Submission - Logical Components

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt (Student)
   |  xem danh sách / bắt đầu / lưu (gzip) / nộp / chấm AI Practice
   v
+------------------------------------ backend -------------------------------------+
| AttemptController --> AttemptStarter (advisory lock) --> U08 (bài, quiz, lượt)   |
|                   --> DraftSaver --> DocumentModelPort (U09)                     |
|                   --> AttemptSubmitter --> SubmissionSubmittedPort (U15, GRADED) |
|                                        --> PracticeResultPort (U15, quiz)        |
|                                        --> CodeRunPort.grade (U13, Code Lab)     |
|                   --> Chấm với AI --> PracticeGradingPort (U13)                  |
| StudentViewController --> AttemptQueryService --> U04, U05, U08, U14, U15        |
| AssignmentLifecycleAdapter (onRetired) --> đặt deadline_at = now                 |
| AttemptQueryService (SubmissionQueryPort cho U13, U15, U16)                      |
| Repository (attempts + trigger bất biến)                                         |
+----------------------------------------------------------------------------------+
            | scanner tự nộp (U03)
            v
 worker: AttemptDeadlineScanner --> AttemptSubmitter
```

**Text alternative**: Student mở Student Assignments, Assignment Detail, Quiz Practice History/Detail và danh sách quiz của học liệu qua `StudentViewController`; `AttemptQueryService` ghép bài/quiz của U08, ghi danh U04, học liệu U05, trạng thái nhóm U14, điểm U15 với lượt của mình. Bắt đầu lượt qua `AttemptStarter` (khóa theo người học và bài), lưu nháp qua `DraftSaver` (kiểm tài liệu bằng U09), nộp qua `AttemptSubmitter`. Chỉ bài tập `GRADED` gọi `SubmissionSubmittedPort` của U15 trong cùng transaction; quiz nhờ U15 chấm qua `PracticeResultPort`, Practice Code Lab nhờ U13 chạy test, Practice Text/Diagram Essay chỉ gọi `PracticeGradingPort` khi Student bấm "Chấm với AI". Khi bài hoặc quiz bị ngưng, U08 gọi `AssignmentLifecycleAdapter` để đặt hạn ngay cho mọi lượt dở; `AttemptDeadlineScanner` trong worker tự nộp chúng ở lượt quét kế tiếp, cũng như lượt hết giờ, hết hạn. Các unit khác đọc bài nộp qua `AttemptQueryService`.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `StudentViewService` | backend | F1, F2, F3; ghép danh sách gộp theo lớp |
| `AttemptStarter` | backend | F4; P1 |
| `DraftSaver` | backend | F5, F5a; P2, P6 |
| `AttemptSubmitter` | backend, worker | F6, F7; P3 |
| `AttemptDeadlineScanner` | worker | F7; P5 |
| `AssignmentLifecycleAdapter` | backend, worker | Cài `AssignmentLifecyclePort.onRetired` của U08: đặt hạn ngay cho lượt dở |
| `AttemptQueryService` | backend | F8, F9, F10, F11 |

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
| SECURITY-08 | Compliant | Kiểm chủ sở hữu, ghi danh lớp; Admin, Teacher không gọi được API người học |
| SECURITY-15 | Compliant | P3, P4 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

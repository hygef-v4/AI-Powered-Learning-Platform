# U11 Attempt & Submission - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (người học)
   |  bắt đầu / lưu (gzip) / nộp
   v
 +------------------------------- backend -----------------------------------+
 | AttemptController --> AttemptStarter (advisory lock) --> JobPort (U02)    |
 |                   --> DraftSaver --> DocumentModelPort (U09)              |
 |                   --> AttemptSubmitter --> SubmissionSubmittedPort (U15)  |
 | PublicationLifecycleAdapter (onRetired) --> JobPort (U02)                 |
 | AttemptQueryService (SubmissionQueryPort cho U13, U15, U16)               |
 | Repository (submissions + trigger bất biến)                               |
 +---------------------------------------------------------------------------+
            | job ATTEMPT_AUTO_SUBMIT
            v
 worker: AutoSubmitHandler --> AttemptSubmitter
```

**Text alternative**: Người học bắt đầu lượt qua `AttemptStarter` (khóa theo người học và bài, tạo job tự nộp), lưu nháp qua `DraftSaver` (kiểm tài liệu bằng U09), nộp qua `AttemptSubmitter` (gọi `SubmissionSubmittedPort` của U15 trong cùng transaction). Khi bài bị ngừng giao, U08 gọi `PublicationLifecycleAdapter` để tạo job tự nộp mọi lượt dở. Worker chạy job tự nộp đúng hạn. Các unit khác đọc bài nộp qua `AttemptQueryService`.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AttemptStarter` | backend | F2; P1 |
| `DraftSaver` | backend | F3; P2, P6 |
| `AttemptSubmitter` | backend, worker | F4, F5; P3 |
| `AutoSubmitHandler` | worker | F5; P5 |
| `PublicationLifecycleAdapter` | backend, worker | Cài `PublicationLifecyclePort.onRetired` của U08: tạo job tự nộp |
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

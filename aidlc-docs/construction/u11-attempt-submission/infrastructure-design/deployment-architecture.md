# U11 Attempt & Submission - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
+-------------+     +-------+     +--------------+     +----------+
| Trình duyệt | --> | nginx | --> | backend: U11 | --> | postgres |
+-------------+     +-------+     +--------------+     +----------+
                                         |                  ^
                                         v                  |
                                    +---------+             |
                                    |  redis  |             |
                                    +---------+             |
                            +--------------------------------+
                            | worker: AttemptDeadlineScanner |
                            +--------------------------------+
```

**Text alternative**: Student xem danh sách, làm quiz và bài tập, lưu và nộp qua Nginx (route lưu tối đa 12 MB, nén gzip) tới module U11 trong backend; U11 ghi PostgreSQL và giới hạn tần suất lưu bằng Redis. Bài tập `GRADED` gọi `SubmissionSubmittedPort` để U15 tạo dòng `evaluations` chờ chấm trong cùng transaction; quiz được U15 chấm theo đáp án, Practice Code Lab chấm theo test (U13), Practice Text/Diagram Essay chấm bằng AI khi Student bấm chấm. Scanner `AttemptDeadlineScanner` trong worker đọc `attempts.deadline_at` và tự nộp lượt hết giờ quiz, hết hạn bài tập hoặc bị ngưng giao (U08 báo qua `AssignmentLifecyclePort`). Không thêm container.

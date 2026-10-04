# U11 Attempt & Submission - Deployment Architecture

```
 Trình duyệt --HTTPS (lưu gzip ≤ 12 MB)--> [nginx] --> [backend: U11] --> [postgres]
                                                          |      |
                                                          |      +--> [redis: rate limit]
                                                          +--scanner tự nộp (worker)
                                                                             |
                                                  [worker: tự nộp theo hạn hoặc ngưng giao]
```

**Text alternative**: Người học lưu và nộp bài qua Nginx (lưu tối đa 12 MB, nén gzip) tới module U11 trong backend; U11 ghi PostgreSQL, giới hạn tần suất lưu bằng Redis, scanner trong worker tự nộp lượt quá hạn. Bài `GRADED` gọi `SubmissionSubmittedPort` để U15 tạo dòng `evaluations` chờ chấm trong cùng transaction; bài `PRACTICE` dùng chấm xác định, hoặc AI khi Student bấm chấm. Scanner trong worker tự nộp theo hạn và theo ngưng giao (U08 báo qua `AssignmentLifecyclePort`); U15 chỉ nhận bài `GRADED`.

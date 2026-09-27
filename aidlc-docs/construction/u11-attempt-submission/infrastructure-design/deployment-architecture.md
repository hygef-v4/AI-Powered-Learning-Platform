# U11 Attempt & Submission - Deployment Architecture

```
 Trình duyệt --HTTPS (lưu gzip ≤ 12 MB)--> [nginx] --> [backend: U11] --> [postgres]
                                                          |      |
                                                          |      +--> [redis: rate limit]
                                                          +--job--> [rabbitmq: jobs.scheduled]
                                                                             |
                                                  [worker: tự nộp theo hạn hoặc ngưng giao]
```

**Text alternative**: Người học lưu và nộp bài qua Nginx (lưu tối đa 12 MB, nén gzip) tới module U11 trong backend; U11 ghi PostgreSQL, giới hạn tần suất lưu bằng Redis, tạo job tự nộp qua RabbitMQ; khi nộp gọi `SubmissionSubmittedPort` để U15 tạo job chấm trong cùng transaction. Worker chạy job tự nộp theo hạn và theo ngưng giao (U08 báo qua `PublicationLifecyclePort`); đã nộp được gửi tới U15, U16.

# U11 Attempt & Submission - Deployment Architecture

```
 Trình duyệt --HTTPS (lưu gzip ≤ 12 MB)--> [nginx] --> [backend: U11] --> [postgres]
                                                          |      |
                                                          |      +--> [redis: rate limit]
                                                          +--job/event--> [rabbitmq]
                                                                             |
                                        [worker: tự nộp, nghe u08.assignment.retired]
                                                                             |
                                             event u11.submission.submitted --> U15, U16
```

**Text alternative**: Người học lưu và nộp bài qua Nginx (lưu tối đa 12 MB, nén gzip) tới module U11 trong backend; U11 ghi PostgreSQL, giới hạn tần suất lưu bằng Redis, tạo job tự nộp và phát event qua RabbitMQ. Worker chạy job tự nộp, nghe event ngừng giao của U08, và event bài đã nộp được gửi tới U15, U16.

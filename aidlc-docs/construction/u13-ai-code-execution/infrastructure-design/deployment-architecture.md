# U13 AI & Code Execution - Deployment Architecture

```
                 mạng edge / internal                         mạng sandbox (internal: true)
 [nginx] --> [backend: U13] ----------------------------+---> [judge0-server] --> [judge0-workers]
                 |                                      |          |                 (isolate,
                 +--job--> [rabbitmq] --> [worker: U13] +          +--> [judge0-db]   privileged)
                 |                            |                    +--> [judge0-redis]
                 v                            v
          [postgres] [redis]         generativelanguage.googleapis.com (Gemini, Internet)
```

**Text alternative**: Backend và worker của U13 nằm trong mạng `internal` như các unit khác và nối thêm mạng `sandbox` để gọi `judge0-server`. Mạng `sandbox` không ra được Internet; trong đó có `judge0-server`, `judge0-workers` (chạy mã trong isolate, cần privileged), `judge0-db`, `judge0-redis`. Backend tạo job qua RabbitMQ; worker gọi Gemini qua Internet và gọi Judge0 qua mạng `sandbox`; dữ liệu U13 nằm trong PostgreSQL, bộ đếm trong Redis.

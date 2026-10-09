# U13 AI & Code Execution - Deployment Architecture

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding từ màn xem học liệu (UC 15, 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
     network edge / internal                     network sandbox (internal: true)

 +-------+     +--------------+                  +---------------+     +----------------+
 | nginx | --> | backend: U13 | ---------------> | judge0-server | --> | judge0-workers |
 +-------+     +--------------+                  +---------------+     | isolate,       |
                  |        |                       ^    |      |       | privileged     |
                  | job    | SQL                   |    v      v       +----------------+
                  v        v                       |  +-----------+  +--------------+
            +----------+  +-----------------+      |  | judge0-db |  | judge0-redis |
            | rabbitmq |  | postgres, redis |      |  +-----------+  +--------------+
            +----------+  +-----------------+      |
                  |              ^                 |
                  v              | SQL             |
            +-------------+      |                 |
            | worker: U13 | -----+-----------------+
            +-------------+
                  |
                  v
            +-----------------------------------+
            | Gemini API (Internet, HTTPS 443)  |
            +-----------------------------------+
```

**Text alternative**: Backend và worker của U13 nằm trong mạng `internal` như các unit khác và nối thêm mạng `sandbox` để gọi `judge0-server`. Mạng `sandbox` không ra được Internet; trong đó có `judge0-server`, `judge0-workers` (chạy mã trong isolate, cần privileged), `judge0-db`, `judge0-redis`. Backend tạo job qua RabbitMQ; worker gọi Gemini qua Internet và gọi Judge0 qua mạng `sandbox`. Dữ liệu U13 (`ai_suggestions`, `code_runs`) và cài đặt AI (`system_settings` của U03) nằm trong PostgreSQL; bộ đếm chi phí và tần suất trong Redis. Không có container mới ngoài 4 container Judge0.

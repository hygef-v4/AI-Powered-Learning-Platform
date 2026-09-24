# U02 Audit, Job & Outbox - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Messaging | Spring AMQP với RabbitMQ | Đã chốt RabbitMQ; Spring AMQP có ack thủ công, prefetch, tự kết nối lại |
| Bảng `jobs`, `audit_events` | PostgreSQL + Spring Data JPA | Đã chốt; `claim` dùng `UPDATE ... WHERE status = 'PENDING'` để chỉ một worker thắng |
| Lượt quét | `@Scheduled` trong worker | Chỉ một worker, không cần khóa phân tán |
| Worker | Cùng project Maven backend, profile `worker` | Không phải duy trì hai codebase |
| Lưu JSON audit | Cột `jsonb` | Đã chốt trong ERD |
| Test | JUnit 5, Testcontainers (PostgreSQL, RabbitMQ) | Theo NFR-004 |

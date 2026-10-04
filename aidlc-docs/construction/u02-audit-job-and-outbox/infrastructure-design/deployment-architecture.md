# U02 Audit - Deployment Architecture

```
 mạng internal
 +--------------------------------------------------+
 |  [backend] --INSERT audit_logs (cùng transaction)--> [postgres] |
 |  [worker]  --INSERT audit_logs (handler)----------> [postgres] |
 +--------------------------------------------------+
```

**Text alternative**: Backend và worker ghi `audit_logs` vào PostgreSQL trong transaction nghiệp vụ; audit không đi qua RabbitMQ nên RabbitMQ lỗi không ảnh hưởng audit. Tra cứu audit chạy ở backend.
